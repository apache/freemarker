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

import java.util.Iterator;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.ObjectWrapperWithAPISupport;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateModelWithAPISupport;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.apache.freemarker.core.model.WrappingTemplateModel;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Adapts an {@link Iterator} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateIterableModel}. The resulting {@link TemplateIterableModel} can only be listed (iterated) once.
 * If the user tries list the variable for a second time, an exception will be thrown instead of silently gettig an
 * empty (or partial) listing.
 * 
 * <p>
 * Thread safety: A {@link DefaultListAdapter} is as thread-safe as the array that it wraps is. Normally you only
 * have to consider read-only access, as the FreeMarker template language doesn't allow writing these sequences (though
 * of course, Java methods called from the template can violate this rule).
 * 
 * <p>
 * This adapter is used by {@link DefaultObjectWrapper} if its {@code useAdaptersForCollections} property is
 * {@code true}, which is the default when its {@code incompatibleImprovements} property is 2.3.22 or higher.
 */
public class DefaultIteratorAdapter extends WrappingTemplateModel implements TemplateIterableModel,
        AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport {

    @SuppressFBWarnings(value="SE_BAD_FIELD", justification="We hope it's Seralizable")
    private final Iterator iterator;
    private boolean iteratorOwnedBySomeone;

    /**
     * Factory method for creating new adapter instances.
     * 
     * @param iterator
     *            The iterator to adapt; can't be {@code null}.
     */
    public static DefaultIteratorAdapter adapt(Iterator iterator, ObjectWrapper wrapper) {
        return new DefaultIteratorAdapter(iterator, wrapper);
    }

    private DefaultIteratorAdapter(Iterator iterator, ObjectWrapper wrapper) {
        super(wrapper);
        this.iterator = iterator;
    }

    @Override
    public Object getWrappedObject() {
        return iterator;
    }

    @Override
    public Object getAdaptedObject(Class hint) {
        return getWrappedObject();
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return new SimpleTemplateModelIterator();
    }

    @Override
    public TemplateModel getAPI() throws TemplateException {
        return ((ObjectWrapperWithAPISupport) getObjectWrapper()).wrapAsAPI(iterator);
    }

    /**
     * Not thread-safe.
     */
    private class SimpleTemplateModelIterator implements TemplateModelIterator {

        private boolean iteratorOwnedByMe;

        @Override
        public TemplateModel next() throws TemplateException {
            if (!iteratorOwnedByMe) {
                checkNotOwner();
                iteratorOwnedBySomeone = true;
                iteratorOwnedByMe = true;
            }

            Object value = iterator.next();
            return value instanceof TemplateModel ? (TemplateModel) value : wrap(value);
        }

        @Override
        public boolean hasNext() throws TemplateException {
            // Calling hasNext may looks safe, but I have met sync. problems.
            if (!iteratorOwnedByMe) {
                checkNotOwner();
            }

            return iterator.hasNext();
        }

        private void checkNotOwner() throws TemplateException {
            if (iteratorOwnedBySomeone) {
                throw new TemplateException(
                        "This value wraps a java.util.Iterator, thus it can be listed only once.");
            }
        }
    }

}
