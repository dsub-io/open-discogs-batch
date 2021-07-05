package io.dsub.discogs.batch.argument.handler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.dsub.discogs.batch.container.PostgreSQLContainerBaseTest;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DefaultArgumentHandlerIntegrationTest extends PostgreSQLContainerBaseTest {

  private final ArgumentHandler handler = new DefaultArgumentHandler();

  @Test
  void shouldHandleMalformedUrlArgumentFlag() throws InvalidArgumentException {
    String[] args = new String[]{"url=" + jdbcUrl, "user=" + username, "pass=" + password};
    Assertions.assertDoesNotThrow(() -> handler.resolve(args));
    String[] resolved = handler.resolve(args);
    for (String s : resolved) {
      assertThat(s.startsWith("--")).isTrue();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"--m", "m", "--mount", "mount"})
  void whenOptionArgGiven__ShouldAddAsOption__RegardlessOfDashPresented(String arg)
      throws InvalidArgumentException {
    String[] args = {"url=" + jdbcUrl, "user=" + username, "pass=" + password, arg};
    args = handler.resolve(args);
    assertThat(args).contains("--mount");
  }

  @Test
  void shouldReplacePlurals() throws InvalidArgumentException {
    String[] args = {"urls=" + jdbcUrl, "user=" + username, "pass=" + password, "etags=hello"};
    args = handler.resolve(args);
    for (String arg : args) {
      String head = arg.split("=")[0];
      if (head.contains("pass")) {
        continue;
      }
      assertThat(arg.split("=")[0].matches(".*s$")).isFalse();
    }
  }
}
