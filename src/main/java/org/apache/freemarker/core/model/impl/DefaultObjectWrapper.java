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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.impl.beans.BeansWrapper;
import org.apache.freemarker.core.model.impl.beans.BeansWrapperConfiguration;
import org.apache.freemarker.dom.NodeModel;
import org.w3c.dom.Node;

/**
 * The default implementation of the {@link ObjectWrapper} interface. Usually, you don't need to create instances of
 * this, as an instance of this is already the default value of the
 * {@link Configuration#setObjectWrapper(ObjectWrapper) object_wrapper setting}. Then the
 * {@link #DefaultObjectWrapper(Version) incompatibleImprovements} of the {@link DefaultObjectWrapper} will be the
 * same that you have set for the {@link Configuration} itself.
 * 
 * <p>
 * If you still need to create an instance, that should be done with an {@link DefaultObjectWrapperBuilder} (or
 * with {@link Configuration#setSetting(String, String)} with {@code "object_wrapper"} key), not with
 * its constructor, as that allows FreeMarker to reuse singletons.
 *
 * <p>
 * This class is only thread-safe after you have finished calling its setter methods, and then safely published it (see
 * JSR 133 and related literature). When used as part of {@link Configuration}, of course it's enough if that was safely
 * published and then left unmodified.
 */
public class DefaultObjectWrapper extends org.apache.freemarker.core.model.impl.beans.BeansWrapper {
    
    /**
     * Use {@link DefaultObjectWrapperBuilder} instead if possible. Instances created with this constructor won't share
     * the class introspection caches with other instances. See {@link BeansWrapper#BeansWrapper(Version)} (the
     * superclass constructor) for more details.
     * 
     * @param incompatibleImprovements
     *            It's the same as in {@link BeansWrapper#BeansWrapper(Version)}.
     * 
     * @since 2.3.21
     */
    public DefaultObjectWrapper(Version incompatibleImprovements) {
        this(new DefaultObjectWrapperConfiguration(incompatibleImprovements) { }, false);
    }

    /**
     * Use {@link #DefaultObjectWrapper(DefaultObjectWrapperConfiguration, boolean)} instead if possible;
     * it does the same, except that it tolerates a non-{@link DefaultObjectWrapperConfiguration} configuration too.
     * 
     * @since 2.3.21
     */
    protected DefaultObjectWrapper(BeansWrapperConfiguration bwCfg, boolean writeProtected) {
        super(bwCfg, writeProtected, false);
        DefaultObjectWrapperConfiguration dowDowCfg = bwCfg instanceof DefaultObjectWrapperConfiguration
                ? (DefaultObjectWrapperConfiguration) bwCfg
                : new DefaultObjectWrapperConfiguration(bwCfg.getIncompatibleImprovements()) { }; 
        finalizeConstruction(writeProtected);
    }

    /**
     * Calls {@link BeansWrapper#BeansWrapper(BeansWrapperConfiguration, boolean)} and sets up
     * {@link DefaultObjectWrapper}-specific fields.
     * 
     * @since 2.3.22
     */
    protected DefaultObjectWrapper(DefaultObjectWrapperConfiguration dowCfg, boolean writeProtected) {
        this((BeansWrapperConfiguration) dowCfg, writeProtected);
    }
    
    /**
     * Wraps the parameter object to {@link TemplateModel} interface(s). Simple types like numbers, strings, booleans
     * and dates will be wrapped into the corresponding {@code SimpleXxx} classes (like {@link SimpleNumber}).
     * {@link Map}-s, {@link List}-s, other {@link Collection}-s, arrays and {@link Iterator}-s will be wrapped into the
     * corresponding {@code DefaultXxxAdapter} classes ({@link DefaultMapAdapter}), depending on). After that, the
     * wrapping is handled by {@link #handleUnknownType(Object)}, so see more there.
     */
    @Override
    public TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj == null) {
            return super.wrap(null);
        }
        if (obj instanceof TemplateModel) {
            return (TemplateModel) obj;
        }
        if (obj instanceof String) {
            return new SimpleScalar((String) obj);
        }
        if (obj instanceof Number) {
            return new SimpleNumber((Number) obj);
        }
        if (obj instanceof java.util.Date) {
            if (obj instanceof java.sql.Date) {
                return new SimpleDate((java.sql.Date) obj);
            }
            if (obj instanceof java.sql.Time) {
                return new SimpleDate((java.sql.Time) obj);
            }
            if (obj instanceof java.sql.Timestamp) {
                return new SimpleDate((java.sql.Timestamp) obj);
            }
            return new SimpleDate((java.util.Date) obj, getDefaultDateType());
        }
        final Class<?> objClass = obj.getClass();
        if (objClass.isArray()) {
            return DefaultArrayAdapter.adapt(obj, this);
        }
        if (obj instanceof Collection) {
            return obj instanceof List
                    ? DefaultListAdapter.adapt((List<?>) obj, this)
                    : DefaultNonListCollectionAdapter.adapt((Collection<?>) obj, this);
        }
        if (obj instanceof Map) {
            return DefaultMapAdapter.adapt((Map<?, ?>) obj, this);
        }
        if (obj instanceof Boolean) {
            return obj.equals(Boolean.TRUE) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
        if (obj instanceof Iterator) {
            return DefaultIteratorAdapter.adapt((Iterator<?>) obj, this);
        }
        if (obj instanceof Iterable) {
            return DefaultIterableAdapter.adapt((Iterable<?>) obj, this);
        }
        return handleUnknownType(obj);
    }
    
    /**
     * Called for an object that isn't considered to be of a "basic" Java type, like for an application specific type,
     * or for a W3C DOM_WRAPPER node. In its default implementation, W3C {@link Node}-s will be wrapped as {@link NodeModel}-s
     * (allows DOM_WRAPPER tree traversal), others will be wrapped using {@link BeansWrapper#wrap(Object)}.
     * 
     * <p>
     * When you override this method, you should first decide if you want to wrap the object in a custom way (and if so
     * then do it and return with the result), and if not, then you should call the super method (assuming the default
     * behavior is fine with you).
     */
    protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
        if (obj instanceof Node) {
            return wrapDomNode(obj);
        }
        return super.wrap(obj); 
    }
    
    public TemplateModel wrapDomNode(Object obj) {
        return NodeModel.wrap((Node) obj);
    }

    /**
     * Converts an array to a java.util.List.
     */
    protected Object convertArray(Object arr) {
        // FM 2.4: Use Arrays.asList instead
        final int size = Array.getLength(arr);
        ArrayList list = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(Array.get(arr, i));
        }
        return list;
    }

    /**
     * Returns the lowest version number that is equivalent with the parameter version.
     * 
     * @since 2.3.22
     */
    protected static Version normalizeIncompatibleImprovementsVersion(Version incompatibleImprovements) {
        return BeansWrapper.normalizeIncompatibleImprovementsVersion(incompatibleImprovements);
    }

    /**
     * @since 2.3.22
     */
    @Override
    protected String toPropertiesString() {
        String bwProps = super.toPropertiesString();
        
        // Remove simpleMapWrapper, as its irrelevant for this wrapper:
        if (bwProps.startsWith("simpleMapWrapper")) {
            int smwEnd = bwProps.indexOf(',');
            if (smwEnd != -1) {
                bwProps = bwProps.substring(smwEnd + 1).trim();
            }
        }
        
        return "";
    }
    
}
