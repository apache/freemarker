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
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.cache.ConditionalTemplateConfigurationFactory;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.template.Configuration;
import freemarker.template.SimpleDate;
import freemarker.template.Template;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;
import freemarker.test.TemplateTest;

public class DateFormatTest extends TemplateTest {
    
    /** 2015-09-06T12:00:00Z */
    private static long T = 1441540800000L;
    private static TemplateDateModel TM = new SimpleDate(new Date(T), TemplateDateModel.DATETIME);
    
    @Before
    public void setup() {
        Configuration cfg = getConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        cfg.setLocale(Locale.US);
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+01:00"));
        cfg.setSQLDateAndTimeTimeZone(TimeZone.getTimeZone("UTC"));
        
        cfg.setCustomDateFormats(ImmutableMap.of(
                "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE,
                "loc", LocAndTZSensitiveTemplateDateFormatFactory.INSTANCE,
                "div", EpochMillisDivTemplateDateFormatFactory.INSTANCE,
                "appMeta", AppMetaTemplateDateFormatFactory.INSTANCE,
                "htmlIso", HTMLISOTemplateDateFormatFactory.INSTANCE));
    }

    @Test
    public void testCustomFormat() throws Exception {
        addToDataModel("d", new Date(123456789));
        assertOutput(
                "${d?string.@epoch} ${d?string.@epoch} <#setting locale='de_DE'>${d?string.@epoch}",
                "123456789 123456789 123456789");
        
        getConfiguration().setDateTimeFormat("@epoch");
        assertOutput(
                "<#assign d = d?datetime>"
                + "${d} ${d?string} <#setting locale='de_DE'>${d}",
                "123456789 123456789 123456789");
        
        getConfiguration().setDateTimeFormat("@htmlIso");
        assertOutput(
                "<#assign d = d?datetime>"
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
        
        getConfiguration().setDateTimeFormat("@loc");
        assertOutput(
                "<#assign d = d?datetime>"
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
        getConfiguration().setDateTimeFormat("iso");
        assertOutput(
                "${d?string.@loc} ${d?string.@loc} ${d?datetime?isoLocal} "
                + "<#setting timeZone='GMT+02:00'>"
                + "${d?string.@loc} ${d?string.@loc} ${d?datetime?isoLocal} "
                + "<#setting timeZone='GMT+01:00'>"
                + "${d?string.@loc} ${d?string.@loc} ${d?datetime?isoLocal}",
                "123456789@en_US:GMT+01:00 123456789@en_US:GMT+01:00 1970-01-02T11:17:36+01:00 "
                + "123456789@en_US:GMT+02:00 123456789@en_US:GMT+02:00 1970-01-02T12:17:36+02:00 "
                + "123456789@en_US:GMT+01:00 123456789@en_US:GMT+01:00 1970-01-02T11:17:36+01:00");
        
        getConfiguration().setDateTimeFormat("@loc");
        assertOutput(
                "<#assign d = d?datetime>"
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
        getConfiguration().setDateTimeFormat("x1");
        assertErrorContains("${.now}", "\"x1\"", "'x'");
        assertErrorContains("${.now?string}", "\"x1\"", "'x'");
        getConfiguration().setDateTimeFormat("short");
        assertErrorContains("${.now?string('x2')}", "\"x2\"", "'x'");
    }

    @Test
    public void testCustomParameterized() throws Exception {
        Configuration cfg = getConfiguration();
        addToDataModel("d", new SimpleDate(new Date(12345678L), TemplateDateModel.DATETIME));
        cfg.setDateTimeFormat("@div 1000");
        assertOutput("${d}", "12345");
        assertOutput("${d?string}", "12345");
        assertOutput("${d?string.@div_100}", "123456");
        
        assertErrorContains("${d?string.@div_xyz}", "\"@div_xyz\"", "\"xyz\"");
        cfg.setDateTimeFormat("@div");
        assertErrorContains("${d}", "\"datetime_format\"", "\"@div\"", "format parameter is required");
    }
    
    @Test
    public void testUnknownCustomFormat() throws Exception {
        {
            getConfiguration().setDateTimeFormat("@noSuchFormat");
            Throwable exc = assertErrorContains(
                    "${.now}",
                    "\"@noSuchFormat\"", "\"noSuchFormat\"", "\"datetime_format\"");
            assertThat(exc.getCause(), instanceOf(UndefinedCustomFormatException.class));
            
        }
        {
            getConfiguration().setDateFormat("@noSuchFormatD");
            assertErrorContains(
                    "${.now?date}",
                    "\"@noSuchFormatD\"", "\"noSuchFormatD\"", "\"date_format\"");
        }
        {
            getConfiguration().setTimeFormat("@noSuchFormatT");
            assertErrorContains(
                    "${.now?time}",
                    "\"@noSuchFormatT\"", "\"noSuchFormatT\"", "\"time_format\"");
        }

        {
            getConfiguration().setDateTimeFormat("");
            Throwable exc = assertErrorContains("${.now?string('@noSuchFormat2')}",
                    "\"@noSuchFormat2\"", "\"noSuchFormat2\"");
            assertThat(exc.getCause(), instanceOf(UndefinedCustomFormatException.class));
        }
    }
    
    @Test
    public void testNullInModel() throws Exception {
        addToDataModel("d", new MutableTemplateDateModel());
        assertErrorContains("${d}", "nothing inside it");
        assertErrorContains("${d?string}", "nothing inside it");
    }
    
    @Test
    public void testIcIAndEscaping() throws Exception {
        Configuration cfg = getConfiguration();
        addToDataModel("d", new SimpleDate(new Date(12345678L), TemplateDateModel.DATETIME));
        
        testIcIAndEscapingWhenCustFormsAreAccepted(cfg);
        
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_23);
        testIcIAndEscapingWhenCustFormsAreAccepted(cfg);
        
        cfg.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>emptyMap());
        
        cfg.setDateTimeFormat("@epoch");
        assertErrorContains("${d}", "\"@epoch\"");
        cfg.setDateTimeFormat("'@'yyyy");
        assertOutput("${d}", "@1970");
        cfg.setDateTimeFormat("@@yyyy");
        assertOutput("${d}", "@@1970");
        
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        cfg.setDateTimeFormat("@epoch");
        assertErrorContains("${d}", "custom", "\"epoch\"");
        cfg.setDateTimeFormat("'@'yyyy");
        assertOutput("${d}", "@1970");
        cfg.setDateTimeFormat("@@yyyy");
        assertOutput("${d}", "@@1970");
    }

    protected void testIcIAndEscapingWhenCustFormsAreAccepted(Configuration cfg) throws IOException, TemplateException {
        cfg.setDateTimeFormat("@epoch");
        assertOutput("${d}", "12345678");
        cfg.setDateTimeFormat("'@'yyyy");
        assertOutput("${d}", "@1970");
        cfg.setDateTimeFormat("@@yyyy");
        assertOutput("${d}", "@@1970");
    }
    
    @Test
    public void testEnvironmentGetters() throws Exception {
        Template t = new Template(null, "", getConfiguration());
        Environment env = t.createProcessingEnvironment(null, null);
        
        Configuration cfg = getConfiguration();
        
        String dateFormatStr = "yyyy.MM.dd. (Z)";
        String timeFormatStr = "HH:mm";
        String dateTimeFormatStr = "yyyy.MM.dd. HH:mm";
        cfg.setDateFormat(dateFormatStr);
        cfg.setTimeFormat(timeFormatStr);
        cfg.setDateTimeFormat(dateTimeFormatStr);
        
        // Test that values are coming from the cache if possible 
        for (Class dateClass : new Class[] { Date.class, Timestamp.class, java.sql.Date.class, Time.class } ) {
            for (int dateType
                    : new int[] { TemplateDateModel.DATE, TemplateDateModel.TIME, TemplateDateModel.DATETIME }) {
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
                env.getTemplateDateFormat(TemplateDateModel.DATETIME, Date.class).formatToPlainText(TM));
        assertEquals("2015.09.06. 13:00!",
                env.getTemplateDateFormat(dateTimeFormatStr2, TemplateDateModel.DATETIME, Date.class).formatToPlainText(TM));
        
        assertEquals("2015.09.06. (+0100)",
                env.getTemplateDateFormat(TemplateDateModel.DATE, Date.class).formatToPlainText(TM));
        assertEquals("2015.09.06. (+0100)!",
                env.getTemplateDateFormat(dateFormatStr2, TemplateDateModel.DATE, Date.class).formatToPlainText(TM));
        
        assertEquals("13:00",
                env.getTemplateDateFormat(TemplateDateModel.TIME, Date.class).formatToPlainText(TM));
        assertEquals("13:00!",
                env.getTemplateDateFormat(timeFormatStr2, TemplateDateModel.TIME, Date.class).formatToPlainText(TM));
        
        assertEquals("2015.09.06. 13:00",
                env.getTemplateDateFormat(TemplateDateModel.DATETIME, Timestamp.class).formatToPlainText(TM));
        assertEquals("2015.09.06. 13:00!",
                env.getTemplateDateFormat(dateTimeFormatStr2, TemplateDateModel.DATETIME, Timestamp.class).formatToPlainText(TM));

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
                    = env.getTemplateDateFormat(dateTimeFormatStrLoc, TemplateDateModel.DATETIME, Date.class);
            assertEquals("2015.09.06. 13:00 Sunday", format1.formatToPlainText(TM));
            // Different locale (not cached):
            assertEquals("2015.09.06. 13:00 Sonntag",
                    env.getTemplateDateFormat(dateTimeFormatStrLoc, TemplateDateModel.DATETIME, Date.class,
                            Locale.GERMANY).formatToPlainText(TM));
            // Different locale and zone (not cached):
            assertEquals("2015.09.06. 14:00 Sonntag",
                    env.getTemplateDateFormat(dateTimeFormatStrLoc, TemplateDateModel.DATETIME, Date.class,
                            Locale.GERMANY, TimeZone.getTimeZone("GMT+02"), TimeZone.getTimeZone("GMT+03")).formatToPlainText(TM));
            // Different locale and zone (not cached):
            assertEquals("2015.09.06. 15:00 Sonntag",
                    env.getTemplateDateFormat(dateTimeFormatStrLoc, TemplateDateModel.DATETIME, java.sql.Date.class,
                            Locale.GERMANY, TimeZone.getTimeZone("GMT+02"), TimeZone.getTimeZone("GMT+03")).formatToPlainText(TM));
            // Check for corrupted cache:
            TemplateDateFormat format2
                    = env.getTemplateDateFormat(dateTimeFormatStrLoc, TemplateDateModel.DATETIME, Date.class);
            assertEquals("2015.09.06. 13:00 Sunday", format2.formatToPlainText(TM));
            assertSame(format1, format2);
        }
        
        addToDataModel("d", TM);
        assertErrorContains("${d?string('[wrong]')}", "format string", "[wrong]");
        cfg.setDateFormat("[wrong d]");
        cfg.setDateTimeFormat("[wrong dt]");
        cfg.setTimeFormat("[wrong t]");
        assertErrorContains("${d?date}", "\"date_format\"", "[wrong d]");
        assertErrorContains("${d?datetime}", "\"datetime_format\"", "[wrong dt]");
        assertErrorContains("${d?time}", "\"time_format\"", "[wrong t]");
    }
    
    @Test
    public void testAlieses() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setCustomDateFormats(ImmutableMap.of(
                "d", new AliasTemplateDateFormatFactory("yyyy-MMM-dd"),
                "m", new AliasTemplateDateFormatFactory("yyyy-MMM"),
                "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE));
        
        TemplateConfiguration tc = new TemplateConfiguration();
        tc.setCustomDateFormats(ImmutableMap.of(
                "m", new AliasTemplateDateFormatFactory("yyyy-MMMM"),
                "i", new AliasTemplateDateFormatFactory("@epoch")));
        cfg.setTemplateConfigurations(new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*2*"), tc));
        
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
    public void testAlieses2() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setCustomDateFormats(ImmutableMap.of(
                "d", new AliasTemplateDateFormatFactory("yyyy-MMM",
                        ImmutableMap.of(
                                new Locale("en"), "yyyy-MMM'_en'",
                                Locale.UK, "yyyy-MMM'_en_GB'",
                                Locale.FRANCE, "yyyy-MMM'_fr_FR'"))));
        cfg.setDateTimeFormat("@d");
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
        Configuration cfg = getConfiguration();
        cfg.setDateFormat("@epoch");
        cfg.setDateTimeFormat("@epoch");
        cfg.setTimeFormat("@epoch");
        
        addToDataModel("t", String.valueOf(T));
        
        assertOutput(
                "${t?date?string.xs_u} ${t?date()?string.xs_u}",
                "2015-09-06Z 2015-09-06Z");
        assertOutput(
                "${t?time?string.xs_u} ${t?time()?string.xs_u}",
                "12:00:00Z 12:00:00Z");
        assertOutput(
                "${t?datetime?string.xs_u} ${t?datetime()?string.xs_u}",
                "2015-09-06T12:00:00Z 2015-09-06T12:00:00Z");
    }

    @Test
    public void testAppMetaRoundtrip() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        cfg.setDateFormat("@appMeta");
        cfg.setDateTimeFormat("@appMeta");
        cfg.setTimeFormat("@appMeta");
        
        addToDataModel("t", String.valueOf(T) + "/foo");
        
        assertOutput(
                "${t?date} ${t?date()}",
                T + " " + T + "/foo");
        assertOutput(
                "${t?time} ${t?time()}",
                T + " " + T + "/foo");
        assertOutput(
                "${t?datetime} ${t?datetime()}",
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

        public Date getAsDate() throws TemplateModelException {
            return date;
        }

        public int getDateType() {
            return DATETIME;
        }
        
    }
    
}
