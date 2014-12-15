package freemarker.core;

/**
 * Thrown by {@link DirectiveCallPlace#getOrCreateCustomData(Object, freemarker.template.utility.ObjectFactory)}
 * 
 * @since 2.3.22
 */
public class CallPlaceCustomDataInitializationException extends Exception {

    public CallPlaceCustomDataInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
