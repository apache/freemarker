package freemarker.core;

import freemarker.template.TemplateDateModel;

/**
 * Thrown when a {@link TemplateDateModel} can't be formatted because it's type is {@link TemplateDateModel#UNKNOWN}.
 */
// [Advanced formatting: planned public]
final class UnknownDateTypeFormattingUnsupportedException extends UnformattableDateException {

    public UnknownDateTypeFormattingUnsupportedException() {
        super(MessageUtil.UNKNOWN_DATE_TO_STRING_ERROR_MESSAGE);
    }
    
}
