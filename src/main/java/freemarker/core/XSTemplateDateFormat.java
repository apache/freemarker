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
import java.util.TimeZone;

import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.CalendarFieldsToDateConverter;
import freemarker.template.utility.DateUtil.DateParseException;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;

/**
 * XML Schema format.
 */
final class XSTemplateDateFormat extends ISOLikeTemplateDateFormat {

    XSTemplateDateFormat(
            String settingValue, int parsingStart,
            int dateType,
            boolean zonelessInput,
            TimeZone timeZone,
            ISOLikeTemplateDateFormatFactory factory,
            Environment env)
            throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        super(settingValue, parsingStart, dateType, zonelessInput, timeZone, factory, env);
    }
    
    @Override
    protected String format(Date date, boolean datePart, boolean timePart, boolean offsetPart, int accuracy,
            TimeZone timeZone, DateToISO8601CalendarFactory calendarFactory) {
        return DateUtil.dateToXSString(
                date, datePart, timePart, offsetPart, accuracy, timeZone, calendarFactory);
    }

    @Override
    protected Date parseDate(String s, TimeZone tz, CalendarFieldsToDateConverter calToDateConverter)
            throws DateParseException {
        return DateUtil.parseXSDate(s, tz, calToDateConverter);
    }

    @Override
    protected Date parseTime(String s, TimeZone tz, CalendarFieldsToDateConverter calToDateConverter)
            throws DateParseException {
        return DateUtil.parseXSTime(s, tz, calToDateConverter);
    }

    @Override
    protected Date parseDateTime(String s, TimeZone tz,
            CalendarFieldsToDateConverter calToDateConverter) throws DateParseException {
        return DateUtil.parseXSDateTime(s, tz, calToDateConverter);
    }

    @Override
    protected String getDateDescription() {
        return "W3C XML Schema date";
    }

    @Override
    protected String getTimeDescription() {
        return "W3C XML Schema time";
    }

    @Override
    protected String getDateTimeDescription() {
        return "W3C XML Schema dateTime";
    }

    @Override
    protected boolean isXSMode() {
        return true;
    }

}
