/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.template.utility;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date and time related utilities.
 */
public class DateUtil {

    /**
     * Show hours (24h); always 2 digits, like {@code 00}, {@code 05}, etc.
     */
    public static final int ACCURACY_HOURS = 4;
    
    /**
     * Show hours and minutes (even if minutes is 00).
     */
    public static final int ACCURACY_MINUTES = 5;
    
    /**
     * Show hours, minutes and seconds (even if seconds is 00).
     */
    public static final int ACCURACY_SECONDS = 6;
    
    /**
     * Show hours, minutes and seconds and up to 3 fraction second digits, without trailing 0-s in the fraction part. 
     */
    public static final int ACCURACY_MILLISECONDS = 7;
    
    /**
     * Show hours, minutes and seconds and exactly 3 fraction second digits (even if it's 000)
     */
    public static final int ACCURACY_MILLISECONDS_FORCED = 8;
    
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    
    private static final String REGEX_XS_TIME_ZONE
            = "Z|(?:[-+][0-9]{2}:[0-9]{2})";
    private static final String REGEX_ISO8601_BASIC_TIME_ZONE
            = "Z|(?:[-+][0-9]{2}(?:[0-9]{2})?)";
    private static final String REGEX_ISO8601_EXTENDED_TIME_ZONE
            = "Z|(?:[-+][0-9]{2}(?::[0-9]{2})?)";
    
    private static final String REGEX_XS_OPTIONAL_TIME_ZONE
            = "(" + REGEX_XS_TIME_ZONE + ")?";
    private static final String REGEX_ISO8601_BASIC_OPTIONAL_TIME_ZONE
            = "(" + REGEX_ISO8601_BASIC_TIME_ZONE + ")?";
    private static final String REGEX_ISO8601_EXTENDED_OPTIONAL_TIME_ZONE
            = "(" + REGEX_ISO8601_EXTENDED_TIME_ZONE + ")?";
    
    private static final String REGEX_XS_DATE_BASE
            = "(-?[0-9]+)-([0-9]{2})-([0-9]{2})";
    private static final String REGEX_ISO8601_BASIC_DATE_BASE
            = "(-?[0-9]{4,}?)([0-9]{2})([0-9]{2})";
    private static final String REGEX_ISO8601_EXTENDED_DATE_BASE
            = "(-?[0-9]{4,})-([0-9]{2})-([0-9]{2})";
    
    private static final String REGEX_XS_TIME_BASE
            = "([0-9]{2}):([0-9]{2}):([0-9]{2})(?:\\.([0-9]+))?";
    private static final String REGEX_ISO8601_BASIC_TIME_BASE
            = "([0-9]{2})(?:([0-9]{2})(?:([0-9]{2})(?:[\\.,]([0-9]+))?)?)?";
    private static final String REGEX_ISO8601_EXTENDED_TIME_BASE
            = "([0-9]{2})(?::([0-9]{2})(?::([0-9]{2})(?:[\\.,]([0-9]+))?)?)?";
        
    private static final Pattern PATTERN_XS_DATE = Pattern.compile(
            REGEX_XS_DATE_BASE + REGEX_XS_OPTIONAL_TIME_ZONE);
    private static final Pattern PATTERN_ISO8601_BASIC_DATE = Pattern.compile(
            REGEX_ISO8601_BASIC_DATE_BASE); // No time zone allowed here
    private static final Pattern PATTERN_ISO8601_EXTENDED_DATE = Pattern.compile(
            REGEX_ISO8601_EXTENDED_DATE_BASE); // No time zone allowed here

    private static final Pattern PATTERN_XS_TIME = Pattern.compile(
            REGEX_XS_TIME_BASE + REGEX_XS_OPTIONAL_TIME_ZONE);
    private static final Pattern PATTERN_ISO8601_BASIC_TIME = Pattern.compile(
            REGEX_ISO8601_BASIC_TIME_BASE + REGEX_ISO8601_BASIC_OPTIONAL_TIME_ZONE);
    private static final Pattern PATTERN_ISO8601_EXTENDED_TIME = Pattern.compile(
            REGEX_ISO8601_EXTENDED_TIME_BASE + REGEX_ISO8601_EXTENDED_OPTIONAL_TIME_ZONE);
    
