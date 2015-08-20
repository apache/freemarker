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

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * A holder for builtins that operate exclusively on markup output left-hand value.
 */
class BuiltInsForMarkupOutputs {
    
    static class markupBI extends BuiltInForMarkupOutput {

        @Override
        protected TemplateModel calculateResult(TemplateMarkupOutputModel model) throws TemplateModelException {
            return new SimpleScalar(model.getOutputFormat().getMarkup(model));
        }
        
    }
    
}
