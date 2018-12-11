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

import java.util.Enumeration;
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
 * Adapts an {@link Enumeration} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateIterableModel}. Putting aside that it wraps an {@link Enumeration} instead of an {@link Iterator},
 * this is identical to {@link DefaultIteratorAdapter}, so see further details there.
 */
@SuppressWarnings("serial")
public class DefaultEnumerationAdapter extends WrappingTemplateModel implements TemplateIterableModel,
        AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport {

    @SuppressFBWarnings(value="SE_BAD_FIELD", justification="We hope it's Seralizable")
    private final Enumeration<?> enumeration;
    private boolean enumerationOwnedBySomeone;

    /**
     * Factory method for creating new adapter instances.
     *
     * @param enumeration
     *            The enumeration to adapt; can't be {@code null}.
     */
    public static DefaultEnumerationAdapter adapt(Enumeration<?> enumeration, ObjectWrapper wrapper) {
        return new DefaultEnumerationAdapter(enumeration, wrapper);
    }

    private DefaultEnumerationAdapter(Enumeration<?> enumeration, ObjectWrapper wrapper) {
        super(wrapper);
        this.enumeration = enumeration;
    }

    @Override
    public Object getWrappedObject() {
        return enumeration;
    }

    @Override
    public Object getAdaptedObject(Class<?> hint) {
        return getWrappedObject();
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return new SimpleTemplateModelIterator();
    }

    @Override
    public TemplateModel getAPI() throws TemplateException {
        return ((ObjectWrapperWithAPISupport) getObjectWrapper()).wrapAsAPI(enumeration);
    }

    /**
     * Not thread-safe.
     */
    private class SimpleTemplateModelIterator implements TemplateModelIterator {

        private boolean enumerationOwnedByMe;

        @Override
        public TemplateModel next() throws TemplateException {
            if (!enumerationOwnedByMe) {
                checkNotOwner();
                enumerationOwnedBySomeone = true;
                enumerationOwnedByMe = true;
            }

            Object value = enumeration.nextElement();
            return value instanceof TemplateModel ? (TemplateModel) value : wrap(value);
        }

        @Override
        public boolean hasNext() throws TemplateException {
            // Calling hasNext may looks safe, but I have met sync. problems.
            if (!enumerationOwnedByMe) {
                checkNotOwner();
            }

            return enumeration.hasMoreElements();
        }

        private void checkNotOwner() throws TemplateException {
            if (enumerationOwnedBySomeone) {
                throw new TemplateException(
                        "This iterator value wraps a java.util.Enumeration, thus it can be listed only once.");
            }
        }
    }

}
