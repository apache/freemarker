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
import freemarker.template.utility.StringUtil;

/**
 * Shows a number in base N number system. Can only format numbers that fit into an {@code int},
 * however, optionally you can specify a fallback format. This format has one required parameter,
 * the numerical system base. That can be optionally followed by "|" and a fallback format.
 */
public class BaseNTemplateNumberFormatFactory extends TemplateNumberFormatFactory {

    public static final BaseNTemplateNumberFormatFactory INSTANCE
            = new BaseNTemplateNumberFormatFactory();
    
    private BaseNTemplateNumberFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws InvalidFormatParametersException {
        TemplateNumberFormat fallbackFormat;
        {
            int barIdx = params.indexOf('|');
            if (barIdx != -1) {
                String fallbackFormatStr = params.substring(barIdx + 1);
                params = params.substring(0, barIdx);
                try {
                    fallbackFormat = env.getTemplateNumberFormat(fallbackFormatStr, locale);
                } catch (TemplateValueFormatException e) {
                    throw new InvalidFormatParametersException(
                            "Couldn't get the fallback number format (specified after the \"|\"), "
                            + StringUtil.jQuote(fallbackFormatStr) + ". Reason: " + e.getMessage(),
                            e);
                }
            } else {
                fallbackFormat = null;
            }
        }
        
        int base;
        try {
            base = Integer.parseInt(params);
        } catch (NumberFormatException e) {
            if (params.length() == 0) {
                throw new InvalidFormatParametersException(
                        "A format parameter is required to specify the numerical system base.");
            }
            throw new InvalidFormatParametersException(
                    "The format paramter must be an integer, but was (shown quoted): "
                    + StringUtil.jQuote(params));
        }
        if (base < 2) {
            throw new InvalidFormatParametersException("A base must be at least 2.");
        }
        return new BaseNTemplateNumberFormat(base, fallbackFormat);
    }

    private static class BaseNTemplateNumberFormat extends TemplateNumberFormat {

        private final int base;
        private final TemplateNumberFormat fallbackFormat;
        
        private BaseNTemplateNumberFormat(int base, TemplateNumberFormat fallbackFormat) {
            this.base = base;
            this.fallbackFormat = fallbackFormat;
        }
        
        @Override
        public String formatToPlainText(TemplateNumberModel numberModel)
                throws TemplateModelException, TemplateValueFormatException {
            Number n = TemplateFormatUtil.getNonNullNumber(numberModel);
            try {
                return Integer.toString(NumberUtil.toIntExact(n), base);
            } catch (ArithmeticException e) {
                if (fallbackFormat == null) {
                    throw new UnformattableValueException(
                            n + " doesn't fit into an int, and there was no fallback format "
                            + "specified.");
                } else {
                    return fallbackFormat.formatToPlainText(numberModel);
                }
            }
        }

        @Override
        public boolean isLocaleBound() {
            return false;
        }

        @Override
        public String getDescription() {
            return "base " + base;
        }
        
    }

}
