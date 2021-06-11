package io.dsub.discogs.batch.argument.validator;

import io.dsub.discogs.batch.argument.ArgType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;

/** {@link ArgumentValidator} implementation to validate year-month formatting */
public class YearMonthValidator implements ArgumentValidator {

  private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("^[\\d]{4}-[\\d]{1,2}$");
  private static final Pattern YEAR_MONTH_SINGLE_DIGIT_PATTERN =
      Pattern.compile("^[\\d]{4}-[\\d]$");
  private static final Pattern YEAR_PATTERN = Pattern.compile("^[\\d]{4}$");

  /**
   * validates year and year-month entry received by following logic.
   *
   * <p>1. are year and year-month entry exists at the same time
   *
   * <p>2. are any of those have duplicated values
   *
   * <p>3. validate the parsed data and examine its values
   *
   * @param args argument to be validated.
   * @return validation result containing any issues invloved, or simple an empty result.
   */
  @Override
  public ValidationResult validate(ApplicationArguments args) {
    ValidationResult result = new DefaultValidationResult();
    Map<ArgType, List<String>> argMap = extractYearMonthArgs(args);
    Set<ArgType> keySet = argMap.keySet();

    // report the duplicated entry issue
    if (keySet.size() > 1) {
      String argNames =
          keySet.stream().map(ArgType::getGlobalName).collect(Collectors.joining(","));
      return result.withIssues("expected one of following, but we got both: " + argNames + ".");
    }

    if (keySet.size() == 0) {
      return result;
    }

    // extract the only key supposed to be exists.
    ArgType argType = keySet.iterator().next();
    List<String> argValues = argMap.get(argType);

    // if argument values appear more than once, or has separated value with ',' delimiter.
    if (argValues.size() > 1 || (argValues.size() > 0 && argValues.get(0).split(",").length > 1)) {
      return result.withIssues(
          argType.getGlobalName() + " is not supposed to have multiple values");
    }

    // we can assure that there is only one value exists.
    String value = argValues.get(0);
    if (value == null || value.isBlank()) {
      return result.withIssues("no value is mapped to the argument " + argType.getGlobalName());
    }

    // test by pattern
    boolean isYear = argType.equals(ArgType.YEAR);
    Pattern pattern = isYear ? YEAR_PATTERN : YEAR_MONTH_PATTERN;
    if (!pattern.matcher(value).matches()) {
      String expected = isYear ? "yyyy" : "yyyy-MM";
      return result.withIssues(
          "expected to have value with pattern " + expected + " but got " + value);
    }

    // year should have yyyy, or if yearMonth; yyyy-MM
    int year = isYear ? Integer.parseInt(value) : Integer.parseInt(value, 0, 4, 10);
    int currentYear = LocalDate.now().getYear();

    if (year < 2008 || year > currentYear) {
      return result.withIssues("invalid year given. expected range: 2008 - " + currentYear);
    }

    // conclude for year as we do not have any month entry to eval.
    if (isYear) {
      return result;
    }

    int endIdx = YEAR_MONTH_SINGLE_DIGIT_PATTERN.matcher(value).matches() ? 6 : 7;

    int month = Integer.parseInt(value, 5, endIdx, 10);
    if (month < 1 || month > 12) {
      String msg = String.format("invalid month given: %d. expected between 1-12..", month);
      return result.withIssues(msg);
    }

    int currentMonth = LocalDate.now().getMonthValue();
    if (year == currentYear && month > currentMonth) {
      return result.withIssues(
          "cannot set the yearMonth value to future. given: "
              + value
              + ", current: "
              + currentYear
              + "-"
              + currentMonth);
    }

    // conclude the yearMonth argType evaluation.
    return result;
  }

  // assuming year month is optional argument.
  private Map<ArgType, List<String>> extractYearMonthArgs(ApplicationArguments args) {
    return args.getOptionNames().stream()
        .filter(ArgType::contains)
        .filter(
            key -> {
              ArgType t = ArgType.getTypeOf(key);
              return t == ArgType.YEAR || t == ArgType.YEAR_MONTH;
            })
        .collect(Collectors.toMap(ArgType::getTypeOf, args::getOptionValues));
  }
}
