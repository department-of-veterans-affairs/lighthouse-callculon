package gov.va.api.lighthouse.callculon;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  public enum RequestMethod {
    GET
  }

  public enum Protocol {
    HTTP,
    HTTPS
  }

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
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Notification {
    private Slack slack;
  }

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
