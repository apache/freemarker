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

import freemarker.core.Environment.NestedElementTemplateDirectiveBody;
import freemarker.core.ThreadInterruptionSupportTemplatePostProcessor.ThreadInterruptionCheck;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.utility.StringUtil;

/**
 * [2.4] Should become public somehow; this is more intelligent than a {@code null} check, for example, when the body
 * only contains a thread interruption check, it treats it as valid. Indicates that the directive shouldn't have
 * nested content, but it had. This will probably become public when the directive/method stuff was reworked.
 */
class NestedContentNotSupportedException extends TemplateException {

    public static void check(TemplateDirectiveBody body) throws NestedContentNotSupportedException {
        if (body == null) {
            return;
        }
        if (body instanceof NestedElementTemplateDirectiveBody) {
            TemplateElement te = ((NestedElementTemplateDirectiveBody) body).getElement();
            if (te == null || te instanceof ThreadInterruptionCheck) {
                return;
            }
        }
        throw new NestedContentNotSupportedException(Environment.getCurrentEnvironment());
    }
    
    
    private NestedContentNotSupportedException(Environment env) {
        this(null, null, env);
    }

    private NestedContentNotSupportedException(Exception cause, Environment env) {
        this(null, cause, env);
    }

    private NestedContentNotSupportedException(String description, Environment env) {
        this(description, null, env);
    }

    private NestedContentNotSupportedException(String description, Exception cause, Environment env) {
        super( "Nested content (body) not supported."
                + (description != null ? " " + StringUtil.jQuote(description) : ""),
                cause, env);
    }
    
}
