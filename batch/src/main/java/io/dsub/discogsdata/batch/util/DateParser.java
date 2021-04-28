package io.dsub.discogsdata.batch.util;

import java.time.LocalDate;

public abstract class DateParser {
    public abstract String replaceNonDigitsToZero(String dateString);

    /**
     * @param dateString as xml dump recorded date in either yyyy-mm-dd, yyyy-mm, yyyy.
     *                   Some may contain character, which indicates either invalid or unknown.
     * @return formatted release date of LocalDate type.
     */
    public abstract LocalDate parse(String dateString);
    public abstract boolean hasValidYear(String dateString);
    public abstract boolean hasValidDay(String dateString);
    public abstract boolean hasValidMonth(String dateString);
}
