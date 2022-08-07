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

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.NullWriter;

/**
 * Tests the caching of the format in the {@link Environment} for the current value of the format setting (like for
 * <code>${temporal}</code>, as opposed to <code>${temporal?string(someSpecialFormat)}</code>).
 *
 * @see TemplateTemporalFormatByFormatStringCachingInEnvironmentTest
 */
public class TemplateTemporalFormatCurrentCachingInEnvironmentTest
        extends TemplateTemporalFormatAbstractCachingInEnvironmentTest {

    @Test
    public void testTemporalClassSeparation() throws Exception {
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
        String[] formats = {"yyyy-MM-dd HH:mm", "yyyyMMddHHmm"};
        genericTest(LocalDateTime.class, true, false, formats);
        genericTest(ZonedDateTime.class, true, true, formats);
        genericTest(OffsetDateTime.class, true, true, formats);
        genericTest(Instant.class, true, true, formats);

        // Locale independent formatters:
        genericTest(LocalDateTime.class, false, false, "iso", "xs");
        genericTest(ZonedDateTime.class, false, true, "iso", "xs");
    }

    @Test
    public void testForDate() throws Exception {
        // Locale dependent formatters:
        genericTest(LocalDate.class, true, false, "yyyy-MM-dd", "yyyyMM-dd");

        // Locale independent formatters:
        genericTest(LocalDate.class, false, false, "iso", "xs");
    }

    @Test
    public void testForTime() throws Exception {
        // Locale dependent formatters:
        genericTest(LocalTime.class, true, false, "HH:mm", "HHmm");
        genericTest(OffsetTime.class, true, true, "HH:mm", "HHmm");

        // Locale independent formatters:
        genericTest(LocalTime.class, false, false, "iso", "xs");
        genericTest(OffsetTime.class, false, true, "iso", "xs");
    }

    @Test
    public void testForYearMonth() throws Exception {
        // Locale dependent formatters:
        genericTest(YearMonth.class, true, false, "yyyy-MM", "yyyyMM");

        // Locale independent formatters:
        genericTest(YearMonth.class, false, false, "iso", "xs");
    }

    @Test
    public void testForYear() throws Exception {
        // Locale dependent formatters:
        genericTest(Year.class, true, false, "yyyy", "yy");

        // Locale independent formatters:
        genericTest(Year.class, false, false, "iso", "xs");
    }

    private void genericTest(
            Class<? extends Temporal> temporalClass,
            boolean localeDependent, boolean timeZoneDependent,
            String... settingValues) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setLocale(Locale.GERMANY);
        cfg.setTimeZone(DateUtil.UTC);
        SettingAssignments settingAssignments = new SettingAssignments(temporalClass, settingValues);
        settingAssignments.execute(cfg, 0);

        Environment env = new Template(null, "", cfg)
                .createProcessingEnvironment(null, NullWriter.INSTANCE);

        TemplateTemporalFormat lastFormat;
        TemplateTemporalFormat newFormat;

        // Note: We call env.clearCachedTemplateTemporalFormatsByFormatString() directly before all
        // env.getTemplateTemporalFormat calls, just to avoid that 2nd level of cache hiding any bugs in the level
        // that we want to test here. But in almost all of these tests scenarios it shouldn't have any effect anyway.

        env.clearCachedTemplateTemporalFormatsByFormatString();
        lastFormat = env.getTemplateTemporalFormat(temporalClass);
        // Assert that it keeps returning the same instance from cache:
        env.clearCachedTemplateTemporalFormatsByFormatString();
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));
        env.clearCachedTemplateTemporalFormatsByFormatString();
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));

        settingAssignments.execute(env, 0);
        // Assert that the cache wasn't cleared when the setting was set to the same value again:
        env.clearCachedTemplateTemporalFormatsByFormatString();
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));

        env.setLocale(Locale.JAPAN); // Possibly clears non-reusable TemplateTemporalFormatCache field
        env.clearCachedTemplateTemporalFormatsByFormatString();
        newFormat = env.getTemplateTemporalFormat(temporalClass);
        if (localeDependent) {
            assertNotSame(lastFormat, newFormat);
        } else {
            env.clearCachedTemplateTemporalFormatsByFormatString();
            assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));
        }
        lastFormat = newFormat;

        env.setLocale(Locale.JAPAN);
        env.clearCachedTemplateTemporalFormatsByFormatString();
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));

        env.setLocale(Locale.GERMANY); // Possibly clears non-reusable TemplateTemporalFormatCache field
        env.setLocale(Locale.JAPAN);
        // Assert that it restores the same instance from TemplateTemporalFormatCache.reusableXxx field:
        env.clearCachedTemplateTemporalFormatsByFormatString();
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));

        env.setTimeZone(OTHER_TIME_ZONE); // Possibly clears non-reusable TemplateTemporalFormatCache field
        env.clearCachedTemplateTemporalFormatsByFormatString();
        newFormat = env.getTemplateTemporalFormat(temporalClass);
        if (timeZoneDependent) {
            assertNotSame(newFormat, lastFormat);
            env.clearCachedTemplateTemporalFormatsByFormatString();
            assertSame(newFormat, env.getTemplateTemporalFormat(temporalClass));
        } else {
            assertSame(newFormat, lastFormat);
        }
        lastFormat = newFormat;

        env.setTimeZone(DateUtil.UTC); // Possibly clears non-reusable TemplateTemporalFormatCache field
        env.setTimeZone(OTHER_TIME_ZONE);
        env.clearCachedTemplateTemporalFormatsByFormatString();
        // Assert that it restores the same instance from TemplateTemporalFormatCache.reusableXxx field:
        assertSame(lastFormat, env.getTemplateTemporalFormat(temporalClass));

        settingAssignments.execute(env, 1); // Clears even TemplateTemporalFormatCache.reusableXxx
        env.clearCachedTemplateTemporalFormatsByFormatString();
        newFormat = env.getTemplateTemporalFormat(temporalClass);
        assertNotSame(lastFormat, newFormat);

        settingAssignments.execute(env, 0); // Clears even TemplateTemporalFormatCache.reusableXxx
        env.clearCachedTemplateTemporalFormatsByFormatString();
        newFormat = env.getTemplateTemporalFormat(temporalClass);
        assertNotSame(lastFormat, newFormat);
    }

}
