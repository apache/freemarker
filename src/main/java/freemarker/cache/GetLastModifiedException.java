package freemarker.cache;

import java.io.IOException;


/**
 * Used be {@link TemplateLoader#getLastModified(Object)} to indicate an error getting the last modification date. That
 * should be just an {@link IOException}, but due to backward compatibility constraints that wasn't possible;
 * {@link TemplateLoader#getLastModified(Object)} doesn't allow throwing checked exception.
 * 
 * @since 2.4.0
 */
public class GetLastModifiedException extends RuntimeException {

    public GetLastModifiedException(String message, Throwable cause) {
        super(message, cause);
    }

    public GetLastModifiedException(String message) {
        super(message);
    }

}
