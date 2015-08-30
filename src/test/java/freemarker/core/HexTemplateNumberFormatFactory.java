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

import java.util.Locale;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.utility.NumberUtil;

public class HexTemplateNumberFormatFactory extends TemplateNumberFormatFactory {

    public static final HexTemplateNumberFormatFactory INSTANCE = new HexTemplateNumberFormatFactory();
    
    private HexTemplateNumberFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public HexLocalizedTemplateNumberFormatFactory createLocalFactory(Environment env, Locale locale) {
        return HexLocalizedTemplateNumberFormatFactory.INSTANCE;
    }
    
    private static class HexLocalizedTemplateNumberFormatFactory extends LocalTemplateNumberFormatFactory {

        private static final HexLocalizedTemplateNumberFormatFactory INSTANCE = new HexLocalizedTemplateNumberFormatFactory();

        private HexLocalizedTemplateNumberFormatFactory() {
            super(null);
        }

        @Override
        public TemplateNumberFormat get(String params) throws InvalidFormatParametersException {
            TemplateNumberFormatUtil.checkHasNoParameters(params);
            return HexTemplateNumberFormat.INSTANCE;
        }

        @Override
        protected void onLocaleChanged() {
            // No op
        }
        
    }
    
    private static class HexTemplateNumberFormat extends TemplateNumberFormat {

        private static final HexTemplateNumberFormat INSTANCE = new HexTemplateNumberFormat();
        
        private HexTemplateNumberFormat() { }
        
        @Override
        public String format(TemplateNumberModel numberModel)
                throws UnformattableNumberException, TemplateModelException {
            Number n = TemplateNumberFormatUtil.getNonNullNumber(numberModel);
            try {
                return Integer.toHexString(NumberUtil.toIntExact(n));
            } catch (ArithmeticException e) {
                throw new UnformattableNumberException(n + " doesn't fit into an int");
            }
        }

        @Override
        public <MO extends TemplateMarkupOutputModel> MO format(TemplateNumberModel dateModel,
                MarkupOutputFormat<MO> outputFormat) throws UnformattableNumberException, TemplateModelException {
            return null;
        }

        @Override
        public boolean isLocaleBound() {
            return false;
        }

        @Override
        public String getDescription() {
            return "hexadecimal int";
        }
        
    }

}
