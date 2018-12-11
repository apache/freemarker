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

import java.util.List;


abstract class BuiltInWithParseTimeParameters extends SpecialBuiltIn {

    abstract void bindToParameters(List/*<ASTExpression>*/ parameters, Token openParen, Token closeParen)
            throws ParseException;

    @Override
    public String getCanonicalForm() {
        StringBuilder buf = new StringBuilder();
        
        buf.append(super.getCanonicalForm());
        
        buf.append("(");
        List/*<ASTExpression>*/args = getArgumentsAsList();
        int size = args.size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                buf.append(", ");
            }
            ASTExpression arg = (ASTExpression) args.get(i);
            buf.append(arg.getCanonicalForm());
        }
        buf.append(")");
        
        return buf.toString();
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return super.getLabelWithoutParameters() + "(...)";
    }        
    
    @Override
    int getParameterCount() {
        return super.getParameterCount() + getArgumentsCount();
    }

    @Override
    Object getParameterValue(int idx) {
        final int superParamCnt = super.getParameterCount();
        if (idx < superParamCnt) {
            return super.getParameterValue(idx); 
        }
        
        final int argIdx = idx - superParamCnt;
        return getArgumentParameterValue(argIdx);
    }
    
    @Override
    ParameterRole getParameterRole(int idx) {
        final int superParamCnt = super.getParameterCount();
        if (idx < superParamCnt) {
            return super.getParameterRole(idx); 
        }
        
        if (idx - superParamCnt < getArgumentsCount()) {
            return ParameterRole.ARGUMENT_VALUE;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    protected ParseException newArgumentCountException(String ordinalityDesc, Token openParen, Token closeParen) {
        return new ParseException(
                "?" + key + "(...) " + ordinalityDesc + " parameters", getTemplate(),
                openParen.beginLine, openParen.beginColumn,
                closeParen.endLine, closeParen.endColumn);
    }

    @Override
    ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
        final ASTExpression clone = super.deepCloneWithIdentifierReplaced_inner(replacedIdentifier, replacement, replacementState);
        cloneArguments(clone, replacedIdentifier, replacement, replacementState);
        return clone;
    }

    protected abstract List getArgumentsAsList();
    
    protected abstract int getArgumentsCount();

    protected abstract ASTExpression getArgumentParameterValue(int argIdx);
    
    protected abstract void cloneArguments(ASTExpression clone, String replacedIdentifier,
            ASTExpression replacement, ReplacemenetState replacementState);
    
}
