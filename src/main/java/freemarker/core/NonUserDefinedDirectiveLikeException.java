package freemarker.core;

import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateTransformModel;

/**
 * A {@link TemplateException} that 
 * indicates that the internals expected an expression
 * to evaluate to user-defined directive, transform or macro value and it didn't.
 * 
 * @since 2.3.21
 */
class NonUserDefinedDirectiveLikeException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] {
        TemplateDirectiveModel.class, TemplateTransformModel.class, Macro.class };
    
    public NonUserDefinedDirectiveLikeException(Environment env) {
        super(env, "Expecting user-defined directive, transform or macro value here");
    }

    public NonUserDefinedDirectiveLikeException(String description, Environment env) {
        super(env, description);
    }

    NonUserDefinedDirectiveLikeException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonUserDefinedDirectiveLikeException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "user-defined directive, transform or macro", EXPECTED_TYPES, env);
    }

    NonUserDefinedDirectiveLikeException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "user-defined directive, transform or macro", EXPECTED_TYPES, tip, env);
    }

    NonUserDefinedDirectiveLikeException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "user-defined directive, transform or macro", EXPECTED_TYPES, tips, env);
    }    

}
