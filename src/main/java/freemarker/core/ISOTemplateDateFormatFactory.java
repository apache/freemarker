package freemarker.core;

import java.util.TimeZone;

class ISOTemplateDateFormatFactory extends ISOLikeTemplateDateFormatFactory {

    public ISOTemplateDateFormatFactory(TimeZone timeZone) {
        super(timeZone);
    }

    public TemplateDateFormat get(int dateType, String formatDescriptor)
            throws java.text.ParseException, UnknownDateTypeFormattingUnsupportedException {
        return new ISOTemplateDateFormat(formatDescriptor, 3, dateType, getTimeZone(), this);
    }

}
