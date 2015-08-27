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
import java.util.Locale;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

public class DummyTemplateNumberFormatFactory extends TemplateNumberFormatFactory {

    public static final DummyTemplateNumberFormatFactory INSTANCE = new DummyTemplateNumberFormatFactory();
    
    private DummyTemplateNumberFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public DummyLocalizedTemplateNumberFormatFactory getLocalizedFactory(Environment env, Locale locale) {
        return DummyLocalizedTemplateNumberFormatFactory.INSTANCE;
    }
    
    private static class DummyLocalizedTemplateNumberFormatFactory extends LocalizedTemplateNumberFormatFactory {

        private static final DummyLocalizedTemplateNumberFormatFactory INSTANCE = new DummyLocalizedTemplateNumberFormatFactory();

        private DummyLocalizedTemplateNumberFormatFactory() {
            super(null, null);
        }

        @Override
        public boolean isLocaleBound() {
            return false;
        }

        @Override
        public TemplateNumberFormat get(String formatDescriptor)
                throws ParseException, TemplateModelException, UnknownDateTypeFormattingUnsupportedException {
            return DummyTemplateNumberFormat.INSTANCE;
        }
        
    }
    
    private static class DummyTemplateNumberFormat extends TemplateNumberFormat {

        private static final DummyTemplateNumberFormat INSTANCE = new DummyTemplateNumberFormat();
        
        private DummyTemplateNumberFormat() { }
        
        @Override
        public String format(TemplateNumberModel numberModel)
                throws UnformattableNumberException, TemplateModelException {
            return "dummy" + numberModel.getAsNumber();
        }

        @Override
        public <MO extends TemplateMarkupOutputModel> MO format(TemplateNumberModel dateModel,
                MarkupOutputFormat<MO> outputFormat) throws UnformattableNumberException, TemplateModelException {
            return null;
        }

        @Override
        public String getDescription() {
            return "dummy";
        }

        @Override
        public boolean isLocaleBound() {
            return false;
        }
        
    }

}
