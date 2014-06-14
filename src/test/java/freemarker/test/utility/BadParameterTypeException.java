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

package freemarker.test.utility;

import freemarker.core.Environment;
import freemarker.template.TemplateModel;
import freemarker.template.utility.StringUtil;

/**
 * Indicates that a named directive/function parameter is not of the expected type.  
 * This is will be public and go into the freemarker.core when the directive/method stuff was reworked.
 */
public class BadParameterTypeException extends ParameterException {

    public BadParameterTypeException(String parameterName, String expectedType, TemplateModel value, Environment env) {
        this(parameterName, expectedType, value, null, null, env);
    }

    public BadParameterTypeException(String parameterName, String expectedType, TemplateModel value,
            Exception cause, Environment env) {
        this(parameterName, expectedType, value, null, cause, env);
    }

    public BadParameterTypeException(String parameterName, String expectedType, TemplateModel value,
            String description, Environment env) {
        this(parameterName, expectedType, value, description, null, env);
    }

    public BadParameterTypeException(
            String parameterName, String expectedType, TemplateModel value, String description, Exception cause, Environment env) {
        super(parameterName,
                "The type of the parameter " + StringUtil.jQuote(parameterName) + " should be " + expectedType
                + ", but the actual value was " + getTypeDescription(value) + "."
                + (description != null ? " " + StringUtil.jQuote(description) : ""),
                cause, env);
    }

    private static String getTypeDescription(TemplateModel value) {
        //FIXME: This should call EvaluationUtil.getTypeDescriptionForDebugging, but that's not visible from here yet.
        return value == null ? "Null" : value.getClass().getName();
    }
    
}
