package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

abstract class BuiltInForHashEx extends BuiltIn {

    TemplateModel _eval(Environment env) throws TemplateException {
        TemplateModel model = target.eval(env);
        if (model instanceof TemplateHashModelEx) {
            return calculateResult((TemplateHashModelEx) model, env);
        }
        throw new NonExtendedHashException(target, model, env);
    }
    
    abstract TemplateModel calculateResult(TemplateHashModelEx hashExModel, Environment env)
            throws TemplateModelException, InvalidReferenceException;
    
    protected InvalidReferenceException newNullPropertyException(
            String propertyName, TemplateModel tm, Environment env) {
        if (env.getFastInvalidReferenceExceptions()) {
            return InvalidReferenceException.FAST_INSTANCE;
        } else {
            return new InvalidReferenceException(
                    new _ErrorDescriptionBuilder(new Object[] {
                        "The exteneded hash (of class ", tm.getClass().getName(), ") has returned null for its \"",
                        propertyName,
                        "\" property. This is maybe a bug. The extended hash was returned by this expression:" })
                    .blame(target),
                    env, this);
        }
    }
    
}