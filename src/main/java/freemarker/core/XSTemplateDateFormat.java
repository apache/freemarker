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
            int dateType, TimeZone timeZone,
            ISOLikeTemplateDateFormatFactory factory)
            throws ParseException, UnknownDateTypeFormattingUnsupportedException {
        super(settingValue, parsingStart, dateType, timeZone, factory);
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
