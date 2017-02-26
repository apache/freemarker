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

package org.apache.freemarker.core.model.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.freemarker.core._DelayedShortClassName;
import org.apache.freemarker.core._TemplateModelException;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.ObjectWrapperWithAPISupport;
import org.apache.freemarker.core.model.TemplateCollectionModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateModelWithAPISupport;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.apache.freemarker.core.model.WrappingTemplateModel;

/**
 * Adapts a non-{@link List} Java {@link Collection} to the corresponding {@link TemplateModel} interface(s), most
 * importantly to {@link TemplateCollectionModelEx}. For {@link List}-s, use {@link DefaultListAdapter}, or else you
 * lose indexed element access.
 * 
 * <p>
 * Thread safety: A {@link DefaultNonListCollectionAdapter} is as thread-safe as the {@link Collection} that it wraps
 * is. Normally you only have to consider read-only access, as the FreeMarker template language doesn't allow writing
 * these collections (though of course, Java methods called from the template can violate this rule).
 *
 * @since 2.3.22
 */
public class DefaultNonListCollectionAdapter extends WrappingTemplateModel implements TemplateCollectionModelEx,
        AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport, Serializable {

    private final Collection collection;

    /**
     * Factory method for creating new adapter instances.
     * 
     * @param collection
     *            The collection to adapt; can't be {@code null}.
     * @param wrapper
     *            The {@link ObjectWrapper} used to wrap the items in the array. Has to be
     *            {@link ObjectWrapperAndUnwrapper} because of planned future features.
     */
    public static DefaultNonListCollectionAdapter adapt(Collection collection, ObjectWrapperWithAPISupport wrapper) {
        return new DefaultNonListCollectionAdapter(collection, wrapper);
    }

    private DefaultNonListCollectionAdapter(Collection collection, ObjectWrapperWithAPISupport wrapper) {
        super(wrapper);
        this.collection = collection;
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateModelException {
        return new DefaultUnassignableIteratorAdapter(collection.iterator(), getObjectWrapper());
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public Object getWrappedObject() {
        return collection;
    }

    @Override
    public Object getAdaptedObject(Class hint) {
        return getWrappedObject();
    }

    @Override
    public boolean contains(TemplateModel item) throws TemplateModelException {
        Object itemPojo = ((ObjectWrapperAndUnwrapper) getObjectWrapper()).unwrap(item);
        try {
            return collection.contains(itemPojo);
        } catch (ClassCastException e) {
            throw new _TemplateModelException(e,
                    "Failed to check if the collection contains the item. Probably the item's Java type, ",
                    itemPojo != null ? new _DelayedShortClassName(itemPojo.getClass()) : "Null",
                    ", doesn't match the type of (some of) the collection items; see cause exception.");
        }
    }

    @Override
    public TemplateModel getAPI() throws TemplateModelException {
        return ((ObjectWrapperWithAPISupport) getObjectWrapper()).wrapAsAPI(collection);
    }

}
