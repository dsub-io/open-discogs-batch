package io.dsub.discogs.batch.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

class DefaultMalformedDateParserUnitTest {

  DefaultMalformedDateParser parser = new DefaultMalformedDateParser();

  @Test
  void whenValidYear__ShouldReturnTrue() {
    assertTrue(parser.isYearValid("1993"));
    assertTrue(parser.isYearValid("1993-hello"));
  }

  @Test
  void whenInvalidYear__ShouldReturnFalse() {
    assertFalse(parser.isYearValid("193x-3"));
    assertFalse(parser.isYearValid(null));
  }

  @Test
  void whenInvalidMonth__ShouldReturnFalse() {
    assertFalse(parser.isMonthValid(null));
    assertFalse(parser.isMonthValid("193X-33"));
    assertFalse(parser.isMonthValid("193X-3X"));
    assertFalse(parser.isMonthValid("193X-"));
    assertFalse(parser.isMonthValid("193X-00"));
  }

  @Test
  void whenValidMonth__ShouldReturnTrue() {
    assertTrue(parser.isMonthValid("193x-03"));
    assertTrue(parser.isMonthValid("193x-03-00"));
    assertTrue(parser.isMonthValid("193x-3"));
    assertTrue(parser.isMonthValid("193x-002"));
  }

  @Test
  void whenValidDay__ShouldReturnTrue() {
    assertTrue(parser.isDayValid("1931-03-28"));
    assertTrue(parser.isDayValid("1931-0003-0028"));
  }

  @Test
  void whenInvalidDay__ShouldReturnFalse() {
    assertFalse(parser.isDayValid(null));
    assertFalse(parser.isDayValid("1931-03-33"));
    assertFalse(parser.isDayValid("1931-03-"));
    assertFalse(parser.isDayValid("1931-02-29"));
    assertFalse(parser.isDayValid("1931-02-00"));
    assertFalse(parser.isDayValid("09990101"));
    assertFalse(parser.isDayValid("20201301"));
  }

  @ParameterizedTest
  @CsvSource({"20201301,2020-01-01", "20200100,2020-01-01", "20200231,2020-02-01"})
  void shouldFallbackForOutOfRangeFlatMonthOrDay(String value, String expected) {
    assertThat(parser.parse(value)).isEqualTo(LocalDate.parse(expected));
  }

  @ParameterizedTest
  @ValueSource(strings = {"09990101", "99990101"})
  void shouldRejectOutOfRangeFlatYears(String value) {
    assertThat(parser.parse(value)).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"0999-01-01", "9999-01-01"})
  void shouldRejectOutOfRangeSeparatedYears(String value) {
    assertThat(parser.parse(value)).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"2020-00", "2020-13"})
  void shouldFallbackForOutOfRangeSeparatedMonths(String value) {
    assertThat(parser.parse(value)).isEqualTo(LocalDate.of(2020, 1, 1));
  }

  @Test
  void whenParse__ShouldHandleChars() {
    // when
    LocalDate parsedDate = parser.parse("193x-1x");

    // then
    assertThat(parsedDate).isNotNull();
    assertThat(parsedDate.getYear()).isEqualTo(1930);
    assertThat(parsedDate.getMonthValue()).isEqualTo(10);
    assertThat(parsedDate.getDayOfMonth()).isEqualTo(1);
  }

  @Test
  void whenParse__shouldHandleNullOrBlank() {
    // when
    LocalDate parsedNull = parser.parse(null);
    LocalDate parsedBlank = parser.parse("");

    // then
    assertThat(parsedNull).isNull();
    assertThat(parsedBlank).isNull();
  }

  @Test
  void whenParse__ShouldHandleMalformedYear() {
    // when
    LocalDate parsedDate = parser.parse("xxxx");

    // then
    assertThat(parsedDate).isNull();
    assertThat(parser.parse("a1")).isNull();
  }

  @Test
  void whenParse__ShouldHandleMonthWithCharacter() {
    // when
    LocalDate parsedDate = parser.parse("1992-1x");

    // then
    assertThat(parsedDate).isNotNull();
    assertThat(parsedDate.getMonth()).isEqualTo(Month.OCTOBER);
    assertThat(parsedDate.getDayOfMonth()).isEqualTo(1);
  }

  @Test
  void whenParse__ShouldHandleMalformedMonth() {
    // when
    LocalDate parsedDate = parser.parse("1992-xx");

    // then
    assertThat(parsedDate).isNotNull();
    assertThat(parsedDate.getMonth()).isEqualTo(Month.JANUARY);
    assertThat(parsedDate.getDayOfMonth()).isEqualTo(1);
  }

  @Test
  void whenParse__ShouldHandleMalformedDay() {
    // when
    LocalDate parsedDate = parser.parse("1992-1x-1c");

    // then
    assertThat(parsedDate).isNotNull();
    assertThat(parsedDate.getMonth()).isEqualTo(Month.OCTOBER);
    assertThat(parsedDate.getDayOfMonth()).isEqualTo(10);
  }

  @Test
  void whenParse__ShouldHandleWellFormedValue() {
    // when
    LocalDate parsedDate = parser.parse("1988-03-18");
    // then
    assertThat(parsedDate).isNotNull();
    assertThat(parsedDate.getYear()).isEqualTo(1988);
    assertThat(parsedDate.getMonth()).isEqualTo(Month.MARCH);
    assertThat(parsedDate.getDayOfMonth()).isEqualTo(18);
  }

  @Test
  void givenDateHasNoDash__ShouldParse() {
    // when
    LocalDate parsedDate = parser.parse("19920405");

    // then
    assertThat(parsedDate.getYear()).isEqualTo(1992);
    assertThat(parsedDate.getMonthValue()).isEqualTo(4);
    assertThat(parsedDate.getDayOfMonth()).isEqualTo(5);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1992x3x2", "1992xxxx", "33xxx212x", "xxxxxxxxx", "dfakfmlk"})
  void givenDateHasMalformed__ShouldParse(String malformedDate) {
    // given
    Assertions.assertDoesNotThrow(() -> parser.parse(malformedDate));
    LocalDate parsedDate = parser.parse(malformedDate);

    // when
    if (parser.isYearValid(malformedDate)) {

      // then
      assertThat(parsedDate).isNotNull();
    } else if (parser.isMonthValid(malformedDate)) {

      // then
      assertThat(parsedDate).isNull();
    }
  }
}
