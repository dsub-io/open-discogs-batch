package io.dsub.discogs.batch.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.argument.validator.DatabaseConnectionValidator;
import io.dsub.discogs.batch.argument.validator.DefaultDatabaseConnectionValidator;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import io.dsub.discogs.batch.container.PostgreSQLContainerBaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Slf4j
public class DefaultDatabaseConnectionValidatorIntegrationTest {

  static final DatabaseConnectionValidator service = new DefaultDatabaseConnectionValidator();

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