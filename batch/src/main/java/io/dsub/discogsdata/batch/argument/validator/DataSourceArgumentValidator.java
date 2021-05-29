package io.dsub.discogsdata.batch.argument.validator;

import static io.dsub.discogsdata.batch.argument.ArgType.PASSWORD;
import static io.dsub.discogsdata.batch.argument.ArgType.URL;
import static io.dsub.discogsdata.batch.argument.ArgType.USERNAME;

import io.dsub.discogsdata.batch.argument.ArgType;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;

/**
 * A validator to check necessary information to perform initialization of {@link
 * javax.sql.DataSource}. The validator will basically require three arguments: user, pass and url.
 * First it will check the existence and duplication of each arguments. Next it will decide if
 * additional validation can be performed. Finally, it will resolve the url format by pattern to
 * check if something is wrong.
 *
 * <p>Results from the first step will be accumulated then returned if any issue exists. Final
 * validation for url will simply validate the format.
 */
public class DataSourceArgumentValidator implements ArgumentValidator {

  public static final String CHARACTERS = "[\\w!@#$%^&*.-]+";
  public static final String QUERY_PART = "[\\w!@#$%^*]+";
  public static final String PORT_RANGE = "[1-9][0-9]{0,4}";
  public static final String REPEATING_QUERY = "(&" + QUERY_PART + "=" + QUERY_PART + ")*)?$";
  public static final List<ArgType> REQUIRED_TYPES = List.of(URL, USERNAME, PASSWORD);
  public static final String JDBC_PREFIX = "^jdbc:";
  public static final String JDBC_VALUES_PATTERN =
      "://"
          + // jdbc header
          CHARACTERS
          + // address
          ":"
          + // to designate address port
          PORT_RANGE
          + // port range of (1-99999) while not 03 or 010
          "/"
          + // directory support
          CHARACTERS
          + // schema name
          "(\\?"
          + QUERY_PART
          + "="
          + QUERY_PART
          + // opening queries
          REPEATING_QUERY; // repeating additional queries.
  public static final List<Pattern> KNOWN_URL_PATTERNS =
      Arrays.stream(DB.values())
          .map(db -> JDBC_PREFIX + db.value() + JDBC_VALUES_PATTERN)
          .map(Pattern::compile)
          .collect(Collectors.toList());

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

    result = checkDuplicates(args, result);
    if (!result.isValid()) { // duplications found on specific required args.
      return result; // return as no more verification is meaningful.
    }

    return checkUrl(args, result);
  }

  /**
   * Validation of datasource url by pattern.
   *
   * @param args             expected to hold url entry.
   * @param validationResult accumulated result of the validation.
   * @return final accumulation of validation result.
   */
  private ValidationResult checkUrl(ApplicationArguments args, ValidationResult validationResult) {
    String jdbcConnectionString =
        args.getNonOptionArgs().stream()
            .filter(s -> s.startsWith(URL.getGlobalName()))
            .map(s -> s.substring(s.indexOf('=') + 1))
            .findFirst()
            .orElse(null);
    if (jdbcConnectionString == null) {
      return validationResult.withIssues("argument %s is missing", URL.getGlobalName());
    }
    // if url doesn't match to required format
    if (!isValidConnURL(jdbcConnectionString)) {
      return validationResult.withIssues(getMalformedURLIssue());
    }
    return validationResult;
  }

  private boolean isValidConnURL(String url) {
    for (Pattern pattern : KNOWN_URL_PATTERNS) {
      if (pattern.matcher(url).matches()) {
        return true;
      }
    }
    return false;
  }

  private String getMalformedURLIssue() {
    return "invalid url format. expected: jdbc:"
        + DB.getNames()
        + "://{address}:{port}/schema_name{?option=value}";
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
        args.getNonOptionArgs().stream()
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
    Map<ArgType, List<String>> targetArguments =
        args.getNonOptionArgs().stream()
            .filter(
                argString ->
                    REQUIRED_TYPES.stream()
                        .anyMatch(
                            argType -> argType.getSynonyms().contains(argString.split("=")[0])))
            .collect(
                Collectors.groupingBy(argString -> ArgType.getTypeOf(argString.split("=")[0])));

    List<String> issuesList =
        targetArguments.entrySet().stream()
            .filter(argTypeListEntry -> argTypeListEntry.getValue().size() > 1)
            .map(entry -> entry.getKey().getGlobalName() + " argument has duplicated entries")
            .collect(Collectors.toList());

    return result.withIssues(issuesList);
  }

  public enum DB {
    MYSQL,
    POSTGRESQL;

    public static List<String> getNames() {
      return Arrays.stream(DB.values()).map(Object::toString).collect(Collectors.toList());
    }

    public String value() {
      return this.toString();
    }

    @Override
    public String toString() {
      return this.name().toLowerCase(Locale.ROOT);
    }
  }
}
