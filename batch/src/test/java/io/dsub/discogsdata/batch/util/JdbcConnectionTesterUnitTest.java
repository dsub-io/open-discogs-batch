package io.dsub.discogsdata.batch.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
class JdbcConnectionTesterUnitTest {

  final JdbcConnectionTester tester = new JdbcConnectionTester();

  @Test
  void shouldPassIfValidDataSourceValueIsPresent() {
    assertDoesNotThrow(
        () ->
            tester.testConnection(
                new String[]{"url=jdbc:h2:~/test", "password", "username=sa"}, "org.h2.Driver"));
  }

  @Test
  void shouldThrowIfInvalidDataSourceValuePassed() {
    assertThatThrownBy(
        () -> tester.testConnection(new String[]{"url=hello", "username=un", "password=pw"}))
        .hasMessage("failed to test connection! no suitable driver found for hello");
  }

  @Test
  void shouldThrowInvalidArgumentExceptionIfAnythingMissing() {
    assertThrows(
        InvalidArgumentException.class,
        () ->
            tester.parseJdbcArgs(new DefaultApplicationArguments("url=something", "username=un")));

    assertThrows(
        InvalidArgumentException.class,
        () ->
            tester.parseJdbcArgs(new DefaultApplicationArguments("url=something", "password=pw")));

    assertThrows(
        InvalidArgumentException.class,
        () -> tester.parseJdbcArgs(new DefaultApplicationArguments("username=un", "password=pw")));

    assertThrows(
        InvalidArgumentException.class,
        () -> tester.parseJdbcArgs(new DefaultApplicationArguments("username=un")));

    assertThrows(
        InvalidArgumentException.class,
        () -> tester.parseJdbcArgs(new DefaultApplicationArguments("password=pw")));

    assertThrows(
        InvalidArgumentException.class,
        () -> tester.parseJdbcArgs(new DefaultApplicationArguments("url=something")));
  }

  @Test
  void shouldParseProperly() {
    String[] arguments = {"url=something", "username=un", "password=pw"};
    ApplicationArguments args = new DefaultApplicationArguments(arguments);
    Map<String, String> parsedArgs = tester.parseJdbcArgs(args);
    Set<String> keySet = parsedArgs.keySet();
    assertThat("url").isIn(keySet);
    assertThat("username").isIn(keySet);
    assertThat("password").isIn(keySet);
    assertThat(parsedArgs.get("url")).isEqualTo("something");
    assertThat(parsedArgs.get("username")).isEqualTo("un");
    assertThat(parsedArgs.get("password")).isEqualTo("pw");
  }
}
