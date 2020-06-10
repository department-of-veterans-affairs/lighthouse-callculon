package gov.va.api.lighthouse.callculon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MrGarveyTheSubstituteTest {

  @Test
  void performsSubstitution() {
    String expected = "{ \"student\" : \"a-aron\", \"warning\" : \"you done messed up a-aron\" }";
    String actual =
        MrGarveyTheSubstitute.builder()
            .resource("/mr-garvey.json")
            .substitutions(Map.of("verb", "messed up", "name", "a-aron"))
            .build()
            .rollCall();
    assertThat(actual).isEqualToIgnoringWhitespace(expected);
  }
}
