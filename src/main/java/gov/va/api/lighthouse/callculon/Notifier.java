package gov.va.api.lighthouse.callculon;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;

public interface Notifier {
  void onFailure(NotificationContext ctx);

  void onSuccess(NotificationContext ctx);

  @Builder
  @Value
  class NotificationContext {
    CallculonConfiguration config;
    String url;
    int statusCode;
    @Builder.Default Optional<String> note = Optional.empty();
    LambdaLogger logger;
  }

  class NotificationFailure extends RuntimeException {
    public NotificationFailure(String message) {
      super(message);
    }

    public NotificationFailure(Throwable cause) {
      super(cause);
    }
  }
}
