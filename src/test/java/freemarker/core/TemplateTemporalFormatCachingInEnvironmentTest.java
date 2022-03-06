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

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.NullWriter;

public class TemplateTemporalFormatCachingInEnvironmentTest {

    @Test
    public void testTemporalClassSeparation() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setLocale(Locale.US);
        cfg.setTimeZone(DateUtil.UTC);

        Environment env = new Template(null, "", cfg)
                .createProcessingEnvironment(null, NullWriter.INSTANCE);

        env.setDateTimeFormat("iso");
        TemplateTemporalFormat lastLocalDateTimeFormat = env.getTemplateTemporalFormat(LocalDateTime.class);
        TemplateTemporalFormat lastOffsetDateTimeFormat = env.getTemplateTemporalFormat(OffsetDateTime.class);
        TemplateTemporalFormat lastZonedDateTimeFormat = env.getTemplateTemporalFormat(ZonedDateTime.class);
        TemplateTemporalFormat lastInstantDateTimeFormat = env.getTemplateTemporalFormat(Instant.class);
        TemplateTemporalFormat lastOffsetTimeFormat = env.getTemplateTemporalFormat(OffsetTime.class);
        TemplateTemporalFormat lastLocalTimeFormat = env.getTemplateTemporalFormat(LocalTime.class);
        TemplateTemporalFormat lastLocalDateFormat = env.getTemplateTemporalFormat(LocalDate.class);
        TemplateTemporalFormat lastYearFormat = env.getTemplateTemporalFormat(Year.class);
        TemplateTemporalFormat lastYearMonthFormat = env.getTemplateTemporalFormat(YearMonth.class);
        env.setDateTimeFormat("long");
        assertNotSame(lastLocalDateTimeFormat, env.getTemplateTemporalFormat(LocalDateTime.class));
        assertNotSame(lastOffsetDateTimeFormat, env.getTemplateTemporalFormat(OffsetDateTime.class));
        assertNotSame(lastZonedDateTimeFormat, env.getTemplateTemporalFormat(ZonedDateTime.class));
        assertNotSame(lastInstantDateTimeFormat, env.getTemplateTemporalFormat(Instant.class));
        assertSame(lastOffsetTimeFormat, env.getTemplateTemporalFormat(OffsetTime.class));
        assertSame(lastLocalTimeFormat, env.getTemplateTemporalFormat(LocalTime.class));
        assertSame(lastLocalDateFormat, env.getTemplateTemporalFormat(LocalDate.class));
        assertSame(lastYearFormat, env.getTemplateTemporalFormat(Year.class));
        assertSame(lastYearMonthFormat, env.getTemplateTemporalFormat(YearMonth.class));

