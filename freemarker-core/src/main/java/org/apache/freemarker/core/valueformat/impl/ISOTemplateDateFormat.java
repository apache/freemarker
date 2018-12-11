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

package org.apache.freemarker.core.valueformat.impl;

import java.util.Date;
import java.util.TimeZone;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.util._DateUtils;
import org.apache.freemarker.core.util._DateUtils.CalendarFieldsToDateConverter;
import org.apache.freemarker.core.util._DateUtils.DateParseException;
import org.apache.freemarker.core.util._DateUtils.DateToISO8601CalendarFactory;
import org.apache.freemarker.core.valueformat.InvalidFormatParametersException;
import org.apache.freemarker.core.valueformat.UnknownDateTypeFormattingUnsupportedException;

class ISOTemplateDateFormat extends ISOLikeTemplateDateFormat {

    ISOTemplateDateFormat(
            String settingValue, int parsingStart,
            int dateType, boolean zonelessInput,
            TimeZone timeZone,
            ISOLikeTemplateDateFormatFactory factory,
            Environment env)
            throws InvalidFormatParametersException, UnknownDateTypeFormattingUnsupportedException {
        super(settingValue, parsingStart, dateType, zonelessInput, timeZone, factory, env);
    }

    @Override
    protected String format(Date date, boolean datePart, boolean timePart, boolean offsetPart, int accuracy,
            TimeZone timeZone, DateToISO8601CalendarFactory calendarFactory) {
        return _DateUtils.dateToISO8601String(
                date, datePart, timePart, timePart && offsetPart, accuracy, timeZone, calendarFactory);
    }

    @Override
    protected Date parseDate(String s, TimeZone tz, CalendarFieldsToDateConverter calToDateConverter)
            throws DateParseException {
        return _DateUtils.parseISO8601Date(s, tz, calToDateConverter);
    }

    @Override
    protected Date parseTime(String s, TimeZone tz, CalendarFieldsToDateConverter calToDateConverter)
            throws DateParseException {
        return _DateUtils.parseISO8601Time(s, tz, calToDateConverter);
    }

    @Override
    protected Date parseDateTime(String s, TimeZone tz,
            CalendarFieldsToDateConverter calToDateConverter) throws DateParseException {
        return _DateUtils.parseISO8601DateTime(s, tz, calToDateConverter);
    }
    
    @Override
    protected String getDateDescription() {
        return "ISO 8601 (subset) date";
    }

    @Override
    protected String getTimeDescription() {
        return "ISO 8601 (subset) time";
    }

    @Override
    protected String getDateTimeDescription() {
        return "ISO 8601 (subset) date-time";
    }

    @Override
    protected boolean isXSMode() {
        return false;
    }

}
