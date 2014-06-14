/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.util.List;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * A holder for builtins that deal with null left-hand values.
 */
class ExistenceBuiltins {

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
                    int argCnt = args.size();
                    if(argCnt == 0) throw MessageUtil.newArgCntError("?default", argCnt, 1, Integer.MAX_VALUE);
                    for (int i = 0; i < argCnt; i++ ) {
                        TemplateModel result = (TemplateModel) args.get(i);
                        if (result != null) return result;
                    }
                    return null;
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
