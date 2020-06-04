package gov.va.api.lighthouse.callculon;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import java.time.Instant;

public class CallculonHandler implements RequestHandler<ScheduledEvent, CallculonResponse> {

  @Override
  public CallculonResponse handleRequest(ScheduledEvent scheduledEvent, Context context) {
    context.getLogger().log("OH MAN GALRIC! " + scheduledEvent + " " + context);
    context.getLogger().log(scheduledEvent.getDetailType());
    context.getLogger().log("" + scheduledEvent.getDetail());

    return CallculonResponse.builder()
        .epochTime(Instant.now().toEpochMilli())
        .statusCode(418)
        .build();
  }
}
