package io.dsub.discogs.batch.argument.validator;

import io.dsub.discogs.batch.argument.ArgType;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.springframework.boot.ApplicationArguments;

/** An argument validator implementation to check if we got unknown argument. */
@NoArgsConstructor
public class KnownArgumentValidator implements ArgumentValidator {

  /**
   * Validation to check if all arguments are something we already know of.
   *
   * @param applicationArguments to be validated.
   * @return empty validation result if all arguments are known. Otherwise, returns a validation
   *     result with reports of unknown arguments.
   */
  @Override
  public ValidationResult validate(ApplicationArguments applicationArguments) {
    List<String> issueList =
        applicationArguments.getOptionNames().stream()
            .filter(name -> !ArgType.contains(name.toLowerCase()))
            .map(name -> "unknown argument: " + name)
            .collect(Collectors.toList());

    issueList.addAll(
        applicationArguments.getNonOptionArgs().stream()
            .filter(name -> !ArgType.contains(name.split("=")[0]))
            .map(name -> "unknown argument: " + name)
            .collect(Collectors.toList()));

    return new DefaultValidationResult(issueList);
  }
}
