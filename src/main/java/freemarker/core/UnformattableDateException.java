package freemarker.core;

import freemarker.template.TemplateDateModel;

/**
 * Thrown when a {@link TemplateDateModel} can't be formatted because of the value/properties of the
 * {@link TemplateDateModel}.  The most often used subclass is {@link UnknownDateTypeFormattingUnsupportedException}. 
 */
// [Advanced formatting: planned public]
abstract class UnformattableDateException extends Exception {

    public UnformattableDateException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnformattableDateException(String message) {
        super(message);
    }

}
