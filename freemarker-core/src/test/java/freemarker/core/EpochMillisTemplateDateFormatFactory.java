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

public class EpochMillisTemplateDateFormatFactory extends TemplateDateFormatFactory {

    public static final EpochMillisTemplateDateFormatFactory INSTANCE
            = new EpochMillisTemplateDateFormatFactory();
    
    private EpochMillisTemplateDateFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public TemplateDateFormat get(String params, int dateType,
            Locale locale, TimeZone timeZone, boolean zonelessInput,
            Environment env)
            throws InvalidFormatParametersException {
        TemplateFormatUtil.checkHasNoParameters(params);
        return EpochMillisTemplateDateFormat.INSTANCE;
    }

    private static class EpochMillisTemplateDateFormat extends TemplateDateFormat {

        private static final EpochMillisTemplateDateFormat INSTANCE
                = new EpochMillisTemplateDateFormat();
        
        private EpochMillisTemplateDateFormat() { }
        
        @Override
        public String formatToPlainText(TemplateDateModel dateModel)
                throws UnformattableValueException, TemplateModelException {
            return String.valueOf(TemplateFormatUtil.getNonNullDate(dateModel).getTime());
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
