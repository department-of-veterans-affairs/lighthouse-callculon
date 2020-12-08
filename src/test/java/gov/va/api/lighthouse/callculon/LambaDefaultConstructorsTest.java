package gov.va.api.lighthouse.callculon;

import org.junit.jupiter.api.Test;

class LambaDefaultConstructorsTest {

  static {
    AwsSecretProcessorTest.configureAwsForTest();
  }

  @Test
  void inputAndOutputModelsHaveDefaultConstructor() {
    /*
     * For Lambda processing to work correctly, the input and output classes must have default
     * constructors.
     */
    new CallculonResponse();
    new CallculonConfiguration.Request();
    new CallculonConfiguration.Deployment();
    new CallculonConfiguration.Notification();
    new CallculonConfiguration.Slack();
    new CallculonHandler();
  }
}
