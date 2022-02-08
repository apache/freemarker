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

package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * A reference to a top-level variable
 */
final class Identifier extends Expression {

    private final String name;

    Identifier(String name) {
        this.name = name;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        try {
            return env.getVariable(name);
        } catch (NullPointerException e) {
            if (env == null) {
                throw new _MiscTemplateException(
                        "Variables are not available (certainly you are in a parse-time executed directive). "
                        + "The name of the variable you tried to read: ", name);
            } else {
                throw e;
            }
        }
    }

    @Override
    public String getCanonicalForm() {
        return _CoreStringUtils.toFTLTopLevelIdentifierReference(name);
    }
    
    /**
     * The name of the identifier without any escaping or other syntactical distortions. 
     */
    String getName() {
        return name;
    }
    
    @Override
    String getNodeTypeSymbol() {
        return getCanonicalForm();
    }

    @Override
    boolean isLiteral() {
        return false;
    }
    
    @Override
    int getParameterCount() {
        return 0;
    }

    @Override
    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        if (this.name.equals(replacedIdentifier)) {
            if (replacementState.replacementAlreadyInUse) {
                Expression clone = replacement.deepCloneWithIdentifierReplaced(null, null, replacementState);
                clone.copyLocationFrom(replacement);
                return clone;
            } else {
                replacementState.replacementAlreadyInUse = true;
                return replacement;
            }
        } else {
            return new Identifier(this.name);
        }
    }

}
