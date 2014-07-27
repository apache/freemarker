package freemarker.core;

import java.util.Date;
import java.util.TimeZone;

import freemarker.template.TemplateDateModel;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.CalendarFieldsToDateConverter;

/**
 * XML Schema format.
 */
class XSTemplateDateFormat extends TemplateDateFormat {

    private final int dateType;
    private final TimeZone timeZone;
    private final Boolean showZoneOffset;
    private final Environment env;

    public XSTemplateDateFormat(int dateType, TimeZone timeZone, Boolean showZoneOffset, Environment env) {
        this.dateType = dateType;
        this.timeZone = timeZone;
        this.showZoneOffset = showZoneOffset;
        this.env = env;
    }

    public String format(Date date) {
        return DateUtil.dateToXSString(
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

    public Date parse(String s) throws java.text.ParseException {
        CalendarFieldsToDateConverter calToDateConverter = env.getCalendarFieldsToDateCalculator();
        if (dateType == TemplateDateModel.DATE) {
            return DateUtil.parseXSDate(s, timeZone, calToDateConverter);
        } else if (dateType == TemplateDateModel.TIME) {
            return DateUtil.parseXSTime(s, timeZone, calToDateConverter);
        } else if (dateType == TemplateDateModel.DATETIME) {
            return DateUtil.parseXSDateTime(s, timeZone, calToDateConverter);
        } else {
            throw new BugException("Unexpected date type: " + dateType);
        }
    }

    public String getDescription() {
        if (dateType == TemplateDateModel.DATE) {
            return "W3C XML Schema date";
        } else if (dateType == TemplateDateModel.TIME) {
            return "W3C XML Schema time";
        } else if (dateType == TemplateDateModel.DATETIME) {
            return "W3C XML Schema dateTime";
        } else {
            return "W3C XML Schema <unknown>";
        }
    }
    
    public boolean isLocaleBound() {
        return false;
    }

    public boolean isTimeZoneBound() {
        return true;
    }

}