    private static final Pattern PATTERN_XS_DATE_TIME = Pattern.compile(
            REGEX_XS_DATE_BASE
            + "T" + REGEX_XS_TIME_BASE
            + REGEX_XS_OPTIONAL_TIME_ZONE);
    private static final Pattern PATTERN_ISO8601_BASIC_DATE_TIME = Pattern.compile(
            REGEX_ISO8601_BASIC_DATE_BASE
            + "T" + REGEX_ISO8601_BASIC_TIME_BASE
            + REGEX_ISO8601_BASIC_OPTIONAL_TIME_ZONE);
    private static final Pattern PATTERN_ISO8601_EXTENDED_DATE_TIME = Pattern.compile(
            REGEX_ISO8601_EXTENDED_DATE_BASE
            + "T" + REGEX_ISO8601_EXTENDED_TIME_BASE
            + REGEX_ISO8601_EXTENDED_OPTIONAL_TIME_ZONE);
    
    private static final Pattern PATTERN_XS_TIME_ZONE = Pattern.compile(
            REGEX_XS_TIME_ZONE);
    
    private static final String MSG_YEAR_0_NOT_ALLOWED
            = "Year 0 is not allowed in XML schema dates. BC 1 is -1, AD 1 is 1.";
    
    private DateUtil() {
        // can't be instantiated
    }
    
    /**
     * Returns the time zone object for the name (or ID). This differs from
     * {@link TimeZone#getTimeZone(String)} in that the latest returns GMT
     * if it doesn't recognize the name, while this throws an
     * {@link UnrecognizedTimeZoneException}.
     * 
     * @throws UnrecognizedTimeZoneException If the time zone name wasn't understood
     */
    public static TimeZone getTimeZone(String name)
    throws UnrecognizedTimeZoneException {
        if (isGMTish(name)) {
            if (name.equalsIgnoreCase("UTC")) {
                return UTC;
            }
            return TimeZone.getTimeZone(name);
        }
        TimeZone tz = TimeZone.getTimeZone(name);
        if (isGMTish(tz.getID())) {
            throw new UnrecognizedTimeZoneException(name);
        }
        return tz;
    }

    /**
     * Tells if a offset or time zone is GMT. GMT is a fuzzy term, it used to
     * referred both to UTC and UT1.
     */
    private static boolean isGMTish(String name) {
        if (name.length() < 3) {
            return false;
        }
        char c1 = name.charAt(0);
        char c2 = name.charAt(1);
        char c3 = name.charAt(2);
        if (
                !(
                       (c1 == 'G' || c1 == 'g')
                    && (c2 == 'M' || c2 == 'm')
                    && (c3 == 'T' || c3 == 't')
                )
                &&
                !(
                       (c1 == 'U' || c1 == 'u')
                    && (c2 == 'T' || c2 == 't')
                    && (c3 == 'C' || c3 == 'c')
                )
                &&
                !(
                       (c1 == 'U' || c1 == 'u')
                    && (c2 == 'T' || c2 == 't')
                    && (c3 == '1')
                )
                ) {
            return false;
        }
        
        if (name.length() == 3) {
            return true;
        }
        
        String offset = name.substring(3);
        if (offset.startsWith("+")) {
            return offset.equals("+0") || offset.equals("+00")
                    || offset.equals("+00:00");
        } else {
            return offset.equals("-0") || offset.equals("-00")
            || offset.equals("-00:00");
        }
    }

