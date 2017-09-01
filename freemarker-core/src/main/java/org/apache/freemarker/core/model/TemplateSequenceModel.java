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

import org.apache.freemarker.core.TemplateException;

/**
 * "sequence" template language data type; an object that contains other objects accessible through an integer 0-based
 * index.
 * <p>
 * Used in templates like: {@code mySeq[index]}, {@code <#list mySeq as i>...</#list>}, {@code mySeq?size}, etc.
 *
 * @see TemplateIterableModel
 */
public interface TemplateSequenceModel extends TemplateCollectionModel {

    TemplateSequenceModel EMPTY_SEQUENCE = new EmptySequenceModel();

    /**
     * Retrieves the template model at the given index, or {@code null} if the index is out of bounds. If index-based
     * access on the backing data structure is not very efficient (say, it's O(N) and you known N can be big), then you
     * should consider only implementing {@link TemplateCollectionModel} instead. Also, if you need to list all items,
     * you shouldn't use this method, but {@link TemplateIterableModel#iterator()}.
     *
     * @return the item at the specified index, or <code>null</code> if the index is out of bounds. Note that a
     * <code>null</code> value is interpreted by FreeMarker as "variable does not exist", and accessing a missing
     * variables is usually considered as an error in the template language, so the usage of a bad index will not remain
     * hidden, unless the default value for that case was also specified in the template.
     */
    TemplateModel get(int index) throws TemplateException;

}
