/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.text.ParseException;
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
            ISOLikeTemplateDateFormatFactory factory)
            throws ParseException, UnknownDateTypeFormattingUnsupportedException {
        super(settingValue, parsingStart, dateType, zonelessInput, timeZone, factory);
    }
    
    protected String format(Date date, boolean datePart, boolean timePart, boolean offsetPart, int accuracy,
            TimeZone timeZone, DateToISO8601CalendarFactory calendarFactory) {
        return DateUtil.dateToXSString(
                date, datePart, timePart, offsetPart, accuracy, timeZone, calendarFactory);
    }

    protected Date parseDate(String s, TimeZone tz, CalendarFieldsToDateConverter calToDateConverter)
            throws DateParseException {
        return DateUtil.parseXSDate(s, tz, calToDateConverter);
    }

    protected Date parseTime(String s, TimeZone tz, CalendarFieldsToDateConverter calToDateConverter)
            throws DateParseException {
        return DateUtil.parseXSTime(s, tz, calToDateConverter);
    }

    protected Date parseDateTime(String s, TimeZone tz,
            CalendarFieldsToDateConverter calToDateConverter) throws DateParseException {
        return DateUtil.parseXSDateTime(s, tz, calToDateConverter);
    }

    protected String getDateDescription() {
        return "W3C XML Schema date";
    }

    protected String getTimeDescription() {
        return "W3C XML Schema time";
    }

    protected String getDateTimeDescription() {
        return "W3C XML Schema dateTime";
    }

    protected boolean isXSMode() {
        return true;
    }

}
