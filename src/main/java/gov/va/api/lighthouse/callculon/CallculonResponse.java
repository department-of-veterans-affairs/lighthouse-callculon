package gov.va.api.lighthouse.callculon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallculonResponse {
  private CallculonConfiguration configuration;
  private int statusCode;
  private String requestTime;
  private String duration;
  private boolean notificationError;
}
