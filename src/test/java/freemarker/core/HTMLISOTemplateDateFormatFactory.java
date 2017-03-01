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
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.CalendarFieldsToDateConverter;
import freemarker.template.utility.DateUtil.DateParseException;

public class HTMLISOTemplateDateFormatFactory extends TemplateDateFormatFactory {

    public static final HTMLISOTemplateDateFormatFactory INSTANCE = new HTMLISOTemplateDateFormatFactory();
    
    private HTMLISOTemplateDateFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput,
            Environment env) throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        TemplateFormatUtil.checkHasNoParameters(params);
        return HTMLISOTemplateDateFormat.INSTANCE;
    }

    private static class HTMLISOTemplateDateFormat extends TemplateDateFormat {

        private static final HTMLISOTemplateDateFormat INSTANCE = new HTMLISOTemplateDateFormat();

        private DateUtil.TrivialDateToISO8601CalendarFactory calendarFactory;

        private CalendarFieldsToDateConverter calToDateConverter;
        
        private HTMLISOTemplateDateFormat() { }
        
        @Override
        public String formatToPlainText(TemplateDateModel dateModel)
                throws UnformattableValueException, TemplateModelException {
            if (calendarFactory == null) {
                calendarFactory = new DateUtil.TrivialDateToISO8601CalendarFactory();
            }
            return DateUtil.dateToISO8601String(
                    TemplateFormatUtil.getNonNullDate(dateModel),
                    true, true, true, DateUtil.ACCURACY_SECONDS, DateUtil.UTC,
                    calendarFactory);
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
                if (calToDateConverter == null) {
                    calToDateConverter = new DateUtil.TrivialCalendarFieldsToDateConverter(); 
                }
                return DateUtil.parseISO8601DateTime(s, DateUtil.UTC, calToDateConverter);
            } catch (DateParseException e) {
                throw new UnparsableValueException("Malformed ISO date-time", e);
            }
        }

        @Override
        public Object format(TemplateDateModel dateModel) throws TemplateValueFormatException, TemplateModelException {
            return HTMLOutputFormat.INSTANCE.fromMarkup(
                    formatToPlainText(dateModel).replace("T", "<span class='T'>T</span>"));
        }

        @Override
        public String getDescription() {
            return "ISO UTC HTML";
        }
        
    }

}
 