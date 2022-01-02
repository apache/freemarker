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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.function.Consumer;

import org.junit.Test;

import freemarker.template.TemplateException;

public class TemporalFormatWithIsoFormatTest extends AbstractTemporalFormatTest {

    private static final Consumer<Configurable> ISO_DATE_TIME_CONFIGURER = conf -> conf.setDateTimeFormat("iso");
    private static final Consumer<Configurable> ISO_DATE_CONFIGURER = conf -> conf.setDateFormat("iso");
    private static final Consumer<Configurable> ISO_TIME_CONFIGURER = conf -> conf.setTimeFormat("iso");

    @Test
    public void testFormatOffsetTime() throws TemplateException, IOException {
        assertEquals(
                "13:01:02Z",
                formatTemporal(
                        ISO_TIME_CONFIGURER,
                        OffsetTime.of(LocalTime.of(13, 1, 2), ZoneOffset.UTC)));
        assertEquals(
                "13:01:02+01:00",
                formatTemporal(
                        ISO_TIME_CONFIGURER,
                        OffsetTime.of(LocalTime.of(13, 1, 2), ZoneOffset.ofHours(1))));
        assertEquals(
                "13:00:00-02:30",
                formatTemporal(
                        ISO_TIME_CONFIGURER,
                        OffsetTime.of(LocalTime.of(13, 0, 0), ZoneOffset.ofHoursMinutesSeconds(-2, -30, 0))));
        assertEquals(
                "13:00:00.0123Z",
                formatTemporal(
                        ISO_TIME_CONFIGURER,
                        OffsetTime.of(LocalTime.of(13, 0, 0, 12_300_000), ZoneOffset.UTC)));
        assertEquals(
                "13:00:00.3Z",
                formatTemporal(
                        ISO_TIME_CONFIGURER,
                        OffsetTime.of(LocalTime.of(13, 0, 0, 300_000_000), ZoneOffset.UTC)));
    }

    @Test
    public void testFormatLocalTime() throws TemplateException, IOException {
        assertEquals(
                "13:01:02",
                formatTemporal(
                        ISO_TIME_CONFIGURER,
                        LocalTime.of(13, 1, 2)));
        assertEquals(
                "13:00:00.0123",
                formatTemporal(
                        ISO_TIME_CONFIGURER,
                        LocalTime.of(13, 0, 0, 12_300_000)));
        assertEquals(
                "13:00:00.3",
                formatTemporal(
                        ISO_TIME_CONFIGURER,
                        LocalTime.of(13, 0, 0, 300_000_000)));
    }

    @Test
    public void testFormatLocalDateTime() throws TemplateException, IOException {
        assertEquals(
                "2021-12-11T13:01:02",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        LocalDateTime.of(2021, 12, 11, 13, 1, 2, 0)));
        assertEquals(
                "2021-12-11T13:01:02.0123",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        LocalDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000)));
        assertEquals(
                "2021-12-11T13:01:02.3",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        LocalDateTime.of(2021, 12, 11, 13, 1, 2, 300_000_000)));
    }

    @Test
    public void testFormatOffsetDateTime() throws TemplateException, IOException {
        assertEquals(
                "2021-12-11T13:01:02Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.UTC)));
        assertEquals(
                "2021-12-11T13:01:02+01:00",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.ofHours(1))));
        assertEquals(
                "2021-12-11T13:01:02-02:30",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.ofHoursMinutesSeconds(-2, -30, 0))));
        assertEquals(
                "2021-12-11T13:01:02.0123Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000, ZoneOffset.UTC)));
        assertEquals(
                "2021-12-11T13:01:02.3Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 300_000_000, ZoneOffset.UTC)));
    }

    @Test
    public void testFormatZonedDateTime() throws TemplateException, IOException {
        assertEquals(
                "2021-12-11T13:01:02Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        ZonedDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.UTC)));
        ZoneId zoneId = ZoneId.of("America/New_York");
        assertEquals(
                "2021-12-11T13:01:02-05:00",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        ZonedDateTime.of(2021, 12, 11, 13, 1, 2, 0, zoneId)));
        assertEquals(
                "2021-07-11T13:01:02-04:00",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        ZonedDateTime.of(2021, 7, 11, 13, 1, 2, 0, zoneId)));
        assertEquals(
                "2021-12-11T13:01:02-02:30",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 0, ZoneOffset.ofHoursMinutesSeconds(-2, -30, 0))));
        assertEquals(
                "2021-12-11T13:01:02.0123Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000, ZoneOffset.UTC)));
        assertEquals(
                "2021-12-11T13:01:02.3Z",
                formatTemporal(
                        ISO_DATE_TIME_CONFIGURER,
                        OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 300_000_000, ZoneOffset.UTC)));
    }

    @Test
    public void testFormatLocalDate() throws TemplateException, IOException {
        assertEquals(
                "2021-12-11",
                formatTemporal(
                        ISO_DATE_CONFIGURER,
                        LocalDate.of(2021, 12, 11)));
    }

    @Test
    public void testParseOffsetDateTime() throws TemplateException, TemplateValueFormatException {
        // ISO extended and ISO basic format:
        for (String s : new String[]{"2021-12-11T13:01:02.0123Z", "20211211T130102.0123Z"}) {
            assertParsingResults(
                    ISO_DATE_TIME_CONFIGURER,
                    s,
                    OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000, ZoneOffset.UTC)) ;
        }

        // Optional parts:
        for (String s : new String[] {
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
                    ISO_DATE_TIME_CONFIGURER,
                    s,
                    OffsetDateTime.of(2021, 12, 11, 13, 0, 0, 0, ZoneOffset.ofHours(2)));
        }

        // TODO Zone default

        try {
            assertParsingResults(
                    ISO_DATE_TIME_CONFIGURER,
                    "2021-12-11",
                    OffsetDateTime.of(2021, 12, 11, 13, 1, 2, 12_300_000, ZoneOffset.UTC));
            fail("OffsetDateTime parsing should have failed");
        } catch (UnparsableValueException e) {
            assertThat(e.getMessage(), allOf(
                    containsString("\"2021-12-11\""),
                    containsString("OffsetDateTime"),
                    containsString("\"T\"")
            ));
        }
    }

    @Test
    public void testParseZonedDateTime() throws TemplateException, TemplateValueFormatException {
        // TODO [FREEMARKER-35]
    }

    @Test
    public void testParseLocalDateTime() throws TemplateException, TemplateValueFormatException {
        // TODO [FREEMARKER-35]
    }

    @Test
    public void testParseInstance() throws TemplateException, TemplateValueFormatException {
        // TODO [FREEMARKER-35]
    }

    @Test
    public void testParseLocalDate() throws TemplateException, TemplateValueFormatException {
        // TODO [FREEMARKER-35]
    }

    @Test
    public void testParseOffsetTime() throws TemplateException, TemplateValueFormatException {
        // TODO [FREEMARKER-35]
    }

    @Test
    public void testParseLocalTime() throws TemplateException, TemplateValueFormatException {
        // TODO [FREEMARKER-35]
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

            String output = formatTemporal(ISO_DATE_CONFIGURER, localDate);
            assertEquals(iso8601String, output);
            assertParsingResults(ISO_DATE_CONFIGURER, iso8601String, localDate);
        }
    }

    @Test
    public void testParseLocaleHasNoEffect() throws TemplateException, TemplateValueFormatException {
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
