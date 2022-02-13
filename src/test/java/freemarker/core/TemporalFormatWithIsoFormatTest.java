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

package freemarker.core;

import static freemarker.template.utility.StringUtil.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

import org.junit.Test;

import freemarker.template.TemplateException;
import freemarker.template.utility.DateUtil;

public class TemporalFormatWithIsoFormatTest extends AbstractTemporalFormatTest {

    private static final Consumer<Configurable> ISO_DATE_CONFIGURATOR = conf -> conf.setDateFormat("iso");
    private static final Consumer<Configurable> ISO_TIME_CONFIGURATOR = conf -> conf.setTimeFormat("iso");
    private static final Consumer<Configurable> ISO_DATE_TIME_CONFIGURATOR = conf -> conf.setDateTimeFormat("iso");

    private static Consumer<Configurable> isoDateTimeConfigurator(TimeZone timeZone) {
        return conf -> { ISO_DATE_TIME_CONFIGURATOR.accept(conf); conf.setTimeZone(timeZone); };
    }
    private static final Consumer<Configurable> ISO_YEAR_MONTH_CONFIGURATOR = conf -> conf.setYearMonthFormat("iso");
    private static final Consumer<Configurable> ISO_YEAR_CONFIGURATOR = conf -> conf.setYearFormat("iso");

    @Test
    public void testFormatOffsetTime() throws TemplateException, IOException {
        assertEquals(
                "13:01:02Z",
                formatTemporal(
                        ISO_TIME_CONFIGURATOR,
                        OffsetTime.of(LocalTime.of(13, 1, 2), ZoneOffset.UTC)));
        assertEquals(
                "13:01:02+01:00",
                formatTemporal(
                        ISO_TIME_CONFIGURATOR,
                        OffsetTime.of(LocalTime.of(13, 1, 2), ZoneOffset.ofHours(1))));
        assertEquals(
                "13:00:00-02:30",
                formatTemporal(
                        ISO_TIME_CONFIGURATOR,
                        OffsetTime.of(LocalTime.of(13, 0, 0), ZoneOffset.ofHoursMinutesSeconds(-2, -30, 0))));
        assertEquals(
                "13:00:00.0123Z",
                formatTemporal(
                        ISO_TIME_CONFIGURATOR,
                        OffsetTime.of(LocalTime.of(13, 0, 0, 12_300_000), ZoneOffset.UTC)));
        assertEquals(
                "04:51:52.3Z",
                formatTemporal(
                        ISO_TIME_CONFIGURATOR,
                        OffsetTime.of(LocalTime.of(4, 51, 52, 300_000_000), ZoneOffset.UTC)));
    }

    @Test
    public void testFormatLocalTime() throws TemplateException, IOException {
        assertEquals(
                "13:01:02",
                formatTemporal(
                        ISO_TIME_CONFIGURATOR,
                        LocalTime.of(13, 1, 2)));
        assertEquals(
                "13:00:00.0123",
                formatTemporal(
                        ISO_TIME_CONFIGURATOR,
                        LocalTime.of(13, 0, 0, 12_300_000)));
        assertEquals(
                "04:51:52.3",
                formatTemporal(
                        ISO_TIME_CONFIGURATOR,
                        LocalTime.of(4, 51, 52, 300_000_000)));
    }

