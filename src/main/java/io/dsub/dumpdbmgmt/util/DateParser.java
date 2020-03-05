package io.dsub.dumpdbmgmt.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;

@Slf4j
@Component(value = "dateParser")
public class DateParser {

    /**
     * @param s as xml dump recorded date in either yyyy-mm-dd, yyyy-mm, yyyy.
     *          Some may contain 19XX, which indicates unknown year, or month,
     *          however, the result will be 1500-01-01, due to high risk of
     *          batch failure.
     * @return formatted release date of LocalDate type.
     *
     * NOTE: IF any malformed date is detected AND it does NOT have year info
     * >> the date will be parsed as Jan 01 1500.
     */
    public static LocalDate parse(String s) {
        if (s == null || s.equals("None")) {
            return LocalDate.of(1500, 1, 1);
        }
        Date d = null;

        try {
            if (s.length() == 4) {
                return LocalDate.of(Integer.parseInt(s), 6, 6);
            }
            if (s.length() == 7) {
                s += "-06";
                return parse(s);
            }

            s = s.replaceAll("-00", "-06");
            final int YEAR_LENGTH = 4;
            final int MONTH_LENGTH = 2;
            final int DAY_LENGTH = 2;
            final int MAX_MONTH = 12;
            final int MAX_DAY = 31;

            int firstDash = s.indexOf('-');
            int secondDash = s.indexOf('-', firstDash + 1);
            int len = s.length();

            if ((firstDash > 0) && (secondDash > 0) && (secondDash < len - 1)) {
                if (firstDash == YEAR_LENGTH &&
                        (secondDash - firstDash > 1 && secondDash - firstDash <= MONTH_LENGTH + 1) &&
                        (len - secondDash > 1 && len - secondDash <= DAY_LENGTH + 1)) {
                    int year = Integer.parseInt(s, 0, firstDash, 10);
                    int month = Integer.parseInt(s, firstDash + 1, secondDash, 10);
                    int day = Integer.parseInt(s, secondDash + 1, len, 10);
                    if ((month >= 1 && month <= MAX_MONTH) && (day >= 1 && day <= MAX_DAY)) {
                        d = new Date(year - 1900, month - 1, day);
                    }
                }
            }
        } catch (Exception e) {
            log.info("Malformed date format detected. {}", e.getMessage());
            return LocalDate.of(1500, 1, 1);
        }
        if (d == null) {
            return LocalDate.of(1500, 1, 1);
        }
        return d.toLocalDate();
    }
}
