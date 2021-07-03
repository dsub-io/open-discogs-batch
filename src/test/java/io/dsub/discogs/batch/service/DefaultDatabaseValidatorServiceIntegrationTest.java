package io.dsub.discogs.batch.service;

import io.dsub.discogs.batch.container.PostgreSQLContainerBaseTest;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class DefaultDatabaseValidatorServiceIntegrationTest {

    static final DatabaseValidatorService service = new DefaultDatabaseValidatorService();

    @Nested
    class PostgreSQLIntegrationTest extends PostgreSQLContainerBaseTest {
        @Test
        void shouldPassIfCredentialsMatch() {
            // when
            ValidationResult result = service.validate(jdbcUrl, username, password);

            // then
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void shouldNotPassIfCredentialsNotMatch() {
            // when
            ValidationResult result = service.validate(jdbcUrl, "hello", password);

            // then
            assertThat(result.isValid()).isFalse();
        }
	}
}