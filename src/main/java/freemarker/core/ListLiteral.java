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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;

final class ListLiteral extends Expression {

    final ArrayList/*<Expression>*/ items;

    ListLiteral(ArrayList items) {
        this.items = items;
        items.trimToSize();
    }

    TemplateModel _eval(Environment env) throws TemplateException {
        SimpleSequence list = new SimpleSequence(items.size());
        for (Iterator it = items.iterator(); it.hasNext();) {
            Expression exp = (Expression) it.next();
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
                return Collections.singletonList(((Expression)items.get(0)).evalAndCoerceToString(env));
            }
            default: {
                List result = new ArrayList(items.size());
                for (ListIterator iterator = items.listIterator(); iterator.hasNext();) {
                    Expression exp = (Expression)iterator.next();
                    result.add(exp.evalAndCoerceToString(env));
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
                return Collections.singletonList(((Expression)items.get(0)).eval(env));
            }
            default: {
                List result = new ArrayList(items.size());
                for (ListIterator iterator = items.listIterator(); iterator.hasNext();) {
                    Expression exp = (Expression)iterator.next();
                    result.add(exp.eval(env));
                }
                return result;
            }
        }
    }

    public String getCanonicalForm() {
        StringBuffer buf = new StringBuffer("[");
        int size = items.size();
        for (int i = 0; i<size; i++) {
            Expression value = (Expression) items.get(i);
            buf.append(value.getCanonicalForm());
            if (i != size-1) {
                buf.append(", ");
            }
        }
        buf.append("]");
        return buf.toString();
    }
    
    String getNodeTypeSymbol() {
        return "[...]";
    }
    
    boolean isLiteral() {
        if (constantValue != null) {
            return true;
        }
        for (int i = 0; i<items.size(); i++) {
            Expression exp = (Expression) items.get(i);
            if (!exp.isLiteral()) {
                return false;
            }
        }
        return true;
    }
    
    // A hacky routine used by VisitNode and RecurseNode
    
    TemplateSequenceModel evaluateStringsToNamespaces(Environment env) throws TemplateException {
        TemplateSequenceModel val = (TemplateSequenceModel) eval(env);
        SimpleSequence result = new SimpleSequence(val.size());
        for (int i=0; i<items.size(); i++) {
            Object itemExpr = items.get(i);
            if (itemExpr instanceof StringLiteral) {
                String s = ((StringLiteral) itemExpr).getAsString();
                try {
                    Environment.Namespace ns = env.importLib(s, null);
                    result.add(ns);
                } 
                catch (IOException ioe) {
                    throw new _MiscTemplateException(((StringLiteral) itemExpr), new Object[] {
                            "Couldn't import library ", new _DelayedJQuote(s), ": ",
                            new _DelayedGetMessage(ioe) });
                }
            }
            else {
                result.add(val.get(i));
            }
        }
        return result;
    }
    
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
		ArrayList clonedValues = (ArrayList)items.clone();
		for (ListIterator iter = clonedValues.listIterator(); iter.hasNext();) {
            iter.set(((Expression)iter.next()).deepCloneWithIdentifierReplaced(
                    replacedIdentifier, replacement, replacementState));
        }
        return new ListLiteral(clonedValues);
    }

    int getParameterCount() {
        return items != null ? items.size() : 0;
    }

    Object getParameterValue(int idx) {
        checkIndex(idx);
        return items.get(idx);
    }

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
