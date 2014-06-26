package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNodeModel;

/**
 * A {@link TemplateException} that 
 * indicates that the internals expected ah expression
 * to evaluate to a node value and it didn't.
 * 
 * @since 2.3.21
 */
public class NonNodeException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] { TemplateNodeModel.class };
    
    public NonNodeException(Environment env) {
        super(env, "Expecting node value here");
    }

    public NonNodeException(String description, Environment env) {
        super(env, description);
    }

    NonNodeException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonNodeException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "node", EXPECTED_TYPES, env);
    }

    NonNodeException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "node", EXPECTED_TYPES, tip, env);
    }

    NonNodeException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "node", EXPECTED_TYPES, tips, env);
    }    

}
