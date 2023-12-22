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
import java.util.AbstractSequentialList;
import java.util.List;

import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.utility.ObjectWrapperWithAPISupport;
import freemarker.template.utility.RichObjectWrapper;

/**
 * Adapts a {@link List} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateSequenceModel}. If you aren't wrapping an already existing {@link List}, but build a sequence
 * specifically to be used from a template, also consider using {@link SimpleSequence} (see comparison there).
 * 
 * <p>
 * Thread safety: A {@link DefaultListAdapter} is as thread-safe as the {@link List} that it wraps is. Normally you only
 * have to consider read-only access, as the FreeMarker template language doesn't allow writing these sequences (though
 * of course, Java methods called from the template can violate this rule).
 * 
 * <p>
 * This adapter is used by {@link DefaultObjectWrapper} if its {@code useAdaptersForCollections} property is
 * {@code true}, which is the default when its {@code incompatibleImprovements} property is 2.3.22 or higher.
 * 
 * @see SimpleSequence
 * @see DefaultArrayAdapter
 * @see TemplateSequenceModel
 * 
 * @since 2.3.22
 */
public class DefaultListAdapter extends WrappingTemplateModel implements TemplateSequenceModel,
        AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport, Serializable {

    protected final List list;

    /**
     * Factory method for creating new adapter instances.
     * 
     * @param list
     *            The list to adapt; can't be {@code null}.
     * @param wrapper
     *            The {@link ObjectWrapper} used to wrap the items in the array.
     */
    public static DefaultListAdapter adapt(List list, RichObjectWrapper wrapper) {
        // [2.4] DefaultListAdapter should implement TemplateCollectionModelEx, so this choice becomes unnecessary
        return list instanceof AbstractSequentialList
                ? new DefaultListAdapterWithCollectionSupport(list, wrapper)
                : new DefaultListAdapter(list, wrapper);
    }

    private DefaultListAdapter(List list, RichObjectWrapper wrapper) {
        super(wrapper);
        this.list = list;
    }

    @Override
    public TemplateModel get(int index) throws TemplateModelException {
        return index >= 0 && index < list.size() ? wrap(list.get(index)) : null;
    }

    @Override
    public int size() throws TemplateModelException {
        return list.size();
    }

    @Override
    public Object getAdaptedObject(Class hint) {
        return getWrappedObject();
    }

    @Override
    public Object getWrappedObject() {
        return list;
    }

    private static class DefaultListAdapterWithCollectionSupport extends DefaultListAdapter implements
            TemplateCollectionModel {

        private DefaultListAdapterWithCollectionSupport(List list, RichObjectWrapper wrapper) {
            super(list, wrapper);
        }

        @Override
        public TemplateModelIterator iterator() throws TemplateModelException {
            return new IteratorToTemplateModelIteratorAdapter(list.iterator(), getObjectWrapper());
        }

    }

    @Override
    public TemplateModel getAPI() throws TemplateModelException {
        return ((ObjectWrapperWithAPISupport) getObjectWrapper()).wrapAsAPI(list);
    }
    
}
