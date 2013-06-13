package freemarker.core;

import freemarker.template.TemplateException;

/**
 * The type of a value differs from what was expected.
 * 
 * @since 2.3.20
 */
public class UnexpectedTypeException extends TemplateException {

    public UnexpectedTypeException(String description, Environment env) {
        super(description, env);
    }

    UnexpectedTypeException(Internal_ErrorDescriptionBuilder description, Environment env) {
        super(description, env, true);
    }
    
}
