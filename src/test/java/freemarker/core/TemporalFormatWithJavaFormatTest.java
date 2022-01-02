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

import static freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
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
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

import org.junit.Test;

import freemarker.template.TemplateException;
import freemarker.template.utility.DateUtil;
import freemarker.test.hamcerst.Matchers;

public class TemporalFormatWithJavaFormatTest extends AbstractTemporalFormatTest {

    @Test
    public void testFormatOffsetTimeAndZones() throws TemplateException, IOException {
        OffsetTime offsetTime = OffsetTime.of(LocalTime.of(10, 0, 0), ZoneOffset.ofHours(1));

        TimeZone timeZone = TimeZone.getTimeZone("America/New_York");

        try {
            assertEquals(
                    "11:00",
                    formatTemporal(
                            conf -> {
                                conf.setTimeFormat("HH:mm");
                                conf.setTimeZone(timeZone);
                            },
                            offsetTime));
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("daylight saving"));
        }

        assertEquals(
                "10:00+01",
                formatTemporal(
                        conf -> {
                            conf.setTimeFormat("HH:mmX");
                            conf.setTimeZone(timeZone);
                        },
                        offsetTime));

    }

    @Test
    public void testFormatZoneConvertedWhenOffsetOrZoneNotShown() throws TemplateException, IOException {
        TimeZone gbZone = TimeZone.getTimeZone("GB");
        assertTrue(gbZone.useDaylightTime());
        // Summer: GMT+1
        // Winter: GMT+0

        TimeZone nyZone = TimeZone.getTimeZone("America/New_York");
        assertTrue(nyZone.useDaylightTime());
        // Summer: GMT-4
        // Winter: GMT-5

        LocalTime localTime = LocalTime.of(10, 30, 0);
        LocalDate winterLocalDate = LocalDate.of(2021, 12, 30);
        LocalDate summerLocalDate = LocalDate.of(2021, 6, 30);
        LocalDateTime winterLocalDateTime = LocalDateTime.of(winterLocalDate, localTime);
        OffsetDateTime winterOffsetDateTime = OffsetDateTime.of(winterLocalDateTime, ZoneOffset.ofHours(2));
        ZonedDateTime winterZonedDateTime = ZonedDateTime.of(winterLocalDateTime, nyZone.toZoneId());
        LocalDateTime summerLocalDateTime = LocalDateTime.of(summerLocalDate, localTime);
        OffsetDateTime summerOffsetDateTime = OffsetDateTime.of(summerLocalDateTime, ZoneOffset.ofHours(2));
        ZonedDateTime summerZonedDateTime = ZonedDateTime.of(summerLocalDateTime, nyZone.toZoneId());

        // If time zone (or offset) is not shown, the value is converted to the FreeMarker time zone:
        assertEquals(
                "2021-06-30 10:30, 2021-06-30 09:30, 2021-06-30 15:30, "
                        + "2021-12-30 10:30, 2021-12-30 08:30, 2021-12-30 15:30",
                formatTemporal(
                        conf -> {
                            conf.setDateTimeFormat("yyyy-MM-dd HH:mm");
                            conf.setTimeZone(gbZone);
                        },
                        summerLocalDateTime, summerOffsetDateTime, summerZonedDateTime,
                        winterLocalDateTime, winterOffsetDateTime, winterZonedDateTime));
        assertEquals(
                "2021-06-30 10:30, 2021-06-30 08:30, 2021-06-30 14:30, "
                        + "2021-12-30 10:30, 2021-12-30 08:30, 2021-12-30 15:30",
                formatTemporal(
                        conf -> {
                            conf.setDateTimeFormat("yyyy-MM-dd HH:mm");
                            conf.setTimeZone(DateUtil.UTC);
                        },
                        summerLocalDateTime, summerOffsetDateTime, summerZonedDateTime,
                        winterLocalDateTime, winterOffsetDateTime, winterZonedDateTime));

        // If the time zone (or offset) is shown, the value is not converted from its original time zone:
        assertEquals(
                "2021-06-30 10:30+02, 2021-06-30 10:30-04, "
                        + "2021-12-30 10:30+02, 2021-12-30 10:30-05",
                formatTemporal(
                        conf -> {
                            conf.setDateTimeFormat("yyyy-MM-dd HH:mmX");
                            conf.setTimeZone(gbZone);
                        },
                        summerOffsetDateTime, summerZonedDateTime,
                        winterOffsetDateTime, winterZonedDateTime));
    }

    @Test
    public void testFormatCanNotFormatLocalIfTimeZoneIsShown() {
        try {
            formatTemporal(
                    conf -> {
                        conf.setDateTimeFormat("yyyy-MM-dd HH:mmX");
                    },
                    LocalDateTime.of(2021, 10, 30, 1, 2));
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(),
                    allOf(
                            containsString("LocalDateTime"),
                            containsString("2021-10-30T01:02"),
                            containsString("yyyy-MM-dd HH:mmX"),
                            anyOf(containsStringIgnoringCase("offset"), containsStringIgnoringCase("zone"))));
        }
    }

    @Test
    public void testFormatStylesAreNotSupportedForYear() {
        try {
            formatTemporal(
                    conf -> {
                        conf.setYearFormat("medium");
                    },
                    Year.of(2021));
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(),
                    allOf(
                            containsString("\"medium\""),
                            containsString(Year.class.getName()),
                            containsStringIgnoringCase("style")));
        }
    }

    @Test
    public void testFormatStylesAreNotSupportedForYearMonth() {
        try {
            formatTemporal(
                    conf -> {
                        conf.setYearMonthFormat("medium");
                    },
                    YearMonth.of(2021, 10));
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(),
                    allOf(
                            containsString("\"medium\""),
                            containsString(YearMonth.class.getName()),
                            containsStringIgnoringCase("style")));
        }
    }

    @Test
    public void testParseDateTime() throws TemplateException, TemplateValueFormatException {
        ZoneId cfgZoneId = ZoneId.of("America/New_York");
        TimeZone cfgTimeZone = TimeZone.getTimeZone(cfgZoneId);

        for (boolean winter : new boolean[] {true, false}) {
            String stringToParse = winter ? "2020-12-10 13:14" : "2020-07-10 13:14";
            LocalDateTime localDateTime = winter
                    ? LocalDateTime.of(2020, 12, 10, 13, 14)
                    : LocalDateTime.of(2020, 07, 10, 13, 14);

            {
                ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, cfgZoneId);
                assertParsingResults(
                        conf -> {
                            conf.setDateTimeFormat("y-MM-dd HH:mm");
                            conf.setTimeZone(cfgTimeZone);
                        },
                        stringToParse, localDateTime,
                        stringToParse, zonedDateTime,
                        stringToParse, zonedDateTime.toInstant(),
                        stringToParse, zonedDateTime.toOffsetDateTime());
            }

            {
                String stringToParseWithOffset = stringToParse + "+02";
                OffsetDateTime offsetDateTime = localDateTime.atOffset(ZoneOffset.ofHours(2));
                ZonedDateTime zonedDateTime = offsetDateTime.toZonedDateTime();
                assertParsingResults(
                        conf -> {
                            conf.setDateTimeFormat("y-MM-dd HH:mmX");
                            conf.setTimeZone(cfgTimeZone);
                        },
                        stringToParseWithOffset, localDateTime,
                        stringToParseWithOffset, zonedDateTime,
                        stringToParseWithOffset, zonedDateTime.toInstant(),
                        stringToParseWithOffset, offsetDateTime);
            }

            {
                ZoneId zoneIdToParse = ZoneId.of("Europe/Prague");
                String stringToParseWithZone = stringToParse + " " + zoneIdToParse.getId();
                ZonedDateTime zonedDateTime = localDateTime.atZone(zoneIdToParse);
                assertParsingResults(
                        conf -> {
                            conf.setDateTimeFormat("y-MM-dd HH:mm z");
                            conf.setTimeZone(cfgTimeZone);
                        },
                        stringToParseWithZone, localDateTime,
                        stringToParseWithZone, zonedDateTime,
                        stringToParseWithZone, zonedDateTime.toInstant(),
                        stringToParseWithZone, zonedDateTime.toOffsetDateTime());
            }
        }
    }

    @Test
    public void testParseWrongFormat() throws TemplateException, TemplateValueFormatException {
        try {
            assertParsingResults(
                    conf -> conf.setDateTimeFormat("y-MM-dd HH:mm"),
                    "2020-12-10 01:14 PM", LocalDateTime.of(2020, 12, 10, 13, 14));
            fail("Parsing should have failed");
        } catch (UnparsableValueException e) {
            assertThat(
                    e.getMessage(),
                    allOf(
                            containsString("\"2020-12-10 01:14 PM\""),
                            containsString("\"y-MM-dd HH:mm\""),
                            containsString("\"en_US\""),
                            containsString("\"UTC\""),
                            containsString("LocalDateTime")
                    )
            );
        }
    }

    @Test
    public void testParseDate() throws TemplateException, TemplateValueFormatException {
        LocalDate localDate = LocalDate.of(2020, 11, 10);
        assertParsingResults(
                conf -> conf.setDateFormat("y-MM-dd"),
                "2020-11-10", localDate);
        assertParsingResults(
                conf -> conf.setDateFormat("yy-MM-dd"),
                "20-11-10", localDate);
    }

    @Test
    public void testParseLocalTime() throws TemplateException, TemplateValueFormatException {
        String stringToParse = "13:14";

        assertParsingResults(
                conf -> conf.setTimeFormat("HH:mm"),
                stringToParse, LocalTime.of(13, 14));

        assertParsingResults(
                conf -> {
                    conf.setTimeFormat("HH:mmX");
                    conf.setTimeZone(TimeZone.getTimeZone("GMT+02"));
                },
                stringToParse + "+02", LocalTime.of(13, 14));
    }

    @Test
    public void testParseLocalization() throws TemplateException, TemplateValueFormatException {
        LocalDate localDate = LocalDate.of(2020, 11, 10);
        for (Locale locale : new Locale[] {
                Locale.CHINA,
                Locale.GERMANY,
                new Locale("th", "TH"), // Because of the Buddhist calendar
                Locale.US
        }) {
            Consumer<Configurable> configurer = conf -> {
                conf.setDateFormat("y MMM dd");
                conf.setLocale(locale);
            };
            String formattedDate = formatTemporal(configurer, localDate);
            assertParsingResults(configurer, formattedDate, localDate);
        }
    }

    @Test
    public void testParseOffsetTime() throws TemplateException, TemplateValueFormatException {
        ZoneId zoneId = ZoneId.of("America/New_York");
        TimeZone timeZone = TimeZone.getTimeZone(zoneId);

        assertParsingResults(
                conf -> {
                    conf.setTimeFormat("HH:mmXX");
                    conf.setTimeZone(timeZone);
                },
                "13:14+0130",
                OffsetTime.of(LocalTime.of(13, 14), ZoneOffset.ofHoursMinutesSeconds(1, 30, 0)));

        try {
            assertParsingResults(
                    conf -> {
                        conf.setTimeFormat("HH:mm");
                        conf.setTimeZone(timeZone);
                    },
                    "13:14",
                    OffsetTime.now() /* Value doesn't matter, as parsing will fail */);
            fail("OffsetTime parsing should have failed when offset is not specified");
        } catch (InvalidFormatParametersException e) {
            assertThat(e.getMessage(), Matchers.containsStringIgnoringCase("daylight saving"));
        }
    }

}