    @Test
    public void testFormatLocalDateTime() throws TemplateException, IOException {
        assertEquals(
                "2021-12-11T13:01:02",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        LocalDateTime.of(2021, 12, 11, 13, 1, 2, 0)));
        assertEquals(
                "2021-12-11T13:01:02.0123",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        LocalDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000)));
        assertEquals(
                "2021-02-03T04:51:52.3",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        LocalDateTime.of(2021, 2, 3, 4, 51, 52, 300_000_000)));
    }

    @Test
    public void testFormatOffsetDateTime() throws TemplateException, IOException {
        assertEquals(
                "2021-12-11T13:01:02Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.UTC)));
        assertEquals(
                "2021-12-11T13:01:02+01:00",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.ofHours(1))));
        assertEquals(
                "2021-12-11T13:01:02-02:30",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.ofHoursMinutesSeconds(-2, -30, 0))));
        assertEquals(
                "2021-12-11T13:01:02.0123Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000, ZoneOffset.UTC)));
        assertEquals(
                "2021-02-03T04:51:52.3Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        OffsetDateTime.of(2021, 2, 3, 4, 51, 52, 300_000_000, ZoneOffset.UTC)));
    }

    @Test
    public void testFormatZonedDateTime() throws TemplateException, IOException {
        assertEquals(
                "2021-12-11T13:01:02Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        ZonedDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.UTC)));
        ZoneId zoneId = ZoneId.of("America/New_York");
        assertEquals(
                "2021-12-11T13:01:02-05:00",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        ZonedDateTime.of(2021, 12, 11, 13, 1, 2, 0, zoneId)));
        assertEquals(
                "2021-07-11T13:01:02-04:00",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        ZonedDateTime.of(2021, 7, 11, 13, 1, 2, 0, zoneId)));
        assertEquals(
                "2021-12-11T13:01:02-02:30",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.ofHoursMinutesSeconds(-2, -30, 0))));
        assertEquals(
                "2021-12-11T13:01:02.0123Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000, ZoneOffset.UTC)));
        assertEquals(
                "2021-02-03T04:51:52.3Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURATOR,
                        OffsetDateTime.of(2021, 2, 3, 4, 51, 52, 300_000_000, ZoneOffset.UTC)));
    }

    @Test
    public void testFormatInstant() throws TemplateException, IOException {
        assertEquals(
                "2021-12-11T13:01:02Z",
                formatTemporal(
                        isoDateTimeConfigurator(DateUtil.UTC),
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.UTC)
                                .toInstant()));
        assertEquals(
                "2021-12-11T13:01:02+01:00",
                formatTemporal(
                        isoDateTimeConfigurator(TimeZone.getTimeZone("GMT+01")),
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.ofHours(1))
                                .toInstant()));
        assertEquals(
                "2021-12-11T13:01:02-02:30",
                formatTemporal(
                        isoDateTimeConfigurator(TimeZone.getTimeZone("GMT-02:30")),
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.ofHoursMinutesSeconds(-2, -30, 0))
                                .toInstant()));
        assertEquals(
                "2021-12-11T13:01:02.0123Z",
                formatTemporal(
                        isoDateTimeConfigurator(DateUtil.UTC),
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000, ZoneOffset.UTC)
                                .toInstant()));
        assertEquals(
                "2021-02-03T04:51:52.3Z",
                formatTemporal(
                        isoDateTimeConfigurator(DateUtil.UTC),
                        OffsetDateTime.of(2021, 2, 3, 4, 51, 52, 300_000_000, ZoneOffset.UTC)
                                .toInstant()));
    }

    @Test
    public void testFormatLocalDate() throws TemplateException, IOException {
        assertEquals(
                "2021-12-11",
                formatTemporal(
                        ISO_DATE_CONFIGURATOR,
                        LocalDate.of(2021, 12, 11)));
    }

    @Test
    public void testFormatYearMonth() throws TemplateException, IOException {
        assertEquals(
                "2021-12",
                formatTemporal(
                        ISO_YEAR_MONTH_CONFIGURATOR,
                        YearMonth.of(2021, 12)));
        assertEquals(
                "1995-01",
                formatTemporal(
                        ISO_YEAR_MONTH_CONFIGURATOR,
                        YearMonth.of(1995, 1)));
    }

    @Test
    public void testFormatYear() throws TemplateException, IOException {
        assertEquals(
                "2021",
                formatTemporal(
                        ISO_YEAR_CONFIGURATOR,
                        Year.of(2021)));
        assertEquals(
                "1995",
                formatTemporal(
                        ISO_YEAR_CONFIGURATOR,
                        Year.of(1995)));
    }

    @Test
    public void testParseOffsetDateTime() throws TemplateException, TemplateValueFormatException {
        testParseOffsetDateTimeAndInstant(OffsetDateTime.class);
    }

    @Test
    public void testParseInstant() throws TemplateException, TemplateValueFormatException {
        testParseOffsetDateTimeAndInstant(Instant.class);
    }

    @Test
    public void testParseZonedDateTime() throws TemplateException, TemplateValueFormatException {
        testParseOffsetDateTimeAndInstant(ZonedDateTime.class);
    }

    private Temporal convertToClass(OffsetDateTime offsetDateTime, Class<? extends Temporal> temporalClass) {
        if (temporalClass == OffsetDateTime.class) {
            return offsetDateTime;
        }
        if (temporalClass == Instant.class) {
            return offsetDateTime.toInstant();
        }
        if (temporalClass == ZonedDateTime.class) {
            return offsetDateTime.toZonedDateTime();
        }
        throw new IllegalArgumentException();
    }

    private void testParseOffsetDateTimeAndInstant(Class<? extends Temporal> temporalClass)
            throws TemplateException, TemplateValueFormatException {
        // ISO extended and ISO basic format:
        for (String parsedString : new String[]{"2021-12-11T13:01:02.0123Z", "20211211T130102.0123Z"}) {
            assertParsingResults(
                    ISO_DATE_TIME_CONFIGURATOR,
                    parsedString,
                    OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000, ZoneOffset.UTC)) ;
        }

        // Optional parts:
        for (String parsedString : new String[] {
                "2021-12-11T13:00:00.0+02:00",
                "2021-12-11T13:00:00+02:00",
                "2021-12-11T13:00+02",
                "2021-12-11T13+02",
                "20211211T130000.0+0200",
                "20211211T130000+0200",
                "20211211T1300+02",
                "20211211T13+02",
        }) {
            assertParsingResults(
                    ISO_DATE_TIME_CONFIGURATOR,
                    parsedString,
                    convertToClass(
                            OffsetDateTime.of(2021, 12, 11, 13, 0, 0, 0, ZoneOffset.ofHours(2)),
                            temporalClass));
        }

        // Negative year:
        for (String parsedString : new String[] {
                "-1000-02-03T04Z",
                "-10000203T04Z"
        }) {
            assertParsingResults(
                    ISO_DATE_TIME_CONFIGURATOR,
                    parsedString,
                    convertToClass(
                            OffsetDateTime.of(-1000, 2, 3, 4, 0, 0, 0, ZoneOffset.UTC),
                            temporalClass));
        }

        // Hour 24:
        for (String parsedString : new String[] {
                "2020-01-02T24Z",
                "2020-01-02T24:00Z",
                "2020-01-02T24:00:00Z",
                "2020-01-02T24:00:00.0Z",
                "2020-01-02T24:00:00.0+00",
                // For local temporals only: "2020-01-02T24:00:00",
                "20200102T24Z",
                "20200102T2400Z",
                "20200102T240000Z",
                "20200102T240000.0Z",
                "20200102T240000.0+00",
                // For local temporals only: "20200102T240000"
        }) {
            assertParsingResults(
                    ISO_DATE_TIME_CONFIGURATOR,
                    parsedString,
                    convertToClass(
                            OffsetDateTime.of(2020, 1, 3, 0, 0, 0, 0, ZoneOffset.UTC),
                            temporalClass));
        }

        // Unlike for the Java format, for ISO we require the string to contain the offset for a non-local target type.
        for (String parsedString : new String[] {
                "2020-01-02T03", "2020-01-02T03:00", "2020-01-02T03:00:00",
                "20200102T03", "20200102T0300", "20200102T030000"}) {
            assertParsingFails(
                    ISO_DATE_TIME_CONFIGURATOR,
                    parsedString,
                    temporalClass,
                    e -> assertThat(e.getMessage(), allOf(
                                containsString(jQuote(parsedString)),
                                containsString("time zone offset"),
                                containsString(temporalClass.getSimpleName()))));
        }

        for (String parsedString : new String[] {
                "2021-12-11", "20211211", "2021-12-11T", "2021-12-11T0Z",
                "2021-12-11T25Z", "2022-02-29T23Z", "2021-13-11T23Z"}) {
            assertParsingFails(
                    ISO_DATE_TIME_CONFIGURATOR,
                    parsedString,
                    temporalClass,
                    e -> {
                        assertThat(e.getMessage(), allOf(
                                containsString(jQuote(parsedString)),
                                containsString(temporalClass.getSimpleName())));
                        if (!parsedString.contains("T")) {
                            assertThat(e.getMessage(), containsString("\"T\""));
                        }
                    });
        }
    }

    @Test
    public void testParseOffsetTime() throws TemplateException, TemplateValueFormatException {
        // TODO [FREEMARKER-35]
    }

    @Test
    public void testParseLocalDateTime() throws TemplateException, TemplateValueFormatException {
        // TODO [FREEMARKER-35]
    }

    @Test
    public void testParseLocalDate() throws TemplateException, TemplateValueFormatException {
        // TODO [FREEMARKER-35]
    }

    @Test
    public void testParseLocalTime() throws TemplateException, TemplateValueFormatException {
        // TODO [FREEMARKER-35]
    }

    @Test
    public void testParseYear() throws TemplateException, TemplateValueFormatException {
        assertParsingResults(ISO_YEAR_CONFIGURATOR, "2021", Year.of(2021));
        assertParsingResults(ISO_YEAR_CONFIGURATOR, "1995", Year.of(1995));
        assertParsingResults(ISO_YEAR_CONFIGURATOR, "95", Year.of(95));
        assertParsingResults(ISO_YEAR_CONFIGURATOR, "-1000", Year.of(-1000));

        assertParsingFails(
                ISO_DATE_TIME_CONFIGURATOR,
                "2021-01",
                Year.class,
                e -> {
                    assertThat(e.getMessage(), allOf(
                            containsString(jQuote("2021-01")),
                            containsString("Year")));
                }
        );
    }

    @Test
    public void testParseYearMonth() throws TemplateException, TemplateValueFormatException {
        assertParsingResults(ISO_YEAR_MONTH_CONFIGURATOR, "2021-01", YearMonth.of(2021, 1));
        assertParsingResults(ISO_YEAR_MONTH_CONFIGURATOR, "202101", YearMonth.of(2021, 1));
        assertParsingResults(ISO_YEAR_MONTH_CONFIGURATOR, "1995-12", YearMonth.of(1995, 12));
        assertParsingResults(ISO_YEAR_MONTH_CONFIGURATOR, "199512", YearMonth.of(1995, 12));
        assertParsingResults(ISO_YEAR_MONTH_CONFIGURATOR, "95-12", YearMonth.of(95, 12));
        assertParsingResults(ISO_YEAR_MONTH_CONFIGURATOR, "9512", YearMonth.of(95, 12));
        assertParsingResults(ISO_YEAR_MONTH_CONFIGURATOR, "-1000-01", YearMonth.of(-1000, 1));
        assertParsingResults(ISO_YEAR_MONTH_CONFIGURATOR, "-100001", YearMonth.of(-1000, 1));

        for (String parsedString : new String[] {"2021", "2021-12-11", "2021-13", "202113"}) {
            assertParsingFails(
                    ISO_YEAR_MONTH_CONFIGURATOR,
                    parsedString,
                    YearMonth.class,
                    e -> {
                        assertThat(e.getMessage(), allOf(
                                containsString(jQuote(parsedString)),
                                containsString("YearMonth")));
                    }
            );
        }
    }

    @Test
    public void testHistoricalDates() throws TemplateException, TemplateValueFormatException {
        for (boolean iso8601NegativeYear : new boolean[] {false, true}) {
            LocalDate localDate = iso8601NegativeYear
                    ? LocalDate.of(-100, 12, 11)
                    : LocalDate.of(0, 12, 11);
            String iso8601String = iso8601NegativeYear
                    ? "-0100-12-11"
                    : "0000-12-11";
            // Just to show that ISO 8601 year 0 is 1 BC:
            {
                String stringWithYearOfEra = iso8601NegativeYear
                        ? "101-12-11 BC"
                        : "1-12-11 BC";
                assertEquals(
                        localDate,
                        new DateTimeFormatterBuilder()
                                .appendPattern("y-MM-dd G")
                                .toFormatter(Locale.ROOT)
                                .withZone(ZoneOffset.UTC)
                                .parse(stringWithYearOfEra, LocalDate::from));
            }

            String output = formatTemporal(ISO_DATE_CONFIGURATOR, localDate);
            assertEquals(iso8601String, output);
            assertParsingResults(ISO_DATE_CONFIGURATOR, iso8601String, localDate);
        }
    }

    @Test
    public void testLocaleHasNoEffect() throws TemplateException, TemplateValueFormatException {
        for (Locale locale : new Locale[] {
                Locale.CHINA,
                Locale.FRANCE,
                new Locale("hi", "IN"),
                new Locale.Builder()
                        .setLocale(Locale.JAPAN)
                        .setUnicodeLocaleKeyword("ca", "japanese")
                        .build()}) {
            LocalDateTime localDateTime = LocalDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000);
            Consumer<Configurable> configurer = cfg -> {
                cfg.setDateTimeFormat("iso");
                cfg.setLocale(locale);
            };
            String output = formatTemporal(configurer, localDateTime);
            String string = "2021-12-11T13:01:02.0123";
            assertEquals(string, output);
            assertParsingResults(configurer, string, localDateTime);
        }
    }

}
