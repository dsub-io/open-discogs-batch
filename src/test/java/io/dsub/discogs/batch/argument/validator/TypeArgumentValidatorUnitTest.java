package io.dsub.discogs.batch.argument.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class TypeArgumentValidatorUnitTest {

  private final TypeArgumentValidator validator = new TypeArgumentValidator();

  @Test
  void malformedEntityValuesAreReported() {
    ValidationResult result =
        validator.validate(
            new DefaultApplicationArguments("--entities=hello", "--entities=world"));

    assertThat(result.getIssues())
        .containsExactlyInAnyOrder("unknown entity value: hello", "unknown entity value: world");
  }

  @Test
  void allSupportedEntityValuesAreAccepted() {
    String[] args =
        List.of("release", "artist", "master", "label").stream()
            .map(value -> "--entities=" + value)
            .toArray(String[]::new);

    assertThat(validator.validate(new DefaultApplicationArguments(args)).isValid()).isTrue();
  }
}
