package freemarker.core;

import java.util.TimeZone;

class XSTemplateDateFormatFactory extends ISOLikeTemplateDateFormatFactory {

    public XSTemplateDateFormatFactory(TimeZone timeZone) {
        super(timeZone);
    }

    public TemplateDateFormat get(int dateType, boolean zonelessInput, String formatDescriptor)
            throws java.text.ParseException, UnknownDateTypeFormattingUnsupportedException {
        // We don't cache these as creating them is cheap (only 10% speedup of ${d?string.xs} with caching)
        return new XSTemplateDateFormat(
                formatDescriptor, 2,
                dateType, zonelessInput,
                getTimeZone(), this);
    }

}
