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

import java.util.Iterator;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.impl.SimpleHash;

/**
 * "extended hash" template language data type; extends {@link TemplateHashModel} by allowing
 * iterating through its keys and values.
 * 
 * <p>In templates they are used like hashes, but these will also work (among others):
 * {@code myExtHash?size}, {@code myExtHash?keys}, {@code myExtHash?values}.
 * @see SimpleHash
 */
public interface TemplateHashModelEx extends TemplateHashModel {

    /**
     * A key-value pair in a hash; used for {@link TemplateHashModelEx.KeyValuePairIterator}.
     */
    interface KeyValuePair {
        
        /**
         * @return Any type of {@link TemplateModel}, maybe {@code null} (if the hash entry key is {@code null}).
         */
        TemplateModel getKey() throws TemplateException;
        
        /**
         * @return Any type of {@link TemplateModel}, maybe {@code null} (if the hash entry value is {@code null}).
         */
        TemplateModel getValue() throws TemplateException;
    }

    /**
     * Iterates over the key-value pairs in a hash. This is very similar to an {@link Iterator}, but has a fixed item
     * type, can throw {@link TemplateException}-s, and has no {@code remove()} method.
     */
    interface KeyValuePairIterator {
    
        TemplateHashModelEx.KeyValuePairIterator EMPTY_KEY_VALUE_PAIR_ITERATOR = new EmptyKeyValuePairIterator();
    
        /**
         * Similar to {@link Iterator#hasNext()}.
         */
        boolean hasNext() throws TemplateException;
        
        /**
         * Similar to {@link TemplateModelIterator#next()}, but returns a {@link KeyValuePair}-s instead of
         * {@link TemplateModel}-s. As such, its behavior is undefined too, if it's called when there's no more
         * items (so you must use {@link #hasNext()}, unless you know how many key-value pairs there are).
         * 
         * @return Not {@code null}
         */
        KeyValuePair next() throws TemplateException;
    }

    /**
     * @return the number of key/value mappings in the hash.
     */
    int getHashSize() throws TemplateException;

    /**
     * @return a iterable returning the keys in the hash. Every element of the returned by the iterable must implement
     * the {@link TemplateStringModel} (as the keys of hashes are always strings).
     */
    TemplateCollectionModel keys() throws TemplateException;

    /**
     * @return An iterable returning the values in the hash. The elements returned by the iterable can be any kind of
     * {@link TemplateModel}-s.
     */
    TemplateCollectionModel values() throws TemplateException;
    

    /**
     * @return The iterator that walks through the key-value pairs in the hash. Not {@code null}. 
     */
    TemplateHashModelEx.KeyValuePairIterator keyValuePairIterator() throws TemplateException;
    
}