        lastLocalDateTimeFormat = env.getTemplateTemporalFormat(LocalDateTime.class);
        lastOffsetDateTimeFormat = env.getTemplateTemporalFormat(OffsetDateTime.class);
        lastZonedDateTimeFormat = env.getTemplateTemporalFormat(ZonedDateTime.class);
        lastInstantDateTimeFormat = env.getTemplateTemporalFormat(Instant.class);
        lastOffsetTimeFormat = env.getTemplateTemporalFormat(OffsetTime.class);
        lastLocalTimeFormat = env.getTemplateTemporalFormat(LocalTime.class);
        lastLocalDateFormat = env.getTemplateTemporalFormat(LocalDate.class);
        lastYearFormat = env.getTemplateTemporalFormat(Year.class);
        lastYearMonthFormat = env.getTemplateTemporalFormat(YearMonth.class);
        env.setTimeFormat("short");
        assertSame(lastLocalDateTimeFormat, env.getTemplateTemporalFormat(LocalDateTime.class));
        assertSame(lastOffsetDateTimeFormat, env.getTemplateTemporalFormat(OffsetDateTime.class));
        assertSame(lastZonedDateTimeFormat, env.getTemplateTemporalFormat(ZonedDateTime.class));
        assertSame(lastInstantDateTimeFormat, env.getTemplateTemporalFormat(Instant.class));
        assertNotSame(lastOffsetTimeFormat, env.getTemplateTemporalFormat(OffsetTime.class));
        assertNotSame(lastLocalTimeFormat, env.getTemplateTemporalFormat(LocalTime.class));
        assertSame(lastLocalDateFormat, env.getTemplateTemporalFormat(LocalDate.class));
        assertSame(lastYearFormat, env.getTemplateTemporalFormat(Year.class));
        assertSame(lastYearMonthFormat, env.getTemplateTemporalFormat(YearMonth.class));
    }

    @Test
    public void testForDateTime() throws Exception {
        // Locale dependent formatters:
        genericTest(LocalDateTime.class,
                (cfg, first) -> cfg.setDateTimeFormat(first ? "yyyy-MM-dd HH:mm" : "yyyyMMddHHmm"),
                true, false);
        genericTest(ZonedDateTime.class,
                (cfg, first) -> cfg.setDateTimeFormat(first ? "yyyy-MM-dd HH:mm" : "yyyyMMddHHmm"),
                true, true);
        genericTest(OffsetDateTime.class,
                (cfg, first) -> cfg.setDateTimeFormat(first ? "yyyy-MM-dd HH:mm" : "yyyyMMddHHmm"),
                true, true);
        genericTest(Instant.class,
                (cfg, first) -> cfg.setDateTimeFormat(first ? "yyyy-MM-dd HH:mm" : "yyyyMMddHHmm"),
                true, true);

        // Locale independent formatters:
        genericTest(LocalDateTime.class,
                (cfg, first) -> cfg.setDateTimeFormat(first ? "iso" : "xs"),
                false, false);
        genericTest(ZonedDateTime.class,
                (cfg, first) -> cfg.setDateTimeFormat(first ? "iso" : "xs"),
                false, true);
    }

    @Test
    public void testForDate() throws Exception {
        // Locale dependent formatters:
        genericTest(LocalDate.class,
                (cfg, first) -> cfg.setDateFormat(first ? "yyyy-MM-dd" : "yyyyMM-dd"),
                true, false);

        // Locale independent formatters:
        genericTest(LocalDate.class,
                (cfg, first) -> cfg.setDateFormat(first ? "iso" : "xs"),
                false, false);
    }

    @Test
    public void testForTime() throws Exception {
        // Locale dependent formatters:
        genericTest(LocalTime.class,
                (cfg, first) -> cfg.setTimeFormat(first ? "HH:mm" : "HHmm"),
                true, false);
        genericTest(OffsetTime.class,
                (cfg, first) -> cfg.setTimeFormat(first ? "HH:mm" : "HHmm"),
                true, true);

        // Locale independent formatters:
        genericTest(LocalTime.class,
                (cfg, first) -> cfg.setTimeFormat(first ? "iso" : "xs"),
                false, false);
        genericTest(OffsetTime.class,
                (cfg, first) -> cfg.setTimeFormat(first ? "iso" : "xs"),
                false, true);
    }

    @Test
    public void testForYearMonth() throws Exception {
        // Locale dependent formatters:
        genericTest(YearMonth.class,
                (cfg, first) -> cfg.setYearMonthFormat(first ? "yyyy-MM" : "yyyyMM"),
                true, false);

        // Locale independent formatters:
        genericTest(YearMonth.class,
                (cfg, first) -> cfg.setYearMonthFormat(first ? "iso" : "xs"),
                false, false);
    }

    @Test
    public void testForYear() throws Exception {
        // Locale dependent formatters:
        genericTest(Year.class,
                (cfg, first) -> cfg.setYearFormat(first ? "yyyy" : "yy"),
                true, false);

        // Locale independent formatters:
        genericTest(Year.class,
                (cfg, first) -> cfg.setYearFormat(first ? "iso" : "xs"),
                false, false);
    }

    private void genericTest(
            Class<? extends Temporal> temporalClass,
            SettingSetter settingSetter,
            boolean localeDependent, boolean timeZoneDependent)
            throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setLocale(Locale.GERMANY);
        cfg.setTimeZone(DateUtil.UTC);
        settingSetter.setSetting(cfg, true);

        Environment env = new Template(null, "", cfg)
                .createProcessingEnvironment(null, NullWriter.INSTANCE);

        TemplateTemporalFormat lastFormat;
        TemplateTemporalFormat newFormat;

        lastFormat = env.getTemplateTemporalFormat(temporalClass);
        // Assert that it keeps returning the same instance from cache:
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));

        settingSetter.setSetting(env, true);
        // Assert that the cache wasn't cleared when the setting was set to the same value again:
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));

        env.setLocale(Locale.JAPAN); // Possibly clears non-reusable TemplateTemporalFormatCache field
        newFormat = env.getTemplateTemporalFormat(temporalClass);
        if (localeDependent) {
            assertNotSame(lastFormat, newFormat);
        } else {
            assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));
        }
        lastFormat = newFormat;

        env.setLocale(Locale.JAPAN);
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));

        env.setLocale(Locale.GERMANY); // Possibly clears non-reusable TemplateTemporalFormatCache field
        env.setLocale(Locale.JAPAN);
        // Assert that it restores the same instance from TemplateTemporalFormatCache.reusableXxx field:
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));

        TimeZone otherTimeZone = TimeZone.getTimeZone("GMT+01");
        env.setTimeZone(otherTimeZone); // Possibly clears non-reusable TemplateTemporalFormatCache field
        newFormat = env.getTemplateTemporalFormat(temporalClass);
        if (timeZoneDependent) {
            assertNotSame(newFormat, lastFormat);
            assertSame(newFormat, env.getTemplateTemporalFormat(temporalClass));
        } else {
            assertSame(newFormat, lastFormat);
        }
        lastFormat = newFormat;

        env.setTimeZone(DateUtil.UTC); // Possibly clears non-reusable TemplateTemporalFormatCache field
        env.setTimeZone(otherTimeZone);
        // Assert that it restores the same instance from TemplateTemporalFormatCache.reusableXxx field:
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));

        settingSetter.setSetting(env, false); // Clears even TemplateTemporalFormatCache.reusableXxx
        newFormat = env.getTemplateTemporalFormat(temporalClass);
        assertNotSame(lastFormat, newFormat);
    }

    @FunctionalInterface
    interface SettingSetter {
        void setSetting(Configurable configurable, boolean firstValue);
    }

}
