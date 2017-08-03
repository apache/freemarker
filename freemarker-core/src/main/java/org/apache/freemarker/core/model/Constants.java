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

package org.apache.freemarker.core.model;

import java.io.Serializable;
import java.util.NoSuchElementException;

import org.apache.freemarker.core.model.TemplateHashModelEx2.KeyValuePairIterator;
import org.apache.freemarker.core.model.impl.SimpleNumber;

/**
 * Frequently used constant {@link TemplateModel} values.
 * 
 * <p>These constants should be stored in the {@link TemplateModel}
 * sub-interfaces, but for bacward compatibility they are stored here instead.
 * Starting from FreeMarker 2.4 they should be copyed (not moved!) into the
 * {@link TemplateModel} sub-interfaces, and this class should be marked as
 * deprecated.</p>
 */
public class Constants {

    public static final TemplateBooleanModel TRUE = TemplateBooleanModel.TRUE;

    public static final TemplateBooleanModel FALSE = TemplateBooleanModel.FALSE;
    
    public static final TemplateScalarModel EMPTY_STRING = (TemplateScalarModel) TemplateScalarModel.EMPTY_STRING;

    public static final TemplateNumberModel ZERO = new SimpleNumber(0);
    
    public static final TemplateNumberModel ONE = new SimpleNumber(1);
    
    public static final TemplateNumberModel MINUS_ONE = new SimpleNumber(-1);
    
    public static final TemplateModelIterator EMPTY_ITERATOR = new EmptyIteratorModel();

    public static final TemplateModel[] EMPTY_TEMPLATE_MODEL_ARRAY = new TemplateModel[0];

    private static class EmptyIteratorModel implements TemplateModelIterator, Serializable {

        @Override
        public TemplateModel next() throws TemplateModelException {
            throw new TemplateModelException("The collection has no more elements.");
        }

        @Override
        public boolean hasNext() throws TemplateModelException {
            return false;
        }
        
    }

    public static final TemplateCollectionModelEx EMPTY_COLLECTION = new EmptyCollectionExModel();
    
    private static class EmptyCollectionExModel implements TemplateCollectionModelEx, Serializable {

        @Override
        public int size() throws TemplateModelException {
            return 0;
        }

        @Override
        public boolean isEmpty() throws TemplateModelException {
            return true;
        }

        @Override
        public TemplateModelIterator iterator() throws TemplateModelException {
            return EMPTY_ITERATOR;
        }
        
    }
    
    public static final TemplateSequenceModel EMPTY_SEQUENCE = new EmptySequenceModel();
    
    private static class EmptySequenceModel implements TemplateSequenceModel, Serializable {
        
        @Override
        public TemplateModel get(int index) throws TemplateModelException {
            return null;
        }
    
        @Override
        public int size() throws TemplateModelException {
            return 0;
        }
        
    }
    
    public static final TemplateHashModelEx EMPTY_HASH = new EmptyHashModel();
    
    private static class EmptyHashModel implements TemplateHashModelEx2, Serializable {
        
        @Override
        public int size() throws TemplateModelException {
            return 0;
        }

        @Override
        public TemplateCollectionModel keys() throws TemplateModelException {
            return EMPTY_COLLECTION;
        }

        @Override
        public TemplateCollectionModel values() throws TemplateModelException {
            return EMPTY_COLLECTION;
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            return null;
        }

        @Override
        public boolean isEmpty() throws TemplateModelException {
            return true;
        }

        @Override
        public KeyValuePairIterator keyValuePairIterator() throws TemplateModelException {
            return EMPTY_KEY_VALUE_PAIR_ITERATOR;
        }
    }

    public static final KeyValuePairIterator EMPTY_KEY_VALUE_PAIR_ITERATOR = EmptyKeyValuePairIterator.INSTANCE;

    private static class EmptyKeyValuePairIterator implements TemplateHashModelEx2.KeyValuePairIterator {

        static final EmptyKeyValuePairIterator INSTANCE = new EmptyKeyValuePairIterator();

        private EmptyKeyValuePairIterator() {
            //
        }

        @Override
        public boolean hasNext() throws TemplateModelException {
            return false;
        }

        @Override
        public TemplateHashModelEx2.KeyValuePair next() throws TemplateModelException {
            throw new NoSuchElementException("Can't retrieve element from empty key-value pair iterator.");
        }

    }

}
