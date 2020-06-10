package gov.va.api.lighthouse.callculon;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Deployment;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Notification;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Slack;
import gov.va.api.lighthouse.callculon.Notifier.NotificationContext;
import gov.va.api.lighthouse.callculon.Notifier.NotificationFailure;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlackNotifierTest {

  @Mock LambdaLogger logger;
  @Mock Function<HttpRequest, HttpResponse<String>> invoker;
  @Mock HttpResponse<String> response;

  private CallculonConfiguration config(boolean onFailure, boolean onSuccess) {
    return CallculonConfiguration.builder()
        .name("fugazi-manual-test")
        .deployment(
            Deployment.builder()
                .product("fugazi")
                .version("1.0.0")
                .enabled(true)
                .cron("0 0 * * *")
                .id("1-fugazi-1-0-0-oicu812")
                .build())
        .notification(
            Notification.builder()
                .slack(
                    Slack.builder()
                        .channel("shanktovoid")
                        .webhook(shanktovoidWebhook().orElse("https://httpstat.us/200"))
                        .onSuccess(onSuccess)
                        .onFailure(onFailure)
                        .build())
                .build())
        .build();
  }

  private NotificationContext failContext(boolean enabled) {
    return NotificationContext.builder()
        .secretProcessor(noSecrets())
        .url("https://fugazi.com/velocipastor")
        .statusCode(419)
        .note(Optional.of("wow this is the greatest"))
        .logger(logger)
        .config(config(enabled, true))
        .build();
  }

  private SecretProcessor noSecrets() {
    return new SecretProcessor() {
      @Override
      public String identifier() {
        return "noop";
      }

      @Override
      public List<String> lookup(List<String> secrets) {
        return secrets;
      }
    };
  }

  @Test
  void onFailureDoesNotSendMessageWhenDisabled() {
    SlackNotifier.builder().invoker(invoker).build().onFailure(failContext(false));
    verifyNoInteractions(invoker);
  }

  @Test
  void onFailureDoesNotSendMessageWhenThereIsNoSlackConfiguration() {
    var ctx =
        NotificationContext.builder()
            .secretProcessor(noSecrets())
            .url("https://fugazi.com/velocipastor")
            .statusCode(419)
            .note(Optional.of("wow this is the greatest"))
            .logger(logger)
            .config(CallculonConfiguration.builder().build())
            .build();
    SlackNotifier.builder().invoker(invoker).build().onFailure(ctx);
    verifyNoInteractions(invoker);
  }

  @Test
  void onFailureSendsMessageWhenEnabled() {
    when(response.statusCode()).thenReturn(200);
    when(invoker.apply(any(HttpRequest.class))).thenReturn(response);
    SlackNotifier.builder().invoker(invoker).build().onFailure(failContext(true));
    verify(invoker).apply(any(HttpRequest.class));
  }

  @Test
  void onSuccessDoesNotSendMessageWhenDisabled() {
    SlackNotifier.builder().invoker(invoker).build().onSuccess(successContext(false));
    verifyNoInteractions(invoker);
  }

  @Test
  void onSuccessDoesNotSendMessageWhenThereIsNoSlackConfiguration() {
    var ctx =
        NotificationContext.builder()
            .secretProcessor(noSecrets())
            .url("https://fugazi.com/velocipastor")
            .statusCode(419)
            .note(Optional.of("wow this is the greatest"))
            .logger(logger)
            .config(CallculonConfiguration.builder().build())
            .build();
    SlackNotifier.builder().invoker(invoker).build().onSuccess(ctx);
    verifyNoInteractions(invoker);
  }

  @Test
  void onSuccessSendsMessageWhenEnabled() {
    when(response.statusCode()).thenReturn(200);
    when(invoker.apply(any(HttpRequest.class))).thenReturn(response);
    SlackNotifier.builder().invoker(invoker).build().onSuccess(successContext(true));
    verify(invoker).apply(any(HttpRequest.class));
  }

  @Test
  @Tag("manual")
  void reallySendNotifications() {
    if (shanktovoidWebhook().isPresent()) {
      SlackNotifier.builder().build().onFailure(failContext(true));
      SlackNotifier.builder().build().onSuccess(successContext(true));
    }
  }

  private Optional<String> shanktovoidWebhook() {
    return Optional.ofNullable(System.getenv("SHANKTOVOID_WEBHOOK"));
  }

  private NotificationContext successContext(boolean enabled) {
    return NotificationContext.builder()
        .secretProcessor(noSecrets())
        .url("https://fugazi.com/velocipastor")
        .statusCode(200)
        .logger(logger)
        .config(config(true, enabled))
        .build();
  }

  @Test
  void unsuccessfulStatusCodeWhileSendingMessageCausesException() {
    when(response.statusCode()).thenReturn(404);
    when(invoker.apply(any(HttpRequest.class))).thenReturn(response);
    assertThatExceptionOfType(NotificationFailure.class)
        .isThrownBy(
            () -> SlackNotifier.builder().invoker(invoker).build().onFailure(failContext(true)));
  }
}
