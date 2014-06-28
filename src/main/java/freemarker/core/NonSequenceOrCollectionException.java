package freemarker.core;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;

/**
 * Indicates that a {@link TemplateSequenceModel} or {@link TemplateCollectionModel} value was expected, but the value
 * had a different type.
 * 
 * @since 2.3.21
 */
public class NonSequenceOrCollectionException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] {
        TemplateSequenceModel.class, TemplateCollectionModel.class
    };
    
    public NonSequenceOrCollectionException(Environment env) {
        super(env, "Expecting sequence or collection value here");
    }

    public NonSequenceOrCollectionException(String description, Environment env) {
        super(env, description);
    }

    NonSequenceOrCollectionException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonSequenceOrCollectionException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "sequence or collection", EXPECTED_TYPES, env);
    }

    NonSequenceOrCollectionException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "sequence or collection", EXPECTED_TYPES, tip, env);
    }

    NonSequenceOrCollectionException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "sequence or collection", EXPECTED_TYPES, tips, env);
    }    

}
