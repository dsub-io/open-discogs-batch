package io.dsub.discogsdata.batch.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class dateParserTest {

    private final DateParser dateParser = DefaultDateParser.getInstance();

    @Test
    void shouldParseInvalidYear() {
        testParseResultBy("199X", 1990);
        testParseResultBy("19XX-1", 1900);
        testParseResultBy("dd", 1500);
        testParseResultBy("xx-1", 1500);
        testParseResultBy("-1-1", 1500);
        testParseResultBy("_-1-1", 1500);
    }

    @Test
    void shouldParseInvalidMonth() {
        testParseResultBy("1990-0X", 1990, 1, 1);
        testParseResultBy("199d-X", 1990, 1, 1);
        testParseResultBy("x-x", 1500, 1);
        testParseResultBy("x-", 1500, 1);
        testParseResultBy("x-_", 1500, 1);
    }

    @Test
    void shouldParseInvalidDay() {
        testParseResultBy("1990-3-32", 1990, 3,1);
        testParseResultBy("1990-1-33", 1990, 1, 1);
        testParseResultBy("1990-1-0", 1990, 1, 1);
        testParseResultBy("1990-1-_", 1990, 1, 1);
        testParseResultBy("1990-1-KL", 1990, 1, 1);
        testParseResultBy("1990-1-", 1990, 1, 1);
        testParseResultBy("1990-3", 1990, 3, 1);
    }

    @Test
    void shouldSwapMonthAndDayIfNecessary() {
        testParseResultBy("1990-24-10", 1990, 10, 24);
        testParseResultBy("1990-31-2", 1990, 1, 2);
        testParseResultBy("1990-28-2", 1990, 2, 28);
        testParseResultBy("1990-31-", 1990, 1, 1);
    }

    @Test
    void shouldHandleEmptyString() {
        testParseResultBy(null, 1500, 1,1);
    }

    @Test
    void shouldReturnValidYearCheck() {
        assertThat(dateParser.hasValidYear("1990"))
                .isTrue();
        assertThat(dateParser.hasValidYear("199"))
                .isFalse();
        assertThat(dateParser.hasValidYear("199x"))
                .isFalse();
        assertThat(dateParser.hasValidYear(""))
                .isFalse();
        assertThat(dateParser.hasValidYear(null))
                .isFalse();
    }

    @Test
    void shouldReturnValidMonthCheck() {
        assertThat(dateParser.hasValidMonth("2000-13")).isFalse();
        assertThat(dateParser.hasValidMonth("2000-13-12")).isFalse();
        assertThat(dateParser.hasValidMonth("2000-12-12")).isTrue();
        assertThat(dateParser.hasValidMonth("2000-")).isFalse();
        assertThat(dateParser.hasValidMonth("2000")).isFalse();
        assertThat(dateParser.hasValidMonth("")).isFalse();
        assertThat(dateParser.hasValidMonth(null)).isFalse();
        assertThat(dateParser.hasValidMonth("3-e")).isFalse();
    }

    @Test
    void shouldReturnValidDayCheck() {
        assertThat(dateParser.hasValidDay("2000-13-12")).isTrue();
        assertThat(dateParser.hasValidDay("2000-2-29")).isTrue();
        assertThat(dateParser.hasValidDay("1999-2-29")).isFalse();
        assertThat(dateParser.hasValidDay("2000-2-28")).isTrue();
        assertThat(dateParser.hasValidDay("2000-2-33")).isFalse();
        assertThat(dateParser.hasValidDay("2000-2-")).isFalse();
        assertThat(dateParser.hasValidDay("2000-2")).isFalse();
        assertThat(dateParser.hasValidDay("2000")).isFalse();
        assertThat(dateParser.hasValidDay("")).isFalse();
        assertThat(dateParser.hasValidDay("null")).isFalse();
        assertThat(dateParser.hasValidDay(null)).isFalse();
    }

    @Test
    void d() {
        assertThat(dateParser.replaceNonDigitsToZero("df3232f32-"))
                .doesNotMatch("[a-zA-Z]")
                .contains("-");
        assertThat(dateParser.replaceNonDigitsToZero(null))
                .hasSizeLessThanOrEqualTo(0)
                .isBlank();
        assertThat(dateParser.replaceNonDigitsToZero("abcde-"))
                .isEqualTo("00000-");
        assertThat(dateParser.replaceNonDigitsToZero("-_-"))
                .isEqualTo("-0-");
        assertThat(dateParser.replaceNonDigitsToZero("2000-03-xx"))
                .isEqualTo("2000-03-00");
    }

    private void testParseResultBy(String dateString, int year, int month, int day) {
        LocalDate result = dateParser.parse(dateString);
        assertThat(result).isEqualTo(LocalDate.of(year,  month, day));
    }

    private void testParseResultBy(String dateString, int year, int month) {
        testParseResultBy(dateString, year, month, 1);
    }

    private void testParseResultBy(String dateString, int year) {
        testParseResultBy(dateString, year, 1);
    }
}