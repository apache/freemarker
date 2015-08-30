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

import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
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
        
        cfg.setCustomDateFormats(ImmutableMap.of(
                "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE));
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
    public void testNullInNumberModel() throws Exception {
        addToDataModel("n", new MutableTemplateDateModel());
        assertErrorContains("${n}", "nothing inside it");
        assertErrorContains("${n?string}", "nothing inside it");
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
