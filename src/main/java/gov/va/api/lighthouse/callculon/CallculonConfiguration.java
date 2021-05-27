package gov.va.api.lighthouse.callculon;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Configure all parts of the Callculon lambda. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallculonConfiguration {

  private String name;
  private Deployment deployment;
  private Request request;
  private Notification notification;

  /** Get notification, creating a default empty value of necessary. */
  public Notification getNotification() {
    if (notification == null) {
      notification = Notification.builder().build();
    }
    return notification;
  }

  /** Request Method. */
  public enum RequestMethod {
    GET
  }

  /** Request protocol. */
  public enum Protocol {
    HTTP,
    HTTPS
  }

  /** Configure the deployed Callculon lambda. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Deployment {
    private boolean enabled;
    private String cron;
    private String product;
    private String version;
    private String id;
    private String environment;
  }

  /** Configure the notification(s) Callculon sends. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Notification {
    private Slack slack;
  }

  /** Configure the request that Callculon will send/test. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Request {
    private Protocol protocol;
    private String hostname;
    private int port;
    private String path;
    private RequestMethod method;
    private Map<String, String> headers;
  }

  /** Configure Slack messaging. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Slack {
    @Builder.Default boolean onFailure = true;
    @Builder.Default boolean onSuccess = false;
    private String webhook;
    private String channel;
  }
}
