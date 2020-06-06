package gov.va.api.lighthouse.callculon;

import static gov.va.api.lighthouse.callculon.CallculonHandler.InvalidConfiguration.check;
import static java.util.Optional.ofNullable;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Protocol;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Request;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

public class CallculonHandler implements RequestHandler<CallculonConfiguration, CallculonResponse> {

  private final HandlerOptions options;

  private final HttpClient client;

  /**
   * Create a new instance from options (or not... whatever) If no options are specified, they will
   * be picked from environment variables, or we'll just assume some defaults if environment
   * variables are not available.
   */
  @Builder
  public CallculonHandler(HandlerOptions maybeOptions) {
    this.options = maybeOptions == null ? HandlerOptions.fromEnvironmentVariables() : maybeOptions;
    this.client =
        HttpClient.newBuilder()
            .followRedirects(Redirect.NEVER)
            .connectTimeout(options.connectTimeout())
            .build();
  }

  @SneakyThrows
  private static URI asUri(Request request) {
    check(request.getHostname() != null, "missing hostname");
    check(request.getPath() != null, "missing path");
    check(request.getPort() > 0, "missing port");
    String protocol =
        ofNullable(request.getProtocol())
            .orElse(Protocol.HTTPS)
            .toString()
            .toLowerCase(Locale.ENGLISH);
    String separator = request.getPath().startsWith("/") ? "" : "/";
    String url =
        protocol
            + "://"
            + request.getHostname()
            + ":"
            + request.getPort()
            + separator
            + request.getPath();
    return new URL(url).toURI();
  }

  private HttpRequest asHttpRequest(Request request) {
    HttpRequest.Builder builder = HttpRequest.newBuilder();
    builder.GET();
    builder.uri(asUri(request));
    if (request.getHeaders() != null) {
      request.getHeaders().forEach(builder::header);
    }
    builder.timeout(options.requestTimeout());
    return builder.build();
  }

  @Override
  @SneakyThrows
  public CallculonResponse handleRequest(CallculonConfiguration config, Context context) {
    LambdaLogger logger = context.getLogger();
    logger.log(titleOf(config));
    HttpRequest request = asHttpRequest(config.getRequest());
    logger.log("Requesting " + request.uri());
    Instant start = Instant.now();
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    Duration requestDuration = Duration.between(start, Instant.now());
    logger.log(
        "Response is " + response.statusCode() + " in " + requestDuration.toMillis() + " millis");
    if (isOk(response)) {
      logger.log("YAY!");
      // TODO send success notification if configured
    } else {
      logger.log("Request failed.");
      // TODO send failure notification
    }

    CallculonResponse result =
        CallculonResponse.builder()
            .statusCode(response.statusCode())
            .requestTime(start.toString())
            .duration(requestDuration.toString())
            .notificationError(true) // TODO populate once sending notifications
            .build();

    logger.log("" + result);
    return result;
  }

  private boolean isOk(HttpResponse<String> response) {
    return response.statusCode() < 200 || response.statusCode() > 299;
  }

  String titleOf(CallculonConfiguration config) {
    return config.getName()
        + " ["
        + config.getDeployment().getId()
        + "] ("
        + config.getDeployment().getProduct()
        + ' '
        + config.getDeployment().getVersion()
        + ")";
  }

  @Builder
  @Getter
  @Accessors(fluent = true)
  public static class HandlerOptions {

    public static final String OPTION_CONNECT_TIMEOUT = "CALLCULON_CONNECT_TIMEOUT";

    public static final String OPTION_REQUEST_TIMEOUT = "CALLCULON_REQUEST_TIMEOUT";

    @NonNull private final Duration connectTimeout;

    @NonNull private final Duration requestTimeout;

    /**
     * Create options from System environment variables.
     *
     * <pre>
     * CALLCULON_CONNECT_TIMEOUT = ISO 8601 Duration (PT20S)
     * CALLCULON_REQUEST_TIMEOUT = ISO 8601 Duration (PT120S)
     * </pre>
     */
    public static HandlerOptions fromEnvironmentVariables() {
      return fromEnvironmentVariables(System.getenv());
    }

    /**
     * Create options from a given environment map.
     *
     * <pre>
     * CALLCULON_CONNECT_TIMEOUT = ISO 8601 Duration (PT20S)
     * CALLCULON_REQUEST_TIMEOUT = ISO 8601 Duration (PT120S)
     * </pre>
     */
    public static HandlerOptions fromEnvironmentVariables(Map<String, String> env) {
      return HandlerOptions.builder()
          .connectTimeout(Duration.parse(env.getOrDefault(OPTION_CONNECT_TIMEOUT, "PT20S")))
          .requestTimeout(Duration.parse(env.getOrDefault(OPTION_REQUEST_TIMEOUT, "PT120S")))
          .build();
    }
  }

  public static class InvalidConfiguration extends RuntimeException {

    public InvalidConfiguration(String message) {
      super(message);
    }

    static void check(boolean condition, String message) {
      if (!condition) {
        throw new InvalidConfiguration(message);
      }
    }
  }
}
