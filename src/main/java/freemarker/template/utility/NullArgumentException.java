package freemarker.template.utility;

/**
 * Indicates that an argument that must be non-{@code null} was {@code null}. 
 * 
 * @since 2.3.20
 */
public class NullArgumentException extends IllegalArgumentException {
    
    public NullArgumentException(String argumentName) {
        super("The \"" + argumentName + "\" argument can't be null");
    }
    
    /**
     * Convenience method to protect against a {@code null} argument.
     */
    public static void check(String argumentName, Object argumentValue) {
        if (argumentValue == null) {
            throw new NullArgumentException(argumentName);
        }
    }

}
