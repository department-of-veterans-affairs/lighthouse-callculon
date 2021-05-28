package gov.va.api.lighthouse.callculon;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;

/** Defines an interface for sending notifications from Callculon. */
public interface Notifier {
  void onFailure(NotificationContext ctx);

  void onSuccess(NotificationContext ctx);

  /** Defines the basic parts required to send a notification. */
  @Builder
  @Value
  class NotificationContext {
    SecretProcessor secretProcessor;
    CallculonConfiguration config;
    String url;
    int statusCode;
    @Builder.Default Optional<String> note = Optional.empty();
    LambdaLogger logger;
  }

  /** Defines exceptions that can be thrown on a notification failure. */
  class NotificationFailure extends RuntimeException {
    public NotificationFailure(String message) {
      super(message);
    }

    public NotificationFailure(Throwable cause) {
      super(cause);
    }
  }
}
