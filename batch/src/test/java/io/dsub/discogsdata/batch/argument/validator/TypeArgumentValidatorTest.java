package io.dsub.discogsdata.batch.argument.validator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

class TypeArgumentValidatorTest {

  final TypeArgumentValidator validator = new TypeArgumentValidator();

  @BeforeEach
  void setUp() {
  }

  @Test
  void whenDuplicatedTypeArgExists__ThenShouldIncludeAllOfThemInReport() {

    ApplicationArguments args = new DefaultApplicationArguments("--types=hello", "--type=hi");
    // when
    ValidationResult result = validator.validate(args);

    // then
    assertThat(result.getIssues().size()).isEqualTo(1);
    assertThat(result.getIssues().get(0)).contains("types", "type");
  }

  @Test
  void whenMalformedTypeValueExists__ThenShouldReportEachOfThemInReport() {
    List<String> values = List.of("--types=hello", "--types=world", "--types=malformed");
    ApplicationArguments args = new DefaultApplicationArguments(values.toArray(String[]::new));

    // when
    ValidationResult result = validator.validate(args);

    // then
    assertThat(result.getIssues().size()).isEqualTo(3);
    for (String value : values) {
      assertThat("unknown type argument value: " + value.replaceAll("--types=", ""))
          .isIn(result.getIssues());
    }
  }

  @Test
  void whenProperTypeValuesPresented__ThenShouldNotReportAnyIssue() {
    String prefix = "--type=";
    List<String> wantedTypes = List.of("release", "artist", "master", "label");
    String[] optionArgs = wantedTypes.stream()
        .map(value -> prefix + value)
        .collect(Collectors.toList())
        .toArray(String[]::new);

    ApplicationArguments args = new DefaultApplicationArguments(optionArgs);

    // when
    ValidationResult result = validator.validate(args);

    // then
    assertThat(result.getIssues().size()).isEqualTo(0);
  }
}
