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
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.TimeZone;
import java.util.function.Consumer;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.SimpleTemporal;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.utility.DateUtil;

public class TemporalFormatTest {

    @Test
    public void testOffsetTimeAndZones() throws TemplateException, IOException {
        OffsetTime offsetTime = OffsetTime.of(LocalTime.of(10, 0, 0), ZoneOffset.ofHours(1));

        TimeZone zoneWithoutDST = TimeZone.getTimeZone("GMT+2");
        assertFalse(zoneWithoutDST.useDaylightTime());

        TimeZone zoneWithDST = TimeZone.getTimeZone("America/New_York");
        assertTrue(zoneWithDST.useDaylightTime());

        assertEquals(
                "11:00",
                formatTemporal(
                        conf -> {
                            conf.setOffsetTimeFormat("HH:mm");
                            conf.setTimeZone(zoneWithoutDST);
                        },
                        offsetTime));

        try {
            assertEquals(
                    "11:00",
                    formatTemporal(
                            conf -> {
                                conf.setOffsetTimeFormat("HH:mm");
                                conf.setTimeZone(zoneWithDST);
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
                            conf.setOffsetTimeFormat("HH:mmX");
                            conf.setTimeZone(zoneWithDST);
                        },
                        offsetTime));

        assertEquals(
                "10:00+01",
                formatTemporal(
                        conf -> {
                            conf.setOffsetTimeFormat("HH:mmX");
                            conf.setTimeZone(zoneWithoutDST);
                        },
                        offsetTime));
    }

    @Test
    public void testZoneConvertedWhenOffsetOrZoneNotShown() throws TemplateException, IOException {
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
                            conf.setLocalDateTimeFormat("yyyy-MM-dd HH:mm");
                            conf.setOffsetDateTimeFormat("yyyy-MM-dd HH:mm");
                            conf.setZonedDateTimeFormat("yyyy-MM-dd HH:mm");
                            conf.setTimeZone(gbZone);
                        },
                        summerLocalDateTime, summerOffsetDateTime, summerZonedDateTime,
                        winterLocalDateTime, winterOffsetDateTime, winterZonedDateTime));
        assertEquals(
                "2021-06-30 10:30, 2021-06-30 08:30, 2021-06-30 14:30, "
                        + "2021-12-30 10:30, 2021-12-30 08:30, 2021-12-30 15:30",
                formatTemporal(
                        conf -> {
                            conf.setLocalDateTimeFormat("yyyy-MM-dd HH:mm");
                            conf.setOffsetDateTimeFormat("yyyy-MM-dd HH:mm");
                            conf.setZonedDateTimeFormat("yyyy-MM-dd HH:mm");
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
                            conf.setOffsetDateTimeFormat("yyyy-MM-dd HH:mmX");
                            conf.setZonedDateTimeFormat("yyyy-MM-dd HH:mmX");
                            conf.setTimeZone(gbZone);
                        },
                        summerOffsetDateTime, summerZonedDateTime,
                        winterOffsetDateTime, winterZonedDateTime));
    }

    @Test
    public void testCanNotFormatLocalIfTimeZoneIsShown() {
        try {
            formatTemporal(
                    conf -> {
                        conf.setLocalDateTimeFormat("yyyy-MM-dd HH:mmX");
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
    public void testStylesAreNotSupportedForYear() {
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
    public void testStylesAreNotSupportedForYearMonth() {
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

    static private String formatTemporal(Consumer<Configurable> configurer, Temporal... values) throws
            TemplateException {
        Configuration conf = new Configuration(Configuration.VERSION_2_3_32);

        configurer.accept(conf);

        Environment env = null;
        try {
            env = new Template(null, "", conf).createProcessingEnvironment(null, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        StringBuilder sb = new StringBuilder();
        for (Temporal value : values) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(env.formatTemporalToPlainText(new SimpleTemporal(value), null, false));
        }

        return sb.toString();
    }
}
