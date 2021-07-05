package io.dsub.discogs.batch.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.argument.validator.DefaultDatabaseConnectionValidator;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import io.dsub.discogs.batch.testutil.LogSpy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

class DefaultDatabaseConnectionValidatorUnitTest {

  final DefaultDatabaseConnectionValidator validator = new DefaultDatabaseConnectionValidator();

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @Test
  void shouldPassIfValidDataSourceValueIsPresent() {
    ApplicationArguments args = getArgs("--url=jdbc:mysql://", "--password", "--username=sa");

    // when
    ValidationResult validationResult = validator.validate(args);

    // then
    assertThat(validationResult.isValid()).isFalse();
  }

  @Test
  void shouldReportIfInvalidValueHasBeenPassed() {
    // when
    ValidationResult result = validator
        .validate(getArgs("--url=hello", "--username=un", "--password=pw"));

    // then
    assertThat(result.getIssues()).contains("failed to allocate driver for url: hello");
  }

  @Test
  void whenArgumentExceptionIfUrlMissing__ShouldReturnValidationResult() {

    // when
    ValidationResult result = validator.validate(getArgs("--password=something", "--username=un"));

    // then
    assertThat(result.isValid()).isFalse();
    assertThat(result.getIssues()).contains("url cannot be null or blank");
  }

  private ApplicationArguments getArgs(String... args) {
    return new DefaultApplicationArguments(args);
  }
}
