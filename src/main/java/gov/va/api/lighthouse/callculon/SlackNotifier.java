package gov.va.api.lighthouse.callculon;

import static java.util.Map.entry;

import gov.va.api.lighthouse.callculon.CallculonConfiguration.Deployment;
import gov.va.api.lighthouse.callculon.CallculonConfiguration.Slack;
import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.SneakyThrows;

public class SlackNotifier implements Notifier {

  private final Function<HttpRequest, HttpResponse<String>> invoker;

  /**
   * Create a new instance, optionally specifying an invoker to process the HTTP request. This
   * exists to make testing easier. If no invoker is specified, a default HTTP client will be
   * created and used.
   */
  @Builder
  public SlackNotifier(Function<HttpRequest, HttpResponse<String>> invoker) {
    this.invoker = invoker == null ? SlackNotifier::defaultInvoker : invoker;
  }

  /** Create a default instance. */
  public static SlackNotifier defaultInstance() {
    return SlackNotifier.builder().build();
  }

  private static HttpResponse<String> defaultInvoker(HttpRequest request) {
    try {
      return HttpClient.newBuilder()
          .sslContext(SecurityContexts.relaxed())
          .build()
          .send(request, BodyHandlers.ofString());
    } catch (InterruptedException | IOException e) {
      throw new NotificationFailure(e);
    }
  }

  /**
   * Replace the standard `*` with something that is not sensitive in slack since it interferes with
   * bold formating.
   */
  private String asterisks(String cron) {
    return cron.replace("*", "\uFF0A");
  }

  private Deployment deployment(NotificationContext ctx) {
    return ctx.getConfig().getDeployment();
  }

  private String emoji() {
    List<String> emojis =
        List.of(
            "100",
            "dagger_knife",
            "grin",
            "guitar",
            "icecream",
            "motor_scooter",
            "pizza",
            "sparkling_heart",
            "sunglasses",
            "taco",
            "the_horns",
            "trumpet",
            "unicorn_face"
            //
            );
    return emojis.get(new SecureRandom().nextInt(emojis.size()));
  }

  private String nice() {
    List<String> messages =
        List.of(
            "I talk a lot, so I've learned to just tune myself out...",
            "Oh, it is on, like a prawn who yawns at dawn.",
            "I'm not superstitious, but I am a little stitious.",
            "You only live once? False. You live every day. You only die once.",
            "Come on guys. Early worm gets the worm.",
            "I wanna do a cartwheel. But real casual like. "
                + "Not enough to make a big deal out of it, "
                + "but I know everyone saw it. One stunning, gorgeous cartwheel.",
            "The only problem is whenever I try to make a taco, I get too excited and crush it.",
            "You guys I'm like really smart now. You don't even know. You could ask me, Kelly "
                + "what's the biggest company in the world? And I'd be like, "
                + "blah blah blah, blah blah blah blah blah blah."
                + "Giving you the exact right answer.",
            "I am Beyonce, always.",
            "I'm fast. To give you a reference point. "
                + "I'm somewhere between a snake and a mongoose. "
                + "And a panther.",
            "And I knew exactly what to do. "
                + "But in a much more real sense, I had no idea what to do.",
            "I just want to lie on the beach and eat hot dogs. That's all I've ever wanted.");
    return "_" + messages.get(new SecureRandom().nextInt(messages.size())) + "_";
  }

  @Override
  public void onFailure(NotificationContext ctx) {
    if (!slack(ctx).isOnFailure()) {
      return;
    }
    String message =
        MrGarveyTheSubstitute.builder()
            .resource("/slack-failure-message-template.json")
            .substitutions(
                Map.of(
                    "environment", deployment(ctx).getEnvironment(),
                    "channel", slack(ctx).getChannel(),
                    "name", ctx.getConfig().getName(),
                    "url", ctx.getUrl(),
                    "statusCode", String.valueOf(ctx.getStatusCode()),
                    "note", ctx.getNote().orElse("HTTP status " + ctx.getStatusCode()),
                    "product", deployment(ctx).getProduct(),
                    "version", deployment(ctx).getVersion(),
                    "cron", asterisks(deployment(ctx).getCron()),
                    "deploymentId", deployment(ctx).getId()))
            .build()
            .rollCall();
    post(ctx, message);
  }

  @Override
  public void onSuccess(NotificationContext ctx) {
    if (!slack(ctx).isOnSuccess()) {
      return;
    }
    String message =
        MrGarveyTheSubstitute.builder()
            .resource("/slack-successful-message-template.json")
            .substitutions(
                Map.ofEntries(
                    entry("environment", deployment(ctx).getEnvironment()),
                    entry("channel", slack(ctx).getChannel()),
                    entry("emoji", emoji()),
                    entry("name", ctx.getConfig().getName()),
                    entry("url", ctx.getUrl()),
                    entry("statusCode", String.valueOf(ctx.getStatusCode())),
                    entry("note", ctx.getNote().orElse(nice())),
                    entry("product", deployment(ctx).getProduct()),
                    entry("version", deployment(ctx).getVersion()),
                    entry("cron", asterisks(deployment(ctx).getCron())),
                    entry("deploymentId", deployment(ctx).getId())))
            .build()
            .rollCall();
    post(ctx, message);
  }

  @SneakyThrows
  private void post(NotificationContext ctx, String message) {
    ctx.getLogger().log("Notifying Slack channel " + slack(ctx).getChannel());
    HttpResponse<String> response =
        invoker.apply(
            HttpRequest.newBuilder()
                .uri(new URL(ctx.getSecretProcessor().apply(slack(ctx).getWebhook())).toURI())
                .POST(BodyPublishers.ofString(message))
                .build());

    if (response.statusCode() != 200) {
      throw new NotificationFailure("Status: " + response.statusCode() + ": " + response.body());
    }
  }

  private Slack slack(NotificationContext ctx) {
    Slack slack = ctx.getConfig().getNotification().getSlack();
    return slack == null ? Slack.builder().onFailure(false).onSuccess(false).build() : slack;
  }
}
