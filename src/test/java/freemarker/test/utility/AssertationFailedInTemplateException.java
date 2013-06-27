package freemarker.test.utility;

import freemarker.core.Environment;
import freemarker.template.TemplateException;

/**
 * Fills the role of {@link junit.framework.AssertionFailedError}, but carries the template location information.  
 */
public class AssertationFailedInTemplateException extends TemplateException {

    public AssertationFailedInTemplateException(Environment env) {
        super(env);
    }

    public AssertationFailedInTemplateException(String description, Environment env) {
        super(description, env);
    }

    public AssertationFailedInTemplateException(Exception cause, Environment env) {
        super(cause, env);
    }

    public AssertationFailedInTemplateException(String description, Exception cause, Environment env) {
        super(description, cause, env);
    }

}
