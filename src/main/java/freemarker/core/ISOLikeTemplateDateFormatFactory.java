package freemarker.core;

import java.util.TimeZone;

import freemarker.template.utility.DateUtil.CalendarFieldsToDateConverter;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;
import freemarker.template.utility.DateUtil.TrivialCalendarFieldsToDateConverter;
import freemarker.template.utility.DateUtil.TrivialDateToISO8601CalendarFactory;

abstract class ISOLikeTemplateDateFormatFactory extends TemplateDateFormatFactory {
    
    private DateToISO8601CalendarFactory dateToCalenderFieldsCalculator;
    private CalendarFieldsToDateConverter calendarFieldsToDateConverter;

    public ISOLikeTemplateDateFormatFactory(TimeZone timeZone) {
        super(timeZone);
    }

    public boolean isLocaleBound() {
        return false;
    }

    public DateToISO8601CalendarFactory getISOBuiltInCalendar() {
        DateToISO8601CalendarFactory r = dateToCalenderFieldsCalculator;
        if (r == null) {
            r = new TrivialDateToISO8601CalendarFactory();
            dateToCalenderFieldsCalculator = r;
        }
        return r;
    }

    public CalendarFieldsToDateConverter getCalendarFieldsToDateCalculator() {
        CalendarFieldsToDateConverter r = calendarFieldsToDateConverter;
        if (r == null) {
            r = new TrivialCalendarFieldsToDateConverter();
            calendarFieldsToDateConverter = r;
        }
        return r;
    }

}
