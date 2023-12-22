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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template._ObjectWrappers;

final class ListLiteral extends Expression {

    final ArrayList<Expression> items;

    ListLiteral(ArrayList<Expression> items) {
        this.items = items;
        items.trimToSize();
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        SimpleSequence list = new SimpleSequence(items.size(), _ObjectWrappers.SAFE_OBJECT_WRAPPER);
        for (Expression exp : items) {
            TemplateModel tm = exp.eval(env);
            if (env == null || !env.isClassicCompatible()) {
                exp.assertNonNull(tm, env);
            }
            list.add(tm);
        }
        return list;
    }

    /**
     * For {@link TemplateMethodModel} calls, but not for {@link TemplateMethodModelEx}-es, returns the list of
     * arguments as {@link String}-s.
     */
    List/*<String>*/ getValueList(Environment env) throws TemplateException {
        int size = items.size();
        switch(size) {
            case 0: {
                return Collections.EMPTY_LIST;
            }
            case 1: {
                return Collections.singletonList(items.get(0).evalAndCoerceToPlainText(env));
            }
            default: {
                List result = new ArrayList(items.size());
                for (ListIterator iterator = items.listIterator(); iterator.hasNext(); ) {
                    Expression exp = (Expression) iterator.next();
                    result.add(exp.evalAndCoerceToPlainText(env));
                }
                return result;
            }
        }
    }

    /**
     * For {@link TemplateMethodModelEx} calls, returns the list of arguments as {@link TemplateModel}-s.
     */
    List/*<TemplateModel>*/ getModelList(Environment env) throws TemplateException {
        int size = items.size();
        switch(size) {
            case 0: {
                return Collections.EMPTY_LIST;
            }
            case 1: {
                return Collections.singletonList(items.get(0).eval(env));
            }
            default: {
                List result = new ArrayList(items.size());
                for (ListIterator iterator = items.listIterator(); iterator.hasNext(); ) {
                    Expression exp = (Expression) iterator.next();
                    result.add(exp.eval(env));
                }
                return result;
            }
        }
    }

    @Override
    public String getCanonicalForm() {
        StringBuilder buf = new StringBuilder("[");
        int size = items.size();
        for (int i = 0; i < size; i++) {
            Expression value = items.get(i);
            buf.append(value.getCanonicalForm());
            if (i != size - 1) {
                buf.append(", ");
            }
        }
        buf.append("]");
        return buf.toString();
    }
    
    @Override
    String getNodeTypeSymbol() {
        return "[...]";
    }
    
    @Override
    boolean isLiteral() {
        if (constantValue != null) {
            return true;
        }
        for (int i = 0; i < items.size(); i++) {
            Expression exp = items.get(i);
            if (!exp.isLiteral()) {
                return false;
            }
        }
        return true;
    }
    
    // A hacky routine used by VisitNode and RecurseNode
    
    TemplateSequenceModel evaluateStringsToNamespaces(Environment env) throws TemplateException {
        TemplateSequenceModel val = (TemplateSequenceModel) eval(env);
        SimpleSequence result = new SimpleSequence(val.size(), _ObjectWrappers.SAFE_OBJECT_WRAPPER);
        for (int i = 0; i < items.size(); i++) {
            Object itemExpr = items.get(i);
            if (itemExpr instanceof StringLiteral) {
                String s = ((StringLiteral) itemExpr).getAsString();
                try {
                    Environment.Namespace ns = env.importLib(s, null);
                    result.add(ns);
                } catch (IOException ioe) {
                    throw new _MiscTemplateException(((StringLiteral) itemExpr),
                            "Couldn't import library ", new _DelayedJQuote(s), ": ",
                            new _DelayedGetMessage(ioe));
                }
            } else {
                result.add(val.get(i));
            }
        }
        return result;
    }
    
    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
		ArrayList clonedValues = (ArrayList) items.clone();
		for (ListIterator iter = clonedValues.listIterator(); iter.hasNext(); ) {
            iter.set(((Expression) iter.next()).deepCloneWithIdentifierReplaced(
                    replacedIdentifier, replacement, replacementState));
        }
        return new ListLiteral(clonedValues);
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
