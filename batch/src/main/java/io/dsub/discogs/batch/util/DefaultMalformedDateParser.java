package io.dsub.discogs.batch.util;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultMalformedDateParser implements MalformedDateParser {

  private static final Pattern YEAR_PATTERN = Pattern.compile(
      "^([\\d]{4}).*"
  );

  private static final Pattern YEAR_PRESENT = Pattern.compile(
      "^([\\w]{4}).*"
  );

  private static final Pattern MONTH_PATTERN = Pattern.compile(
      "^[\\w]{2,4}[- /.](0*[1-9]|1[0-2])[- /.]?"
  );

  private static final Pattern MONTH_PRESENT = Pattern.compile(
      "^[\\w]{2,4}[- /.](0*[\\w]{1,2}).*"
  );

  private static final Pattern DAY_PATTERN = Pattern.compile(
      "^[\\d]{4}([- /.])(0*([1-9]|1[0-2]))\\1(0*(3[0-1]|[1-2][0-9]|[1-9]))$"
  );

  private static final Pattern FLAT_PATTERN = Pattern.compile(
      "^([\\d]{4})([\\d]{2})([\\d]{2})$"
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
  public LocalDate parse(String source) {
    if (source == null) {
      return null;
    }

    String normalized = source.replaceAll("[^\\d._/ -]", "0");

    if (normalized.isBlank() || normalized.replaceAll("0", "").length() == 0) {
      return null;
    }

    int year = parseYear(normalized);
    if (year < 1) {
      return null;
    }

    int month = parseMonth(normalized);

    if (month < 0) {
      return LocalDate.of(year, 1, 1);
    }

    int day = parseDay(normalized);

    if (day < 0) {
      return LocalDate.of(year, month, 1);
    }

    return LocalDate.of(year, month, day);
  }

  private int parseDay(String date) {
    Matcher flatMatcher = FLAT_PATTERN.matcher(date);
    Matcher dayMatcher = DAY_PATTERN.matcher(date);
    if (!flatMatcher.matches() && !dayMatcher.matches()) {
      return -1;
    }

    int year = parseYear(date);
    int month = parseMonth(date);
    if (year < 0 || month < 0) {
      return -1;
    }
    int maxDay = getMaxDayOfMonth(year, month);

    String possibleDay;

    if (flatMatcher.matches()) {
      possibleDay = flatMatcher.group(3);
    } else {
      possibleDay = dayMatcher.group(4);
    }

    int day = Integer.parseInt(possibleDay);

    return day <= 0 || day > maxDay ? -1 : day;
  }

  private int parseYear(String date) {
    Matcher matcher = FLAT_PATTERN.matcher(date);
    if (matcher.matches()) {
      int year = Integer.parseInt(matcher.group(1));
      return year > 1000 && year < LocalDate.now().getYear() + 1 ? year : -1;
    }
    matcher = YEAR_PATTERN.matcher(date);
    if (matcher.matches()) {
      String possibleYear = matcher.group(1);
      int year = Integer.parseInt(possibleYear);
      if (year > 1000 && year < LocalDate.now().getYear() + 1) {
        return year;
      }
    }
    return -1;
  }

  private int parseMonth(String date) {
    Matcher matcher = FLAT_PATTERN.matcher(date);
    if (matcher.matches()) {
      int month = Integer.parseInt(matcher.group(2));
      if (month > 0 && month < 13) {
        return month;
      }
    }
    matcher = MONTH_PRESENT.matcher(date);
    if (matcher.matches()) {
      String possibleMonth = matcher.group(1);
      int monthValue = Integer.parseInt(possibleMonth);
      if (monthValue > 0 && monthValue < 13) {
        return monthValue;
      }
    }
    return -1;
  }

  private int getMaxDayOfMonth(int year, int month) {
    return LocalDate.of(year, month, 1).lengthOfMonth();
  }
}
