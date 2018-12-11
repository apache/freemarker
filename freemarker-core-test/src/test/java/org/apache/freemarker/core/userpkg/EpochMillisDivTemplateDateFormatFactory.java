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
package org.apache.freemarker.core.userpkg;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.core.valueformat.InvalidFormatParametersException;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateFormatUtil;
import org.apache.freemarker.core.valueformat.UnformattableValueException;
import org.apache.freemarker.core.valueformat.UnknownDateTypeFormattingUnsupportedException;
import org.apache.freemarker.core.valueformat.UnparsableValueException;

public class EpochMillisDivTemplateDateFormatFactory extends TemplateDateFormatFactory {

    public static final EpochMillisDivTemplateDateFormatFactory INSTANCE = new EpochMillisDivTemplateDateFormatFactory();
    
    private EpochMillisDivTemplateDateFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput,
            Environment env) throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        int divisor;
        try {
            divisor = Integer.parseInt(params);
        } catch (NumberFormatException e) {
            if (params.length() == 0) {
                throw new InvalidFormatParametersException(
                        "A format parameter is required, which specifies the divisor.");
            }
            throw new InvalidFormatParametersException(
                    "The format paramter must be an integer, but was (shown quoted): " + _StringUtils.jQuote(params));
        }
        return new EpochMillisDivTemplateDateFormat(divisor);
    }

    private static class EpochMillisDivTemplateDateFormat extends TemplateDateFormat {

        private final int divisor;
        
        private EpochMillisDivTemplateDateFormat(int divisor) {
            this.divisor = divisor;
        }
        
        @Override
        public String formatToPlainText(TemplateDateModel dateModel)
                throws UnformattableValueException, TemplateException {
            return String.valueOf(TemplateFormatUtil.getNonNullDate(dateModel).getTime() / divisor);
        }

        @Override
        public boolean isLocaleBound() {
            return false;
        }

        @Override
        public boolean isTimeZoneBound() {
            return false;
        }

        @Override
        public Date parse(String s, int dateType) throws UnparsableValueException {
            try {
                return new Date(Long.parseLong(s));
            } catch (NumberFormatException e) {
                throw new UnparsableValueException("Malformed long");
            }
        }

        @Override
        public String getDescription() {
            return "millis since the epoch";
        }
        
    }

}
