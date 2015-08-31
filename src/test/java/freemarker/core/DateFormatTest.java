/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.SimpleDate;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;
import freemarker.test.TemplateTest;

@SuppressWarnings("boxing")
public class DateFormatTest extends TemplateTest {
    
    @Before
    public void setup() {
        Configuration cfg = getConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        cfg.setLocale(Locale.US);
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+01:00"));
        
        cfg.setCustomDateFormats(ImmutableMap.of(
                "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE,
                "loc", LocAndTZSensitiveTemplateDateFormatFactory.INSTANCE,
                "div", EpochMillisDivTemplateDateFormatFactory.INSTANCE));
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
        cfg.setDateTimeFormat("@@yyyy");
        assertOutput("${d}", "@1970");
        cfg.setDateTimeFormat("@epoch");
        assertOutput("${d}", "12345678");
        
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_23);
        cfg.setDateTimeFormat("@@yyyy");
        assertOutput("${d}", "@@1970");
        cfg.setDateTimeFormat("@epoch");
        assertErrorContains("${d}", "\"@epoch\"");
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
