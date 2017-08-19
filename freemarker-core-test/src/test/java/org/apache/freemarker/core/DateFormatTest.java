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
package org.apache.freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.impl.SimpleDate;
import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.userpkg.AppMetaTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.EpochMillisDivTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.EpochMillisTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.HTMLISOTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.LocAndTZSensitiveTemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.UndefinedCustomFormatException;
import org.apache.freemarker.core.valueformat.impl.AliasTemplateDateFormatFactory;
import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class DateFormatTest extends TemplateTest {
    
    /** 2015-09-06T12:00:00Z */
    private static long T = 1441540800000L;
    private static TemplateDateModel TM = new SimpleDate(new Date(T), TemplateDateModel.DATE_TIME);
    
    private TestConfigurationBuilder createConfigurationBuilder() {
        return new TestConfigurationBuilder()
                .locale(Locale.US)
                .timeZone(TimeZone.getTimeZone("GMT+01:00"))
                .sqlDateAndTimeTimeZone(TimeZone.getTimeZone("UTC"))
                .customDateFormats(ImmutableMap.of(
                        "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE,
                        "loc", LocAndTZSensitiveTemplateDateFormatFactory.INSTANCE,
                        "div", EpochMillisDivTemplateDateFormatFactory.INSTANCE,
                        "appMeta", AppMetaTemplateDateFormatFactory.INSTANCE,
                        "htmlIso", HTMLISOTemplateDateFormatFactory.INSTANCE));
    }

    @Override
    protected Configuration createDefaultConfiguration() throws Exception {
        return createConfigurationBuilder().build();
    }

    @Test
    public void testCustomFormat() throws Exception {
        addToDataModel("d", new Date(123456789));
        assertOutput(
                "${d?string.@epoch} ${d?string.@epoch} <#setting locale='de_DE'>${d?string.@epoch}",
                "123456789 123456789 123456789");

        setConfigurationWithDateTimeFormat("@epoch");
        assertOutput(
                "<#assign d = d?dateTime>"
                + "${d} ${d?string} <#setting locale='de_DE'>${d}",
                "123456789 123456789 123456789");

        setConfigurationWithDateTimeFormat("@htmlIso");
        assertOutput(
                "<#assign d = d?dateTime>"
                + "${d} ${d?string} <#setting locale='de_DE'>${d}",
                "1970-01-02<span class='T'>T</span>10:17:36Z "
                + "1970-01-02T10:17:36Z "
                + "1970-01-02<span class='T'>T</span>10:17:36Z");
    }

    @Test
    public void testLocaleChange() throws Exception {
        addToDataModel("d", new Date(123456789));
        assertOutput(
                "${d?string.@loc} ${d?string.@loc} "
                + "<#setting locale='de_DE'>"
                + "${d?string.@loc} ${d?string.@loc} "
                + "<#setting locale='en_US'>"
                + "${d?string.@loc} ${d?string.@loc}",
                "123456789@en_US:GMT+01:00 123456789@en_US:GMT+01:00 "
                + "123456789@de_DE:GMT+01:00 123456789@de_DE:GMT+01:00 "
                + "123456789@en_US:GMT+01:00 123456789@en_US:GMT+01:00");

        setConfigurationWithDateTimeFormat("@loc");
        assertOutput(
                "<#assign d = d?dateTime>"
                + "${d} ${d?string} "
                + "<#setting locale='de_DE'>"
                + "${d} ${d?string} "
                + "<#setting locale='en_US'>"
                + "${d} ${d?string}",
                "123456789@en_US:GMT+01:00 123456789@en_US:GMT+01:00 "
                + "123456789@de_DE:GMT+01:00 123456789@de_DE:GMT+01:00 "
                + "123456789@en_US:GMT+01:00 123456789@en_US:GMT+01:00");
    }

    @Test
    public void testTimeZoneChange() throws Exception {
        addToDataModel("d", new Date(123456789));
        setConfigurationWithDateTimeFormat("iso");
        assertOutput(
                "${d?string.@loc} ${d?string.@loc} ${d?dateTime?isoLocal} "
                + "<#setting timeZone='GMT+02:00'>"
                + "${d?string.@loc} ${d?string.@loc} ${d?dateTime?isoLocal} "
                + "<#setting timeZone='GMT+01:00'>"
                + "${d?string.@loc} ${d?string.@loc} ${d?dateTime?isoLocal}",
                "123456789@en_US:GMT+01:00 123456789@en_US:GMT+01:00 1970-01-02T11:17:36+01:00 "
                + "123456789@en_US:GMT+02:00 123456789@en_US:GMT+02:00 1970-01-02T12:17:36+02:00 "
                + "123456789@en_US:GMT+01:00 123456789@en_US:GMT+01:00 1970-01-02T11:17:36+01:00");

        setConfigurationWithDateTimeFormat("@loc");
        assertOutput(
                "<#assign d = d?dateTime>"
                + "${d} ${d?string} "
                + "<#setting timeZone='GMT+02:00'>"
                + "${d} ${d?string} "
                + "<#setting timeZone='GMT+01:00'>"
                + "${d} ${d?string}",
                "123456789@en_US:GMT+01:00 123456789@en_US:GMT+01:00 "
                + "123456789@en_US:GMT+02:00 123456789@en_US:GMT+02:00 "
                + "123456789@en_US:GMT+01:00 123456789@en_US:GMT+01:00");
    }
    
    @Test
    public void testWrongFormatStrings() throws Exception {
        setConfigurationWithDateTimeFormat("x1");
        assertErrorContains("${.now}", "\"x1\"", "'x'");
        assertErrorContains("${.now?string}", "\"x1\"", "'x'");
        setConfigurationWithDateTimeFormat("short");
        assertErrorContains("${.now?string('x2')}", "\"x2\"", "'x'");
        assertErrorContains("${.now?string('[wrong]')}", "format string", "[wrong]");

        setConfiguration(createConfigurationBuilder()
                .dateFormat("[wrong d]")
                .dateTimeFormat("[wrong dt]")
                .timeFormat("[wrong t]")
                .build());
        assertErrorContains("${.now?date}", "\"dateFormat\"", "[wrong d]");
        assertErrorContains("${.now?dateTime}", "\"dateTimeFormat\"", "[wrong dt]");
        assertErrorContains("${.now?time}", "\"timeFormat\"", "[wrong t]");
    }

    @Test
    public void testCustomParameterized() throws Exception {
        Configuration cfg = getConfiguration();
        addToDataModel("d", new SimpleDate(new Date(12345678L), TemplateDateModel.DATE_TIME));
        setConfigurationWithDateTimeFormat("@div 1000");
        assertOutput("${d}", "12345");
        assertOutput("${d?string}", "12345");
        assertOutput("${d?string.@div_100}", "123456");
        
        assertErrorContains("${d?string.@div_xyz}", "\"@div_xyz\"", "\"xyz\"");
        setConfigurationWithDateTimeFormat("@div");
        assertErrorContains("${d}", "\"dateTimeFormat\"", "\"@div\"", "format parameter is required");
    }
    
    @Test
    public void testUnknownCustomFormat() throws Exception {
        {
            setConfigurationWithDateTimeFormat("@noSuchFormat");
            Throwable exc = assertErrorContains(
                    "${.now}",
                    "\"@noSuchFormat\"", "\"noSuchFormat\"", "\"dateTimeFormat\"");
            assertThat(exc.getCause(), instanceOf(UndefinedCustomFormatException.class));
            
        }
        {
            setConfiguration(createConfigurationBuilder().dateFormat("@noSuchFormatD").build());
            assertErrorContains(
                    "${.now?date}",
                    "\"@noSuchFormatD\"", "\"noSuchFormatD\"", "\"dateFormat\"");
        }
        {
            setConfiguration(createConfigurationBuilder().timeFormat("@noSuchFormatT").build());
            assertErrorContains(
                    "${.now?time}",
                    "\"@noSuchFormatT\"", "\"noSuchFormatT\"", "\"timeFormat\"");
        }

        {
            setConfigurationWithDateTimeFormat("");
            Throwable exc = assertErrorContains("${.now?string('@noSuchFormat2')}",
                    "\"@noSuchFormat2\"", "\"noSuchFormat2\"");
            assertThat(exc.getCause(), instanceOf(UndefinedCustomFormatException.class));
        }
    }

    private void setConfigurationWithDateTimeFormat(String formatString) {
        setConfiguration(createConfigurationBuilder().dateTimeFormat(formatString).build());
    }

    @Test
    public void testNullInModel() throws Exception {
        addToDataModel("d", new MutableTemplateDateModel());
        assertErrorContains("${d}", "nothing inside it");
        assertErrorContains("${d?string}", "nothing inside it");
    }
    
    @Test
    public void testIcIAndEscaping() throws Exception {
        addToDataModel("d", new SimpleDate(new Date(12345678L), TemplateDateModel.DATE_TIME));
        
        setConfigurationWithDateTimeFormat("@epoch");
        assertOutput("${d}", "12345678");
        setConfigurationWithDateTimeFormat("'@'yyyy");
        assertOutput("${d}", "@1970");
        setConfigurationWithDateTimeFormat("@@yyyy");
        assertOutput("${d}", "@@1970");

        setConfiguration(createConfigurationBuilder()
                .customDateFormats(Collections.<String, TemplateDateFormatFactory>emptyMap())
                .dateTimeFormat("@epoch")
                .build());
        assertErrorContains("${d}", "custom", "\"epoch\"");
    }

    @Test
    public void testEnvironmentGetters() throws Exception {
        String dateFormatStr = "yyyy.MM.dd. (Z)";
        String timeFormatStr = "HH:mm";
        String dateTimeFormatStr = "yyyy.MM.dd. HH:mm";

        setConfiguration(createConfigurationBuilder()
                .dateFormat(dateFormatStr)
                .timeFormat(timeFormatStr)
                .dateTimeFormat(dateTimeFormatStr)
                .build());

        Configuration cfg = getConfiguration();

        Template t = new Template(null, "", cfg);
        Environment env = t.createProcessingEnvironment(null, null);
        
        // Test that values are coming from the cache if possible
        for (Class dateClass : new Class[] { Date.class, Timestamp.class, java.sql.Date.class, Time.class } ) {
            for (int dateType
                    : new int[] { TemplateDateModel.DATE, TemplateDateModel.TIME, TemplateDateModel.DATE_TIME }) {
                String formatString =
                        dateType == TemplateDateModel.DATE ? cfg.getDateFormat() :
                        (dateType == TemplateDateModel.TIME ? cfg.getTimeFormat()
                        : cfg.getDateTimeFormat());
                TemplateDateFormat expectedF = env.getTemplateDateFormat(formatString, dateType, dateClass);
                assertSame(expectedF, env.getTemplateDateFormat(dateType, dateClass)); // Note: Only reads the cache
                assertSame(expectedF, env.getTemplateDateFormat(formatString, dateType, dateClass));
                assertSame(expectedF, env.getTemplateDateFormat(formatString, dateType, dateClass, cfg.getLocale()));
                assertSame(expectedF, env.getTemplateDateFormat(formatString, dateType, dateClass, cfg.getLocale(),
                        cfg.getTimeZone(), cfg.getSQLDateAndTimeTimeZone()));
            }
        }

        String dateFormatStr2 = dateFormatStr + "'!'";
        String timeFormatStr2 = timeFormatStr + "'!'";
        String dateTimeFormatStr2 = dateTimeFormatStr + "'!'";
        
        assertEquals("2015.09.06. 13:00",
                env.getTemplateDateFormat(TemplateDateModel.DATE_TIME, Date.class).formatToPlainText(TM));
        assertEquals("2015.09.06. 13:00!",
                env.getTemplateDateFormat(dateTimeFormatStr2, TemplateDateModel.DATE_TIME, Date.class).formatToPlainText(TM));
        
        assertEquals("2015.09.06. (+0100)",
                env.getTemplateDateFormat(TemplateDateModel.DATE, Date.class).formatToPlainText(TM));
        assertEquals("2015.09.06. (+0100)!",
                env.getTemplateDateFormat(dateFormatStr2, TemplateDateModel.DATE, Date.class).formatToPlainText(TM));
        
        assertEquals("13:00",
                env.getTemplateDateFormat(TemplateDateModel.TIME, Date.class).formatToPlainText(TM));
        assertEquals("13:00!",
                env.getTemplateDateFormat(timeFormatStr2, TemplateDateModel.TIME, Date.class).formatToPlainText(TM));
        
        assertEquals("2015.09.06. 13:00",
                env.getTemplateDateFormat(TemplateDateModel.DATE_TIME, Timestamp.class).formatToPlainText(TM));
        assertEquals("2015.09.06. 13:00!",
                env.getTemplateDateFormat(dateTimeFormatStr2, TemplateDateModel.DATE_TIME, Timestamp.class).formatToPlainText(TM));

        assertEquals("2015.09.06. (+0000)",
                env.getTemplateDateFormat(TemplateDateModel.DATE, java.sql.Date.class).formatToPlainText(TM));
        assertEquals("2015.09.06. (+0000)!",
                env.getTemplateDateFormat(dateFormatStr2, TemplateDateModel.DATE, java.sql.Date.class).formatToPlainText(TM));

        assertEquals("12:00",
                env.getTemplateDateFormat(TemplateDateModel.TIME, Time.class).formatToPlainText(TM));
        assertEquals("12:00!",
                env.getTemplateDateFormat(timeFormatStr2, TemplateDateModel.TIME, Time.class).formatToPlainText(TM));

        {
            String dateTimeFormatStrLoc = dateTimeFormatStr + " EEEE";
            // Gets into cache:
            TemplateDateFormat format1
                    = env.getTemplateDateFormat(dateTimeFormatStrLoc, TemplateDateModel.DATE_TIME, Date.class);
            assertEquals("2015.09.06. 13:00 Sunday", format1.formatToPlainText(TM));
            // Different locale (not cached):
            assertEquals("2015.09.06. 13:00 Sonntag",
                    env.getTemplateDateFormat(dateTimeFormatStrLoc, TemplateDateModel.DATE_TIME, Date.class,
                            Locale.GERMANY).formatToPlainText(TM));
            // Different locale and zone (not cached):
            assertEquals("2015.09.06. 14:00 Sonntag",
                    env.getTemplateDateFormat(dateTimeFormatStrLoc, TemplateDateModel.DATE_TIME, Date.class,
                            Locale.GERMANY, TimeZone.getTimeZone("GMT+02"), TimeZone.getTimeZone("GMT+03")).formatToPlainText(TM));
            // Different locale and zone (not cached):
            assertEquals("2015.09.06. 15:00 Sonntag",
                    env.getTemplateDateFormat(dateTimeFormatStrLoc, TemplateDateModel.DATE_TIME, java.sql.Date.class,
                            Locale.GERMANY, TimeZone.getTimeZone("GMT+02"), TimeZone.getTimeZone("GMT+03")).formatToPlainText(TM));
            // Check for corrupted cache:
            TemplateDateFormat format2
                    = env.getTemplateDateFormat(dateTimeFormatStrLoc, TemplateDateModel.DATE_TIME, Date.class);
            assertEquals("2015.09.06. 13:00 Sunday", format2.formatToPlainText(TM));
            assertSame(format1, format2);
        }
    }

    @Test
    public void testAliases() throws Exception {
        setConfiguration(createConfigurationBuilder()
                .customDateFormats(ImmutableMap.of(
                        "d", new AliasTemplateDateFormatFactory("yyyy-MMM-dd"),
                        "m", new AliasTemplateDateFormatFactory("yyyy-MMM"),
                        "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE))
                .templateConfigurations(
                        new ConditionalTemplateConfigurationFactory(
                                new FileNameGlobMatcher("*2*"),
                                new TemplateConfiguration.Builder()
                                        .customDateFormats(ImmutableMap.<String, TemplateDateFormatFactory>of(
                                                "m", new AliasTemplateDateFormatFactory("yyyy-MMMM"),
                                                "i", new AliasTemplateDateFormatFactory("@epoch")))
                                        .build()))
                .build());

        addToDataModel("d", TM);
        String commonFtl = "${d?string.@d} ${d?string.@m} "
                + "<#setting locale='fr_FR'>${d?string.@m} "
                + "<#attempt>${d?string.@i}<#recover>E</#attempt>";
        addTemplate("t1.ftl", commonFtl);
        addTemplate("t2.ftl", commonFtl);
        
        // 2015-09-06T12:00:00Z
        assertOutputForNamed("t1.ftl", "2015-Sep-06 2015-Sep 2015-sept. E");
        assertOutputForNamed("t2.ftl", "2015-Sep-06 2015-September 2015-septembre " + T);
    }
    
    @Test
    public void testAliases2() throws Exception {
        setConfiguration(
                createConfigurationBuilder()
                .customDateFormats(ImmutableMap.<String, TemplateDateFormatFactory>of(
                        "d", new AliasTemplateDateFormatFactory("yyyy-MMM",
                                ImmutableMap.of(
                                        new Locale("en"), "yyyy-MMM'_en'",
                                        Locale.UK, "yyyy-MMM'_en_GB'",
                                        Locale.FRANCE, "yyyy-MMM'_fr_FR'"))))
                .dateTimeFormat("@d")
                .build());
        addToDataModel("d", TM);
        assertOutput(
                "<#setting locale='en_US'>${d} "
                + "<#setting locale='en_GB'>${d} "
                + "<#setting locale='en_GB_Win'>${d} "
                + "<#setting locale='fr_FR'>${d} "
                + "<#setting locale='hu_HU'>${d}",
                "2015-Sep_en 2015-Sep_en_GB 2015-Sep_en_GB 2015-sept._fr_FR 2015-szept.");
    }
    
    /**
     * ?date() and such are new in 2.3.24.
     */
    @Test
    public void testZeroArgDateBI() throws IOException, TemplateException {
        setConfiguration(
                createConfigurationBuilder()
                .dateFormat("@epoch")
                .dateTimeFormat("@epoch")
                .timeFormat("@epoch")
                .build());

        addToDataModel("t", String.valueOf(T));
        
        assertOutput(
                "${t?date?string.xs_u} ${t?date()?string.xs_u}",
                "2015-09-06Z 2015-09-06Z");
        assertOutput(
                "${t?time?string.xs_u} ${t?time()?string.xs_u}",
                "12:00:00Z 12:00:00Z");
        assertOutput(
                "${t?dateTime?string.xs_u} ${t?dateTime()?string.xs_u}",
                "2015-09-06T12:00:00Z 2015-09-06T12:00:00Z");
    }

    @Test
    public void testAppMetaRoundtrip() throws IOException, TemplateException {
        setConfiguration(
                createConfigurationBuilder()
                .dateFormat("@appMeta")
                .dateTimeFormat("@appMeta")
                .timeFormat("@appMeta")
                .build());

        addToDataModel("t", String.valueOf(T) + "/foo");
        
        assertOutput(
                "${t?date} ${t?date()}",
                T + " " + T + "/foo");
        assertOutput(
                "${t?time} ${t?time()}",
                T + " " + T + "/foo");
        assertOutput(
                "${t?dateTime} ${t?dateTime()}",
                T + " " + T + "/foo");
    }
    
    @Test
    public void testUnknownDateType() throws IOException, TemplateException {
        addToDataModel("u", new Date(T));
        assertErrorContains("${u?string}", "isn't known");
        assertOutput("${u?string('yyyy')}", "2015");
        assertOutput("<#assign s = u?string>${s('yyyy')}", "2015");
    }
    
    private static class MutableTemplateDateModel implements TemplateDateModel {
        
        private Date date;

        public void setDate(Date date) {
            this.date = date;
        }

        @Override
        public Date getAsDate() throws TemplateException {
            return date;
        }

        @Override
        public int getDateType() {
            return DATE_TIME;
        }
        
    }
    
}
