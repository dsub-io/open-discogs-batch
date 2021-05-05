package io.dsub.discogsdata.batch.argument.handler;

import io.dsub.discogsdata.batch.argument.formatter.ArgumentFormatter;
import io.dsub.discogsdata.batch.argument.formatter.ArgumentNameFormatter;
import io.dsub.discogsdata.batch.argument.formatter.CompositeArgumentFormatter;
import io.dsub.discogsdata.batch.argument.formatter.JdbcUrlFormatter;
import io.dsub.discogsdata.batch.argument.validator.*;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A default implementation of {@link ArgumentHandler}. */
@Primary
@Component
public class DefaultArgumentHandler implements ArgumentHandler {

  private final ArgumentFormatter argumentFormatter;
  private final ArgumentValidator argumentValidator;
  /**
   * Splits multiple values for OPTIONS into individual entries. It will pass through the
   * NonOptional arguments even if it has several arguments with ',' delimiters.
   */
  private final Function<String, List<String>> splitMultiValues =
      arg -> {
        if (arg.matches("^--.*") && arg.indexOf('=') < arg.length()) {
          String flagHead = arg.substring(0, arg.indexOf("="));
          String valueString = arg.substring(arg.indexOf("=") + 1);
          return List.of(valueString.split(",")).stream()
              .map(value -> String.join("=", flagHead, value))
              .collect(Collectors.toList());
        }
        return List.of(arg);
      };

  /** Default no-arg constructor. */
  public DefaultArgumentHandler() {
    CompositeArgumentValidator validator = new CompositeArgumentValidator();
    validator.addValidator(new DataSourceArgumentValidator());
    validator.addValidator(new KnownArgumentValidator());
    validator.addValidator(new MappedValueValidator());
    CompositeArgumentFormatter formatter = new CompositeArgumentFormatter();
    formatter.addFormatter(new ArgumentNameFormatter());
    formatter.addFormatter(new JdbcUrlFormatter());
    argumentValidator = validator;
    argumentFormatter = formatter;
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

    return arguments.getSourceArgs();
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
            .reduce(
                new ArrayList<>(),
                (l, r) -> {
                  l.addAll(r);
                  return l;
                });
    return new DefaultApplicationArguments(normalizedResult.toArray(new String[0]));
  }
}
