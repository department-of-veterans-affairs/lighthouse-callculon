package gov.va.api.lighthouse.callculon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

class AwsSecretProcessorTest {

  @Test
  void canPerformLookups() {
    String result =
        AwsSecretProcessor.builder()
            .ssmInvoker(
                mockParameters(
                    Map.of(
                        "/dvp/fugazi/app/i1",
                        "ay!",
                        "/dvp/fugazi/app/i2",
                        "I",
                        "/dvp/fugazi/app/i3",
                        "eye")))
            .build()
            .apply(
                "aws-secret(/dvp/fugazi/app/i1)"
                    + "aws-secret(/dvp/fugazi/app/i2)"
                    + " lost me "
                    + "aws-secret(/dvp/fugazi/app/i3)"
                    + "!");
    assertThat(result).isEqualTo("ay!I lost me eye!");
  }

  private Function<GetParametersRequest, GetParametersResponse> mockParameters(
      Map<String, String> secrets) {
    return request ->
        GetParametersResponse.builder()
            .parameters(
                secrets.entrySet().stream()
                    .map(e -> Parameter.builder().name(e.getKey()).value(e.getValue()).build())
                    .collect(Collectors.toList()))
            .build();
  }
}
