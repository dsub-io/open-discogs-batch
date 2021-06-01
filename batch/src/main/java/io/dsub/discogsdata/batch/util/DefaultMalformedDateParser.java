package io.dsub.discogsdata.batch.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

public class DefaultMalformedDateParser implements MalformedDateParser {

  private static final Pattern YEAR_PATTERN = Pattern.compile(
      "^[\\d]{4}.*"
  );

  private static final Pattern YEAR_PRESENT = Pattern.compile(
      "^[\\w]{4}.*"
  );

  private static final Pattern MONTH_PATTERN = Pattern.compile(
      "^[\\w]{2,4}-(0*[1-9]|1[0-2])(-.*)?"
  );

  private static final Pattern MONTH_PRESENT = Pattern.compile(
      "^[\\w]{2,4}-0*[\\w]{1,2}.*"
  );

  private static final Pattern DAY_PATTERN = Pattern.compile(
      "^[\\d]{4}-(0*[1-9]|1[0-2])-0*([1-9]|[1-2][0-9]|3[0-1])$"
  );

  private static final Pattern FLAT_PATTERN = Pattern.compile(
      "^[\\d]{8}$"
  );

  @Override
  public boolean isMonthValid(String date) {
    if (date == null) {
      return false;
    }
    return MONTH_PATTERN.matcher(date).matches();
  }

  @Override
  public boolean isYearValid(String date) {
    if (date == null) {
      return false;
    }
    return YEAR_PATTERN.matcher(date).matches();
  }

  @Override
  public boolean isDayValid(String date) {
    if (date == null) {
      return false;
    }
    return parseDay(date) > 0;
  }

  @Override
  public LocalDate parse(String date) {
    if (date == null || date.isBlank()) {
      return null;
    }

    int year = parseYear(date);
    if (year < 1) {
      return null;
    }

    int month = parseMonth(date);

    if (month < 0) {
      return LocalDate.of(year, 1, 1);
    }

    int day = parseDay(date);

    if (day < 0) {
      return LocalDate.of(year, month, 1);
    }

    return LocalDate.of(year, month, day);
  }

  private int parseDay(String date) {
    if (DAY_PATTERN.matcher(date).matches() || FLAT_PATTERN.matcher(date).matches()) {
      int year = parseYear(date);
      int month = parseMonth(date);
      LocalDate parsedDate = LocalDate.of(year, month, 1);
      int day;
      if (date.indexOf('-') > 0) {
        day = Integer.parseInt(date.split("-")[2].replaceAll("[^\\d]", "0"));
      } else {
        day = Integer.parseInt(date, 6, date.length() - 1, 10);
      }
      int maxDay = parsedDate.lengthOfMonth();
      if (day > 0 && day <= maxDay) {
        return day;
      }
    }
    return -1;
  }

  private int parseYear(String date) {
    if (FLAT_PATTERN.matcher(date).matches()) {
      return Integer.parseInt(date, 0, 4, 10);
    }
    if (YEAR_PRESENT.matcher(date).matches()) {
      String possibleYear = date.split("-")[0].replaceAll("[^\\d]", "0");
      int yearValue = Integer.parseInt(possibleYear);
      if (yearValue > 1000) {
        return yearValue;
      }
    }
    return -1;
  }

  private int parseMonth(String date) {
    if (FLAT_PATTERN.matcher(date).matches()) {
      int month = Integer.parseInt(date, 4, 6, 10);
      if (month > 0 && month < 13) {
        return month;
      }
    }
    if (MONTH_PRESENT.matcher(date).matches()) {
      String possibleMonth = date.split("-")[1].replaceAll("[^\\d]", "0");
      int monthValue = Integer.parseInt(possibleMonth);
      if (monthValue > 0 && monthValue < 13) {
        return monthValue;
      }
    }
    return -1;
  }
}
