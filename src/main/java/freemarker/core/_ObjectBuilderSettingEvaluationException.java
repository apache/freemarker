package freemarker.core;

import freemarker.template.utility.StringUtil;

/**
 * Don't use this; used internally by FreeMarker, might changes without notice.
 * Thrown by {@link _ObjectBuilderSettingEvaluator}.
 */
public class _ObjectBuilderSettingEvaluationException extends Exception {
    
    private final Throwable cause;

    public _ObjectBuilderSettingEvaluationException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    public _ObjectBuilderSettingEvaluationException(String message) {
        super(message);
        this.cause = null;
    }

    public _ObjectBuilderSettingEvaluationException(String expected, String src, int location) {
        super("Expected a(n) " + expected + ", but "
                + (location < src.length()
                        ? "found character " + StringUtil.jQuote("" + src.charAt(location)) + " at position "
                            + (location + 1) + "."
                        : "the end of the parsed string was reached.") );
        cause = null;
    }
    
    public Throwable getCause() {
        return cause;
    }
        
}
