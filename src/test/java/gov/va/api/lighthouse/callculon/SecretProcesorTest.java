package gov.va.api.lighthouse.callculon;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import gov.va.api.lighthouse.callculon.SecretProcesor.InvalidSecretSpecification;
import gov.va.api.lighthouse.callculon.SecretProcesor.MissingLookupValue;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class SecretProcesorTest {

  private static final SecretProcesor UPCASE =
      new SecretProcesor() {
        @Override
        public String identifier() {
          return "up";
        }

        @Override
        public List<String> lookup(List<String> secrets) {
          return secrets.stream().map(s -> s.toUpperCase(Locale.ENGLISH)).collect(toList());
        }
      };

  @Test
  void conversion() {
    assertThat(UPCASE.apply("up(awesome)")).isEqualTo("AWESOME");
    assertThat(UPCASE.apply("An up(awesome) up(possum)!")).isEqualTo("An AWESOME POSSUM!");
    assertThat(UPCASE.apply("An.up(awesome)up(possum)!")).isEqualTo("An.AWESOMEPOSSUM!");
    assertThat(UPCASE.apply("An_up(awesome)up(possum)!")).isEqualTo("An_AWESOMEPOSSUM!");
    assertThat(UPCASE.apply("An up(  awesome  ) up( possum)!")).isEqualTo("An AWESOME POSSUM!");
  }

  @Test
  void invalidSpecificationsThrowsExceptions() {
    for (String bad :
        List.of(
            "up(",
            "up()",
            "up(xxx",
            "x up(",
            "x up() x",
            "x up(xxx x",
            "x up(xxx yyy) x",
            "x.up(x",
            "x_up(")) {
      assertThatExceptionOfType(InvalidSecretSpecification.class)
          .describedAs(bad)
          .isThrownBy(() -> UPCASE.apply(bad));
    }
  }

  @Test
  void notEnoughValuesThrowsException() {
    SecretProcesor whoops =
        new SecretProcesor() {
          @Override
          public String identifier() {
            return "whoops";
          }

          @Override
          public List<String> lookup(List<String> secrets) {
            return List.of("just one");
          }
        };
    assertThatExceptionOfType(MissingLookupValue.class)
        .isThrownBy(() -> whoops.apply("whoops(one)  whoops(nope)"));
  }
}
