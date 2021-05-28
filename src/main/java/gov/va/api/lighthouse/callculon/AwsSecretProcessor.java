package gov.va.api.lighthouse.callculon;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.Builder;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

/** Look up secrets within an AWS Parameter Store. */
public class AwsSecretProcessor implements SecretProcessor {

  private final Function<GetParametersRequest, GetParametersResponse> ssmInvoker;

  /**
   * The injectable SSM invoker is primarily to allow testing everything _except_ the actual call to
   * AWS parameter store. However, it could be use for some obtuse case where the SSM client needs
   * to be configured special. If one is not specified, a default SSM client will be created and
   * used.
   */
  @Builder
  public AwsSecretProcessor(Function<GetParametersRequest, GetParametersResponse> ssmInvoker) {
    this.ssmInvoker = ssmInvoker == null ? defaultSsmInvoker() : ssmInvoker;
  }

  /** Create a new default instance. */
  public static AwsSecretProcessor defaultInstance() {
    return AwsSecretProcessor.builder().build();
  }

  private static Function<GetParametersRequest, GetParametersResponse> defaultSsmInvoker() {
    SsmClient ssmClient = SsmClient.builder().build();
    return ssmClient::getParameters;
  }

  @Override
  public String identifier() {
    return "aws-secret";
  }

  @Override
  public List<String> lookup(List<String> secrets) {
    GetParametersRequest request =
        GetParametersRequest.builder().names(secrets).withDecryption(true).build();
    GetParametersResponse response = ssmInvoker.apply(request);
    /* Don't trust the order returned, so we need to extract and force order to match. */
    Map<String, String> values =
        response.parameters().stream().collect(toMap(Parameter::name, Parameter::value));
    return secrets.stream().map(values::get).filter(Objects::nonNull).collect(toList());
  }
}
