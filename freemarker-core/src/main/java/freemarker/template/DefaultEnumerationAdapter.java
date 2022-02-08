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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.utility.ObjectWrapperWithAPISupport;

/**
 * Adapts an {@link Enumeration} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateCollectionModel}. Putting aside that it wraps an {@link Enumeration} instead of an {@link Iterator},
 * this is identical to {@link DefaultIteratorAdapter}, so see further details there.
 * 
 * @since 2.3.26
 */
@SuppressWarnings("serial")
public class DefaultEnumerationAdapter extends WrappingTemplateModel implements TemplateCollectionModel,
        AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport, Serializable {

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
    public TemplateModelIterator iterator() throws TemplateModelException {
        return new SimpleTemplateModelIterator();
    }

    @Override
    public TemplateModel getAPI() throws TemplateModelException {
        return ((ObjectWrapperWithAPISupport) getObjectWrapper()).wrapAsAPI(enumeration);
    }
    
    /**
     * Not thread-safe.
     */
    private class SimpleTemplateModelIterator implements TemplateModelIterator {

        private boolean enumerationOwnedByMe;

        @Override
        public TemplateModel next() throws TemplateModelException {
            if (!enumerationOwnedByMe) {
                checkNotOwner();
                enumerationOwnedBySomeone = true;
                enumerationOwnedByMe = true;
            }

            if (!enumeration.hasMoreElements()) {
                throw new TemplateModelException("The collection has no more items.");
            }

            Object value = enumeration.nextElement();
            return value instanceof TemplateModel ? (TemplateModel) value : wrap(value);
        }

        @Override
        public boolean hasNext() throws TemplateModelException {
            // Calling hasNext may looks safe, but I have met sync. problems.
            if (!enumerationOwnedByMe) {
                checkNotOwner();
            }

            return enumeration.hasMoreElements();
        }

        private void checkNotOwner() throws TemplateModelException {
            if (enumerationOwnedBySomeone) {
                throw new TemplateModelException(
                        "This collection value wraps a java.util.Enumeration, thus it can be listed only once.");
            }
        }
    }

}
