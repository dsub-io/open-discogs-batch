package io.dsub.discogs.batch.argument.validator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class PositiveIntegerArgumentValidatorUnitTest {

  private final PositiveIntegerArgumentValidator validator =
      new PositiveIntegerArgumentValidator();

  @Test
  void positiveChunkSizeAndMaxWorkersAreAccepted() {
    ValidationResult result =
        validator.validate(
            new DefaultApplicationArguments("--chunkSize=5000", "--maxWorkers=4"));

    assertThat(result.isValid()).isTrue();
  }

  @Test
  void zeroAndNegativeValuesAreRejected() {
    ValidationResult result =
        validator.validate(
            new DefaultApplicationArguments("--chunkSize=-1", "--maxWorkers=0"));

    assertThat(result.getIssues())
        .containsExactlyInAnyOrder(
            "chunk-size must be a positive integer",
            "max-workers must be a positive integer");
  }

  @Test
  void valuesLargerThanAnIntegerAreRejected() {
    ValidationResult result =
        validator.validate(new DefaultApplicationArguments("--maxWorkers=2147483648"));

    assertThat(result.getIssues())
        .containsExactly("max-workers must be a positive integer");
  }
}
