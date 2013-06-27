/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.template.utility;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {

    public static final int ACCURACY_HOURS = 4;
    public static final int ACCURACY_MINUTES = 5;
    public static final int ACCURACY_SECONDS = 6;
    public static final int ACCURACY_MILLISECONDS = 7;
    
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    
    private final static TimeZoneOffsetCalculator TIME_ZONE_OFFSET_CALCULATOR
            = getTimeZoneOffsetCalculator();
    
    private static TimeZoneOffsetCalculator getTimeZoneOffsetCalculator() {
        try {
            Class cl = Class.forName(
                    "freemarker.template.utility.J2SE14TimeZoneOffsetCalculator");
            return (TimeZoneOffsetCalculator) cl.newInstance();
        } catch (final Throwable e) {
            return new TimeZoneOffsetCalculator() {
                public int getOffset(TimeZone tz, Date date) {
                    throw new RuntimeException(
                            "Failed to create TimeZoneOffsetCalculator. " +
                            "Note that this feature requires at least " +
                            "Java 1.4.\nCause exception: " + e);
                }
                
            };
        }
    }
    
    private DateUtil() {
        // can't be instantiated
    }
    
    /**
     * Returns the time zone object for the name (or ID). This differs from
     * {@link TimeZone#getTimeZone(String)} in that the latest returns GMT
     * if it doesn't recognize the name, while this throws an
     * {@link UnrecognizedTimeZoneException}.
     * 
     * @throws UnrecognizedTimeZoneException 
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
     * Format a date, time or date+time with one of the ISO 8601 extended
     * formats. Examples of possible outputs:
     * {@code "2005-11-27T15:30:00+02:00"}, {@code "2005-11-27"},
     * {@code "15:30:00Z"}. Note the {@code ":00"} in the time zone offset;
     * this is not required by ISO 8601, but included for compatibility with
     * the XML Schema date/time formats.
     * 
     * This method is thread-safe.
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
     *        at different points in time in different zones.      
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
        if (!timePart && offsetPart) {
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
            maxLength = 10;  // YYYY-MM-DD
        } else {
            if (!datePart) {
                maxLength = 18;  // HH:MM:SS.mmm+00:00
            } else {
                maxLength = 10 + 1 + 18;
            }
        }
        char[] res = new char[maxLength];
        int dstIdx = 0;
        
        if (datePart) {
            int x = cal.get(Calendar.YEAR);
            if (x > 0 && cal.get(Calendar.ERA) == GregorianCalendar.BC) {
                x = -x + 1;
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
                        if (x != 0) {
                            if (x > 999) {
                                // Shouldn't ever happen...
                                throw new RuntimeException(
                                        "Calendar.MILLISECOND > 999");
                            }
                            res[dstIdx++] = '.';
                            do {
                                res[dstIdx++] = (char) ('0' + (x / 100));
                                x = x % 100 * 10;
                            } while (x != 0);
                        }
                    }
                }
            }
        }

        if (offsetPart) {
            if (timeZone == UTC) {
                res[dstIdx++] = 'Z';
            } else {
                int dt = TIME_ZONE_OFFSET_CALCULATOR.getOffset(timeZone, date);
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
     * Used internally by {@link DateUtil}; don't use it's implementations for
     * anything else.
     */
    public interface DateToISO8601CalendarFactory {
        
        /**
         * Returns a {@link GregorianCalendar} with the desired time zone and
         * time and US locale. The returned calendar is used as read-only.
         * It's guaranteed that within a thread the instance returned last time
         * is not in use anymore when this method is called again. 
         */
        GregorianCalendar get(TimeZone tz, Date date);
    }
    
    /**
     * Non-thread-safe factory that hard-references a calendar internally.
     */
    public static final class TrivialDateToISO8601CalendarFactory
            implements DateToISO8601CalendarFactory {
        
        private GregorianCalendar calendar;

        public GregorianCalendar get(TimeZone tz, Date date) {
            if (calendar == null) {
                calendar = new GregorianCalendar(tz, Locale.US);
            } else {
                calendar.setTimeZone(tz);
            }
            calendar.setTime(date);
            return calendar;
        }
        
        
    }
    
    interface TimeZoneOffsetCalculator {
        int getOffset(TimeZone tz, Date date);
    }

}
