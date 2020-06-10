package gov.va.api.lighthouse.callculon;

import static gov.va.api.lighthouse.callculon.CallculonHandler.InvalidConfiguration.check;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Protocol;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Request;
import gov.va.api.lighthouse.callculon.Notifier.NotificationContext;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
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

  private final SecretProcessor secretProcessor;

  private final Notifier notifier;

  /** Create a new instance initialing options from environment variables if available. */
  public CallculonHandler() {
    this(null, null, null);
  }

  /**
   * Create a new instance from options (or not... whatever) If no options are specified, they will
   * be picked from environment variables, or we'll just assume some defaults if environment
   * variables are not available.
   */
  @Builder
  public CallculonHandler(
      HandlerOptions options, SecretProcessor secretProcessor, Notifier notifier) {
    this.options = options == null ? HandlerOptions.fromEnvironmentVariables() : options;
    this.secretProcessor =
        secretProcessor == null ? AwsSecretProcessor.defaultInstance() : secretProcessor;
    this.client =
        HttpClient.newBuilder()
            .followRedirects(Redirect.NEVER)
            .connectTimeout(this.options.connectTimeout())
            .build();
    this.notifier = notifier == null ? SlackNotifier.defaultInstance() : notifier;
  }

  private HttpRequest asHttpRequest(Request request) {
    HttpRequest.Builder builder = HttpRequest.newBuilder();
    builder.GET();
    builder.uri(asUri(request));
    if (request.getHeaders() != null) {
      request.getHeaders().forEach((name, value) -> builder.header(name, secret(value)));
    }
    builder.timeout(options.requestTimeout());
    return builder.build();
  }

  @SneakyThrows
  private URI asUri(Request request) {
    check(request.getHostname() != null, "missing hostname");
    check(request.getPath() != null, "missing path");
    check(request.getPort() > 0, "missing port");
    String protocol =
        ofNullable(request.getProtocol())
            .orElse(Protocol.HTTPS)
            .toString()
            .toLowerCase(Locale.ENGLISH);
    String secretPath = secret(request.getPath());
    String separator = secretPath.startsWith("/") ? "" : "/";
    String url =
        protocol + "://" + request.getHostname() + ":" + request.getPort() + separator + secretPath;
    return new URL(url).toURI();
  }

  @Override
  @SneakyThrows
  public CallculonResponse handleRequest(CallculonConfiguration config, Context context) {
    context.getLogger().log(titleOf(config));
    var start = Instant.now();

    var request = asHttpRequest(config.getRequest());
    context.getLogger().log("Requesting " + request.uri());

    var response = client.send(request, BodyHandlers.ofString());
    var notificationContext =
        NotificationContext.builder()
            .config(config)
            .logger(context.getLogger())
            .url(request.uri().toString())
            .statusCode(response.statusCode())
            .build();
    var requestDuration = Duration.between(start, Instant.now());
    context
        .getLogger()
        .log(
            format(
                "Response is %d, call took %d ms",
                notificationContext.getStatusCode(), requestDuration.toMillis()));

    var notificationStatus = sendNotifications(notificationContext);

    CallculonResponse result =
        CallculonResponse.builder()
            .configuration(config)
            .statusCode(notificationContext.getStatusCode())
            .requestTime(start.toString())
            .duration(requestDuration.toString())
            .notificationError(notificationStatus == NotificationStatus.ERROR)
            .build();

    context.getLogger().log(result.toString());
    return result;
  }

  private boolean isOk(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  private String secret(String configValue) {
    return secretProcessor.apply(configValue);
  }

  private NotificationStatus sendNotifications(NotificationContext notificationContext) {
    try {
      if (isOk(notificationContext.getStatusCode())) {
        notifier.onSuccess(notificationContext);
      } else {
        notifier.onFailure(notificationContext);
      }
      return NotificationStatus.OK;
    } catch (Exception e) {
      notificationContext.getLogger().log("Failed to send notification.");
      notificationContext
          .getLogger()
          .log(e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
      return NotificationStatus.ERROR;
    }
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

  enum NotificationStatus {
    OK,
    ERROR
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
