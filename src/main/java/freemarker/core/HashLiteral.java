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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ListIterator;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template._TemplateAPI;

final class HashLiteral extends Expression {

    private final ArrayList keys, values;
    private final int size;

    HashLiteral(ArrayList/*<Expression>*/ keys, ArrayList/*<Expression>*/ values) {
        this.keys = keys;
        this.values = values;
        this.size = keys.size();
        keys.trimToSize();
        values.trimToSize();
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        return new SequenceHash(env);
    }

    @Override
    public String getCanonicalForm() {
        StringBuilder buf = new StringBuilder("{");
        for (int i = 0; i < size; i++) {
            Expression key = (Expression) keys.get(i);
            Expression value = (Expression) values.get(i);
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
    String getNodeTypeSymbol() {
        return "{...}";
    }

    @Override
    boolean isLiteral() {
        if (constantValue != null) {
            return true;
        }
        for (int i = 0; i < size; i++) {
            Expression key = (Expression) keys.get(i);
            Expression value = (Expression) values.get(i);
            if (!key.isLiteral() || !value.isLiteral()) {
                return false;
            }
        }
        return true;
    }


    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
		ArrayList clonedKeys = (ArrayList) keys.clone();
		for (ListIterator iter = clonedKeys.listIterator(); iter.hasNext(); ) {
            iter.set(((Expression) iter.next()).deepCloneWithIdentifierReplaced(
                    replacedIdentifier, replacement, replacementState));
        }
		ArrayList clonedValues = (ArrayList) values.clone();
		for (ListIterator iter = clonedValues.listIterator(); iter.hasNext(); ) {
            iter.set(((Expression) iter.next()).deepCloneWithIdentifierReplaced(
                    replacedIdentifier, replacement, replacementState));
        }
    	return new HashLiteral(clonedKeys, clonedValues);
    }

    private class SequenceHash implements TemplateHashModelEx2 {

        private HashMap map; // maps keys to integer offset
        private TemplateCollectionModel keyCollection, valueCollection; // ordered lists of keys and values

        SequenceHash(Environment env) throws TemplateException {
            if (_TemplateAPI.getTemplateLanguageVersionAsInt(HashLiteral.this) >= _TemplateAPI.VERSION_INT_2_3_21) {
                map = new LinkedHashMap();
                for (int i = 0; i < size; i++) {
                    Expression keyExp = (Expression) keys.get(i);
                    Expression valExp = (Expression) values.get(i);
                    String key = keyExp.evalAndCoerceToPlainText(env);
                    TemplateModel value = valExp.eval(env);
                    if (env == null || !env.isClassicCompatible()) {
                        valExp.assertNonNull(value, env);
                    }
                    map.put(key, value);
                }
            } else {
                // Legacy hash literal, where repeated keys were kept when doing ?values or ?keys, yet overwritten when
                // doing hash[key].
                map = new HashMap();
                ArrayList keyList = new ArrayList(size);
                ArrayList valueList = new ArrayList(size);
                for (int i = 0; i < size; i++) {
                    Expression keyExp = (Expression) keys.get(i);
                    Expression valExp = (Expression) values.get(i);
                    String key = keyExp.evalAndCoerceToPlainText(env);
                    TemplateModel value = valExp.eval(env);
                    if (env == null || !env.isClassicCompatible()) {
                        valExp.assertNonNull(value, env);
                    }
                    map.put(key, value);
                    keyList.add(key);
                    valueList.add(value);
                }
                keyCollection = new CollectionAndSequence(new SimpleSequence(keyList));
                valueCollection = new CollectionAndSequence(new SimpleSequence(valueList));
            }
        }

        public int size() {
            return size;
        }

        public TemplateCollectionModel keys() {
            if (keyCollection == null) {
                // This can only happen when IcI >= 2.3.21, an the map is a LinkedHashMap.
                keyCollection = new CollectionAndSequence(new SimpleSequence(map.keySet()));
            }
            return keyCollection;
        }

        public TemplateCollectionModel values() {
            if (valueCollection == null) {
                // This can only happen when IcI >= 2.3.21, an the map is a LinkedHashMap.
                valueCollection = new CollectionAndSequence(new SimpleSequence(map.values()));
            }
            return valueCollection;
        }

        public TemplateModel get(String key) {
            return (TemplateModel) map.get(key);
        }

        public boolean isEmpty() {
            return size == 0;
        }
        
        @Override
        public String toString() {
            return getCanonicalForm();
        }

        public KeyValuePairIterator keyValuePairIterator() throws TemplateModelException {
            return new KeyValuePairIterator() {
                private final TemplateModelIterator keyIterator = keys().iterator();
                private final TemplateModelIterator valueIterator = values().iterator();

                public boolean hasNext() throws TemplateModelException {
                    return keyIterator.hasNext();
                }

                public KeyValuePair next() throws TemplateModelException {
                    return new KeyValuePair() {
                        private final TemplateModel key = keyIterator.next();
                        private final TemplateModel value = valueIterator.next();

                        public TemplateModel getKey() throws TemplateModelException {
                            return key;
                        }

                        public TemplateModel getValue() throws TemplateModelException {
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
