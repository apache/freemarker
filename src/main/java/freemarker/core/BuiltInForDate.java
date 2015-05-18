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

import java.util.Date;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

abstract class BuiltInForDate extends BuiltIn {
    TemplateModel _eval(Environment env)
            throws TemplateException
    {
        TemplateModel model = target.eval(env);
        if (model instanceof TemplateDateModel) {
            TemplateDateModel tdm = (TemplateDateModel) model;
            return calculateResult(EvalUtil.modelToDate(tdm, target), tdm.getDateType(), env);
        } else {
            throw newNonDateException(env, model, target);
        }
    }

    /** Override this to implement the built-in. */
    protected abstract TemplateModel calculateResult(
            Date date, int dateType, Environment env)
    throws TemplateException;
    
    static TemplateException newNonDateException(Environment env, TemplateModel model, Expression target)
            throws InvalidReferenceException {
        TemplateException e;
        if(model == null) {
            e = InvalidReferenceException.getInstance(target, env);
        } else {
            e = new NonDateException(target, model, "date", env);
        }
        return e;
    }
    
}