package freemarker.core;

/**
 * @since 2.3.24
 */
public class UndefinedCustomFormatException extends InvalidFormatStringException {

    public UndefinedCustomFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public UndefinedCustomFormatException(String message) {
        super(message);
    }

}
