package freemarker.core;

import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;

/**
 * Indicates that a {@link TemplateHashModelEx} value was expected, but the value had a different type.
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
