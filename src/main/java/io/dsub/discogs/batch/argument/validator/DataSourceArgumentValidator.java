package io.dsub.discogs.batch.argument.validator;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.datasource.DBType;
import io.dsub.discogs.batch.util.DataSourceUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;

/**
 * A validator to check necessary information to perform initialization of {@link
 * javax.sql.DataSource}. The validator will basically require three arguments: user, pass and url.
 * This validator will check the existence and duplication of such arguments.
 * <p>
 * Results from the first step will be accumulated then returned if any issue exists. Final
 * validation for url will simply validate the format.
 */
public class DataSourceArgumentValidator implements ArgumentValidator {

  private static final Pattern JDBC_PROD_NAME_PATTERN = Pattern.compile("jdbc:(\\w+)://.*");

  public static final List<ArgType> REQUIRED_TYPES =
      List.of(ArgType.URL, ArgType.USERNAME, ArgType.PASSWORD);

  /**
   * Validation for {@link ApplicationArguments} if necessary arguments do exist.
   *
   * @param args to be validated.
   * @return result of the validation.
   */
  @Override
  public ValidationResult validate(ApplicationArguments args) {
    ValidationResult result = new DefaultValidationResult();

    result = checkIfExists(args, result);
    if (!result.isValid()) { // some db connection argument is missing.
      return result; // return as no more verification is meaningful.
    }

    // if duplication found, result will contain issues.
    result = checkDuplicates(args, result);
    if (!result.isValid()) {
      return result;
    }

    return checkIfSupported(args.getOptionValues(ArgType.URL.getGlobalName()).get(0));
  }

  private ValidationResult checkIfSupported(String url) {
    ValidationResult result = new DefaultValidationResult();
    Matcher m = JDBC_PROD_NAME_PATTERN.matcher(url);
    if (m.matches() && m.group(1) != null) {
      if (DBType.getTypeOf(m.group(1)) == null) {
        return result.withIssue("database product \"" + m.group(1) + "\" is not supported");
      }
    }
    return result;
  }

  /**
   * Validation of the existence of the required entries.
   *
   * @param args   to be validated.
   * @param result {@link ValidationResult} to be accumulated.
   * @return accumulated issues during the validation.
   */
  private ValidationResult checkIfExists(ApplicationArguments args, ValidationResult result) {

    // get types that are present in args.
    List<ArgType> foundTypes =
        args.getOptionNames().stream()
            .map(arg -> arg.split("=")[0])
            .map(ArgType::getTypeOf)
            .filter(Objects::nonNull)
            .filter(REQUIRED_TYPES::contains)
            .collect(Collectors.toList());

    // find missing arguments by comparison.
    List<String> missingArgumentIssues =
        REQUIRED_TYPES.stream()
            .filter(requiredType -> !foundTypes.contains(requiredType))
            .map(missingType -> missingType.getGlobalName() + " argument is missing")
            .collect(Collectors.toList());

    // returns the result
    return result.withIssues(missingArgumentIssues);
  }

  /**
   * Validates duplication of required argument types.
   *
   * @param args   to be validated.
   * @param result {@link ValidationResult} to be accumulated.
   * @return accumulated issues during the validation.
   */
  private ValidationResult checkDuplicates(ApplicationArguments args, ValidationResult result) {
    // filtered arguments of required type only.
    List<String> issues = new ArrayList<>();
    for (String optionName : args.getOptionNames()) {
      ArgType type = ArgType.getTypeOf(optionName);
      if (type == null || !REQUIRED_TYPES.contains(type)) {
        continue;
      }

      if (args.getOptionValues(optionName).size() > 1) {
        issues.add(type.getGlobalName() + " argument has duplicated entries");
      }
    }

    return result.withIssues(issues);
  }
}
