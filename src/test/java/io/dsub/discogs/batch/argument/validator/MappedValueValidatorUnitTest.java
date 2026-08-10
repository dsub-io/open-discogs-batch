package io.dsub.discogs.batch.argument.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.argument.ArgType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

class MappedValueValidatorUnitTest {

  final ArgumentValidator validator = new MappedValueValidator();

  @Test
  void validate() {
    String chunkSize = "--chunkSize=100";
    ApplicationArguments args = new DefaultApplicationArguments(chunkSize);
    ValidationResult result = validator.validate(args);
    assertThat(result.isValid()).isTrue();
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void shouldReportForNullValue(ArgType argType) {
    ApplicationArguments args = new DefaultApplicationArguments(
        "--" + argType.getGlobalName() + "=");
    ValidationResult result = validator.validate(args);

    if (argType.getMinValuesCount() == 0) {
      assertThat(result.getIssues().size()).isEqualTo(0);
    } else {
      assertThat(result.getIssues().size()).isEqualTo(1);
      assertThat(result.getIssues().get(0))
          .isEqualTo("missing value for " + argType.getGlobalName());
    }
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void shouldReportNonSupportedMultipleValues(ArgType argType) {
    Class<?> supportedType = argType.getSupportedType();
    StringBuilder argBuilder = new StringBuilder("--").append(argType.getGlobalName()).append("=");

    if (supportedType.equals(String.class)) {
      argBuilder.append("hello,world,something");
    } else if (supportedType.equals(Long.class)) {
      argBuilder.append("333,22,44");
    }

    ApplicationArguments args = new DefaultApplicationArguments(argBuilder.toString());
    ValidationResult result = validator.validate(args);

    int min = argType.getMinValuesCount();
    int max = argType.getMaxValuesCount();
    if (max < 3 || min > 3) {
      if (min == max) {
        assertThat(result.getIssues().get(0))
            .isEqualTo(argType.getGlobalName() + " expected " + min + " items but got 3 item");
      } else {
        assertThat(result.getIssues().get(0))
            .isEqualTo(
                argType.getGlobalName()
                    + " expected "
                    + min
                    + " to "
                    + max
                    + " items but got 3 item");
      }
    }
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void shouldReportUnsupportedValueType(ArgType argType) {
    Class<?> supportedType = argType.getSupportedType();
    if (supportedType.equals(String.class)) {
      return;
    }

    StringBuilder argBuilder = new StringBuilder()
        .append("--");

    int requiredCount = argType.getMinValuesCount();

    String value = "some...string";

    for (int i = 0; i < requiredCount; i++) {
      if (i == 0) {
        argBuilder.append(argType.getGlobalName()).append("=");
      }
      argBuilder.append(value);
      if (i < requiredCount - 1) {
        argBuilder.append(",");
      }
    }

    ApplicationArguments args = new DefaultApplicationArguments(argBuilder.toString());
    ValidationResult result = validator.validate(args);

    String expectedMsg =
        "invalid type for "
            + argType.getGlobalName()
            + ". supported = "
            + supportedType.getSimpleName();
    assertThat(result.getIssues().size()).isEqualTo(1);
    assertThat(result.getIssues().get(0)).isEqualTo(expectedMsg);
  }

  @Test
  void ignoresUnknownAndNullOptionCollections() {
    ApplicationArguments unknown = new DefaultApplicationArguments("--unknown=value");
    assertThat(validator.validate(unknown).isValid()).isTrue();

    ApplicationArguments nullValues = mock(ApplicationArguments.class);
    when(nullValues.getOptionNames()).thenReturn(Set.of("entities"));
    when(nullValues.getOptionValues("entities")).thenReturn(null);
    assertThat(validator.validate(nullValues).isValid()).isTrue();
  }

  @Test
  void filtersNullAndBlankMultiValuesAndReportsRangeViolations() {
    ValidationResult filtered =
        validator.validate(
            new DefaultApplicationArguments("--entities=artist,null,,release", "--cleanup=null"));
    assertThat(filtered.isValid()).isTrue();

    ValidationResult tooMany =
        validator.validate(
            new DefaultApplicationArguments(
                List.of("artist", "label", "master", "release", "artist").stream()
                    .map(value -> "--entities=" + value)
                    .toArray(String[]::new)));
    assertThat(tooMany.getIssues())
        .containsExactly("entities expected value count of 1 to 4 but got 5 item");
  }
}
