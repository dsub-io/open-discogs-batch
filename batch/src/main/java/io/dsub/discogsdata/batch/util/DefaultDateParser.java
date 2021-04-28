package io.dsub.discogsdata.batch.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Slf4j
public class DefaultDateParser extends DateParser {

    private static final DefaultDateParser INSTANCE = new DefaultDateParser();

    public static DefaultDateParser getInstance() {
        return INSTANCE;
    }

    public static final LocalDate UNKNOWN = LocalDate.of(1500, 1, 1);
    private static final int[] EMPTY_PART_RESULT = new int[]{1500, 1, 1};

    private static final Pattern NON_DIGIT = Pattern.compile(".*[^\\d-].*");
    private static final Pattern HAS_VALID_MONTH = Pattern.compile("^[^-]{0,4}-(1[0-2]|0?[1-9])(-.*)?");
    private static final Pattern HAS_VALID_DAY = Pattern.compile("^.+-.{1,2}-([1-9]|[0]?1-9|[1-2][1-9]|3[0-1])");

    private static final Pattern REQUIRE_MONTH_DAY_SWAP =
            Pattern.compile("^.*-(1[3-9]|2[0-9]|3[1-2])-([0]?[1-9]|1[0-2])$");

    private DefaultDateParser() {
    }

    @Override
    public String replaceNonDigitsToZero(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return "";
        }
        return dateString.replaceAll("[^\\d-]", "0");
    }

    @Override
    public LocalDate parse(String dateString) {

        if (dateString == null || dateString.isBlank()) {
            return UNKNOWN;
        }

        String s = replaceNonDigitsToZero(dateString).trim();
        s = replaceDayMonthIfNecessary(s);

        int[] parsed = parseDateString(s);

        if (parsed[0] == 0 || parsed[0] > LocalDate.now().getYear() || parsed[0] < 1800) {
            parsed[0] = 1500;
        }

        if (parsed[1] <= 0 || parsed[1] > 12) {
            parsed[1] = 1;
        }

        if (parsed[2] <= 0 || parsed[2] > 31) {
            return LocalDate.of(parsed[0], parsed[1], 1);
        }

        int maxDay = getMaxDayBy(parsed[0], parsed[1]);

        if (maxDay < parsed[2]) {
            parsed[2] = 1;
        }

        return LocalDate.of(parsed[0], parsed[1], parsed[2]);
    }

    @Override
    public boolean hasValidYear(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return false;
        }
        String yearString = dateString.split("-")[0];
        if (NON_DIGIT.matcher(yearString).matches()) {
            return false;
        }
        int year = Integer.parseInt(yearString);
        return year >= 1800 && year <= LocalDate.now().getYear();
    }

    @Override
    public boolean hasValidDay(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return false;
        }

        String s = replaceNonDigitsToZero(dateString);
        // if day is between (0)1 ~ 31
        if (HAS_VALID_DAY.matcher(s).matches()) {
            // if month is between (0)1-12
            if (hasValidMonth(s)) {
                String[] parts = s.split("-");
                int year = Integer.parseInt(parts[0]);
                // we cannot know what would be the maxDay of the month (i.e. February)
                if (year <= 1800 || year > LocalDate.now().getYear()) {
                    return true;
                }
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                // returns if day is equals or lower than the max day of the month and is greater than 0
                return day <= getMaxDayBy(year, month) && day > 0;
            }
            // only day matches, hence returning (does not have to think about year value)
            return true;
        }
        return false;
    }

    @Override
    public boolean hasValidMonth(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return false;
        }
        return HAS_VALID_MONTH.matcher(dateString).matches();
    }

    public String replaceDayMonthIfNecessary(String dateString) {
        if (REQUIRE_MONTH_DAY_SWAP.matcher(dateString).matches()) {
            int[] dateParts = parseDateString(dateString);
            int maxDay = getMaxDayBy(dateParts[0], dateParts[2]);
            if (dateParts[1] > maxDay) {
                return dateString;
            }
            return dateParts[0] + "-" + dateParts[2] + "-" + dateParts[1];
        }
        return dateString;
    }

    private int getMaxDayBy(int year, int month) {
        return LocalDate.of(year, month, 1)
                .plusMonths(1)
                .minusDays(1)
                .getDayOfMonth();
    }

    private int[] parseDateString(String in) {
        if (in == null || in.isBlank()) {
            return EMPTY_PART_RESULT;
        }
        String s = replaceNonDigitsToZero(in);
        String[] parts = s.split("-");
        int[] result = EMPTY_PART_RESULT.clone();

        for (int i = 0; i < parts.length && i < 3; i++) {
            result[i] = parts[i].isBlank() ? 1 : Integer.parseInt(parts[i]);
        }

        return result;
    }
}
