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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.utility.StringUtil;

/**
 * Formats like {@code %G} in {@code printf}, with the specified number of significant digits. Also has special
 * formatter for HTML output format, where it uses the HTML "sup" element for exponents.
 */
public class PrintfGTemplateNumberFormatFactory extends TemplateNumberFormatFactory {

    public static final PrintfGTemplateNumberFormatFactory INSTANCE = new PrintfGTemplateNumberFormatFactory();
    
    private PrintfGTemplateNumberFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws InvalidFormatParametersException {
        Integer significantDigits;
        if (!params.isEmpty()) {
            try {
                significantDigits = Integer.valueOf(params);
            } catch (NumberFormatException e) {
                throw new InvalidFormatParametersException(
                        "The format parameter must be an integer, but was (shown quoted) "
                        + StringUtil.jQuote(params) + ".");
            }
        } else {
            // Use the default of %G
            significantDigits = null;
        }
        return new PrintfGTemplateNumberFormat(significantDigits, locale);
    }

    private static class PrintfGTemplateNumberFormat extends TemplateNumberFormat {
        
        private final Locale locale;
        private final String printfFormat; 

        private PrintfGTemplateNumberFormat(Integer significantDigits, Locale locale) {
            this.printfFormat = "%" + (significantDigits != null ? "." + significantDigits : "") + "G";
            this.locale = locale;
        }
        
        @Override
        public String formatToPlainText(TemplateNumberModel numberModel)
                throws UnformattableValueException, TemplateModelException {
            final Number n = TemplateFormatUtil.getNonNullNumber(numberModel);
            
            // printf %G only accepts Double, BigDecimal and Float 
            final Number gCompatibleN;
            if (n instanceof Double  || n instanceof BigDecimal || n instanceof Float) {
                gCompatibleN = n;
            } else {
                if (n instanceof BigInteger) {
                    gCompatibleN = new BigDecimal((BigInteger) n);                        
                } else if (n instanceof Long) {
                    gCompatibleN = BigDecimal.valueOf(n.longValue());
                } else {
                    gCompatibleN = Double.valueOf(n.doubleValue());
                }
            }
            
            return String.format(locale, printfFormat, gCompatibleN);
        }

        @Override
        public Object format(TemplateNumberModel numberModel)
                throws UnformattableValueException, TemplateModelException {
            String strResult = formatToPlainText(numberModel);
            
            int expIdx = strResult.indexOf('E');
            if (expIdx == -1) {
                return strResult;
            }
                
            String expStr = strResult.substring(expIdx + 1);
            int expSignifNumBegin = 0;
            while (expSignifNumBegin < expStr.length() && isExpSignifNumPrefix(expStr.charAt(expSignifNumBegin))) {
                expSignifNumBegin++;
            }
            
            return HTMLOutputFormat.INSTANCE.fromMarkup(
                    strResult.substring(0, expIdx)
                    + "*10<sup>"
                    + (expStr.charAt(0) == '-' ? "-" : "") + expStr.substring(expSignifNumBegin)
                    + "</sup>");
        }

        private boolean isExpSignifNumPrefix(char c) {
            return c == '+' || c == '-' || c == '0';
        }

        @Override
        public boolean isLocaleBound() {
            return true;
        }

        @Override
        public String getDescription() {
            return "printf " + printfFormat;
        }
        
    }

}
