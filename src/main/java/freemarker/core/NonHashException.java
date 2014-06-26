package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

/**
 * A {@link TemplateException} that 
 * indicates that the internals expected an expression
 * to evaluate to a hash value and it didn't.
 * 
 * @since 2.3.21
 */
public class NonHashException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] { TemplateHashModel.class };
    
    public NonHashException(Environment env) {
        super(env, "Expecting hash value here");
    }

    public NonHashException(String description, Environment env) {
        super(env, description);
    }

    NonHashException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonHashException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "hash", EXPECTED_TYPES, env);
    }

    NonHashException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "hash", EXPECTED_TYPES, tip, env);
    }

    NonHashException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "hash", EXPECTED_TYPES, tips, env);
    }    

}
