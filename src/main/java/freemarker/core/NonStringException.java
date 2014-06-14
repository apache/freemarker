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

/**
 * A {@link TemplateException} that 
 * indicates that the internals expected an expression
 * to evaluate to a string or numeric value and it didn't.
 */
public class NonStringException extends UnexpectedTypeException {

    private static final String DEFAULT_DESCRIPTION
            = "Expecting " + NonStringException.TYPES_USABLE_WHERE_STRING_IS_EXPECTED + " value here";
    
    static final String TYPES_USABLE_WHERE_STRING_IS_EXPECTED
            = "string or something automatically convertible to string (number, date or boolean)";

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
        super(blamed, model, NonStringException.TYPES_USABLE_WHERE_STRING_IS_EXPECTED, env);
    }

    NonStringException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, NonStringException.TYPES_USABLE_WHERE_STRING_IS_EXPECTED, tip, env);
    }

    NonStringException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, NonStringException.TYPES_USABLE_WHERE_STRING_IS_EXPECTED, tips, env);
    }
        
}
