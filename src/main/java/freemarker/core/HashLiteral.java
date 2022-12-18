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
import java.util.List;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template._ObjectWrappers;
import freemarker.template._TemplateAPI;
import freemarker.template._VersionInts;

@SuppressWarnings("deprecation")
final class HashLiteral extends Expression {

    private final List<? extends Expression> keys, values;
    private final int size;

    HashLiteral(List<? extends Expression> keys, List<? extends Expression> values) {
        this.keys = keys;
        this.values = values;
        this.size = keys.size();
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        return new SequenceHash(env);
    }

    @Override
    public String getCanonicalForm() {
        StringBuilder buf = new StringBuilder("{");
        for (int i = 0; i < size; i++) {
            Expression key = keys.get(i);
            Expression value = values.get(i);
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
            Expression key = keys.get(i);
            Expression value = values.get(i);
            if (!key.isLiteral() || !value.isLiteral()) {
                return false;
            }
        }
        return true;
    }


    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
		List<Expression> clonedKeys = new ArrayList<>(keys.size());
        for (Expression key : keys) {
            clonedKeys.add(key.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
        }

        List<Expression> clonedValues = new ArrayList<>(values.size());
        for (Expression value : values) {
            clonedValues.add(value.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
        }
        
    	return new HashLiteral(clonedKeys, clonedValues);
    }

    @SuppressWarnings("deprecation")
    private class SequenceHash implements TemplateHashModelEx2 {

        private HashMap<String, TemplateModel> map; // maps keys to integer offset
        private TemplateCollectionModel keyCollection, valueCollection; // ordered lists of keys and values

        SequenceHash(Environment env) throws TemplateException {
            if (_TemplateAPI.getTemplateLanguageVersionAsInt(HashLiteral.this) >= _VersionInts.V_2_3_21) {
                map = new LinkedHashMap<>();
                for (int i = 0; i < size; i++) {
                    Expression keyExp = keys.get(i);
                    Expression valExp = values.get(i);
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
                map = new HashMap<>();
                SimpleSequence keyList = new SimpleSequence(size, _ObjectWrappers.SAFE_OBJECT_WRAPPER);
                SimpleSequence valueList = new SimpleSequence(size, _ObjectWrappers.SAFE_OBJECT_WRAPPER);
                for (int i = 0; i < size; i++) {
                    Expression keyExp = keys.get(i);
                    Expression valExp = values.get(i);
                    String key = keyExp.evalAndCoerceToPlainText(env);
                    TemplateModel value = valExp.eval(env);
                    if (env == null || !env.isClassicCompatible()) {
                        valExp.assertNonNull(value, env);
                    }
                    map.put(key, value);
                    keyList.add(key);
                    valueList.add(value);
                }
                keyCollection = new CollectionAndSequence(keyList);
                valueCollection = new CollectionAndSequence(valueList);
            }
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public TemplateCollectionModel keys() {
            if (keyCollection == null) {
                // This can only happen when IcI >= 2.3.21, an the map is a LinkedHashMap.
                keyCollection = new CollectionAndSequence(
                        new SimpleSequence(map.keySet(), _ObjectWrappers.SAFE_OBJECT_WRAPPER));
            }
            return keyCollection;
        }

        @Override
        public TemplateCollectionModel values() {
            if (valueCollection == null) {
                // This can only happen when IcI >= 2.3.21, an the map is a LinkedHashMap.
                valueCollection = new CollectionAndSequence(
                        new SimpleSequence(map.values(), _ObjectWrappers.SAFE_OBJECT_WRAPPER));
            }
            return valueCollection;
        }

        @Override
        public TemplateModel get(String key) {
            return map.get(key);
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }
        
        @Override
        public String toString() {
            return getCanonicalForm();
        }

        @Override
        public KeyValuePairIterator keyValuePairIterator() throws TemplateModelException {
            return new KeyValuePairIterator() {
                private final TemplateModelIterator keyIterator = keys().iterator();
                private final TemplateModelIterator valueIterator = values().iterator();

                @Override
                public boolean hasNext() throws TemplateModelException {
                    return keyIterator.hasNext();
                }

                @Override
                public KeyValuePair next() throws TemplateModelException {
                    return new KeyValuePair() {
                        private final TemplateModel key = keyIterator.next();
                        private final TemplateModel value = valueIterator.next();

                        @Override
                        public TemplateModel getKey() {
                            return key;
                        }

                        @Override
                        public TemplateModel getValue() {
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
