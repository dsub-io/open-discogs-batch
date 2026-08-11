package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

class DatabaseSchemaUnitTest {

  @Test
  void defaultAndCustomSchemasExposeSafeSqlValues() {
    DatabaseSchema defaultSchema = DatabaseSchema.from(new DefaultApplicationArguments());
    assertThat(defaultSchema.isPublic()).isTrue();
    assertThat(defaultSchema.quotedName()).isEqualTo("\"public\"");

    DatabaseSchema custom =
        DatabaseSchema.from(
            new DefaultApplicationArguments("--databaseSchema=open_discogs"));
    assertThat(custom.isPublic()).isFalse();
    assertThat(custom.quotedName()).isEqualTo("\"open_discogs\"");
    assertThat(custom.connectionInitializationSql())
        .isEqualTo("SET search_path TO \"open_discogs\", \"public\"");
  }

  @Test
  void everyInvalidNameBoundaryIsRejected() {
    for (String value :
        new String[] {null, " ", "OpenDiscogs", "open-discogs", "1schema", "a".repeat(64)}) {
      assertThat(DatabaseSchema.isValidName(value)).isFalse();
      assertThatThrownBy(() -> new DatabaseSchema(value))
          .isInstanceOf(InvalidArgumentException.class)
          .hasMessage(DatabaseSchema.INVALID_NAME_MESSAGE);
    }
    assertThat(DatabaseSchema.isValidName("_" + "a".repeat(62))).isTrue();
  }

  @Test
  void malformedOptionCardinalityIsRejected() {
    ApplicationArguments noValues = mock(ApplicationArguments.class);
    when(noValues.containsOption("databaseSchema")).thenReturn(true);
    when(noValues.getOptionValues("databaseSchema")).thenReturn(null);
    assertThatThrownBy(() -> DatabaseSchema.from(noValues))
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessageContaining("exactly one value");

    ApplicationArguments duplicateValues = mock(ApplicationArguments.class);
    when(duplicateValues.containsOption("databaseSchema")).thenReturn(true);
    when(duplicateValues.getOptionValues("databaseSchema")).thenReturn(List.of("one", "two"));
    assertThatThrownBy(() -> DatabaseSchema.from(duplicateValues))
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessageContaining("exactly one value");
  }
}
