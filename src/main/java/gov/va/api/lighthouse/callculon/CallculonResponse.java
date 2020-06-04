package gov.va.api.lighthouse.callculon;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallculonResponse {

  int statusCode;

  Instant timeStamp;
}