    /**
     * Format a date, time or dateTime with one of the ISO 8601 extended
     * formats that is also compatible with the XML Schema format (as far as you
     * don't have dates in the BC era). Examples of possible outputs:
     * {@code "2005-11-27T15:30:00+02:00"}, {@code "2005-11-27"},
     * {@code "15:30:00Z"}. Note the {@code ":00"} in the time zone offset;
     * this is not required by ISO 8601, but included for compatibility with
     * the XML Schema format. Regarding the B.C. issue, those dates will be
     * one year off when read back according the XML Schema format, because of a
     * mismatch between that format and ISO 8601:2000 Second Edition.  
     * 
     * <p>This method is thread-safe.
     * 
     * @param date the date to convert to ISO 8601 string
     * @param datePart whether the date part (year, month, day) will be included
     *        or not
     * @param timePart whether the time part (hours, minutes, seconds,
     *        milliseconds) will be included or not
     * @param offsetPart whether the time zone offset part will be included or
     *        not. This will be shown as an offset to UTC (examples:
     *        {@code "+01"}, {@code "-02"}, {@code "+04:30"}) or as {@code "Z"}
     *        for UTC (and for UT1 and for GMT+00, since the Java platform
     *        doesn't really care about the difference).
     *        Note that this can't be {@code true} when {@code timePart} is
     *        {@code false}, because ISO 8601 (2004) doesn't mention such
     *        patterns.
     * @param accuracy tells which parts of the date/time to drop. The
     *        {@code datePart} and {@code timePart} parameters are stronger than
     *        this. Note that when {@link #ACCURACY_MILLISECONDS} is specified,
     *        the milliseconds part will be displayed as fraction seconds
     *        (like {@code "15:30.00.25"}) with the minimum number of
     *        digits needed to show the milliseconds without precision lose.
     *        Thus, if the milliseconds happen to be exactly 0, no fraction
     *        seconds will be shown at all.
     * @param timeZone the time zone in which the date/time will be shown. (You
     *        may find {@link DateUtil#UTC} handy here.) Note
     *        that although date-only formats has no time zone offset part,
     *        the result still depends on the time zone, as days start and end
     *        at different points on the time line in different zones.      
     * @param calendarFactory the factory that will create the calendar used
     *        internally for calculations. The point of this parameter is that
     *        creating a new calendar is relatively expensive, so it's desirable
     *        to reuse calendars and only set their time and zone. (This was
     *        tested on Sun JDK 1.6 x86 Win, where it gave 2x-3x speedup.) 
     */
    public static String dateToISO8601String(
            Date date,
            boolean datePart, boolean timePart, boolean offsetPart,
            int accuracy,
            TimeZone timeZone,
            DateToISO8601CalendarFactory calendarFactory) {
        return dateToString(date, datePart, timePart, offsetPart, accuracy, timeZone, false, calendarFactory);
    }

    /**
     * Same as {@link #dateToISO8601String}, but gives XML Schema compliant format.
     */
    public static String dateToXSString(
            Date date,
            boolean datePart, boolean timePart, boolean offsetPart,
            int accuracy,
            TimeZone timeZone,
            DateToISO8601CalendarFactory calendarFactory) {
        return dateToString(date, datePart, timePart, offsetPart, accuracy, timeZone, true, calendarFactory);
    }
    
