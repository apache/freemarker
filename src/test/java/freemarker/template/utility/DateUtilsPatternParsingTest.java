/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.template.utility;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Move pattern parsing related tests from {@link DateUtilTest} to here.
 */
public class DateUtilsPatternParsingTest {
    private static final ZoneId SAMPLE_ZONE_ID = ZoneId.of("America/New_York");
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");
    private static final ZonedDateTime SAMPLE_ZDT
            = ZonedDateTime.of(2021, 12, 25, 13, 30, 55, 534200000, SAMPLE_ZONE_ID);
    private static final ZonedDateTime[] SAMPLE_ZDTS = new ZonedDateTime[] {
            SAMPLE_ZDT,
            ZonedDateTime.of(2009, 8, 7, 6, 5, 4, 3, UTC_ZONE_ID),
            ZonedDateTime.of(2010, 8, 7, 6, 5, 4, 300000000, ZoneId.ofOffset("GMT", ZoneOffset.ofHoursMinutes(10, 30))),
            ZonedDateTime.of(2011, 8, 7, 6, 5, 4, 30000000, ZoneId.ofOffset("GMT", ZoneOffset.ofHoursMinutes(-10, -30))),
            ZonedDateTime.of(2012, 8, 7, 6, 5, 4, 3000000, ZoneId.ofOffset("GMT", ZoneOffset.ofHours(1))),
            ZonedDateTime.of(2013, 8, 7, 6, 5, 4, 300000, ZoneId.ofOffset("GMT", ZoneOffset.ofHours(-1))),
            ZonedDateTime.of(1995, 2, 28, 1, 30, 55, 0, SAMPLE_ZONE_ID),
            ZonedDateTime.of(12345, 1, 1, 0, 0, 0, 0, UTC_ZONE_ID),
    };

    // Most likely supported on different test systems
    private static final Locale SAMPLE_LOCALE = Locale.US;

