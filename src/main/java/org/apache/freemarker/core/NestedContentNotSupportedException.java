/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import org.apache.freemarker.core.Environment.NestedElementTemplateDirectiveBody;
import org.apache.freemarker.core.ThreadInterruptionSupportTemplatePostProcessor.ASTThreadInterruptionCheck;
import org.apache.freemarker.core.model.TemplateDirectiveBody;
import org.apache.freemarker.core.util._StringUtil;

/**
 * Used in custom {@link org.apache.freemarker.core.model.TemplateDirectiveModel}-s to check if the directive invocation
 * has no body. This is more intelligent than a {@code null} check; for example, when the body
 * only contains a thread interruption check node, it treats it as valid.
 */
public class NestedContentNotSupportedException extends TemplateException {

    public static void check(TemplateDirectiveBody body) throws NestedContentNotSupportedException {
        if (body == null) {
            return;
        }
        if (body instanceof NestedElementTemplateDirectiveBody) {
            _ASTElement[] tes = ((NestedElementTemplateDirectiveBody) body).getChildrenBuffer();
            if (tes == null || tes.length == 0
                    || tes[0] instanceof ASTThreadInterruptionCheck && (tes.length == 1 || tes[1] == null)) {
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
                + (description != null ? " " + _StringUtil.jQuote(description) : ""),
                cause, env);
    }
    
}
