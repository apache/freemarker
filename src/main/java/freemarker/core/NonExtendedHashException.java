package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;

/**
 * A {@link TemplateException} that 
 * indicates that the internals expected an expression
 * to evaluate to a extended hash value and it didn't.
 * 
 * @since 2.3.21
 */
public class NonExtendedHashException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] { TemplateHashModelEx.class };
    
    public NonExtendedHashException(Environment env) {
        super(env, "Expecting extended hash value here");
    }

    public NonExtendedHashException(String description, Environment env) {
        super(env, description);
    }

    NonExtendedHashException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonExtendedHashException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "extended hash", EXPECTED_TYPES, env);
    }

    NonExtendedHashException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "extended hash", EXPECTED_TYPES, tip, env);
    }

    NonExtendedHashException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "extended hash", EXPECTED_TYPES, tips, env);
    }    

}
