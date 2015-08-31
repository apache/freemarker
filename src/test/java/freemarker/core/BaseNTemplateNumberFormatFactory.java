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
import freemarker.template.utility.StringUtil;

public class BaseNTemplateNumberFormatFactory extends TemplateNumberFormatFactory {

    public static final BaseNTemplateNumberFormatFactory INSTANCE = new BaseNTemplateNumberFormatFactory();
    
    private BaseNTemplateNumberFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public BaseNLocalizedTemplateNumberFormatFactory createLocalFactory(Environment env, Locale locale) {
        return BaseNLocalizedTemplateNumberFormatFactory.INSTANCE;
    }
    
    private static class BaseNLocalizedTemplateNumberFormatFactory extends LocalTemplateNumberFormatFactory {

        private static final BaseNLocalizedTemplateNumberFormatFactory INSTANCE
                = new BaseNLocalizedTemplateNumberFormatFactory();
        
        private BaseNLocalizedTemplateNumberFormatFactory() {
            super(null, null);
        }

        @Override
        public TemplateNumberFormat get(String params) throws InvalidFormatParametersException {
            int base;
            try {
                base = Integer.parseInt(params);
            } catch (NumberFormatException e) {
                if (params.length() == 0) {
                    throw new InvalidFormatParametersException(
                            "A format parameter is required, which specifies the numerical system base.");
                }
                throw new InvalidFormatParametersException(
                        "The format paramter must be an integer, but was (shown quoted): " + StringUtil.jQuote(params));
            }
            return new BaseNTemplateNumberFormat(base);
        }

        @Override
        protected void onLocaleChanged() {
            // No op
        }
        
    }
    
    private static class BaseNTemplateNumberFormat extends TemplateNumberFormat {

        private final int base;
        
        private BaseNTemplateNumberFormat(int base) {
            this.base = base;
        }
        
        @Override
        public String format(TemplateNumberModel numberModel)
                throws UnformattableNumberException, TemplateModelException {
            Number n = TemplateNumberFormatUtil.getNonNullNumber(numberModel);
            try {
                return Integer.toString(NumberUtil.toIntExact(n), base);
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
