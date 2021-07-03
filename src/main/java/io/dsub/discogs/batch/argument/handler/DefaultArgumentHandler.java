package io.dsub.discogs.batch.argument.handler;

import io.dsub.discogs.batch.argument.DBType;
import io.dsub.discogs.batch.argument.formatter.ArgumentFormatter;
import io.dsub.discogs.batch.argument.formatter.ArgumentNameFormatter;
import io.dsub.discogs.batch.argument.formatter.CompositeArgumentFormatter;
import io.dsub.discogs.batch.argument.formatter.JdbcUrlFormatter;
import io.dsub.discogs.batch.argument.validator.*;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A default implementation of {@link ArgumentHandler}.
 */
public class DefaultArgumentHandler implements ArgumentHandler {

    private final ArgumentFormatter argumentFormatter;
    private final ArgumentValidator argumentValidator;
    /**
     * Splits multiple values for OPTIONS into individual entries. It will pass through the
     * NonOptional arguments even if it has several arguments with ',' delimiters.
     */
    private final Function<String, List<String>> splitMultiValues =
            arg -> {
                if (arg.matches("^--.*") && arg.indexOf('=') < arg.length() && arg.indexOf('=') > 0) {
                    String flagHead = arg.substring(0, arg.indexOf("="));
                    String valueString = arg.substring(arg.indexOf("=") + 1);
                    return List.of(valueString.split(",")).stream()
                            .map(value -> String.join("=", flagHead, value))
                            .collect(Collectors.toList());
                }
                return List.of(arg);
            };

    /**
     * Default no-arg constructor.
     */
    public DefaultArgumentHandler() {
        CompositeArgumentValidator validator =
                new CompositeArgumentValidator()
                        .addValidator(new DataSourceArgumentValidator())
                        .addValidator(new KnownArgumentValidator())
                        .addValidator(new MappedValueValidator())
                        .addValidator(new TypeArgumentValidator())
                        .addValidator(new YearMonthValidator());
        CompositeArgumentFormatter formatter =
                new CompositeArgumentFormatter()
                        .addFormatter(new ArgumentNameFormatter())
                        .addFormatter(new JdbcUrlFormatter());
        this.argumentValidator = validator;
        this.argumentFormatter = formatter;
    }

    /**
     * Resolves url formatting if the entry exists. Then passes everything into a validator to perform
     * actual validation.
     *
     * @param args given arguments.
     * @return resolved arguments.
     * @throws InvalidArgumentException if any issue exists on validation result.
     */
    @Override
    public String[] resolve(String[] args) throws InvalidArgumentException {

        ApplicationArguments arguments = normalizeArguments(new DefaultApplicationArguments(args));
        ValidationResult validationResult = this.argumentValidator.validate(arguments);

        if (!validationResult.isValid()) {
            String reasons = String.join(",", "{" + validationResult.getIssues() + "}");
            throw new InvalidArgumentException(reasons);
        }

        String[] finalized = getArgumentsWithDriverClassName(arguments);
        finalized =  addRequiredFlags(finalized);
        return finalized;
    }

    public String[] addRequiredFlags(String[] args) {
        String[] normalized = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.matches("(^--).*")) {
                normalized[i] = "--" + arg;
                continue;
            }
            normalized[i] = arg;
        }
        return normalized;
    }

    public String[] getArgumentsWithDriverClassName(ApplicationArguments arguments) {
        List<String> args = new ArrayList<>(List.of(arguments.getSourceArgs()));
        String driverClassName = null;
        loop:
        for (String arg : args) {
            if (!arg.startsWith("url=")) {
                continue;
            }
            for (DBType type : DBType.values()) {
                if (arg.matches(".*" + type.value() + ".*")) {
                    driverClassName = type.getDriverClassName();
                    break loop;
                }
            }
        }
        args.add("driver-class-name=" + driverClassName);
        return args.toArray(String[]::new);
    }

    /**
     * Generates new instance of {@link ApplicationArguments} with normalized key and value.
     *
     * @param args to be normalized.
     * @return normalized instance.
     */
    public ApplicationArguments normalizeArguments(ApplicationArguments args) {
        List<String> normalizedResult =
                Arrays.stream(args.getSourceArgs())
                        .map(argumentFormatter::format)
                        .map(splitMultiValues)
                        .reduce(new ArrayList<>(),
                                (l, r) -> {
                                    l.addAll(r);
                                    return l;
                                });
        return new DefaultApplicationArguments(normalizedResult.toArray(new String[0]));
    }
}
