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

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

/**
 * Indicates that a {@link TemplateScalarModel} value was expected (or maybe something that can be automatically coerced
 * to that), but the value had a different type.
 */
public class NonStringException extends UnexpectedTypeException {

    static final String STRING_COERCABLE_TYPES_DESC
            = "string or something automatically convertible to string (number, date or boolean)";
    
    static final Class[] STRING_COERCABLE_TYPES = new Class[] {
        TemplateScalarModel.class, TemplateNumberModel.class, TemplateDateModel.class, TemplateBooleanModel.class
    };
    
    private static final String DEFAULT_DESCRIPTION
            = "Expecting " + NonStringException.STRING_COERCABLE_TYPES_DESC + " value here";

    public NonStringException(Environment env) {
        super(env, DEFAULT_DESCRIPTION);
    }

    public NonStringException(String description, Environment env) {
        super(env, description);
    }
 
    NonStringException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonStringException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, NonStringException.STRING_COERCABLE_TYPES_DESC, STRING_COERCABLE_TYPES, env);
    }

    NonStringException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, NonStringException.STRING_COERCABLE_TYPES_DESC, STRING_COERCABLE_TYPES, tip, env);
    }

    NonStringException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, NonStringException.STRING_COERCABLE_TYPES_DESC, STRING_COERCABLE_TYPES, tips, env);
    }
        
}
