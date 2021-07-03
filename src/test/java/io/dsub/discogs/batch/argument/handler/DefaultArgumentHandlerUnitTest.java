package io.dsub.discogs.batch.argument.handler;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultArgumentHandlerUnitTest {

    private final ArgumentHandler handler = new DefaultArgumentHandler();

    @Test
    void shouldThrowIfRequiredArgumentsAreNotFulfilled() {
        String[] args = new String[]{"hello", "world"};
        assertThrows(InvalidArgumentException.class, () -> handler.resolve(args));
        try {
            handler.resolve(args);
        } catch (InvalidArgumentException e) {
            List<String> issues =
                    Arrays.stream(e.getMessage().split(","))
                            .map(s -> s.replaceAll("[{}\\[\\]]", "").trim())
                            .collect(Collectors.toList());
            assertThat("username argument is missing").isIn(issues);
            assertThat("password argument is missing").isIn(issues);
            assertThat("url argument is missing").isIn(issues);
        }
    }

    @Test
    void shouldHandleMalformedUrlArgumentFlag() throws InvalidArgumentException {
        String[] first = new String[]{"--url=something:3306/data", "user=world", "pass=333"};
        Assertions.assertDoesNotThrow(() -> handler.resolve(first));
        String[] resolved = handler.resolve(first);
        for (String s : resolved) {
            if (s.matches(".*url.*")) {
                assertThat(s.startsWith("--")).isTrue();
            }
        }

        String[] second = new String[]{"-url=something:3306/data", "user=world", "pass=333"};
        Assertions.assertDoesNotThrow(() -> handler.resolve(second));
        resolved = handler.resolve(second);
        for (String s : resolved) {
            if (s.matches(".*url.*")) {
                assertThat(s.startsWith("--")).isTrue();
            }
        }
    }

    @Test
    void shouldAddDefaultPortFromUrlValue() throws InvalidArgumentException {
        String[] properArgs = {"url=something/data", "user=world", "pass=33201"};
        Assertions.assertDoesNotThrow(() -> handler.resolve(properArgs));
        String[] resolved = handler.resolve(properArgs);
        for (String s : resolved) {
            if (!s.startsWith("url")) {
                continue;
            }
            assertThat(s).contains(":3306");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"--m", "m", "--mount", "mount"})
    void whenOptionArgGiven__ShouldAddAsOption__RegardlessOfDashPresented(String arg)
            throws InvalidArgumentException {
        String[] args = {"url=something/data", "-user=world", "pass=33201", arg};
        args = handler.resolve(args);
        assertThat(args).contains("--mount");
    }

    @Test
    void shouldAppendOptionFlags() throws InvalidArgumentException {
        String[] args = {"url=something/data", "-user=world", "pass=33201"};

        args = handler.resolve(args);

        Pattern p = Pattern.compile("[-]?[-]?([\\w_,.-]*)=.*");

        for (String arg : args) {
            Matcher m = p.matcher(arg);
            boolean match = m.matches();
            System.out.println("match? >> " + match + " ? " + arg);
            ArgType argType = ArgType.getTypeOf(m.group(1));
            System.out.println("argType >> " + argType + " FROM " + arg);
            assertThat(argType).isNotNull();
            assertThat(arg.matches("^--.*")).isTrue();
        }

        args = new String[]{"--url=something/data", "-user=world", "pass=33201"};
        args = handler.resolve(args);
        for (String arg : args) {
            ArgType argType = ArgType.getTypeOf(arg.substring(0, arg.indexOf('=') < 0 ? arg.length() - 1 : arg.indexOf('=')));
            assertThat(argType).isNotNull();
            assertThat(arg.matches("^--.*")).isTrue();
        }
    }

    @Test
    void shouldNotReportWhenUrlValueIsSingleChar() {
        String[] args = {"url=s", "user=world", "pass=33201"};
        Assertions.assertDoesNotThrow(() -> handler.resolve(args));
    }

    @Test
    void shouldAppendDefaultSchemaName() throws InvalidArgumentException {
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
    void shouldReplacePlurals() throws InvalidArgumentException {
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
    void shouldReturnValidUrlEntryAsIs() throws InvalidArgumentException {
        String[] args = {"url=jdbc:mysql://something:3306/data", "user=world", "pass=33201"};
        for (String arg : handler.resolve(args)) {
            if (arg.matches("^url.*")) {
                assertThat(arg.matches("^--url=jdbc:mysql://something:3306/data.*")).isTrue();
            }
        }
    }

    @Test
    void shouldAddDefaultJdbcMysqlHeaderFromUrlValue() throws InvalidArgumentException {
        String[] args = {"url=something/data", "user=world", "pass=33201"};
        args = handler.resolve(args);
        for (String arg : args) {
            if (!arg.contains("url")) {
                continue;
            }
            assertThat(arg).matches("^--url=jdbc:mysql://something:3306/data.*");
        }

        args = new String[]{"url=mysql://something/data", "user=world", "pass=33201"};
        for (String arg : handler.resolve(args)) {
            if (arg.contains("url")) {
                assertThat(arg).matches("^--url=jdbc:mysql://something:3306/data.*");
            }
        }

        args = new String[]{"url=jdbc:something/data", "user=world", "pass=33201"};
        for (String arg : handler.resolve(args)) {
            if (arg.contains("url")) {
                assertThat(arg).matches("^--url=jdbc:mysql://something:3306/data.*");
            }
        }
    }
}
