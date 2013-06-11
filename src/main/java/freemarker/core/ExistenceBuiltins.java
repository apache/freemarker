package freemarker.core;

import java.util.List;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * A holder for builtins that operate exclusively on hash left-hand value.
 */
public class ExistenceBuiltins {

    // Can't be instantiated
    private ExistenceBuiltins() { }

    private static abstract class ExistenceBuiltIn extends BuiltIn {
    
        protected TemplateModel evalMaybeNonexistentTarget(Environment env) throws TemplateException {
            TemplateModel tm;
            if (target instanceof ParentheticalExpression) {
                boolean lastFIRE = env.setFastInvalidReferenceExceptions(true);
                try {
                    tm = target.eval(env);
                } catch (InvalidReferenceException ire) {
                    tm = null;
                } finally {
                    env.setFastInvalidReferenceExceptions(lastFIRE);
                }
            } else {
                tm = target.eval(env);
            }
            return tm;
        }
        
    }
    
    static class defaultBI extends ExistenceBuiltins.ExistenceBuiltIn {
        TemplateModel _eval(final Environment env) throws TemplateException {
            TemplateModel model = evalMaybeNonexistentTarget(env);
            return model == null ? FIRST_NON_NULL_METHOD : new ConstantMethod(model);
        }

        private static class ConstantMethod implements TemplateMethodModelEx
        {
            private final TemplateModel constant;

            ConstantMethod(TemplateModel constant) {
                this.constant = constant;
            }

            public Object exec(List args) {
                return constant;
            }
        }

        /**
         * A method that goes through the arguments one by one and returns
         * the first one that is non-null. If all args are null, returns null.
         */
        private static final TemplateMethodModelEx FIRST_NON_NULL_METHOD =
            new TemplateMethodModelEx() {
                public Object exec(List args) throws TemplateModelException {
                    if(args.isEmpty()) {
                        throw new TemplateModelException(
                            "?default(arg) expects at least one argument.");
                    }
                    TemplateModel result = null;
                    for (int i = 0; i< args.size(); i++ ) {
                        result = (TemplateModel) args.get(i);
                        if (result != null) {
                            break;
                        }
                    }
                    return result;
                }
            };
    }
    
    static class existsBI extends ExistenceBuiltins.ExistenceBuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            return evalMaybeNonexistentTarget(env) == null ? TemplateBooleanModel.FALSE : TemplateBooleanModel.TRUE;
        }
    
        boolean evalToBoolean(Environment env) throws TemplateException {
            return _eval(env) == TemplateBooleanModel.TRUE;
        }
    }

    static class has_contentBI extends ExistenceBuiltins.ExistenceBuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            return Expression.isEmpty(evalMaybeNonexistentTarget(env))
                    ? TemplateBooleanModel.FALSE
                    : TemplateBooleanModel.TRUE;
        }
    
        boolean evalToBoolean(Environment env) throws TemplateException {
            return _eval(env) == TemplateBooleanModel.TRUE;
        }
    }

    static class if_existsBI extends ExistenceBuiltins.ExistenceBuiltIn {
        TemplateModel _eval(Environment env)
                throws TemplateException
        {
            TemplateModel model = evalMaybeNonexistentTarget(env);
            return model == null ? TemplateModel.NOTHING : model;
        }
    }
    
}
