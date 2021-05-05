package io.dsub.discogsdata.batch;

import io.dsub.discogsdata.batch.argument.ArgType;
import io.dsub.discogsdata.batch.argument.handler.ArgumentHandler;
import io.dsub.discogsdata.batch.argument.handler.DefaultArgumentHandler;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultArgumentHandlerTest {

    private final ArgumentHandler handler = new DefaultArgumentHandler();

    @Test
    void shouldThrowIfRequiredArgumentsAreNotFulfilled() {
        String[] args = new String[]{"hello", "world"};
        assertThrows(InvalidArgumentException.class, () -> handler.resolve(args));
        try {
            handler.resolve(args);
        } catch (InvalidArgumentException e) {
            List<String> issues = Arrays.stream(e.getMessage().split(","))
                    .map(s -> s.replaceAll("[{}\\[\\]]", "").trim())
                    .collect(Collectors.toList());
            assertThat("username argument is missing").isIn(issues);
            assertThat("password argument is missing").isIn(issues);
            assertThat("url argument is missing").isIn(issues);
        }
    }

    @Test
    void shouldThrowIfUrlValueIsEmptyOrOnlyContainingEqualsSign() {
        String[] first = new String[]{"--url=", "user=world", "pass=333"};
        assertThrows(InvalidArgumentException.class, () -> handler.resolve(first));

        String[] second = new String[]{"--url", "user=world", "pass=333"};
        assertThrows(InvalidArgumentException.class, () -> handler.resolve(second));
    }

    @Test
    void shouldHandleMalformedUrlArgumentFlag() {
        String[] first = new String[]{"--url=something:3306/data", "user=world", "pass=333"};
        assertDoesNotThrow(() -> handler.resolve(first));
        String[] resolved = handler.resolve(first);
        for (String s : resolved) {
            if (s.matches(".*url.*")) {
                assertThat(s.startsWith("--")).isFalse();
            }
        }

        String[] second = new String[]{"-url=something:3306/data", "user=world", "pass=333"};
        assertDoesNotThrow(() -> handler.resolve(second));
        resolved = handler.resolve(second);
        for (String s : resolved) {
            if (s.matches(".*url.*")) {
                assertThat(s.startsWith("--")).isFalse();
            }
        }
    }

    @Test
    void shouldAddDefaultPortFromUrlValue() {
        String[] properArgs = {"url=something/data", "user=world", "pass=33201"};
        assertDoesNotThrow(() -> handler.resolve(properArgs));
        String[] resolved = handler.resolve(properArgs);
        for (String s : resolved) {
            if (!s.startsWith("url")) {
                continue;
            }
            assertThat(s).contains(":3306");
        }
    }

    @Test
    void shouldAppendOptionFlags() {
        String[] args = {"url=something/data", "-user=world", "pass=33201"};
        args = handler.resolve(args);
        for (String arg : args) {
            ArgType argType = ArgType.getTypeOf(arg.substring(arg.indexOf('.') + 1, arg.indexOf('=')));
            assertThat(argType).isNotNull();
            if (argType.isRequired()) {
                assertThat(arg.matches("^[\\w].*")).isTrue();
            } else {
                assertThat(arg.matches("^--.*")).isTrue();
            }
        }

        args = new String[]{"--url=something/data", "-user=world", "pass=33201"};
        args = handler.resolve(args);
        for (String arg : args) {
            ArgType argType = ArgType.getTypeOf(arg.substring(arg.indexOf('.') + 1, arg.indexOf('=')));
            assertThat(argType).isNotNull();
            if (argType.isRequired()) {
                assertThat(arg.matches("^[\\w].*")).isTrue();
            } else {
                assertThat(arg.matches("^--.*")).isTrue();
            }
        }
    }

    @Test
    void shouldNotReportWhenUrlValueIsSingleChar() {
        String[] args = {"url=s", "user=world", "pass=33201"};
        assertDoesNotThrow(() -> handler.resolve(args));
    }

    @Test
    void shouldAppendDefaultSchemaName() {
        String[] args = {"url=localhost:3306", "user=world", "pass=33201"};
        args = handler.resolve(args);
        for (String arg : args) {
            String[] parts = arg.split("=");
            if (parts.length < 2 || !parts[0].contains("url")) {
                continue;
            }
            String urlString = parts[1];
            assertThat(urlString).contains("/discogs_data");
        }
    }

    @Test
    void shouldReplacePlurals() {
        String[] args = {"urls=something/data", "users=world", "pass=33201", "etags=hello"};
        args = handler.resolve(args);
        for (String arg : args) {
            String head = arg.split("=")[0];
            if (head.contains("pass")) {
                continue;
            }
            assertThat(arg.split("=")[0].matches(".*s$")).isFalse();
        }
    }

    @Test
    void shouldReturnValidUrlEntryAsIs() {
        String[] args = {"url=jdbc:mysql://something:3306/data", "user=world", "pass=33201"};
        for (String arg : handler.resolve(args)) {
            if (arg.matches("^url.*")) {
                assertThat(arg.matches("^url=jdbc:mysql://something:3306/data$")).isTrue();
            }
        }
    }

    @Test
    void shouldAddDefaultJdbcMysqlHeaderFromUrlValue() {
        String[] args = {"url=something/data", "user=world", "pass=33201"};
        args = handler.resolve(args);
        for (String arg : args) {
            if (!arg.contains("url")) {
                continue;
            }
            assertThat(arg).matches("^url=jdbc:mysql://something:3306/data$");
        }

        args = new String[]{"url=mysql://something/data", "user=world", "pass=33201"};
        for (String arg : handler.resolve(args)) {
            if (arg.contains("url")) {
                assertThat(arg).matches("^url=jdbc:mysql://something:3306/data$");
            }
        }

        args = new String[]{"url=jdbc:something/data", "user=world", "pass=33201"};
        for (String arg : handler.resolve(args)) {
            if (arg.contains("url")) {
                assertThat(arg).matches("^url=jdbc:mysql://something:3306/data$");
            }
        }
    }
}