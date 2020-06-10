package gov.va.api.lighthouse.callculon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MrGarveyTheSubstituteTest {

  @Test
  void performsSubstitution() {
    String expected =
        "{\n"
            + "    \"student\" : \"a-aron\",\n"
            + "    \"warning\" : \"you done messed up a-aron\"\n"
            + "}";
    String actual =
        MrGarveyTheSubstitute.builder()
            .resource("/mr-garvey.json")
            .substitutions(Map.of("verb", "messed up", "name", "a-aron"))
            .build()
            .rollCall();
    assertThat(actual.trim()).isEqualTo(expected.trim());
  }
}
