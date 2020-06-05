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
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

public class CallculonHandler implements RequestHandler<CallculonConfiguration, CallculonResponse> {

  private final HandlerOptions options;

  private final HttpClient client;

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
    return new StringBuilder()
        .append(config.getName())
        .append(" [")
        .append(config.getDeployment().getId())
        .append("] (")
        .append(config.getDeployment().getProduct())
        .append(' ')
        .append(config.getDeployment().getVersion())
        .append(")")
        .toString();
  }

  @Builder
  @Getter
  @Accessors(fluent = true)
  public static class HandlerOptions {
    @NonNull private final Duration connectTimeout;

    @NonNull private final Duration requestTimeout;

    static Duration duration(String name, String defaultValue) {
      String value = System.getenv(name);
      return Duration.parse(value == null ? defaultValue : value);
    }

    public static HandlerOptions fromEnvironmentVariables() {
      return HandlerOptions.builder()
          .connectTimeout(duration("CALLCULON_CONNECT_TIMEOUT", "PT20S"))
          .requestTimeout(duration("CALLCULON_REQUEST_TIMEOUT", "PT120S"))
          .build();
    }
  }

  public static class InvalidConfiguration extends RuntimeException {

    public InvalidConfiguration(String message) {
      super(message);
    }

    public InvalidConfiguration(String message, Throwable cause) {
      super(message, cause);
    }

    static void check(boolean condition, String message) {
      if (!condition) {
        throw new InvalidConfiguration(message);
      }
    }
  }
}
