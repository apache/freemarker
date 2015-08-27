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

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang.NotImplementedException;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;

public class DummyTemplateDateFormatFactory extends TemplateDateFormatFactory {

    public static final DummyTemplateDateFormatFactory INSTANCE = new DummyTemplateDateFormatFactory();
    
    private DummyTemplateDateFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public DummyLocalizedTemplateDateFormatFactory getLocalizedFactory(Environment env, Locale locale, TimeZone tz) {
        return DummyLocalizedTemplateDateFormatFactory.INSTANCE;
    }
    
    private static class DummyLocalizedTemplateDateFormatFactory extends LocalizedTemplateDateFormatFactory {

        private static final DummyLocalizedTemplateDateFormatFactory INSTANCE = new DummyLocalizedTemplateDateFormatFactory();

        private DummyLocalizedTemplateDateFormatFactory() {
            super(null, null, null);
        }

        @Override
        public boolean isLocaleBound() {
            return false;
        }

        @Override
        public TemplateDateFormat get(int dateType, boolean zonelessInput, String formatDescriptor) {
            return DummyTemplateDateFormat.INSTANCE;
        }
        
    }
    
    private static class DummyTemplateDateFormat extends TemplateDateFormat {

        private static final DummyTemplateDateFormat INSTANCE = new DummyTemplateDateFormat();
        
        private DummyTemplateDateFormat() { }
        
        @Override
        public String format(TemplateDateModel dateModel)
                throws UnformattableDateException, TemplateModelException {
            return "dummy" + dateModel.getAsDate().getTime();
        }

        @Override
        public String getDescription() {
            return "dummy";
        }

        @Override
        public boolean isLocaleBound() {
            return false;
        }

        @Override
        public <MO extends TemplateMarkupOutputModel> MO format(TemplateDateModel dateModel,
                MarkupOutputFormat<MO> outputFormat) throws UnformattableNumberException, TemplateModelException {
            throw new NotImplementedException();
        }

        @Override
        public Date parse(String s) throws ParseException {
            throw new NotImplementedException();
        }
        
    }

}
