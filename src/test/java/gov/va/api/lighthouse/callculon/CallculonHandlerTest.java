package gov.va.api.lighthouse.callculon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Deployment;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Notification;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Protocol;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Request;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.RequestMethod;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Slack;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.mockserver.MockServer;

@ExtendWith(MockitoExtension.class)
class CallculonHandlerTest {

  @Mock Context ctx;
  @Mock LambdaLogger logger;
  MockServer server;
  MockServerClient mockHttp;

  @BeforeEach
  void _mockContext() {
    when(ctx.getLogger()).thenReturn(logger);
    doAnswer(
            invocation -> {
              System.out.println(invocation.getArguments()[0]);
              return null;
            })
        .when(logger)
        .log(Mockito.anyString());
    server = new MockServer();
    mockHttp = new MockServerClient("localhost", server.getLocalPort());
  }

  private CallculonConfiguration config(String path) {
    return CallculonConfiguration.builder()
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
                .protocol(Protocol.HTTP)
                .hostname("localhost")
                .path(path)
                .port(server.getLocalPort())
                .method(RequestMethod.GET)
                .headers(Map.of("taco", "tuesday"))
                .build())
        .build();
  }

  private CallculonHandler handler() {
    return new CallculonHandler(
        CallculonHandler.HandlerOptions.builder()
            .connectTimeout(Duration.ofSeconds(5))
            .requestTimeout(Duration.ofSeconds(10))
            .build());
  }

  @Test
  void notOkResponse() {
    mockHttp
        .when(request().withPath("/teapot"))
        .respond(response().withStatusCode(419).withBody("i'm a teapot."));
    String path = "/teapot";
    CallculonConfiguration event = config(path);
    CallculonResponse response = handler().handleRequest(event, ctx);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(419);
    assertThat(response.getDuration()).isNotNull();
    assertThat(response.getRequestTime()).isNotNull();
  }

  @Test
  void okResponse() {
    mockHttp
        .when(request().withPath("/ok"))
        .respond(response().withStatusCode(200).withBody("Good job buddy!"));
    String path = "/ok";
    CallculonConfiguration event = config(path);
    CallculonResponse response = handler().handleRequest(event, ctx);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getDuration()).isNotNull();
    assertThat(response.getRequestTime()).isNotNull();
  }
}
