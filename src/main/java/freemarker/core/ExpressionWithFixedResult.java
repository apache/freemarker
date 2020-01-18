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
 * Mimics an expression (the "source expression"), but returns the predefined "fixed result" whenever it's evaluated.
 */
class ExpressionWithFixedResult extends Expression {
    private final TemplateModel fixedResult;
    private final Expression sourceExpression;

    ExpressionWithFixedResult(TemplateModel fixedResult, Expression sourceExpression) {
        this.fixedResult = fixedResult;
        this.sourceExpression = sourceExpression;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        return fixedResult;
    }

    @Override
    boolean isLiteral() {
        return sourceExpression.isLiteral();
    }

    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(String replacedIdentifier, Expression replacement,
            ReplacemenetState replacementState) {
        return new ExpressionWithFixedResult(
                fixedResult,
                sourceExpression.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }

    @Override
    public String getCanonicalForm() {
        return sourceExpression.getCanonicalForm();
    }

    @Override
    String getNodeTypeSymbol() {
        return sourceExpression.getNodeTypeSymbol();
    }

    @Override
    int getParameterCount() {
        return sourceExpression.getParameterCount();
    }

    @Override
    Object getParameterValue(int idx) {
        return sourceExpression.getParameterValue(idx);
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        return sourceExpression.getParameterRole(idx);
    }


}