    private static final List<Locale> SAMPLE_LOCALES;
    static {
        LocalDate localDate = LocalDate.of(2021, 12, 1);
        SAMPLE_LOCALES = ImmutableList.of(
                // Locales picked more or less arbitrarily, in alphabetical order
                Locale.CHINA,
                new Locale("ar", "AE"),
                new Locale("fi", "FI"),
                Locale.GERMAN,
                new Locale("hi", "IN"),
                Locale.JAPANESE,
                Locale.ROOT,
                new Locale("ru", "RU"),
                Locale.US,
                new Locale("th", "TH") // Uses buddhist calendar
        ).stream()
                .filter(locale -> !(
                        new DateTimeFormatterBuilder()
                                .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT_STANDALONE)
                                .toFormatter(locale)
                                .format(localDate))
                        .equals("12"))
                .collect(Collectors.toList());
        System.out.println("!!T Sample locales: " + SAMPLE_LOCALES); // TODO Remove this
    }

    @Test
    public void testHasEnoughSampleLocales() {
        if (SAMPLE_LOCALES.size() < 4) {
            throw new AssertionError("Too many locales were filtered out from SAMPLE_LOCALE. " +
                    "We only have these left: " + SAMPLE_LOCALES);
        }
    }

    @Test
    public void testBasics() {
        for (String pattern : new String[] {
                "yyyy-MM-dd HH:mm:ss.SSS",
                "'Date:' yy MMM d (E), 'Time:' hh:mm a, 'Zone:' z"}) {
            for (ZonedDateTime zdt : SAMPLE_ZDTS) {
                assertSDFAndDTFOutputsEqual(pattern, zdt, SAMPLE_LOCALE);
            }
        }
    }

    @Test
    public void testAllLettersAndWidths() {
        for (String letter : new String[] {
                "G", "y", "Y", "M", "L", "w", "W", "D", "d", "F", "E", "u", "a", "H", "k", "K", "h", "m", "s", "S",
                "z", "Z", "X"}) {
            for (int width = 1; width <= 6; width++) {
                if (letter.equals("X") && width > 3) {
                    // Not supported by SimpleDateFormat.
                    continue;
                }
                String pattern = StringUtils.repeat(letter, width);
                for (ZonedDateTime zdt : SAMPLE_ZDTS) {
                    for (Locale locale : SAMPLE_LOCALES) {
                        assertSDFAndDTFOutputsEqual(pattern, zdt, locale);
                    }
                }
            }
        }
    }

    @Test
    public void testEscaping() {
        assertSDFAndDTFOutputsEqual("''", SAMPLE_ZDT, SAMPLE_LOCALE);
        assertSDFAndDTFOutputsEqual("''''", SAMPLE_ZDT, SAMPLE_LOCALE);
        assertSDFAndDTFOutputsEqual("'v'y'v'", SAMPLE_ZDT, SAMPLE_LOCALE);
        assertSDFAndDTFOutputsEqual("'v''v'", SAMPLE_ZDT, SAMPLE_LOCALE);
        assertSDFAndDTFOutputsEqual("'v''''v'", SAMPLE_ZDT, SAMPLE_LOCALE);
    }

    @Test
    public void testWeekBasedNumericalFields() {
        // SDF always starts the week with Monday (numerical value 1), regardless of Locale.
        ZonedDateTime zdt = SAMPLE_ZDT;
        for (int i = 0; i < 1000; i++) {
            for (Locale locale : SAMPLE_LOCALES) {
                assertSDFAndDTFOutputsEqual("w", zdt, locale);
                assertSDFAndDTFOutputsEqual("W", zdt, locale);
                assertSDFAndDTFOutputsEqual("u", zdt, locale);
            }
            zdt = zdt.plusDays(1);
        }
    }


    @Test
    public void testStandaloneOrNot() {
        for (Locale locale : SAMPLE_LOCALES) {
            assertSDFAndDTFOutputsEqual("MMM", SAMPLE_ZDT, locale);
            assertSDFAndDTFOutputsEqual("y MMM", SAMPLE_ZDT, locale);
            assertSDFAndDTFOutputsEqual("MMMM", SAMPLE_ZDT, locale);
            assertSDFAndDTFOutputsEqual("y MMMM", SAMPLE_ZDT, locale);
        }
    }

    @Test
    public void testCalendars() {
        Locale baseLocale = new Locale("th", "TH");
        for (String forcedCalendarType : new String[] {null, "buddhist", "japanese", "gregory"}) {
            Locale locale = forcedCalendarType != null
                    ? new Locale.Builder()
                            .setLocale(baseLocale)
                            .setUnicodeLocaleKeyword("ca", forcedCalendarType)
                            .build()
                    : baseLocale;
            assertSDFAndDTFOutputsEqual("y M d", SAMPLE_ZDT, locale);
        }
    }

    @Test
    public void testHistoricalDate() throws ParseException {
        LocalDate temporal = LocalDate.of(-123, 4, 5);

        // There are historical calendar changes in play that depend on locale. j.u did take those into account, but
        // java.time doesn't anymore, and we can't realistically fix that. So we will limit the locales we test for to
        // some that used Julian calendar till 1528, and then Gregorian calendar.

        // We can't convert the LocalDate to Date as usual, because j.t assumes Gregorian calendar even before 1528.
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("y M d", Locale.US);
        TimeZone timeZone = TimeZone.getTimeZone(UTC_ZONE_ID);
        simpleDateFormat.setTimeZone(timeZone);
        Date date = simpleDateFormat.parse("-123 4 5"); // Interpreted with Julian calendar

        for (Locale locale : new Locale[] {Locale.US, Locale.GERMAN}) {
            assertSDFAndDTFOutputsEqual("y M d G", date, timeZone, temporal, locale);
        }
    }

    @Test
    public void testInvalidPatternExceptions() {
        try {
            DateUtil.dateTimeFormatterFromSimpleDateFormatPattern("y v", SAMPLE_LOCALE);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("\"v\""));
        }

        try {
            DateUtil.dateTimeFormatterFromSimpleDateFormatPattern("XXXX", SAMPLE_LOCALE);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("4"));
        }
    }

    @Test
    public void testParsingBasics() throws ParseException {
        assertEquals(
                LocalDateTime.of(2021, 12, 23, 1, 2, 3),
                LocalDateTime.from(
                        DateUtil.dateTimeFormatterFromSimpleDateFormatPattern("yyyyMMddHHmmss", SAMPLE_LOCALE)
                                .parse("20211223010203")));
    }

    @Test
    public void testParsingWidthRestrictions() throws ParseException {
        // Year allows more digits than specified:
        assertLocalDateParsing(
                LocalDate.of(12021, 2, 3),
                "yyyyMMdd", "120210203", SAMPLE_LOCALE);
        assertLocalDateParsing(
                LocalDate.of(321, 2, 3),
                "yMMdd", "3210203", SAMPLE_LOCALE);

        // But not less:
        assertLocalDateParsingFails("yyyyMMdd", "3210203", SAMPLE_LOCALE);
        // SimpleDateFormat is more lenient here:
        new SimpleDateFormat("yyyyMMdd", SAMPLE_LOCALE).parse("3210203");
        // But being strict is certainly a safer, so we don't mimic SimpleDateFormat behavior in this case.

        // Year has arbitrary 0 padding, month and day on has that up to 2 digits:
        assertLocalDateParsing(
                LocalDate.of(2021, 1, 2),
                "y-M-d", "2021-1-2", SAMPLE_LOCALE);
        assertLocalDateParsing(
                LocalDate.of(2021, 10, 20),
                "y-M-d", "2021-10-20", SAMPLE_LOCALE);
        assertLocalDateParsing(
                LocalDate.of(2021, 1, 2),
                "y-M-d", "02021-01-02", SAMPLE_LOCALE);

        assertLocalDateParsingFails("y-M-d", "2021-010-20", SAMPLE_LOCALE);
        assertLocalDateParsingFails("y-M-d", "2021-10-020", SAMPLE_LOCALE);
    }

    @Test
    public void testParsingCaseInsensitive() throws ParseException {
        assertLocalDateParsing(
                LocalDate.of(2021, 1, 2),
                "y-MMM-d", "2021-jAn-02", SAMPLE_LOCALE);
        // SimpleDateFormat is case-insensitive too:
        new SimpleDateFormat("y-MMM-d", SAMPLE_LOCALE).parse("2021-jAn-02");
    }

    @Test
    public void testParsingLocale() throws ParseException {
        assertLocalDateParsing(
                LocalDate.of(2021, 1, 12),
                "y-MMM-d", "2021-\u044F\u043D\u0432-12", new Locale("ru", "RU"));
        assertLocalDateParsing(
                LocalDate.of(2021, 1, 12),
                "y-MMM-d", "\u0968\u0966\u0968\u0967-\u091C\u0928\u0935\u0930\u0940-\u0967\u0968",
                new Locale("hi", "IN"));
    }

    private void assertSDFAndDTFOutputsEqual(String pattern, ZonedDateTime zdt, Locale locale) {
        assertSDFAndDTFOutputsEqual(pattern, Date.from(zdt.toInstant()), TimeZone.getTimeZone(zdt.getZone()), zdt, locale);
    }

    private void assertSDFAndDTFOutputsEqual(String pattern, Date date, TimeZone timeZone, Temporal temporal, Locale locale) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
        sdf.setTimeZone(timeZone);

        DateTimeFormatter dtf = DateUtil.dateTimeFormatterFromSimpleDateFormatPattern(pattern, locale);

        String sdfOutput = sdf.format(date);
        String dtfOutput = dtf.format(temporal);
        if (!sdfOutput.equals(dtfOutput)) {
            fail("Output of\n"
                    + "SDF(" + StringUtil.jQuote(pattern) + ", " + date.toInstant().atZone(timeZone.toZoneId()) + "), and\n"
                    + "DTF(" + dtf + ", " + temporal + ") differs, with locale " + locale + ":\n"
                    + "SDF: " + sdfOutput + "\n"
                    + "DTF: " + dtfOutput);
        }
    }

    private void assertLocalDateParsing(LocalDate temporal, String pattern, String string, Locale locale) {
        try {
            assertEquals(
                    temporal,
                    parseLocalDate(pattern, string, locale));
        } catch (DateTimeParseException e) {
            throw new AssertionError(
                "Failed to parse " + StringUtil.jQuote(string)
                        + " with pattern " + StringUtil.jQuote(pattern)
                        + " and locale " + locale + ".",
                    e);
        }
    }

    private void assertLocalDateParsingFails(String pattern, String string, Locale locale) {
        try {
            parseLocalDate(pattern, string, locale);
            fail("Parsing was expected to fail for: "
                    + StringUtil.jQuote(pattern) + ", " + StringUtil.jQuote(string) + ", " + locale);
        } catch (DateTimeParseException e) {
            // Expected
        }
    }

    private LocalDate parseLocalDate(String pattern, String string, Locale locale) {
        return LocalDate.from(
                DateUtil.dateTimeFormatterFromSimpleDateFormatPattern(pattern, locale)
                        .parse(string));
    }

}
