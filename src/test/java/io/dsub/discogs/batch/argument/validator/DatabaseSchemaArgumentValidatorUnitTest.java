package io.dsub.discogs.batch.argument.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

class DatabaseSchemaArgumentValidatorUnitTest {

  private final DatabaseSchemaArgumentValidator validator =
      new DatabaseSchemaArgumentValidator();

  @Test
  void absentOrValidSchemaIsAccepted() {
    assertThat(validator.validate(new DefaultApplicationArguments()).isValid()).isTrue();
    assertThat(
            validator
                .validate(new DefaultApplicationArguments("--databaseSchema=open_discogs"))
                .isValid())
        .isTrue();
  }

  @Test
  void malformedOrDuplicateSchemaIsRejected() {
    assertThat(
            validator
                .validate(new DefaultApplicationArguments("--databaseSchema=Open-Discogs"))
                .isValid())
        .isFalse();
    assertThat(
            validator
                .validate(
                    new DefaultApplicationArguments(
                        "--databaseSchema=one", "--databaseSchema=two"))
                .isValid())
        .isFalse();
  }

  @Test
  void optionWithoutValuesIsRejected() {
    ApplicationArguments arguments = mock(ApplicationArguments.class);
    when(arguments.containsOption("databaseSchema")).thenReturn(true);
    when(arguments.getOptionNames()).thenReturn(Set.of("databaseSchema"));
    when(arguments.getOptionValues("databaseSchema")).thenReturn(null);

    assertThat(validator.validate(arguments).getIssues())
        .containsExactly("database-schema must have exactly one value");
  }
}
