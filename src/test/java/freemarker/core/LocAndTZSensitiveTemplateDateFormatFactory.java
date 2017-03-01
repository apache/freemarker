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

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;

public class LocAndTZSensitiveTemplateDateFormatFactory extends TemplateDateFormatFactory {

    public static final LocAndTZSensitiveTemplateDateFormatFactory INSTANCE = new LocAndTZSensitiveTemplateDateFormatFactory();
    
    private LocAndTZSensitiveTemplateDateFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput,
            Environment env) throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        TemplateFormatUtil.checkHasNoParameters(params);
        return new LocAndTZSensitiveTemplateDateFormat(locale, timeZone);
    }

    private static class LocAndTZSensitiveTemplateDateFormat extends TemplateDateFormat {

        private final Locale locale;
        private final TimeZone timeZone;
        
        public LocAndTZSensitiveTemplateDateFormat(Locale locale, TimeZone timeZone) {
            this.locale = locale;
            this.timeZone = timeZone;
        }

        @Override
        public String formatToPlainText(TemplateDateModel dateModel)
                throws UnformattableValueException, TemplateModelException {
            return String.valueOf(TemplateFormatUtil.getNonNullDate(dateModel).getTime() + "@" + locale + ":" + timeZone.getID());
        }

        @Override
        public boolean isLocaleBound() {
            return true;
        }

        @Override
        public boolean isTimeZoneBound() {
            return true;
        }

        @Override
        public Date parse(String s, int dateType) throws UnparsableValueException {
            try {
                int atIdx = s.indexOf("@");
                if (atIdx == -1) {
                    throw new UnparsableValueException("Missing @");
                }
                return new Date(Long.parseLong(s.substring(0, atIdx)));
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
