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

  @Test
  void supportsShortSchemeIpv6DefaultPortAndLiteralPlusCredentials() {
    assertThat(
            expander.expand(
                new String[] {"database_url=postgres://user+name:pass+word@[::1]/discogs"}))
        .containsExactly(
            "--url=jdbc:postgresql://[::1]/discogs",
            "--username=user+name",
            "--password=pass+word");
  }

  @Test
  void rejectsEveryMalformedDatabaseUrlBoundary() {
    assertThat(expander.expand(new String[] {"--cleanup"})).containsExactly("--cleanup");
    for (String argument :
        new String[] {
          "--database-url",
          "--database-url=",
          "--database-url=relative",
          "--database-url=mysql://user:pass@db/discogs",
          "--database-url=postgresql:///discogs",
          "--database-url=postgresql://user@db/discogs",
          "--database-url=postgresql://user:pass@db/",
          "--database-url=postgresql://user:pass@db/%"
        }) {
      assertThatThrownBy(() -> expander.expand(new String[] {argument}))
          .as(argument)
          .isInstanceOf(InvalidArgumentException.class);
    }
  }
}
