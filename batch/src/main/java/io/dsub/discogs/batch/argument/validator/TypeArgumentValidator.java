package io.dsub.discogs.batch.argument.validator;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.ApplicationArguments;

public class TypeArgumentValidator implements ArgumentValidator {

  private static final Pattern TYPE_VALUE_PATTERN =
      Pattern.compile("^((ARTIST)|(RELEASE)|(MASTER)|(LABEL))$", Pattern.CASE_INSENSITIVE);

  private static final Pattern TYPE_PATTERN =
      Pattern.compile("^type[s]?$", Pattern.CASE_INSENSITIVE);

  @Override
  public ValidationResult validate(ApplicationArguments args) {
    // init
    ValidationResult result = new DefaultValidationResult();

    // collect possible duplicate type argument.
    List<String> typeArgNames =
        args.getOptionNames().stream()
            .filter(name -> TYPE_PATTERN.matcher(name).matches())
            .collect(Collectors.toList());

    // empty means we do not need to validate anything.
    if (typeArgNames.isEmpty()) {
      return result;
    }

    // duplicated entries to be picked up from here.
    if (typeArgNames.size() > 1) {
      String msg = "duplicated type argument exists: " + Strings.join(typeArgNames, ',');
      return result.withIssues(msg);
    }

    // collect issues if any of argument does not match to the criteria.
    List<String> issues =
        args.getOptionValues(typeArgNames.get(0)).stream()
            .filter(val -> !TYPE_VALUE_PATTERN.matcher(val).matches())
            .map(val -> "unknown type argument value: " + val)
            .collect(Collectors.toList());

    // will be empty if there was no issue.
    return result.withIssues(issues);
  }
}
