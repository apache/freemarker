/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.template;

import java.io.Serializable;
import java.util.Iterator;

import freemarker.ext.util.WrapperTemplateModel;

/**
 * Adapts an {@link Iterator} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateCollectionModel}. The resulting {@link TemplateCollectionModel} can only be iterated once.
 * 
 * <p>
 * Thread safety: A {@link DefaultListAdapter} is as thread-safe as the array that it wraps is. Normally you only
 * have to consider read-only access, as the FreeMarker template language doesn't allow writing these sequences (though
 * of course, Java methods called from the template can violate this rule).
 * 
 * <p>
 * This adapter is used by {@link DefaultObjectWrapper} if its {@code useAdaptersForCollections} property is
 * {@code true}, which is the default when its {@code incompatibleImprovements} property is 2.3.22 or higher.
 * 
 * @since 2.3.22
 */
public class DefaultIteratorAdapter extends WrappingTemplateModel implements TemplateCollectionModel,
        AdapterTemplateModel, WrapperTemplateModel, Serializable {

    private final Iterator iterator;
    private boolean iteratorOwned;

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

    public Object getWrappedObject() {
        return iterator;
    }

    public Object getAdaptedObject(Class hint) {
        return getWrappedObject();
    }

    public TemplateModelIterator iterator() throws TemplateModelException {
        return new SimpleTemplateModelIterator();
    }

    /**
     * Not thread-safe.
     */
    private class SimpleTemplateModelIterator implements TemplateModelIterator {

        private boolean iteratorOwnedByMe;

        public TemplateModel next() throws TemplateModelException {
            if (!iteratorOwnedByMe) {
                takeIteratorOwnership();
            }

            if (!iterator.hasNext()) {
                throw new TemplateModelException("The collection has no more items.");
            }

            Object value = iterator.next();
            return value instanceof TemplateModel ? (TemplateModel) value : wrap(value);
        }

        public boolean hasNext() throws TemplateModelException {
            // Calling hasNext may looks safe, but I have met sync. problems.
            if (!iteratorOwnedByMe) {
                takeIteratorOwnership();
            }

            return iterator.hasNext();
        }

        private void takeIteratorOwnership() throws TemplateModelException {
            if (iteratorOwned) {
                throw new TemplateModelException(
                        "This collection value wraps a java.util.Iterator, thus it can be listed only once.");
            } else {
                iteratorOwned = true;
                iteratorOwnedByMe = true;
            }
        }
    }

}
