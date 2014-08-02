package freemarker.core;

import java.util.TimeZone;

class XSTemplateDateFormatFactory extends ISOLikeTemplateDateFormatFactory {

    public XSTemplateDateFormatFactory(TimeZone timeZone) {
        super(timeZone);
    }

    public TemplateDateFormat get(int dateType, String formatDescriptor)
            throws java.text.ParseException, UnknownDateTypeFormattingUnsupportedException {
        return new XSTemplateDateFormat(formatDescriptor, 2, dateType, getTimeZone(), this);
    }

}
