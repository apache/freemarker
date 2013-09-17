package freemarker.core;

/**
 * An unexpected state was reached that is certainly caused by a bug in FreeMarker.
 * 
 * @since 2.3.21
 */
public class BugException extends RuntimeException {

    // Java 5: add the constructors with cause exception.
    
    private static final String COMMON_MESSAGE
        = "A bug was detected in FreeMarker; please report it with stack-trace";

    public BugException() {
        super(COMMON_MESSAGE);
    }

    public BugException(String message) {
        super(COMMON_MESSAGE + ": " + message);
    }

    public BugException(int value) {
        this(String.valueOf(value));
    }

}
