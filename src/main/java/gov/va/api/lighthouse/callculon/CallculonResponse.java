package gov.va.api.lighthouse.callculon;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallculonResponse {

  int statusCode;

  long epochTime;
}
