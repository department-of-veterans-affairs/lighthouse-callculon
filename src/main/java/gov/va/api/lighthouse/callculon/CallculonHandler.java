package gov.va.api.lighthouse.callculon;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.time.Instant;

public class CallculonHandler implements RequestHandler<String, CallculonResponse> {

  @Override
  public CallculonResponse handleRequest(String event, Context context) {
    context.getLogger().log("OH MAN GALRIC! " + context.getAwsRequestId());
    context.getLogger().log(event);

    return CallculonResponse.builder()
        .epochTime(Instant.now().toEpochMilli())
        .statusCode(418)
        .build();
  }
}
