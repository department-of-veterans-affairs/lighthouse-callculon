package gov.va.api.lighthouse.callculon;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import java.time.Instant;

public class CallculonHandler implements RequestHandler<ScheduledEvent, CallculonResponse> {

  @Override
  public CallculonResponse handleRequest(ScheduledEvent scheduledEvent, Context context) {
    System.out.println("OH MAN GALRIC! " + scheduledEvent + " " + context);
    return CallculonResponse.builder().timeStamp(Instant.now()).statusCode(418).build();
  }
}
