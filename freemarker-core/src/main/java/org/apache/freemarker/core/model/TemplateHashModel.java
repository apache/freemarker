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
 * "hash" template language data type: an object that contains other objects accessible through string keys
 * (sub-variable names). It, in itself, doesn't support listing the keys or values ({@link TemplateHashModelEx} does).
 * 
 * <p>In templates they are used like {@code myHash.myKey} or {@code myHash[myDynamicKey]}. 
 */
public interface TemplateHashModel extends TemplateModel {

    TemplateHashModelEx EMPTY_HASH = new EmptyHashModel();

    /**
     * Gets a <tt>TemplateModel</tt> from the hash.
     *
     * @param key the name by which the <tt>TemplateModel</tt>
     * is identified in the template.
     * @return the <tt>TemplateModel</tt> referred to by the key,
     * or null if not found.
     */
    TemplateModel get(String key) throws TemplateException;

}
