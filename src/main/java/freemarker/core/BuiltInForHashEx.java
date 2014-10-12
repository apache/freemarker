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