package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;

/**
 * A {@link TemplateException} that 
 * indicates that the internals expected an expression
 * to evaluate to a method value and it didn't.
 * 
 * @since 2.3.21
 */
public class NonMethodException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] { TemplateMethodModel.class };
    
    public NonMethodException(Environment env) {
        super(env, "Expecting method value here");
    }

    public NonMethodException(String description, Environment env) {
        super(env, description);
    }

    NonMethodException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonMethodException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "method", EXPECTED_TYPES, env);
    }

    NonMethodException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "method", EXPECTED_TYPES, tip, env);
    }

    NonMethodException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "method", EXPECTED_TYPES, tips, env);
    }    

}
