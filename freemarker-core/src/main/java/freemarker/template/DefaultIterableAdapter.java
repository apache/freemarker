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
import java.util.Collection;
import java.util.Iterator;

import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.utility.ObjectWrapperWithAPISupport;

/**
 * Adapts an {@link Iterable} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateCollectionModel}. This should only be used if {@link Collection} is not implemented by the adapted
 * object, because then {@link DefaultListAdapter} and {@link DefaultNonListCollectionAdapter} gives more functionality.
 * 
 * <p>
 * Thread safety: A {@link DefaultIterableAdapter} is as thread-safe as the {@link Iterable} that it wraps is. Normally
 * you only have to consider read-only access, as the FreeMarker template language doesn't provide means to call
 * {@link Iterator} modifier methods (though of course, Java methods called from the template can violate this rule).
 * 
 * <p>
 * This adapter is used by {@link DefaultObjectWrapper} if its {@link DefaultObjectWrapper#setIterableSupport(boolean)
 * iterableSupport} property is {@code true}, which is not the default for backward compatibility (so you have to set it
 * explicitly).
 * 
 * @since 2.3.25
 */
@SuppressWarnings("serial")
public class DefaultIterableAdapter extends WrappingTemplateModel implements TemplateCollectionModel,
        AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport, Serializable {
    
    private final Iterable<?> iterable;

    /**
     * Factory method for creating new adapter instances.
     * 
     * @param iterable
     *            The collection to adapt; can't be {@code null}.
     * @param wrapper
     *            The {@link ObjectWrapper} used to wrap the items in the array. Has to be
     *            {@link ObjectWrapperAndUnwrapper} because of planned future features.
     */
    public static DefaultIterableAdapter adapt(Iterable<?> iterable, ObjectWrapperWithAPISupport wrapper) {
        return new DefaultIterableAdapter(iterable, wrapper);
    }

    private DefaultIterableAdapter(Iterable<?> iterable, ObjectWrapperWithAPISupport wrapper) {
        super(wrapper);
        this.iterable = iterable;
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateModelException {
        return new IteratorToTemplateModelIteratorAdapter(iterable.iterator(), getObjectWrapper());
    }

    @Override
    public Object getWrappedObject() {
        return iterable;
    }

    @Override
    public Object getAdaptedObject(Class hint) {
        return getWrappedObject();
    }

    @Override
    public TemplateModel getAPI() throws TemplateModelException {
        return ((ObjectWrapperWithAPISupport) getObjectWrapper()).wrapAsAPI(iterable);
    }

}
