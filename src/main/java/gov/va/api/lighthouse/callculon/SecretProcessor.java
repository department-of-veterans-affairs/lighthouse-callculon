package gov.va.api.lighthouse.callculon;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Responsible for processing a value in the configuration and performing substitution on it or
 * otherwise modifying. The result of applying this function will be used a the new value.
 */
public interface SecretProcessor extends Function<String, String> {

  /**
   * Based on a configuration value, this apply will parse out secret tokens, collect them and
   * invoke the implementations {@link #lookup(List)} method and perform substitution.
   *
   * <p>The secret token is based on the implementations {@link #identifier()}. It is
   *
   * <ul>
   *   <li>the ${identifier}
   *   <li>an open parenthesis
   *   <li>any number of whitespaces
   *   <li>one or more non-whitespace characters
   *   <li>any number of whitespaces
   *   <li>a closing parenthesis
   * </ul>
   *
   * Examples
   *
   * <pre>
   *   foo(bar-ack-ick)
   *   foo( bar/ack/ick )
   * </pre>
   */
  @Override
  default String apply(String configValue) {
    /* We need to find token on a word break. */
    Pattern pattern =
        Pattern.compile("(^|\\p{Punct}|\\s|\\G)" + identifier() + "\\(\\s*([^\\s]+?)\\s*\\)");
    Scanner scanner = new Scanner(configValue);
    List<MatchResult> matches = scanner.findAll(pattern).collect(toList());
    scanner.close();
    /* If there are no secrets, then dip on out. */
    if (matches.isEmpty()) {
      checkForInvalidSecretSpecifications(configValue, configValue);
      return configValue;
    }
    List<String> leadingSeparators = matches.stream().map(m -> m.group(1)).collect(toList());
    List<String> secrets = matches.stream().map(m -> m.group(2)).collect(toList());
    List<String> values = lookup(secrets);
    if (values.size() != secrets.size()) {
      throw new MissingLookupValue(secrets, secrets.size(), values.size());
    }

    /*
     * We'll do everything backwards to make sure our match indexes aren't invalidated as we
     * manipulate the string value.
     */
    String result = configValue;
    for (int i = matches.size() - 1; i >= 0; i--) {
      MatchResult secretMatch = matches.get(i);
      result =
          result.substring(0, secretMatch.start())
              + leadingSeparators.get(i)
              + values.get(i)
              + result.substring(secretMatch.end());
    }
    /*
     * One final check to make sure we don't have a case where a value had some valid and some
     * invalid secrets.
     */
    checkForInvalidSecretSpecifications(configValue, result);

    return result;
  }

  /**
   * Look for any partially defined secrets in the currentConfigValue. If found, an error with the
   * originalConfigValue will be emitted. The original value is used to protect any secrets that
   * might have been successfully processed.
   */
  private void checkForInvalidSecretSpecifications(
      String originalConfigValue, String currentConfigValue) {
    /*
     * If the secret token is still partially there, then it wasn't specified correctly. For
     * example, 'foo(' or 'foo(bar' are missing closing braces.
     */
    if (currentConfigValue.matches("(^|.*\\p{Punct}|.*\\s)" + identifier() + "\\(.*")) {
      throw new InvalidSecretSpecification(originalConfigValue);
    }
  }

  /**
   * The identifying marker for this secret process. This will be in the configuration value as
   * ${identifier}(token), e.g. aws-secret(/dvp/qa/wow/neat).
   */
  String identifier();

  List<String> lookup(List<String> secrets);

  class InvalidSecretSpecification extends RuntimeException {
    public InvalidSecretSpecification(String value) {
      super(value);
    }
  }

  class MissingLookupValue extends RuntimeException {
    public MissingLookupValue(List<String> secrets, int expected, int got) {
      super("Expected " + expected + ", got " + got + ":" + secrets);
    }
  }
}
