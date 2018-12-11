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

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.impl.DefaultListAdapter;

/**
 * Used to iterate over a set of template models <em>once</em>; usually returned from {@link
 * TemplateIterableModel#iterator()}. Note that it's not a {@link TemplateModel}. Note that the implementation of this
 * interface may assume that the collection of elements that we iterate through is not changing after the {@link
 * TemplateModelIterator} was created, as far as the {@link TemplateModelIterator} is still in use. If they still
 * change, the methods of this interface might throw any kind of exception or gives and inconsistent view of the
 * elements (like partially old element, partially new elements). Of course, implementations of this interface may
 * specify a more specific behavior. Notably, the {@link TemplateModelIterator} return by {@link
 * DefaultListAdapter#iterator()} gives the same concurrency guarantees as the wrapped {@link List} object.
 */
public interface TemplateModelIterator {

    TemplateModelIterator EMPTY_ITERATOR = new EmptyIteratorModel();

    /**
     * Returns the next item. It must not be called if there are no more items, as the behavior is undefined then
     * (typically, {@link NoSuchElementException} or {@link IndexOutOfBoundsException} will be thrown, or {@code null}
     * will be returned). Hence, you should almost always call {@link #hasNext()} before this method, and only call this
     * method if that has returned {@code true}. (Note that the implementation still can't assume that {@link
     * #hasNext()} is always called before {@link #next()}; the caller might knows that there's a next item for a
     * different reason, like already knows the size of the collection.)
     */
    TemplateModel next() throws TemplateException;

    /**
     * @return whether there are any more items to iterate over.
     */
    boolean hasNext() throws TemplateException;

}
