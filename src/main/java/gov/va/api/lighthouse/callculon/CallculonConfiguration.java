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

  String name;
  Deployment deployment;
  Request request;
  Notification notification;

  public enum RequestMethod {
    GET
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Deployment {
    boolean enabled;
    String cron;
    String product;
    String version;
    String id;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Notification {
    Slack slack;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Request {
    String hostname;
    int port;
    String path;
    RequestMethod method;
    Map<String, String> headers;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Slack {
    String webhook;
    String channel;
    @Builder.Default boolean onFailure = true;
    @Builder.Default boolean onSuccess = false;
  }
}
