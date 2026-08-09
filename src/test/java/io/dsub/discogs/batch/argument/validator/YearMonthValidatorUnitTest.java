package io.dsub.discogs.batch.argument.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.DefaultApplicationArguments;

class YearMonthValidatorUnitTest {

  private final YearMonthValidator validator = new YearMonthValidator();

  @Test
  void absentDumpMonthIsValid() {
    assertThat(validator.validate(new DefaultApplicationArguments()).isValid()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-07", "2008-03"})
  void validDumpMonthIsAccepted(String value) {
    ValidationResult result =
        validator.validate(new DefaultApplicationArguments("--dumpMonth=" + value));

    assertThat(result.isValid()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-7", "2026-00", "2026-13", "2008-02", "not-a-month"})
  void malformedOrTooOldDumpMonthIsRejected(String value) {
    ValidationResult result =
        validator.validate(new DefaultApplicationArguments("--dumpMonth=" + value));

    assertThat(result.isValid()).isFalse();
  }

  @Test
  void futureDumpMonthIsRejected() {
    String future = YearMonth.now().plusMonths(1).toString();

    ValidationResult result =
        validator.validate(new DefaultApplicationArguments("--dumpMonth=" + future));

    assertThat(result.isValid()).isFalse();
  }

  @Test
  void duplicateDumpMonthIsRejected() {
    ValidationResult result =
        validator.validate(
            new DefaultApplicationArguments("--dumpMonth=2026-06", "--dumpMonth=2026-07"));

    assertThat(result.isValid()).isFalse();
  }
}
