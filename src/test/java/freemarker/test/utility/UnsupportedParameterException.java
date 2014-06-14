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
import freemarker.template.utility.StringUtil;

/**
 * Indicates that a named directive/function parameter is unsupported (like a typo).  
 * This is will be public and go into the freemarker.core when the directive/method stuff was reworked.
 */
class UnsupportedParameterException extends ParameterException {

    public UnsupportedParameterException(String parameterName, Environment env) {
        this(parameterName, null, null, env);
    }

    public UnsupportedParameterException(String parameterName, Exception cause, Environment env) {
        this(parameterName, null, cause, env);
    }

    public UnsupportedParameterException(String parameterName, String description, Environment env) {
        this(parameterName, description, null, env);
    }

    public UnsupportedParameterException(String parameterName, String description, Exception cause, Environment env) {
        super(parameterName,
                "Unsuppored parameter: " + StringUtil.jQuote(parameterName)
                + (description == null ? "." : ". " + StringUtil.jQuote(description)),
                cause, env);
    }

}
