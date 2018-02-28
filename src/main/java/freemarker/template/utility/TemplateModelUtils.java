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
package freemarker.template.utility;

import freemarker.core._MessageUtil;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateHashModelEx2.KeyValuePair;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;

/**
 * Static utility method related to {@link TemplateModel}-s that didn't fit elsewhere.
 * 
 * @since 2.3.28
 */
public final class TemplateModelUtils {

    // Private to prevent instantiation
    private TemplateModelUtils() {
        // no op.
    }

    /**
     * {@link TemplateHashModelExKeyValuePairIterator} that even works for a non-{@link TemplateHashModelEx2}
     * {@link TemplateHashModelEx}. This is used to simplify code that needs to iterate through the key-value pairs of
     * {@link TemplateHashModelEx}-s, as with this you don't have to handle non-{@link TemplateHashModelEx2}-s
     * separately. For non-{@link TemplateHashModelEx2} values the iteration will throw {@link TemplateModelException}
     * if it reaches a key that's not a string ({@link TemplateScalarModel}).
     */
    public static final TemplateHashModelEx2.KeyValuePairIterator getKeyValuePairIterator(TemplateHashModelEx hash)
            throws TemplateModelException {
        return hash instanceof TemplateHashModelEx2 ? ((TemplateHashModelEx2) hash).keyValuePairIterator()
                : new TemplateHashModelExKeyValuePairIterator(hash);
    }

    private static class TemplateHashModelExKeyValuePairIterator implements TemplateHashModelEx2.KeyValuePairIterator {

        private final TemplateHashModelEx hash;
        private final TemplateModelIterator keyIter;

        private TemplateHashModelExKeyValuePairIterator(TemplateHashModelEx hash) throws TemplateModelException {
            this.hash = hash;
            keyIter = hash.keys().iterator();
        }

        public boolean hasNext() throws TemplateModelException {
            return keyIter.hasNext();
        }

        public KeyValuePair next() throws TemplateModelException {
            final TemplateModel key = keyIter.next();
            if (!(key instanceof TemplateScalarModel)) {
                throw _MessageUtil.newKeyValuePairListingNonStringKeyExceptionMessage(key, hash);
            }

            return new KeyValuePair() {

                public TemplateModel getKey() throws TemplateModelException {
                    return key;
                }

                public TemplateModel getValue() throws TemplateModelException {
                    return hash.get(((TemplateScalarModel) key).getAsString());
                }

            };
        }

    }

}