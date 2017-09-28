package freemarker.core;

/**
 * Exception that's not really an exception, just used for flow control.
 */
@SuppressWarnings("serial")
class FlowControlException extends RuntimeException {

    FlowControlException() {
        super();
    }

    FlowControlException(String message) {
        super(message);
    }
    
}
