package gov.va.api.lighthouse.callculon;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * Mr. Garvey performs simple substitution on a class path resource. Given a resource path, it will
 * read the resource and perform substitutions, replacing any occurrences of `${key}` in the
 * provided map of substitutions.
 */
@Builder
public class MrGarveyTheSubstitute {
  @NonNull private final Map<String, String> substitutions;
  @NonNull private final String resource;

  @SneakyThrows
  String rollCall() {
    try (InputStream stream = getClass().getResourceAsStream(resource)) {
      String template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      for (var entry : substitutions.entrySet()) {
        template = template.replace("${" + entry.getKey() + "}", entry.getValue());
      }
      return template;
    }
  }
}
