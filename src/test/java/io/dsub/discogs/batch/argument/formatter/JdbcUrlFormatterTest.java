package io.dsub.discogs.batch.argument.formatter;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class JdbcUrlFormatterTest {

  JdbcUrlFormatter formatter = new JdbcUrlFormatter();

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @Test
  void whenSchemaOrDatabaseMissing__ShouldAddDefault() {
    String[] args = new String[]{"url=jdbc:postgresql://localhost:3306?serverTimeZone=UTC",
        "user=root", "pass=gozldwmf77", "m"};

    String[] formatted = formatter.format(args);

    Optional<String> urlArg = Arrays.stream(formatted)
        .filter(arg -> arg.startsWith("url"))
        .map(arg -> arg.replace("url=", ""))
        .findFirst();

    assertThat(urlArg).isPresent();

    String arg = urlArg.get();

    assertThat(arg).matches(".*discogs.*");

    assertThat(logSpy.getLogsByExactLevelAsString(Level.INFO, true))
        .hasSize(1)
        .anyMatch(s -> s.matches("^default database or schema missing.*"));
  }

  @Test
  void whenArrayIsNull__ShouldReturnNull() {

    // when
    String[] formatted = formatter.format(null);

    // then
    assertThat(formatted).isNull();
  }

  @Test
  void whenArrayIsEmpty__ShouldReturnEmptyArray() {
    String[] args = new String[0];

    // when
    String[] formatted = formatter.format(args);

    // then
    assertThat(formatted)
        .isNotNull()
        .isEmpty();
  }

  @Test
  void whenUrlIsEmpty__ShouldReturnNull() {

  }
}
