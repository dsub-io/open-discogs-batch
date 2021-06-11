package io.dsub.discogs.batch.util;

import java.time.LocalDate;

public interface MalformedDateParser {

  boolean isMonthValid(String date);

  boolean isYearValid(String date);

  boolean isDayValid(String date);

  LocalDate parse(String date);
}
