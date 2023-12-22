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
import freemarker.template.utility.NumberUtil;

public class HexTemplateNumberFormatFactory extends TemplateNumberFormatFactory {

    public static final HexTemplateNumberFormatFactory INSTANCE = new HexTemplateNumberFormatFactory();
    
    private HexTemplateNumberFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws InvalidFormatParametersException {
        TemplateFormatUtil.checkHasNoParameters(params);
        return HexTemplateNumberFormat.INSTANCE;
    }

    private static class HexTemplateNumberFormat extends TemplateNumberFormat {

        private static final HexTemplateNumberFormat INSTANCE = new HexTemplateNumberFormat();
        
        private HexTemplateNumberFormat() { }
        
        @Override
        public String formatToPlainText(TemplateNumberModel numberModel)
                throws UnformattableValueException, TemplateModelException {
            Number n = TemplateFormatUtil.getNonNullNumber(numberModel);
            try {
                return Integer.toHexString(NumberUtil.toIntExact(n));
            } catch (ArithmeticException e) {
                throw new UnformattableValueException(n + " doesn't fit into an int");
            }
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
