package freemarker.core;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;

/**
 * Indicates that a {@link TemplateSequenceModel} value was expected, but the value had a different type.
 * 
 * @since 2.3.21
 */
public class NonSequenceException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] { TemplateSequenceModel.class };
    
    public NonSequenceException(Environment env) {
        super(env, "Expecting sequence value here");
    }

    public NonSequenceException(String description, Environment env) {
        super(env, description);
    }

    NonSequenceException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonSequenceException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "sequence", EXPECTED_TYPES, env);
    }

    NonSequenceException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "sequence", EXPECTED_TYPES, tip, env);
    }

    NonSequenceException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "sequence", EXPECTED_TYPES, tips, env);
    }    

}
