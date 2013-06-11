package freemarker.core;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * A holder for builtins that operate exclusively on hash left-hand value.
 */
class HashBuiltins {

    // Can't be instantiated
    private HashBuiltins() { }
    
    private static abstract class HashExBuiltin extends BuiltIn {

        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateHashModelEx) {
                return calculateResult((TemplateHashModelEx) model, env);
            }
            throw target.newUnexpectedTypeException(model, "extended hash", env);
        }
        
        abstract TemplateModel calculateResult(TemplateHashModelEx hashExModel, Environment env)
                throws TemplateModelException, InvalidReferenceException;
        
        protected InvalidReferenceException newNullPropertyException(
                String propertyName, TemplateModel tm, Environment env) {
            return target.newInvalidReferenceException(
                    "The exteneded hash (of class " + tm.getClass().getName() + ") has returned null for its \""
                    + propertyName + "\" property. This is maybe a bug. "
                    + "The extended hash was returned by this expression:",
                    env);
        }
        
    }
    
    static class keysBI extends HashExBuiltin {

        TemplateModel calculateResult(TemplateHashModelEx hashExModel, Environment env)
                throws TemplateModelException, InvalidReferenceException {
            TemplateCollectionModel keys = hashExModel.keys();
            if (keys == null) throw newNullPropertyException("keys", hashExModel, env);
            return keys instanceof TemplateSequenceModel ? keys : new CollectionAndSequence(keys);
        }
        
    }

    static class valuesBI extends HashExBuiltin {
        TemplateModel calculateResult(TemplateHashModelEx hashExModel, Environment env)
                throws TemplateModelException, InvalidReferenceException {
            TemplateCollectionModel values = hashExModel.values();
            if (values == null) throw newNullPropertyException("values", hashExModel, env);
            return values instanceof TemplateSequenceModel ? values : new CollectionAndSequence(values);
        }
    }
    
}
