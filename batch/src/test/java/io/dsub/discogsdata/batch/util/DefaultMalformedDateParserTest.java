package io.dsub.discogsdata.batch.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DefaultMalformedDateParserTest {

  DefaultMalformedDateParser parser = new DefaultMalformedDateParser();

  @Test
  void whenValidYear__ShouldReturnTrue() {
    assertTrue(parser.isYearValid("1993"));
    assertTrue(parser.isYearValid("1993-hello"));
  }

  @Test
  void whenInvalidYear__ShouldReturnFalse() {
    assertFalse(parser.isYearValid("193x-3"));
  }

  @Test
  void whenInvalidMonth__ShouldReturnFalse() {
    assertFalse(parser.isMonthValid("193X-33"));
    assertFalse(parser.isMonthValid("193X-3X"));
    assertFalse(parser.isMonthValid("193X-"));
    assertFalse(parser.isMonthValid("193X-00"));
  }

  @Test
  void whenValidMonth__ShouldReturnTrue() {
    assertTrue(parser.isMonthValid("193x-03"));
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
    assertFalse(parser.isDayValid("1931-03-33"));
    assertFalse(parser.isDayValid("1931-03-"));
    assertFalse(parser.isDayValid("1931-02-29"));
    assertFalse(parser.isDayValid("1931-02-00"));
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
    //then
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
    assertDoesNotThrow(() -> parser.parse(malformedDate));
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