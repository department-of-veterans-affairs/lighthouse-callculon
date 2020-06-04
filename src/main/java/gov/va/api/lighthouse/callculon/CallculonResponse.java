package gov.va.api.lighthouse.callculon;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class CallculonResponse {

  int statusCode;

  long epochTime;
}
