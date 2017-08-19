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
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.util._DateUtils;
import org.apache.freemarker.core.util._DateUtils.CalendarFieldsToDateConverter;
import org.apache.freemarker.core.util._DateUtils.DateParseException;
import org.apache.freemarker.core.valueformat.InvalidFormatParametersException;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateFormatUtil;
import org.apache.freemarker.core.valueformat.TemplateValueFormatException;
import org.apache.freemarker.core.valueformat.UnformattableValueException;
import org.apache.freemarker.core.valueformat.UnknownDateTypeFormattingUnsupportedException;
import org.apache.freemarker.core.valueformat.UnparsableValueException;

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

        private _DateUtils.TrivialDateToISO8601CalendarFactory calendarFactory;

        private CalendarFieldsToDateConverter calToDateConverter;
        
        private HTMLISOTemplateDateFormat() { }
        
        @Override
        public String formatToPlainText(TemplateDateModel dateModel)
                throws UnformattableValueException, TemplateException {
            if (calendarFactory == null) {
                calendarFactory = new _DateUtils.TrivialDateToISO8601CalendarFactory();
            }
            return _DateUtils.dateToISO8601String(
                    TemplateFormatUtil.getNonNullDate(dateModel),
                    true, true, true, _DateUtils.ACCURACY_SECONDS, _DateUtils.UTC,
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
                    calToDateConverter = new _DateUtils.TrivialCalendarFieldsToDateConverter();
                }
                return _DateUtils.parseISO8601DateTime(s, _DateUtils.UTC, calToDateConverter);
            } catch (DateParseException e) {
                throw new UnparsableValueException("Malformed ISO date-time", e);
            }
        }

        @Override
        public Object format(TemplateDateModel dateModel) throws TemplateValueFormatException, TemplateException {
            return HTMLOutputFormat.INSTANCE.fromMarkup(
                    formatToPlainText(dateModel).replace("T", "<span class='T'>T</span>"));
        }

        @Override
        public String getDescription() {
            return "ISO UTC HTML";
        }
        
    }

}
 