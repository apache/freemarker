package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * The type of a value differs from what was expected.
 * 
 * @since 2.3.20
 */
public class UnexpectedTypeException extends TemplateException {

    public UnexpectedTypeException(Environment env, String description) {
        super(description, env);
    }

    UnexpectedTypeException(Environment env, Internal_ErrorDescriptionBuilder description) {
        super(null, env, description, true);
    }

    UnexpectedTypeException(
            Expression blamed, TemplateModel model, String expectedType, Environment env)
            throws InvalidReferenceException {
        super(null, env, newDesciptionBuilder(blamed, model, expectedType, env), true);
    }

    UnexpectedTypeException(
            Expression blamed, TemplateModel model, String expectedType, String tip, Environment env)
            throws InvalidReferenceException {
        super(null, env, newDesciptionBuilder(blamed, model, expectedType, env).tip(tip), true);
    }

    UnexpectedTypeException(
            Expression blamed, TemplateModel model, String expectedType, String[] tips, Environment env) throws InvalidReferenceException {
        super(null, env, newDesciptionBuilder(blamed, model, expectedType, env).tips(tips), true);
    }
    
    private static Internal_ErrorDescriptionBuilder newDesciptionBuilder(
            Expression blamed, TemplateModel model, String expectedType, Environment env)
            throws InvalidReferenceException {
        if (model == null) throw InvalidReferenceException.getInstance(blamed, env);
        return new Internal_ErrorDescriptionBuilder(
                unexpectedTypeErrorDescription(expectedType, model))
                .blame(blamed);
    }

    private static Object[] unexpectedTypeErrorDescription(String expectedType, TemplateModel model) {
        return new Object[] {
                "Expected ", new Internal_DelayedAOrAn(expectedType), ", but this evaluated to ",
                new Internal_DelayedAOrAn(new Internal_DelayedFTLTypeDescription(model)), ":"};
    }
    
}
