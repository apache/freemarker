package freemarker.core;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.CalendarFieldsToDateConverter;
import freemarker.template.utility.DateUtil.DateParseException;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;

final class ISOTemplateDateFormat extends ISOLikeTemplateDateFormat {

    ISOTemplateDateFormat(
            String settingValue, int parsingStart,
            int dateType, TimeZone timeZone,
            ISOLikeTemplateDateFormatFactory factory)
            throws ParseException, UnknownDateTypeFormattingUnsupportedException {
        super(settingValue, parsingStart, dateType, timeZone, factory);
    }

    protected String format(Date date, boolean datePart, boolean timePart, boolean offsetPart, int accuracy,
            TimeZone timeZone, DateToISO8601CalendarFactory calendarFactory) {
        return DateUtil.dateToISO8601String(
                date, datePart, timePart, timePart && offsetPart, accuracy, timeZone, calendarFactory);
    }

    protected Date parseDate(String s, TimeZone tz, CalendarFieldsToDateConverter calToDateConverter)
            throws DateParseException {
        return DateUtil.parseISO8601Date(s, tz, calToDateConverter);
    }

    protected Date parseTime(String s, TimeZone tz, CalendarFieldsToDateConverter calToDateConverter)
            throws DateParseException {
        return DateUtil.parseISO8601Time(s, tz, calToDateConverter);
    }

    protected Date parseDateTime(String s, TimeZone tz,
            CalendarFieldsToDateConverter calToDateConverter) throws DateParseException {
        return DateUtil.parseISO8601DateTime(s, tz, calToDateConverter);
    }
    
    protected String getDateDescription() {
        return "ISO 8601 (subset) date";
    }

    protected String getTimeDescription() {
        return "ISO 8601 (subset) time";
    }

    protected String getDateTimeDescription() {
        return "ISO 8601 (subset) date-time";
    }

    protected boolean isXSMode() {
        return false;
    }

}
