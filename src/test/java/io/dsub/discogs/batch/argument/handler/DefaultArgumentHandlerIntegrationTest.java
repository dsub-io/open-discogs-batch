package io.dsub.discogs.batch.argument.handler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DefaultArgumentHandlerIntegrationTest extends PostgreSQLIntegrationSupport {

  private final ArgumentHandler handler = new DefaultArgumentHandler();

  @Test
  void shouldHandleMalformedUrlArgumentFlag() throws InvalidArgumentException {
    String[] args = new String[] {"database-url=" + databaseUrl()};
    Assertions.assertDoesNotThrow(() -> handler.resolve(args));
    String[] resolved = handler.resolve(args);
    for (String s : resolved) {
      assertThat(s.startsWith("--")).isTrue();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"--c", "c", "--cleanup", "cleanup"})
  void whenOptionArgGiven__ShouldAddAsOption__RegardlessOfDashPresented(String arg)
      throws InvalidArgumentException {
    String[] args = {"database-url=" + databaseUrl(), arg};
    args = handler.resolve(args);
    assertThat(args).contains("--cleanup");
  }

  @Test
  void shouldNormalizePublicOptionNames() throws InvalidArgumentException {
    String[] args = {
        "database-url=" + databaseUrl(), "entities=artist", "max-workers=3"
    };
    args = handler.resolve(args);
    assertThat(args).contains("--entities=artist", "--maxWorkers=3");
  }

  @Test
  void shouldAcceptStandardSeparatedOptionValues() {
    String[] args = {"--database-url", databaseUrl(), "--entities", "artist"};

    String[] resolved = handler.resolve(args);

    assertThat(resolved).contains("--entities=artist", "--url=" + jdbcUrl);
  }

  @Test
  void shouldResolveThePublicDatabaseUrlEnvironmentVariable() {
    ArgumentHandler environmentHandler =
        new DefaultArgumentHandler(
            Map.of("OPEN_DISCOGS_BATCH_DATABASE_URL", databaseUrl()));

    String[] resolved = environmentHandler.resolve(new String[] {"--entities=artist"});

    assertThat(resolved)
        .contains("--url=" + jdbcUrl, "--username=" + username, "--password=" + password);
  }

  @Test
  void shouldRejectLegacyDatabaseOptions() {
    Assertions.assertThrows(
        InvalidArgumentException.class,
        () ->
            handler.resolve(
                new String[] {"--url=" + jdbcUrl, "--username=test", "--password=test"}));
  }

  @Test
  void shouldRejectNonPositiveMaxWorkers() {
    InvalidArgumentException exception =
        Assertions.assertThrows(
            InvalidArgumentException.class,
            () ->
                handler.resolve(
                    new String[] {"--database-url=" + databaseUrl(), "--max-workers=0"}));

    assertThat(exception.getMessage()).contains("max-workers must be a positive integer");
  }

  private String databaseUrl() {
    return jdbcUrl
        .replaceFirst("^jdbc:", "")
        .replaceFirst("//", "//" + username + ":" + password + "@");
  }
}
