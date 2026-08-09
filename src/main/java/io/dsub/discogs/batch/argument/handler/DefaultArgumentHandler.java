package io.dsub.discogs.batch.argument.handler;

import io.dsub.discogs.batch.argument.formatter.ArgumentFormatter;
import io.dsub.discogs.batch.argument.formatter.ArgumentNameFormatter;
import io.dsub.discogs.batch.argument.formatter.CompositeArgumentFormatter;
import io.dsub.discogs.batch.argument.formatter.FlagRemovingArgumentFormatter;
import io.dsub.discogs.batch.argument.validator.ArgumentValidator;
import io.dsub.discogs.batch.argument.validator.CompositeArgumentValidator;
import io.dsub.discogs.batch.argument.validator.DataSourceArgumentValidator;
import io.dsub.discogs.batch.argument.validator.DefaultDatabaseConnectionValidator;
import io.dsub.discogs.batch.argument.validator.KnownArgumentValidator;
import io.dsub.discogs.batch.argument.validator.MappedValueValidator;
import io.dsub.discogs.batch.argument.validator.PositiveIntegerArgumentValidator;
import io.dsub.discogs.batch.argument.validator.TypeArgumentValidator;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import io.dsub.discogs.batch.argument.validator.YearMonthValidator;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * A default implementation of {@link ArgumentHandler}.
 */
@Slf4j
public class DefaultArgumentHandler implements ArgumentHandler {

  private final ArgumentFormatter argumentFormatter;
  private final ArgumentValidator argumentValidator;
  private final EnvironmentArgumentProvider environmentArgumentProvider;
  private final DatabaseUrlArgumentExpander databaseUrlArgumentExpander;
  private final SeparatedArgumentCoalescer separatedArgumentCoalescer;
  private final LegacyDatabaseArgumentRejector legacyDatabaseArgumentRejector;
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
    this(System.getenv());
  }

  DefaultArgumentHandler(Map<String, String> environment) {
    CompositeArgumentValidator validator =
        new CompositeArgumentValidator()
            .addValidator(new DataSourceArgumentValidator())
            .addValidator(new DefaultDatabaseConnectionValidator())
            .addValidator(new KnownArgumentValidator())
            .addValidator(new MappedValueValidator())
            .addValidator(new PositiveIntegerArgumentValidator())
            .addValidator(new TypeArgumentValidator())
            .addValidator(new YearMonthValidator());
    CompositeArgumentFormatter formatter =
        new CompositeArgumentFormatter()
            .addFormatter(new FlagRemovingArgumentFormatter())
            .addFormatter(new ArgumentNameFormatter());
    this.argumentValidator = validator;
    this.argumentFormatter = formatter;
    this.environmentArgumentProvider = new EnvironmentArgumentProvider(environment);
    this.databaseUrlArgumentExpander = new DatabaseUrlArgumentExpander();
    this.separatedArgumentCoalescer = new SeparatedArgumentCoalescer();
    this.legacyDatabaseArgumentRejector = new LegacyDatabaseArgumentRejector();
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
    legacyDatabaseArgumentRejector.validate(args);
    String[] coalesced = separatedArgumentCoalescer.coalesce(args);
    String[] withEnvironment = environmentArgumentProvider.apply(coalesced);
    String[] expanded = databaseUrlArgumentExpander.expand(withEnvironment);
    ApplicationArguments arguments = normalizeArguments(new DefaultApplicationArguments(expanded));
    ValidationResult validationResult = this.argumentValidator.validate(arguments);

    if (!validationResult.isValid()) {
      throw new InvalidArgumentException(String.join(",", validationResult.getIssues()));
    }

    return addFlags(arguments.getSourceArgs());
  }

  public String[] addFlags(String[] args) {
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

  /**
   * Generates new instance of {@link ApplicationArguments} with normalized key and value.
   *
   * @param args to be normalized.
   * @return normalized instance.
   */
  public ApplicationArguments normalizeArguments(ApplicationArguments args) {

    String[] formattedArgs = argumentFormatter.format(args.getSourceArgs());

    List<String> normalizedResult =
        Arrays.stream(formattedArgs)
            .map(splitMultiValues)
            .reduce(
                new ArrayList<>(),
                (l, r) -> {
                  l.addAll(r);
                  return l;
                })
            .stream()
            .map(arg -> "--" + arg)
            .collect(Collectors.toList());
    return new DefaultApplicationArguments(normalizedResult.toArray(String[]::new));
  }
}