    private static String dateToString(
            Date date,
            boolean datePart, boolean timePart, boolean offsetPart,
            int accuracy,
            TimeZone timeZone, boolean xsMode,
            DateToISO8601CalendarFactory calendarFactory) {
        if (!xsMode && !timePart && offsetPart) {
            throw new IllegalArgumentException(
                    "ISO 8601:2004 doesn't specify any formats where the "
                    + "offset is shown but the time isn't.");
        }
        
        if (timeZone == null) {
            timeZone = UTC;
        }
        
        GregorianCalendar cal = calendarFactory.get(timeZone, date);

        int maxLength;
        if (!timePart) {
            maxLength = 10 + (xsMode ? 6 : 0);  // YYYY-MM-DD+00:00
        } else {
            if (!datePart) {
                maxLength = 12 + 6;  // HH:MM:SS.mmm+00:00
            } else {
                maxLength = 10 + 1 + 12 + 6;
            }
        }
        char[] res = new char[maxLength];
        int dstIdx = 0;
        
        if (datePart) {
            int x = cal.get(Calendar.YEAR);
            if (x > 0 && cal.get(Calendar.ERA) == GregorianCalendar.BC) {
                x = -x + (xsMode ? 0 : 1);
            }
            if (x >= 0 && x < 9999) {
                res[dstIdx++] = (char) ('0' + x / 1000);
                res[dstIdx++] = (char) ('0' + x % 1000 / 100);
                res[dstIdx++] = (char) ('0' + x % 100 / 10);
                res[dstIdx++] = (char) ('0' + x % 10);
            } else {
                String yearString = String.valueOf(x);
                
                // Re-allocate buffer:
                maxLength = maxLength - 4 + yearString.length();
                res = new char[maxLength];
                
                for (int i = 0; i < yearString.length(); i++) {
                    res[dstIdx++] = yearString.charAt(i);
                }
            }
    
            res[dstIdx++] = '-';
            
            x = cal.get(Calendar.MONTH) + 1;
            dstIdx = append00(res, dstIdx, x);
    
            res[dstIdx++] = '-';
            
            x = cal.get(Calendar.DAY_OF_MONTH);
            dstIdx = append00(res, dstIdx, x);

            if (timePart) {
                res[dstIdx++] = 'T';
            }
        }

        if (timePart) {
            int x = cal.get(Calendar.HOUR_OF_DAY);
            dstIdx = append00(res, dstIdx, x);
    
            if (accuracy >= ACCURACY_MINUTES) {
                res[dstIdx++] = ':';
        
                x = cal.get(Calendar.MINUTE);
                dstIdx = append00(res, dstIdx, x);
        
                if (accuracy >= ACCURACY_SECONDS) {
                    res[dstIdx++] = ':';
            
                    x = cal.get(Calendar.SECOND);
                    dstIdx = append00(res, dstIdx, x);
            
                    if (accuracy >= ACCURACY_MILLISECONDS) {
                        x = cal.get(Calendar.MILLISECOND);
                        int forcedDigits = accuracy == ACCURACY_MILLISECONDS_FORCED ? 3 : 0;
                        if (x != 0 || forcedDigits != 0) {
                            if (x > 999) {
                                // Shouldn't ever happen...
                                throw new RuntimeException(
                                        "Calendar.MILLISECOND > 999");
                            }
                            res[dstIdx++] = '.';
                            do {
                                res[dstIdx++] = (char) ('0' + (x / 100));
                                forcedDigits--;
                                x = x % 100 * 10;
                            } while (x != 0 || forcedDigits > 0);
                        }
                    }
                }
            }
        }

        if (offsetPart) {
            if (timeZone == UTC) {
                res[dstIdx++] = 'Z';
            } else {
                int dt = timeZone.getOffset(date.getTime());
                boolean positive;
                if (dt < 0) {
                    positive = false;
                    dt = -dt;
                } else {
                    positive = true;
                }
                
                dt /= 1000;
                int offS = dt % 60;
                dt /= 60;
                int offM = dt % 60;
                dt /= 60;
                int offH = dt;
                
                if (offS == 0 && offM == 0 && offH == 0) {
                    res[dstIdx++] = 'Z';
                } else {
                    res[dstIdx++] = positive ? '+' : '-';
                    dstIdx = append00(res, dstIdx, offH);
                    res[dstIdx++] = ':';
                    dstIdx = append00(res, dstIdx, offM);
                    if (offS != 0) {
                        res[dstIdx++] = ':';
                        dstIdx = append00(res, dstIdx, offS);
                    }
                }
            }
        }
        
        return new String(res, 0, dstIdx);
    }
    
    /** 
     * Appends a number between 0 and 99 padded to 2 digits.
     */
    private static int append00(char[] res, int dstIdx, int x) {
        res[dstIdx++] = (char) ('0' + x / 10);
        res[dstIdx++] = (char) ('0' + x % 10);
        return dstIdx;
    }
    
