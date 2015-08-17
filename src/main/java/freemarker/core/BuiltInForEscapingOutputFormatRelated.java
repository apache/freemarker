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
import freemarker.template.TemplateModel;
import freemarker.template.utility.NullArgumentException;

abstract class BuiltInForEscapingOutputFormatRelated extends SpecialBuiltIn {
    
    protected EscapingOutputFormat outputFormat;
    
    void bindToOutputFormat(EscapingOutputFormat outputFormat) {
        NullArgumentException.check(outputFormat);
        this.outputFormat = outputFormat;
    }
    
    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        if (outputFormat == null) {
            // The parser should prevent this situation
            throw new _MiscTemplateException(
                    this, env,
                    "?" + key + " can't be used here, as the place isn't bound to an output format.");
        }
        return calculateResult(env);
    }

    protected abstract TemplateModel calculateResult(Environment env)
            throws TemplateException;
    
}