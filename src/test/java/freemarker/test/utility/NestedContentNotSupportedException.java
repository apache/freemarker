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
import freemarker.template.TemplateException;
import freemarker.template.utility.StringUtil;

/**
 * Indicates that a the directive shouldn't have nested content.  
 * This is will be public and go into the freemarker.core when the directive/method stuff was reworked.
 */
public class NestedContentNotSupportedException extends TemplateException {

    public NestedContentNotSupportedException(Environment env) {
        this(null, null, env);
    }

    public NestedContentNotSupportedException(Exception cause, Environment env) {
        this(null, cause, env);
    }

    public NestedContentNotSupportedException(String description, Environment env) {
        this(description, null, env);
    }

    public NestedContentNotSupportedException(String description, Exception cause, Environment env) {
        super( "Nested content (body) not supported."
                + (description == null ? " " + StringUtil.jQuote(description) : ""),
                cause, env);
    }
    
}
