package freemarker.core;

import java.util.Date;
import java.util.TimeZone;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.CalendarFieldsToDateConverter;
import freemarker.template.utility.DateUtil.DateParseException;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;

abstract class ISOLikeTemplateDateFormat  extends TemplateDateFormat {

    protected final int dateType;
    protected final TimeZone timeZone;
    protected final Boolean showZoneOffset;
    protected final Environment env;

    public ISOLikeTemplateDateFormat(int dateType, TimeZone timeZone, Boolean showZoneOffset, Environment env) {
        this.dateType = dateType;
        this.timeZone = timeZone;
        this.showZoneOffset = showZoneOffset;
        this.env = env;
    }

    public final String format(TemplateDateModel dateModel) throws TemplateModelException {
        final Date date = dateModel.getAsDate();
        return format(
                date,
                dateType != TemplateDateModel.TIME,
                dateType != TemplateDateModel.DATE,
                showZoneOffset == null
                        ? !DateUtil.isSQLDateOrTimeClass(date.getClass())
                        : showZoneOffset.booleanValue(),
                DateUtil.ACCURACY_MILLISECONDS,
                timeZone,
                env.getISOBuiltInCalendar());
    }
    
    protected abstract String format(Date date,
            boolean datePart, boolean timePart, boolean offsetPart,
            int accuracy,
            TimeZone timeZone,
            DateToISO8601CalendarFactory calendarFactory);

    public final Date parse(String s) throws java.text.ParseException {
        CalendarFieldsToDateConverter calToDateConverter = env.getCalendarFieldsToDateCalculator();
        if (dateType == TemplateDateModel.DATE) {
            return parseDate(s, timeZone, calToDateConverter);
        } else if (dateType == TemplateDateModel.TIME) {
            return parseTime(s, timeZone, calToDateConverter);
        } else if (dateType == TemplateDateModel.DATETIME) {
            return parseDateTime(s, timeZone, calToDateConverter);
        } else {
            throw new BugException("Unexpected date type: " + dateType);
        }
    }
    
    protected abstract Date parseDate(
            String s, TimeZone tz,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException;
    
    protected abstract Date parseTime(
            String s, TimeZone tz,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException;
    
    protected abstract Date parseDateTime(
            String s, TimeZone tz,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException;

    public final String getDescription() {
        switch (dateType) {
            case TemplateDateModel.DATE: return getDateDescription();
            case TemplateDateModel.TIME: return getTimeDescription();
            case TemplateDateModel.DATETIME: return getDateTimeDescription();
            default: return "<error: wrong format dateType>";
        }
    }
    
    protected abstract String getDateDescription();
    protected abstract String getTimeDescription();
    protected abstract String getDateTimeDescription();
    
    public final boolean isLocaleBound() {
        return false;
    }

    public final boolean isTimeZoneBound() {
        return true;
    }

}
