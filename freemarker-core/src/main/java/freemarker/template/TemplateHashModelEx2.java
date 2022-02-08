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
package freemarker.template;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Adds key-value pair listing capability to {@link TemplateHashModelEx}. While in many cases that can also be achieved
 * with {@link #keys()} and then {@link #get(String)}, that has some problems. One is that {@link #get(String)} only
 * accepts string keys, while {@link #keys()} can return non-string keys too. The other is that calling {@link #keys()}
 * and then {@link #get(String)} for each key can be slower than listing the key-value pairs in one go.
 * 
 * @since 2.3.25
 */
public interface TemplateHashModelEx2 extends TemplateHashModelEx {

    /**
     * @return The iterator that walks through the key-value pairs in the hash. Not {@code null}. 
     */
    KeyValuePairIterator keyValuePairIterator() throws TemplateModelException;
    
    /**
     * A key-value pair in a hash; used for {@link KeyValuePairIterator}.
     *  
     * @since 2.3.25
     */
    interface KeyValuePair {
        
        /**
         * @return Any type of {@link TemplateModel}, maybe {@code null} (if the hash entry key is {@code null}).
         */
        TemplateModel getKey() throws TemplateModelException;
        
        /**
         * @return Any type of {@link TemplateModel}, maybe {@code null} (if the hash entry value is {@code null}).
         */
        TemplateModel getValue() throws TemplateModelException;
    }
    
    /**
     * Iterates over the key-value pairs in a hash. This is very similar to an {@link Iterator}, but has a fixed item
     * type, can throw {@link TemplateModelException}-s, and has no {@code remove()} method. 
     *
     * @since 2.3.25
     */
    interface KeyValuePairIterator {
        
        /**
         * Similar to {@link Iterator#hasNext()}.
         */
        boolean hasNext() throws TemplateModelException;
        
        /**
         * Similar to {@link Iterator#next()}.
         * 
         * @return Not {@code null}
         * 
         * @throws NoSuchElementException
         */
        KeyValuePair next() throws TemplateModelException;
    }
    
}
