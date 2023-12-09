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

import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ListIterator;

/**
 * AST expression node: <code>{ keyExp: valueExp, ... }</code> 
 */
final class ASTExpHashLiteral extends ASTExpression {

    private final ArrayList keys, values;
    private final int size;

    ASTExpHashLiteral(ArrayList/*<ASTExpression>*/ keys, ArrayList/*<ASTExpression>*/ values) {
        this.keys = keys;
        this.values = values;
        size = keys.size();
        keys.trimToSize();
        values.trimToSize();
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        return new LinkedHash(env);
    }

    @Override
    public String getCanonicalForm() {
        StringBuilder buf = new StringBuilder("{");
        for (int i = 0; i < size; i++) {
            ASTExpression key = (ASTExpression) keys.get(i);
            ASTExpression value = (ASTExpression) values.get(i);
            buf.append(key.getCanonicalForm());
            buf.append(": ");
            buf.append(value.getCanonicalForm());
            if (i != size - 1) {
                buf.append(", ");
            }
        }
        buf.append("}");
        return buf.toString();
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return "{...}";
    }

    @Override
    boolean isLiteral() {
        if (constantValue != null) {
            return true;
        }
        for (int i = 0; i < size; i++) {
            ASTExpression key = (ASTExpression) keys.get(i);
            ASTExpression value = (ASTExpression) values.get(i);
            if (!key.isLiteral() || !value.isLiteral()) {
                return false;
            }
        }
        return true;
    }


    @Override
    ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
		ArrayList clonedKeys = (ArrayList) keys.clone();
		for (ListIterator iter = clonedKeys.listIterator(); iter.hasNext(); ) {
            iter.set(((ASTExpression) iter.next()).deepCloneWithIdentifierReplaced(
                    replacedIdentifier, replacement, replacementState));
        }
		ArrayList clonedValues = (ArrayList) values.clone();
		for (ListIterator iter = clonedValues.listIterator(); iter.hasNext(); ) {
            iter.set(((ASTExpression) iter.next()).deepCloneWithIdentifierReplaced(
                    replacedIdentifier, replacement, replacementState));
        }
    	return new ASTExpHashLiteral(clonedKeys, clonedValues);
    }

    private class LinkedHash implements TemplateHashModelEx {

        private HashMap<String, TemplateModel> map;
        private TemplateCollectionModel keyCollection, valueCollection; // ordered lists of keys and values

        LinkedHash(Environment env) throws TemplateException {
            map = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                ASTExpression keyExp = (ASTExpression) keys.get(i);
                ASTExpression valExp = (ASTExpression) values.get(i);
                String key = keyExp.evalAndCoerceToPlainText(env);
                TemplateModel value = valExp.eval(env);
                valExp.assertNonNull(value, env);
                map.put(key, value);
            }
        }

        @Override
        public int getHashSize() {
            return size;
        }

        @Override
        public TemplateCollectionModel keys() {
            if (keyCollection == null) {
                keyCollection = new NativeStringCollectionCollection(map.keySet());
            }
            return keyCollection;
        }

        @Override
        public TemplateCollectionModel values() {
            if (valueCollection == null) {
                valueCollection = new NativeCollection(map.values());
            }
            return valueCollection;
        }

        @Override
        public TemplateModel get(String key) {
            return map.get(key);
        }

        @Override
        public boolean isEmptyHash() {
            return size == 0;
        }
        
        @Override
        public String toString() {
            return getCanonicalForm();
        }

        @Override
        public TemplateHashModelEx.KeyValuePairIterator keyValuePairIterator() throws TemplateException {
            return new TemplateHashModelEx.KeyValuePairIterator() {
                private final TemplateModelIterator keyIterator = keys().iterator();
                private final TemplateModelIterator valueIterator = values().iterator();

                @Override
                public boolean hasNext() throws TemplateException {
                    return keyIterator.hasNext();
                }

                @Override
                public TemplateHashModelEx.KeyValuePair next() throws TemplateException {
                    return new TemplateHashModelEx.KeyValuePair() {
                        private final TemplateModel key = keyIterator.next();
                        private final TemplateModel value = valueIterator.next();

                        @Override
                        public TemplateModel getKey() throws TemplateException {
                            return key;
                        }

                        @Override
                        public TemplateModel getValue() throws TemplateException {
                            return value;
                        }
                        
                    };
                }
                
            };
        }
        
    }

    @Override
    int getParameterCount() {
        return size * 2;
    }

    @Override
    Object getParameterValue(int idx) {
        checkIndex(idx);
        return idx % 2 == 0 ? keys.get(idx / 2) : values.get(idx / 2);
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        checkIndex(idx);
        return idx % 2 == 0 ? ParameterRole.ITEM_KEY : ParameterRole.ITEM_VALUE;
    }

    private void checkIndex(int idx) {
        if (idx >= size * 2) {
            throw new IndexOutOfBoundsException();
        }
    }
    
}
