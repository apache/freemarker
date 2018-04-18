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

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;

/**
 * AST expression node: {@code [ exp, ... ]} 
 */
final class ASTExpListLiteral extends ASTExpression {

    final ArrayList<ASTExpression> items;

    ASTExpListLiteral(ArrayList<ASTExpression> items) {
        this.items = items;
        items.trimToSize();
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        NativeSequence list = new NativeSequence(items.size());
        for (Object item : items) {
            ASTExpression exp = (ASTExpression) item;
            TemplateModel tm = exp.eval(env);
            exp.assertNonNull(tm, env);
            list.add(tm);
        }
        return list;
    }

    public int size() {
        return items.size();
    }

    @Override
    public String getCanonicalForm() {
        StringBuilder buf = new StringBuilder("[");
        int size = items.size();
        for (int i = 0; i < size; i++) {
            ASTExpression value = items.get(i);
            buf.append(value.getCanonicalForm());
            if (i != size - 1) {
                buf.append(", ");
            }
        }
        buf.append("]");
        return buf.toString();
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return "[...]";
    }
    
    @Override
    boolean isLiteral() {
        if (constantValue != null) {
            return true;
        }
        for (int i = 0; i < items.size(); i++) {
            ASTExpression exp = items.get(i);
            if (!exp.isLiteral()) {
                return false;
            }
        }
        return true;
    }
    
    // A hacky routine used by ASTDirVisit and ASTDirRecurse
    TemplateSequenceModel evaluateStringsToNamespaces(Environment env) throws TemplateException {
        TemplateSequenceModel val = (TemplateSequenceModel) eval(env);
        NativeSequence result = new NativeSequence(val.getCollectionSize());
        for (int i = 0; i < items.size(); i++) {
            Object itemExpr = items.get(i);
            if (itemExpr instanceof ASTExpStringLiteral) {
                String s = ((ASTExpStringLiteral) itemExpr).getAsString();
                try {
                    Environment.Namespace ns = env.importLib(s, null);
                    result.add(ns);
                } catch (IOException ioe) {
                    throw new TemplateException(((ASTExpStringLiteral) itemExpr),
                            "Couldn't import library ", new _DelayedJQuote(s), ": ",
                            new _DelayedGetMessage(ioe));
                }
            } else {
                result.add(val.get(i));
            }
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
        ArrayList<ASTExpression> clonedValues = (ArrayList<ASTExpression>) items.clone();
		for (ListIterator<ASTExpression> iter = clonedValues.listIterator(); iter.hasNext(); ) {
            iter.set(iter.next().deepCloneWithIdentifierReplaced(
                    replacedIdentifier, replacement, replacementState));
        }
        return new ASTExpListLiteral(clonedValues);
    }

    @Override
    int getParameterCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    Object getParameterValue(int idx) {
        checkIndex(idx);
        return items.get(idx);
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        checkIndex(idx);
        return ParameterRole.ITEM_VALUE;
    }

    private void checkIndex(int idx) {
        if (items == null || idx >= items.size()) {
            throw new IndexOutOfBoundsException();
        }
    }

}
