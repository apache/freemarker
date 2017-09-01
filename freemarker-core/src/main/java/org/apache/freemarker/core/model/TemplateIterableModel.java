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

import java.util.Collection;

import org.apache.freemarker.core.TemplateException;

/**
 * "iterable" template language data type: a collection of values that can be listed; this interface doesn't add
 * the ability of getting the number of elements, or to return elements by index.
 * 
 * <p>
 * The enumeration should be repeatable if that's possible with reasonable effort, otherwise a second enumeration
 * attempt is allowed to throw an {@link TemplateException}. Generally, the interface user Java code need not
 * handle that kind of exception, as in practice only the template author can handle it, by not listing such collections
 * twice.
 * 
 * <p>
 * Note that to wrap Java's {@link Collection}, you should implement {@link TemplateCollectionModel}, not just this
 * interface.
 */
public interface TemplateIterableModel extends TemplateModel {

    TemplateCollectionModel EMPTY_ITERABLE = new EmptyCollectionModel();

    /**
     * Retrieves an iterator that is used to iterate over the elements of something.
     */
    TemplateModelIterator iterator() throws TemplateException;

}
