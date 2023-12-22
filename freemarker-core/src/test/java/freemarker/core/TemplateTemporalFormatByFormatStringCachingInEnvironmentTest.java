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

import java.io.IOException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import freemarker.template.TemplateException;

/**
 * Tests the caching of the format in the {@link Environment} for a specific format string that's necessarily the
 * current value of the related format setting (like for <code>${temporal?string(someSpecialFormat)}</code>, as opposed
 * to <code>${temporal}</code>).
 */
public class TemplateTemporalFormatByFormatStringCachingInEnvironmentTest
        extends AbstractTemplateTemporalFormatAbstractCachingInEnvironmentTest {

    public void test() throws IOException, TemplateException, TemplateValueFormatException {
        Class<? extends Temporal> temporalClass = LocalDateTime.class;
        TemplateTemporalFormat templateTemporalFormat = env.getTemplateTemporalFormat(temporalClass);
    }

    @Test
    public void testForDateTime() throws Exception {
        // Locale dependent formatters:
        String[] formats = {"yyyy-MM-dd HH:mm", "yyyyMMddHHmm", "yyyyMMdd HHmm"};
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
            String... formatStrings
    ) throws Exception {
        SettingAssignments settingAssignments = new SettingAssignments(temporalClass, formatStrings);

        env.clearCachedTemplateTemporalFormatsByFormatString();

        {
            List<TemplateTemporalFormat> cachedFormats = getFormatsToBeCachedByFormatString(settingAssignments);
            testGetByFormatStringReturnsSameAsEarlierCached(temporalClass, settingAssignments, cachedFormats);
            testCurrentFormatsSameAsEarlierCached(temporalClass, settingAssignments, cachedFormats);
            testGetByFormatStringReturnsSameAsEarlierCached(temporalClass, settingAssignments, cachedFormats);
        }

        {
            List<TemplateTemporalFormat> cachedFormats = getFormatsToBeCachedByFormatString(settingAssignments);
            Locale prevLocale = env.getLocale();
            env.setLocale(Locale.GERMAN);
            try {
                if (localeDependent) {
                    testGetByFormatReturnsDifferentThanEarlierCached(temporalClass, settingAssignments, cachedFormats);
                } else {
                    testGetByFormatStringReturnsSameAsEarlierCached(temporalClass, settingAssignments, cachedFormats);
                }
            } finally {
                env.setLocale(prevLocale);
            }
        }

        {
            List<TemplateTemporalFormat> cachedFormats = getFormatsToBeCachedByFormatString(settingAssignments);
            TimeZone prevTimeZone = env.getTimeZone();
            env.setTimeZone(OTHER_TIME_ZONE);
            try {
                if (timeZoneDependent) {
                    testGetByFormatReturnsDifferentThanEarlierCached(temporalClass, settingAssignments, cachedFormats);
                } else {
                    testGetByFormatStringReturnsSameAsEarlierCached(temporalClass, settingAssignments, cachedFormats);
                }
            } finally {
                env.setTimeZone(prevTimeZone);
            }
        }
    }

    @Test
    public void testCacheOverflow() throws TemplateValueFormatException {
        ArrayList<Locale> locales = new ArrayList<>();
        for (int n = 0; n < 50; n++) {
            locales.add(new Locale("en", "US", "v" + n));
        }

        String formatString = "yyyy-MM-dd";

        List<TemplateTemporalFormat> onceCachedFormats = new ArrayList<>();
        for (Locale locale : locales) {
            env.setLocale(locale);
            onceCachedFormats.add(env.getTemplateTemporalFormat(formatString, LocalDate.class));
        }

        for (int i = 1; i <= 3; i++) {
            env.setLocale(locales.get(locales.size() - i));
            JavaTemplateTemporalFormatFactory.INSTANCE.clear();
            assertSame(
                    env.getTemplateTemporalFormat(formatString, LocalDate.class),
                    onceCachedFormats.get(onceCachedFormats.size() - i));
        }

        env.setLocale(locales.get(0));
        JavaTemplateTemporalFormatFactory.INSTANCE.clear();
        assertNotSame(
                env.getTemplateTemporalFormat(formatString, LocalDate.class),
                onceCachedFormats.get(0));
    }

    private void testGetByFormatReturnsDifferentThanEarlierCached(Class<? extends Temporal> temporalClass,
            SettingAssignments settingAssignments,
            List<TemplateTemporalFormat> cachedFormats) throws
            TemplateValueFormatException {
        for (int valueIndex = 0; valueIndex < settingAssignments.numberOfValues(); valueIndex++) {
            String formatString = settingAssignments.getValue(valueIndex);
            TemplateTemporalFormat earlierFormat = cachedFormats.get(valueIndex);
            TemplateTemporalFormat currentFormat = env.getTemplateTemporalFormat(formatString, temporalClass);
            if (currentFormat == earlierFormat) {
                fail("Current format and earlier cache format shouldn't be the same, bu there were both thus: "
                        + currentFormat);
            }
        }
    }

    private void testCurrentFormatsSameAsEarlierCached(Class<? extends Temporal> temporalClass,
            SettingAssignments settingAssignments,
            List<TemplateTemporalFormat> cachedFormats) throws
            TemplateValueFormatException {
        for (int valueIndex = 0; valueIndex < settingAssignments.numberOfValues(); valueIndex++) {
            settingAssignments.execute(env, valueIndex);
            JavaTemplateTemporalFormatFactory.INSTANCE.clear();
            assertSame(env.getTemplateTemporalFormat(temporalClass), cachedFormats.get(valueIndex));
        }
    }

    private void testGetByFormatStringReturnsSameAsEarlierCached(Class<? extends Temporal> temporalClass,
            SettingAssignments settingAssignments,
            List<TemplateTemporalFormat> cachedFormats) throws
            TemplateValueFormatException {
        for (int valueIndex = 0; valueIndex < settingAssignments.numberOfValues(); valueIndex++) {
            String formatString = settingAssignments.getValue(valueIndex);
            JavaTemplateTemporalFormatFactory.INSTANCE.clear();
            assertSame(env.getTemplateTemporalFormat(formatString, temporalClass), cachedFormats.get(valueIndex));
        }
    }

    private List<TemplateTemporalFormat> getFormatsToBeCachedByFormatString(SettingAssignments settingAssignments)
            throws
            TemplateValueFormatException {
        List<TemplateTemporalFormat> cachedFormats = new ArrayList<>();
        for (int valueIndex = 0; valueIndex < settingAssignments.numberOfValues(); valueIndex++) {
            String formatString = settingAssignments.getValue(valueIndex);
            TemplateTemporalFormat format = env.getTemplateTemporalFormat(formatString,
                    settingAssignments.getTemporalClass());
            cachedFormats.add(format);
        }
        return cachedFormats;
    }

}
