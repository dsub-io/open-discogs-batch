package io.dsub.discogs.batch.service;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import io.dsub.discogs.batch.testutil.LogSpy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
class DefaultDatabaseValidatorServiceUnitTest {

    final DefaultDatabaseValidatorService service = new DefaultDatabaseValidatorService();

    @RegisterExtension
    LogSpy logSpy = new LogSpy();

    @Test
    void shouldPassIfValidDataSourceValueIsPresent() {
        ApplicationArguments args = getArgs("--url=jdbc:mysql://", "--password", "--username=sa");

        // when
        ValidationResult validationResult = service.validate(args);

        // then
        assertThat(validationResult.isValid()).isFalse();
    }

    @Test
    void shouldThrowIfInvalidDataSourceValuePassed() {

        // when
        ValidationResult result = service.validate(getArgs("--url=hello", "--username=un", "--password=pw"));

        // then
        assertThat(result.getIssues()).contains("failed to allocate driver for url: hello");
        assertThat(logSpy.getLogsByExactLevelAsString(Level.ERROR, true)).contains("failed to recognize database product name from hello");
    }

    @Test
    void whenArgumentExceptionIfUrlMissing__ShouldReturnValidationResult() {

        // when
        ValidationResult result = service.validate(getArgs("--password=something", "--username=un"));

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getIssues()).contains("url cannot be null or blank");
    }

    private ApplicationArguments getArgs(String... args) {
        return new DefaultApplicationArguments(args);
    }
}
