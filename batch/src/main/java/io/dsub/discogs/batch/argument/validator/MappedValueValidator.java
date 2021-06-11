package io.dsub.discogs.batch.argument.validator;

import io.dsub.discogs.batch.argument.ArgType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;

/**
 * Validate if argument is required to be mapped or not. If one is not supposed to have a value
 * mapped, it will report with {@link ArgumentValidator}. The same applies to the one that is
 * supposed to be mapped.
 */
public class MappedValueValidator implements ArgumentValidator {

  private static final Pattern LONG_PATTERN = Pattern.compile("^\\d*$");
  private static final Pattern MULTI_VALUE_PATTERN = Pattern.compile(".*,.*");
  private static final String INVALID_TYPE_MSG = "invalid type for %s. supported = %s";
  private static final String MISSING_VALUE_MSG = "missing value for %s";
  private static final String INVALID_SIZE_MSG = "%s expected %d items but got %d item";
  private static final String INVALID_SIZE_RANGE_MSG =
      "%s expected value count of %d to %d but got %d item";

  /**
   * Combined validation for either having mapped but not supposed to be mapped, or the opposite
   * case.
   *
   * @param args args to be evaluated.
   * @return result of validation.
   */
  @Override
  public ValidationResult validate(ApplicationArguments args) {
    Map<ArgType, List<String>> argMap = collectArguments(args);
    ValidationResult result = validateArgumentCount(argMap);
    if (!result.isValid()) {
      return result;
    }
    return result.combine(validateValueType(argMap));
  }

  /**
   * Method to collect all arguments passed to given {@link ApplicationArguments}.
   *
   * @param args given arguments.
   * @return sum of entire arguments.
   */
  private Map<ArgType, List<String>> collectArguments(ApplicationArguments args) {
    Map<ArgType, List<String>> argMap = new ConcurrentHashMap<>();
    // collect optionValues
    for (String optionName : args.getOptionNames()) {
      ArgType type = ArgType.getTypeOf(optionName);
      List<String> values = args.getOptionValues(optionName);
      if (argMap.containsKey(type)) {
        argMap.get(type).addAll(values);
      } else {
        argMap.put(type, values);
      }
    }

    // collect non options
    for (String nonOptionArg : args.getNonOptionArgs()) {
      int delimiterIndex = nonOptionArg.indexOf('=');
      String name = delimiterIndex > 0 ? nonOptionArg.substring(0, delimiterIndex) : nonOptionArg;
      ArgType type = ArgType.getTypeOf(name);
      if (type == null) {
        continue;
      }
      // no value assigned after '='
      if (name.length() == nonOptionArg.length() - 1) {
        if (argMap.containsKey(type) && !argMap.get(type).contains("null")) {
          argMap.get(type).add("null");
        } else if (!argMap.containsKey(type)) {
          argMap.put(type, List.of("null"));
        }
        continue; // concludes the empty or no value assigned
      }

      // assuming the delimiters are ','
      List<String> values =
          Arrays.stream(nonOptionArg.substring(delimiterIndex + 1).split(","))
              .filter(Objects::nonNull)
              .filter(s -> !s.isBlank())
              .collect(Collectors.toList());
      if (argMap.containsKey(type)) {
        argMap.get(type).addAll(values);
      } else {
        argMap.put(type, values);
      }
    }

    return argMap;
  }

  // check count of assigned argument values.
  private ValidationResult validateArgumentCount(Map<ArgType, List<String>> argMap) {
    ValidationResult result = new DefaultValidationResult();
    for (ArgType type : argMap.keySet()) {
      String name = type.getGlobalName();
      List<String> values = new ArrayList<>();
      argMap
          .get(type)
          .forEach(
              value -> {
                if (MULTI_VALUE_PATTERN.matcher(value).matches()) {
                  List<String> multiValue =
                      List.of(value.split(",")).stream()
                          .filter(s -> !s.equalsIgnoreCase("null"))
                          .filter(s -> !s.isBlank())
                          .collect(Collectors.toList());
                  values.addAll(multiValue);
                } else if (!value.equals("null") && !value.isBlank()) {
                  values.add(value);
                }
              });
      if (type.isValueRequired() && values.isEmpty()) {
        result = result.withIssues(String.format(MISSING_VALUE_MSG, name));
        continue;
      }
      int size = values.size();
      int min = type.getMinValuesCount();
      int max = type.getMaxValuesCount();
      if (size > max || size < min) {
        String msg;
        if (max == min) {
          msg = String.format(INVALID_SIZE_MSG, name, min, size);
        } else {
          msg = String.format(INVALID_SIZE_RANGE_MSG, name, min, max, size);
        }
        result = result.withIssues(msg);
      }
    }
    return result;
  }

  // check type of given mapped values are proper
  private ValidationResult validateValueType(Map<ArgType, List<String>> argMap) {
    ValidationResult result = new DefaultValidationResult();
    for (ArgType argType : argMap.keySet()) {
      String name = argType.getGlobalName();
      Class<?> type = argType.getSupportedType();
      if (type.equals(String.class)) { // notion of any value.
        continue;
      }

      String className = argType.getSupportedType().getSimpleName();

      if (type.equals(Long.class)) {
        for (String s : argMap.get(argType)) {
          if (!LONG_PATTERN.matcher(s).matches()) {
            result = result.withIssues(String.format(INVALID_TYPE_MSG, name, className));
          }
        }
      }
    }
    return result;
  }
}
