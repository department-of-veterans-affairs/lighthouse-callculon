package gov.va.api.lighthouse.callculon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import gov.va.api.lighthouse.callculon.CallculonHandler.HandlerOptions;
import gov.va.api.lighthouse.callculon.CallculonHandler.InvalidConfiguration;
import gov.va.api.lighthouse.callculon.Notifier.NotificationContext;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
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
  @Mock Notifier notifier;

  @AfterEach
  void _stopMockServer() {
    if (server != null) {
      server.stop();
      server.close();
      mockHttp.stop(true);
      mockHttp.close();
    }
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
                .environment("test")
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
                .port(server == null ? 80 : server.getLocalPort())
                .method(RequestMethod.GET)
                .headers(Map.of("taco", "tuesday"))
                .build())
        .build();
  }

  @Test
  @SneakyThrows
  void errorSendingRequestIsMarkedAsFailedRequest() {
    HttpClient client = mock(HttpClient.class);
    when(client.send(any(HttpRequest.class), any(BodyHandler.class)))
        .thenThrow(new IOException("fugazi"));
    when(ctx.getLogger()).thenReturn(logger);

    var explodingHandler =
        CallculonHandler.builder()
            .options(
                CallculonHandler.HandlerOptions.builder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .requestTimeout(Duration.ofSeconds(10))
                    .build())
            .secretProcessor(noSecrets())
            .client(client)
            .notifier(notifier)
            .build();

    CallculonConfiguration event = config("/teapot");
    CallculonResponse response = explodingHandler.handleRequest(event, ctx);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(0);
    assertThat(response.getDuration()).isNotNull();
    assertThat(response.getRequestTime()).isNotNull();
    assertThat(response.isNotificationError()).isFalse();
  }

  private CallculonHandler handler() {
    return CallculonHandler.builder()
        .options(
            CallculonHandler.HandlerOptions.builder()
                .connectTimeout(Duration.ofSeconds(5))
                .requestTimeout(Duration.ofSeconds(10))
                .build())
        .secretProcessor(noSecrets())
        .notifier(notifier)
        .build();
  }

  @Test
  void handlerOptionsFromEnvironmentVariablesUseDefaultValues() {
    var env = Map.of("NOPE", "WHATEVER");
    var opts = HandlerOptions.fromEnvironmentVariables(env);
    assertThat(opts.connectTimeout()).isEqualTo(Duration.ofSeconds(20));
    assertThat(opts.requestTimeout()).isEqualTo(Duration.ofSeconds(120));
  }

  @Test
  void handlerOptionsFromEnvironmentVariablesUsesEnvValues() {
    var env =
        Map.of(
            HandlerOptions.OPTION_CONNECT_TIMEOUT,
            "PT99S",
            HandlerOptions.OPTION_REQUEST_TIMEOUT,
            "PT33S");
    var opts = HandlerOptions.fromEnvironmentVariables(env);
    assertThat(opts.connectTimeout()).isEqualTo(Duration.ofSeconds(99));
    assertThat(opts.requestTimeout()).isEqualTo(Duration.ofSeconds(33));
  }

  @Test
  void missingHostnameConfigurationThrowsExceptions() {
    startMockServer();
    CallculonConfiguration event = config("/whatever");
    event.getRequest().setHostname(null);
    assertThatExceptionOfType(InvalidConfiguration.class)
        .isThrownBy(() -> handler().handleRequest(event, ctx));
  }

  @Test
  void missingPathConfigurationThrowsExceptions() {
    startMockServer();
    CallculonConfiguration event = config("/whatever");
    event.getRequest().setPath(null);
    assertThatExceptionOfType(InvalidConfiguration.class)
        .isThrownBy(() -> handler().handleRequest(event, ctx));
  }

  @Test
  void missingPortConfigurationThrowsExceptions() {
    startMockServer();
    CallculonConfiguration event = config("/whatever");
    event.getRequest().setPort(0);
    assertThatExceptionOfType(InvalidConfiguration.class)
        .isThrownBy(() -> handler().handleRequest(event, ctx));
  }

  private SecretProcessor noSecrets() {
    return new SecretProcessor() {
      @Override
      public String identifier() {
        return "topsecret";
      }

      @Override
      public List<String> lookup(List<String> secrets) {
        return secrets;
      }
    };
  }

  @Test
  void notOkResponse() {
    startMockServer();
    mockHttp
        .when(request().withPath("/teapot"))
        .respond(response().withStatusCode(419).withBody("i'm a teapot."));
    CallculonConfiguration event = config("/teapot");
    CallculonResponse response = handler().handleRequest(event, ctx);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(419);
    assertThat(response.getDuration()).isNotNull();
    assertThat(response.getRequestTime()).isNotNull();
    assertThat(response.isNotificationError()).isFalse();
    verify(notifier).onFailure(any(NotificationContext.class));
    verifyNoMoreInteractions(notifier);
  }

  @Test
  void notificationErrorIsMarkedInResponse() {
    startMockServer();
    mockHttp
        .when(request().withPath("/teapot"))
        .respond(response().withStatusCode(419).withBody("i'm a teapot."));
    doThrow(new RuntimeException("fugazi"))
        .when(notifier)
        .onFailure(any(NotificationContext.class));
    CallculonConfiguration event = config("/teapot");
    CallculonResponse response = handler().handleRequest(event, ctx);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(419);
    assertThat(response.getDuration()).isNotNull();
    assertThat(response.getRequestTime()).isNotNull();
    assertThat(response.isNotificationError()).isTrue();
  }

  @Test
  void okResponse() {
    startMockServer();
    mockHttp
        .when(request().withPath("/ok"))
        .respond(response().withStatusCode(200).withBody("Good job buddy!"));
    CallculonConfiguration event = config("/ok");
    CallculonResponse response = handler().handleRequest(event, ctx);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getDuration()).isNotNull();
    assertThat(response.getRequestTime()).isNotNull();
    assertThat(response.isNotificationError()).isFalse();
    verify(notifier).onSuccess(any(NotificationContext.class));
    verifyNoMoreInteractions(notifier);
  }

  @Test
  void secretSubstitutionIsPerformedOnPathAndHeaders() {
    startMockServer();
    mockHttp
        .when(request().withPath("/wow").withHeader("neato", "a b").withHeader("bandito", "c"))
        .respond(response().withStatusCode(200).withBody("nice"));

    CallculonConfiguration event = config("/topsecret(wow)");
    event
        .getRequest()
        .setHeaders(
            Map.of(
                "neato", "topsecret(a) topsecret(b)",
                "bandito", "topsecret(c)"));

    CallculonResponse response = handler().handleRequest(event, ctx);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  void startMockServer() {
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
}
