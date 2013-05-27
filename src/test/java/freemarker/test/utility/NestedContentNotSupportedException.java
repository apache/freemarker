package freemarker.test.utility;

import freemarker.core.Environment;
import freemarker.template.TemplateException;
import freemarker.template.utility.StringUtil;

/**
 * Indicates that a the directive shouldn't have nested content.  
 * This is will be public and go into the freemarker.core when the directive/method stuff was reworked.
 */
public class NestedContentNotSupportedException extends TemplateException {

    public NestedContentNotSupportedException(Environment env) {
        this(null, null, env);
    }

    public NestedContentNotSupportedException(Exception cause, Environment env) {
        this(null, cause, env);
    }

    public NestedContentNotSupportedException(String description, Environment env) {
        this(description, null, env);
    }

    public NestedContentNotSupportedException(String description, Exception cause, Environment env) {
        super( "Nested content (body) not supported."
                + (description == null ? " " + StringUtil.jQuote(description) : ""),
                cause, env);
    }
    
}
