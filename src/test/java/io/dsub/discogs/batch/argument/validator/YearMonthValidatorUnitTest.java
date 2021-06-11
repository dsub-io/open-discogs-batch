package io.dsub.discogs.batch.argument.validator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

class YearMonthValidatorUnitTest {

  final YearMonthValidator validator = new YearMonthValidator();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "year=3333",
        "year=hell",
        "yearmonth=2009-13",
        "yearmonth=2009",
        "year=2001",
        "year=2010-03",
        "yearmonth=2013-00",
        "yearmonth=1991-03"
      })
  void shouldReportInvalidFormats(String arg) {
    ApplicationArguments args = new DefaultApplicationArguments("--" + arg);
    ValidationResult result = validator.validate(args);
    assertThat(result.getIssues().size()).isNotZero();
  }

  @Test
  void shouldReportIssueWhenYearIsOutOfBound() {
    int year = LocalDate.now().getYear() + 1;
    ValidationResult result = validator.validate(new DefaultApplicationArguments("--year=" + year));

    String expectedMsg = "invalid year given. expected range: 2008 - " + (year - 1);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getIssues().get(0)).isEqualTo(expectedMsg);

    int month = 1;
    result =
        validator.validate(new DefaultApplicationArguments("--yearmonth=" + year + "-" + month));
    assertThat(result.isValid()).isFalse();
    assertThat(result.getIssues().get(0)).isEqualTo(expectedMsg);
  }

  @Test
  void shouldReportFutureToBeFalse() {
    int currentYear = LocalDate.now().getYear();
    int month = LocalDate.now().getMonthValue() + 1;
    String arg = String.format("--yearMonth=%s-%s", currentYear, month);
    ValidationResult result = validator.validate(new DefaultApplicationArguments(arg));
    assertThat(result.getIssues().size()).isEqualTo(1);
    assertThat(result.getIssues().get(0)).contains("cannot set the yearMonth value to future");
  }

  @Test
  void shouldReportIfMultipleValuesGiven() {
    String yearMonthArg = "--year_month=2009-09,2013-06";
    ValidationResult result = validator.validate(new DefaultApplicationArguments(yearMonthArg));
    assertThat(result.getIssues().size()).isEqualTo(1);
    assertThat(result.getIssues().get(0))
        .isEqualTo("yearMonth is not supposed to have multiple values");

    String yearArg = "--year=2010,2013";
    result = validator.validate(new DefaultApplicationArguments(yearArg));
    assertThat(result.getIssues().size()).isEqualTo(1);
    assertThat(result.getIssues().get(0)).isEqualTo("year is not supposed to have multiple values");
  }

  @Test
  void shouldReportIfYearMonthAndYearArgGivenAtTheSameTime() {
    String yearMonthArg = "--year-month=2009-09";
    String yearArg = "--year=2010";
    ValidationResult result =
        validator.validate(new DefaultApplicationArguments(yearArg, yearMonthArg));
    assertThat(result.getIssues().size()).isEqualTo(1);
    assertThat(result.getIssues().get(0))
        .contains("expected one of following, but we got both:")
        .contains("year")
        .contains("yearMonth");
  }

  @Test
  void shouldReturnEmptyResultIfNoMatchingArgsGiven() {
    ValidationResult result = validator.validate(new DefaultApplicationArguments());
    assertThat(result.getIssues().size()).isEqualTo(0);
  }

  @ParameterizedTest
  @ValueSource(strings = {"--year=201", "--yearMonth=2010-"})
  void shouldReportIfMalformedArgumentExists(String arg) {
    ValidationResult result = validator.validate(new DefaultApplicationArguments(arg));
    assertThat(result.getIssues().size()).isEqualTo(1);

    if (arg.matches("^--year=.*")) {
      assertThat(result.getIssues().get(0))
          .isEqualTo(
              "expected to have value with pattern yyyy but got " + arg.replaceAll("^--year=", ""));
    } else {
      assertThat(result.getIssues().get(0))
          .isEqualTo(
              "expected to have value with pattern yyyy-MM but got "
                  + arg.replaceAll("^--yearMonth=", ""));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"--year=", "--yearMonth="})
  void shouldReportProperlyIfEmptyValueIsMapped(String arg) {
    ValidationResult result = validator.validate(new DefaultApplicationArguments(arg));
    assertThat(result.getIssues().size()).isEqualTo(1);
    assertThat(result.getIssues().get(0)).contains("no value is mapped to the argument ");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"--year=2009", "--year=2015", "--yearMonth=2010-03", "--yearMonth=2016-6"})
  void shouldNotReportIfAppropriateArgumentsAreGiven(String arg) {
    ValidationResult result = validator.validate(new DefaultApplicationArguments(arg));
    assertThat(result.isValid()).isTrue();
  }
}
