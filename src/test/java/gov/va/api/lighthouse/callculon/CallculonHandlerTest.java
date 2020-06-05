package gov.va.api.lighthouse.callculon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Deployment;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Notification;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Request;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.RequestMethod;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Slack;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CallculonHandlerTest {

  @Mock Context ctx;
  @Mock LambdaLogger logger;

  @BeforeEach
  void _mockContext() {
    when(ctx.getLogger()).thenReturn(logger);
    when(ctx.getAwsRequestId()).thenReturn("mock-" + ctx.hashCode());
  }

  @Test
  void deleteMe() {
    CallculonConfiguration event =
        CallculonConfiguration.builder()
            .name("test")
            .deployment(
                Deployment.builder()
                    .cron("0 0 * * *")
                    .enabled(true)
                    .id("0-test-0-0-0-000")
                    .product("test")
                    .version("0.0.0")
                    .build())
            .notification(
                Notification.builder()
                    .slack(
                        Slack.builder()
                            .channel("#shanktovoid")
                            .webhook("https://hooks.slack.com/services/NOPE/NOPE")
                            .onFailure(true)
                            .onSuccess(false)
                            .build())
                    .build())
            .request(
                Request.builder()
                    .hostname("localhost")
                    .path("/test")
                    .port(9644)
                    .method(RequestMethod.GET)
                    .headers(Map.of("taco", "tuesday"))
                    .build())
            .build();
    CallculonResponse response = new CallculonHandler().handleRequest(event, ctx);
    assertThat(response).isNotNull();
  }
}
