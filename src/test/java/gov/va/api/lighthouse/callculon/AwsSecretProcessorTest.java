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

  static {
    configureAwsForTest();
  }

  /**
   * This is just used to calm down the AWS internals. We don't actually use them during testing,
   * but construction of the configuration chain will fail if one of these is not set.
   */
  public static void configureAwsForTest() {
    if (System.getProperty("aws.region") == null && System.getenv("AWS_REGION") == null) {
      System.setProperty("aws.region", "us-gov-west-1");
    }
  }

  static Function<GetParametersRequest, GetParametersResponse> mockParameters(
      Map<String, String> secrets) {
    return request ->
        GetParametersResponse.builder()
            .parameters(
                secrets.entrySet().stream()
                    .map(e -> Parameter.builder().name(e.getKey()).value(e.getValue()).build())
                    .collect(Collectors.toList()))
            .build();
  }

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

  @Test
  void defaultInstanceReturnsReadyToUseProcessor() {
    assertThat(AwsSecretProcessor.defaultInstance()).isNotNull();
  }
}
