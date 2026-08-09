package io.dsub.discogs.batch.argument.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

class DatabaseUrlArgumentExpanderUnitTest {

  private final DatabaseUrlArgumentExpander expander = new DatabaseUrlArgumentExpander();

  @Test
  void expandsPostgresqlUriIntoInternalSpringArguments() {
    String[] result =
        expander.expand(
            new String[] {
              "--database-url=postgresql://state303:p%40ss%2Bword@DB.EXAMPLE:5433/discogs?sslmode=require",
              "--cleanup"
            });

    assertThat(result)
        .containsExactly(
            "--url=jdbc:postgresql://db.example:5433/discogs?sslmode=require",
            "--username=state303",
            "--password=p@ss+word",
            "--cleanup");
  }

  @Test
  void rejectsMissingCredentialsOrDatabaseName() {
    assertThatThrownBy(
            () -> expander.expand(new String[] {"--database-url=postgresql://db:5432/discogs"}))
        .isInstanceOf(InvalidArgumentException.class);
    assertThatThrownBy(
            () ->
                expander.expand(new String[] {"--database-url=postgresql://user:pass@db:5432"}))
        .isInstanceOf(InvalidArgumentException.class);
  }
}
