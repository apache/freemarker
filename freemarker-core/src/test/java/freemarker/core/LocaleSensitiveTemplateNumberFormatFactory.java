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

import java.util.Locale;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

public class LocaleSensitiveTemplateNumberFormatFactory extends TemplateNumberFormatFactory {

    public static final LocaleSensitiveTemplateNumberFormatFactory INSTANCE = new LocaleSensitiveTemplateNumberFormatFactory();
    
    private LocaleSensitiveTemplateNumberFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws InvalidFormatParametersException {
        TemplateFormatUtil.checkHasNoParameters(params);
        return new LocaleSensitiveTemplateNumberFormat(locale);
    }

    private static class LocaleSensitiveTemplateNumberFormat extends TemplateNumberFormat {
    
        private final Locale locale;
        
        private LocaleSensitiveTemplateNumberFormat(Locale locale) {
            this.locale = locale;
        }
        
        @Override
        public String formatToPlainText(TemplateNumberModel numberModel)
                throws UnformattableValueException, TemplateModelException {
            Number n = numberModel.getAsNumber();
            try {
                return n + "_" + locale;
            } catch (ArithmeticException e) {
                throw new UnformattableValueException(n + " doesn't fit into an int");
            }
        }
    
        @Override
        public boolean isLocaleBound() {
            return true;
        }
    
        @Override
        public String getDescription() {
            return "test locale sensitive";
        }
        
    }

}