    /**
     * Parses an W3C XML Schema date string (not time or date-time).
     * Unlike in ISO 8601:2000 Second Edition, year -1 means B.C 1, and year 0 is invalid. 
     * 
     * @param dateStr the string to parse. 
     * @param defaultTimeZone used if the date doesn't specify the
     *     time zone offset explicitly. Can't be {@code null}.
     * @param calToDateConverter Used internally to calculate the result from the calendar field values.
     *     If you don't have a such object around, you can just use
     *     {@code new }{@link TrivialCalendarFieldsToDateConverter}{@code ()}. 
     * 
     * @throws DateParseException if the date is malformed, or if the time
     *     zone offset is unspecified and the {@code defaultTimeZone} is
     *     {@code null}.
     */
    public static Date parseXSDate(
            String dateStr, TimeZone defaultTimeZone,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException {
        Matcher m = PATTERN_XS_DATE.matcher(dateStr);
        if (!m.matches()) {
            throw new DateParseException("The value didn't match the expected pattern: " + PATTERN_XS_DATE); 
        }
        return parseDate_parseMatcher(
                m, defaultTimeZone, true, calToDateConverter);
    }

    /**
     * Same as {@link #parseXSDate(String, TimeZone, CalendarFieldsToDateConverter)}, but for ISO 8601 dates.
     */
    public static Date parseISO8601Date(
            String dateStr, TimeZone defaultTimeZone,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException {
        Matcher m = PATTERN_ISO8601_EXTENDED_DATE.matcher(dateStr);
        if (!m.matches()) {
            m = PATTERN_ISO8601_BASIC_DATE.matcher(dateStr);
            if (!m.matches()) {
                throw new DateParseException("The value didn't match the expected pattern: "
                            + PATTERN_ISO8601_EXTENDED_DATE + " or "
                            + PATTERN_ISO8601_BASIC_DATE);
            }
        }
        return parseDate_parseMatcher(
                m, defaultTimeZone, false, calToDateConverter);
    }
    
    private static Date parseDate_parseMatcher(
            Matcher m, TimeZone defaultTZ,
            boolean xsMode,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException {
        NullArgumentException.check("defaultTZ", defaultTZ);
        try {
            int year = groupToInt(m.group(1), "year", Integer.MIN_VALUE, Integer.MAX_VALUE);
            
            int era;
            // Starting from ISO 8601:2000 Second Edition, 0001 is AD 1, 0000 is BC 1, -0001 is BC 2.
            // However, according to http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/, XML schemas are based
            // on the earlier version where 0000 didn't exist, and year -1 is BC 1.
            if (year <= 0) {
                era = GregorianCalendar.BC;
                year = -year + (xsMode ? 0 : 1);
                if (year == 0) {
                    throw new DateParseException(MSG_YEAR_0_NOT_ALLOWED);
                }
            } else {
                era = GregorianCalendar.AD;
            }
            
            int month = groupToInt(m.group(2), "month", 1, 12) - 1;
            int day = groupToInt(m.group(3), "day-of-month", 1, 31);

            TimeZone tz = xsMode ? parseMatchingTimeZone(m.group(4), defaultTZ) : defaultTZ;
            
            return calToDateConverter.calculate(era, year, month, day, 0, 0, 0, 0, false, tz);
        } catch (IllegalArgumentException e) {
            // Calendar methods used to throw this for illegal dates.
            throw new DateParseException(
                    "Date calculation faliure. "
                    + "Probably the date is formally correct, but refers "
                    + "to an unexistent date (like February 30)."); 
        }
    }
    
    /**
     * Parses an W3C XML Schema time string (not date or date-time).
     * If the time string doesn't specify the time zone offset explicitly,
     * the value of the {@code defaultTZ} paramter will be used. 
     */  
    public static Date parseXSTime(
            String timeStr, TimeZone defaultTZ, CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException {
        Matcher m = PATTERN_XS_TIME.matcher(timeStr);
        if (!m.matches()) {
            throw new DateParseException("The value didn't match the expected pattern: " + PATTERN_XS_TIME);
        }
        return parseTime_parseMatcher(m, defaultTZ, calToDateConverter);
    }

    /**
     * Same as {@link #parseXSTime(String, TimeZone, CalendarFieldsToDateConverter)} but for ISO 8601 times.
     */
    public static Date parseISO8601Time(
            String timeStr, TimeZone defaultTZ, CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException {
        Matcher m = PATTERN_ISO8601_EXTENDED_TIME.matcher(timeStr);
        if (!m.matches()) {
            m = PATTERN_ISO8601_BASIC_TIME.matcher(timeStr);
            if (!m.matches()) {
                throw new DateParseException("The value didn't match the expected pattern: "
                            + PATTERN_ISO8601_EXTENDED_TIME + " or "
                            + PATTERN_ISO8601_BASIC_TIME);
            }
        }
        return parseTime_parseMatcher(m, defaultTZ, calToDateConverter);
    }
    
    private static Date parseTime_parseMatcher(
            Matcher m, TimeZone defaultTZ,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException {
        NullArgumentException.check("defaultTZ", defaultTZ);
        try {
            // ISO 8601 allows both 00:00 and 24:00,
            // but Calendar.set(...) doesn't if the Calendar is not lenient.
            int hours = groupToInt(m.group(1), "hour-of-day", 0, 24);
            boolean hourWas24;
            if (hours == 24) {
                hours = 0;
                hourWas24 = true;
                // And a day will be added later...
            } else {
                hourWas24 = false;
            }
            
            final String minutesStr = m.group(2);
            int minutes = minutesStr != null ? groupToInt(minutesStr, "minute", 0, 59) : 0;
            
            final String secsStr = m.group(3);
            // Allow 60 because of leap seconds
            int secs = secsStr != null ? groupToInt(secsStr, "second", 0, 60) : 0;
            
            int millisecs = groupToMillisecond(m.group(4));
            
            // As a time is just the distance from the beginning of the day,
            // the time-zone offest should be 0 usually.
            TimeZone tz = parseMatchingTimeZone(m.group(5), defaultTZ);
            
            // Continue handling the 24:00 special case
            int day;
            if (hourWas24) {
                if (minutes == 0 && secs == 0 && millisecs == 0) {
                    day = 2;
                } else {
                    throw new DateParseException(
                            "Hour 24 is only allowed in the case of "
                            + "midnight."); 
                }
            } else {
                day = 1;
            }
            
            return calToDateConverter.calculate(
                    GregorianCalendar.AD, 1970, 0, day, hours, minutes, secs, millisecs, false, tz);
        } catch (IllegalArgumentException e) {
            // Calendar methods used to throw this for illegal dates.
            throw new DateParseException(
                    "Unexpected time calculation faliure."); 
        }
    }
    
    /**
     * Parses an W3C XML Schema date-time string (not date or time).
     * Unlike in ISO 8601:2000 Second Edition, year -1 means B.C 1, and year 0 is invalid. 
     * 
     * @param dateTimeStr the string to parse. 
     * @param defaultTZ used if the dateTime doesn't specify the
     *     time zone offset explicitly. Can't be {@code null}. 
     * 
     * @throws DateParseException if the dateTime is malformed.
     */
    public static Date parseXSDateTime(
            String dateTimeStr, TimeZone defaultTZ, CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException {
        Matcher m = PATTERN_XS_DATE_TIME.matcher(dateTimeStr);
        if (!m.matches()) {
            throw new DateParseException(
                    "The value didn't match the expected pattern: " + PATTERN_XS_DATE_TIME);
        }
        return parseDateTime_parseMatcher(
                m, defaultTZ, true, calToDateConverter);
    }

    /**
     * Same as {@link #parseXSDateTime(String, TimeZone, CalendarFieldsToDateConverter)} but for ISO 8601 format. 
     */
    public static Date parseISO8601DateTime(
            String dateTimeStr, TimeZone defaultTZ, CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException {
        Matcher m = PATTERN_ISO8601_EXTENDED_DATE_TIME.matcher(dateTimeStr);
        if (!m.matches()) {
            m = PATTERN_ISO8601_BASIC_DATE_TIME.matcher(dateTimeStr);
            if (!m.matches()) {
                throw new DateParseException("The value (" + dateTimeStr + ") didn't match the expected pattern: "
                            + PATTERN_ISO8601_EXTENDED_DATE_TIME + " or "
                            + PATTERN_ISO8601_BASIC_DATE_TIME);
            }
        }
        return parseDateTime_parseMatcher(
                m, defaultTZ, false, calToDateConverter);
    }
    
    private static Date parseDateTime_parseMatcher(
            Matcher m, TimeZone defaultTZ,
            boolean xsMode,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException {
        NullArgumentException.check("defaultTZ", defaultTZ);
        try {
            int year = groupToInt(m.group(1), "year", Integer.MIN_VALUE, Integer.MAX_VALUE);
            
            int era;
            // Starting from ISO 8601:2000 Second Edition, 0001 is AD 1, 0000 is BC 1, -0001 is BC 2.
            // However, according to http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/, XML schemas are based
            // on the earlier version where 0000 didn't exist, and year -1 is BC 1.
            if (year <= 0) {
                era = GregorianCalendar.BC;
                year = -year + (xsMode ? 0 : 1);
                if (year == 0) {
                    throw new DateParseException(MSG_YEAR_0_NOT_ALLOWED);
                }
            } else {
                era = GregorianCalendar.AD;
            }
            
            int month = groupToInt(m.group(2), "month", 1, 12) - 1;
            int day = groupToInt(m.group(3), "day-of-month", 1, 31);
            
            // ISO 8601 allows both 00:00 and 24:00,
            // but cal.set(...) doesn't if the Calendar is not lenient.
            int hours = groupToInt(m.group(4), "hour-of-day", 0, 24);
            boolean hourWas24;
            if (hours == 24) {
                hours = 0;
                hourWas24 = true;
                // And a day will be added later...
            } else {
                hourWas24 = false;
            }
            
            final String minutesStr = m.group(5);
            int minutes = minutesStr != null ? groupToInt(minutesStr, "minute", 0, 59) : 0;
            
            final String secsStr = m.group(6);
            // Allow 60 because of leap seconds
            int secs = secsStr != null ? groupToInt(secsStr, "second", 0, 60) : 0;
            
            int millisecs = groupToMillisecond(m.group(7));
            
            // As a time is just the distance from the beginning of the day,
            // the time-zone offest should be 0 usually.
            TimeZone tz = parseMatchingTimeZone(m.group(8), defaultTZ);
            
            // Continue handling the 24:00 specail case
            if (hourWas24) {
                if (minutes != 0 || secs != 0 || millisecs != 0) {
                    throw new DateParseException(
                            "Hour 24 is only allowed in the case of "
                            + "midnight."); 
                }
            }
            
            return calToDateConverter.calculate(
                    era, year, month, day, hours, minutes, secs, millisecs, hourWas24, tz);
        } catch (IllegalArgumentException e) {
            // Calendar methods used to throw this for illegal dates.
            throw new DateParseException(
                    "Date-time calculation faliure. "
                    + "Probably the date-time is formally correct, but "
                    + "refers to an unexistent date-time "
                    + "(like February 30)."); 
        }
    }

    /**
     * Parses the time zone part from a W3C XML Schema date/time/dateTime. 
     * @throws DateParseException if the zone is malformed.
     */
    public static TimeZone parseXSTimeZone(String timeZoneStr)
            throws DateParseException {
        Matcher m = PATTERN_XS_TIME_ZONE.matcher(timeZoneStr);
        if (!m.matches()) {
            throw new DateParseException(
                    "The time zone offset didn't match the expected pattern: " + PATTERN_XS_TIME_ZONE);
        }
        return parseMatchingTimeZone(timeZoneStr, null);
    }

    private static int groupToInt(String g, String gName,
            int min, int max)
            throws DateParseException {
        if (g == null) {
            throw new DateParseException("The " + gName + " part "
                    + "is missing.");
        }

        int start;
        
        // Remove minus sign, so we can remove the 0-s later:
        boolean negative;
        if (g.startsWith("-")) {
            negative = true;
            start = 1;
        } else {
            negative = false;
            start = 0;
        }
        
        // Remove leading 0-s:
        while (start < g.length() - 1 && g.charAt(start) == '0') {
            start++;
        }
        if (start != 0) {
            g = g.substring(start);
        }
        
        try {
            int r = Integer.parseInt(g);
            if (negative) {
                r = -r;
            }
            if (r < min) {
                throw new DateParseException("The " + gName + " part "
                    + "must be at least " + min + ".");
            }
            if (r > max) {
                throw new DateParseException("The " + gName + " part "
                    + "can't be more than " + max + ".");
            }
            return r;
        } catch (NumberFormatException e) {
            throw new DateParseException("The " + gName + " part "
                    + "is a malformed integer.");
        }
    }

    private static TimeZone parseMatchingTimeZone(
            String s, TimeZone defaultZone)
            throws DateParseException {
        if (s == null) {
            return defaultZone;
        }
        if (s.equals("Z")) {
            return DateUtil.UTC;
        }
        
        StringBuffer sb = new StringBuffer(9);
        sb.append("GMT");
        sb.append(s.charAt(0));
        
        String h = s.substring(1, 3);
        groupToInt(h, "offset-hours", 0, 23);
        sb.append(h);
        
        String m;
        int ln = s.length();
        if (ln > 3) {
            int startIdx = s.charAt(3) == ':' ? 4 : 3;
            m = s.substring(startIdx, startIdx + 2);
            groupToInt(m, "offset-minutes", 0, 59);
            sb.append(':');
            sb.append(m);
        }
        
        return TimeZone.getTimeZone(sb.toString());
    }

    private static int groupToMillisecond(String g)
            throws DateParseException {
        if (g == null) {
            return 0;
        }
        
        if (g.length() > 3) {
            g = g.substring(0, 3);
        }
        int i = groupToInt(g, "partial-seconds", 0, Integer.MAX_VALUE);
        return g.length() == 1 ? i * 100 : (g.length() == 2 ? i * 10 : i);
    }
    
    /**
     * Used internally by {@link DateUtil}; don't use its implementations for
     * anything else.
     */
    public interface DateToISO8601CalendarFactory {
        
        /**
         * Returns a {@link GregorianCalendar} with the desired time zone and
         * time and US locale. The returned calendar is used as read-only.
         * It must be guaranteed that within a thread the instance returned last time
         * is not in use anymore when this method is called again.
         */
        GregorianCalendar get(TimeZone tz, Date date);
        
    }

    /**
     * Used internally by {@link DateUtil}; don't use its implementations for anything else.
     */
    public interface CalendarFieldsToDateConverter {

        /**
         * Calculates the {@link Date} from the specified calendar fields.
         */
        Date calculate(int era, int year, int month, int day, int hours, int minutes, int secs, int millisecs,
                boolean addOneDay,
                TimeZone tz);

    }

    /**
     * Non-thread-safe factory that hard-references a calendar internally.
     */
    public static final class TrivialDateToISO8601CalendarFactory
            implements DateToISO8601CalendarFactory {
        
        private GregorianCalendar calendar;
        private TimeZone lastlySetTimeZone;
    
        public GregorianCalendar get(TimeZone tz, Date date) {
            if (calendar == null) {
                calendar = new GregorianCalendar(tz, Locale.US);
                calendar.setGregorianChange(new Date(Long.MIN_VALUE));  // never use Julian calendar
            } else {
                // At least on Java 6, calendar.getTimeZone is slow due to a bug, so we need lastlySetTimeZone.
                if (lastlySetTimeZone != tz) {  // Deliberately `!=` instead of `!<...>.equals()`  
                    calendar.setTimeZone(tz);
                    lastlySetTimeZone = tz;
                }
            }
            calendar.setTime(date);
            return calendar;
        }
        
    }

    /**
     * Non-thread-safe implementation that hard-references a calendar internally.
     */
    public static final class TrivialCalendarFieldsToDateConverter
            implements CalendarFieldsToDateConverter {

        private GregorianCalendar calendar;
        private TimeZone lastlySetTimeZone;

        public Date calculate(int era, int year, int month, int day, int hours, int minutes, int secs, int millisecs,
                boolean addOneDay, TimeZone tz) {
            if (calendar == null) {
                calendar = new GregorianCalendar(tz, Locale.US);
                calendar.setLenient(false);
                calendar.setGregorianChange(new Date(Long.MIN_VALUE));  // never use Julian calendar
            } else {
                // At least on Java 6, calendar.getTimeZone is slow due to a bug, so we need lastlySetTimeZone.
                if (lastlySetTimeZone != tz) {  // Deliberately `!=` instead of `!<...>.equals()`  
                    calendar.setTimeZone(tz);
                    lastlySetTimeZone = tz;
                }
            }

            calendar.set(Calendar.ERA, era);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hours);
            calendar.set(Calendar.MINUTE, minutes);
            calendar.set(Calendar.SECOND, secs);
            calendar.set(Calendar.MILLISECOND, millisecs);
            if (addOneDay) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            return calendar.getTime();
        }

    }
    
    public static final class DateParseException extends ParseException {
        
        public DateParseException(String message) {
            super(message, 0);
        }
        
    }
        
}
