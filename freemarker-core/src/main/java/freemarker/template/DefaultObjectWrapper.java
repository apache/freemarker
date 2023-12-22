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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperConfiguration;
import freemarker.ext.beans.DefaultMemberAccessPolicy;
import freemarker.ext.beans.EnumerationModel;
import freemarker.ext.beans.LegacyDefaultMemberAccessPolicy;
import freemarker.ext.beans.MemberAccessPolicy;
import freemarker.ext.dom.NodeModel;
import freemarker.log.Logger;

/**
 * The default implementation of the {@link ObjectWrapper} interface. Usually, you don't need to create instances of
 * this, as an instance of this is already the default value of the
 * {@link Configuration#setObjectWrapper(ObjectWrapper) object_wrapper setting}. Then the
 * {@link #DefaultObjectWrapper(Version) incompatibleImprovements} of the {@link DefaultObjectWrapper} will be the same
 * that you have set for the {@link Configuration} itself. As of this writing, it's highly recommended to use
 * {@link Configuration#Configuration(Version) incompatibleImprovements} 2.3.22 (or higher).
 * 
 * <p>
 * If you still need to create an instance, that should be done with an {@link DefaultObjectWrapperBuilder} (or
 * with {@link Configuration#setSetting(String, String)} with {@code "object_wrapper"} key), not with
 * its constructor, as that allows FreeMarker to reuse singletons. For new projects, it's recommended to set
 * {@link DefaultObjectWrapperBuilder#setForceLegacyNonListCollections(boolean) forceLegacyNonListCollections} to
 * {@code false}, and {@link DefaultObjectWrapperBuilder#setIterableSupport(boolean) iterableSupport} to {@code true};
 * setting {@code incompatibleImprovements} to 2.3.22 won't do these, as they could break legacy templates too easily.
 *
 * <p>
 * This class is only thread-safe after you have finished calling its setter methods, and then safely published it (see
 * JSR 133 and related literature). When used as part of {@link Configuration}, of course it's enough if that was safely
 * published and then left unmodified.
 */
public class DefaultObjectWrapper extends freemarker.ext.beans.BeansWrapper {
    
    /** @deprecated Use {@link DefaultObjectWrapperBuilder} instead, but mind its performance */
    @Deprecated
    static final DefaultObjectWrapper instance = new DefaultObjectWrapper();
    
    static final private Class<?> JYTHON_OBJ_CLASS;
    
    static final private ObjectWrapper JYTHON_WRAPPER;
    
    private boolean useAdaptersForContainers;
    private boolean forceLegacyNonListCollections;
    private boolean iterableSupport;
    private boolean domNodeSupport;
    private boolean jythonSupport;
    private final boolean useAdapterForEnumerations;

    /**
     * Creates a new instance with the incompatible-improvements-version specified in
     * {@link Configuration#DEFAULT_INCOMPATIBLE_IMPROVEMENTS}.
     * 
     * @deprecated Use {@link DefaultObjectWrapperBuilder}, or in rare cases,
     *          {@link #DefaultObjectWrapper(Version)} instead.
     */
    @Deprecated
    public DefaultObjectWrapper() {
        this(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    }
    
    /**
     * Use {@link DefaultObjectWrapperBuilder} instead if possible. Instances created with this constructor won't share
     * the class introspection caches with other instances. See {@link BeansWrapper#BeansWrapper(Version)} (the
     * superclass constructor) for more details.
     * 
     * @param incompatibleImprovements
     *            It's the same as in {@link BeansWrapper#BeansWrapper(Version)}, plus these  changes:
     *            <ul>
     *              <li>2.3.22 (or higher): The default value of
     *                  {@link #setUseAdaptersForContainers(boolean) useAdaptersForContainers} changes to
     *                  {@code true}.</li>
     *              <li>2.3.24 (or higher): When wrapping an {@link Iterator}, operations on it that only check if the
     *                  collection is empty without reading an element from it, such as {@code ?has_content},
     *                  won't cause the a later iteration (or further emptiness check) to fail anymore. Earlier, in
     *                  certain situations, the second operation has failed saying that the iterator "can be listed only
     *                  once".  
     *              <li>2.3.26 (or higher): {@link Enumeration}-s are wrapped into {@link DefaultEnumerationAdapter}
     *                  instead of into {@link EnumerationModel} (as far as
     *                  {@link #setUseAdaptersForContainers(boolean) useAdaptersForContainers} is {@code true}, which is
     *                  the default). This adapter is cleaner than {@link EnumerationModel} as it only implements the
     *                  minimally required FTL type, which avoids some ambiguous situations. (Note that Java API methods
     *                  aren't exposed anymore as subvariables; if you really need them, you can use {@code ?api}). 
     *                  </li>
     *            </ul>
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
        useAdaptersForContainers = dowDowCfg.getUseAdaptersForContainers();
        useAdapterForEnumerations = useAdaptersForContainers
                && getIncompatibleImprovements().intValue() >= _VersionInts.V_2_3_26;
        forceLegacyNonListCollections = dowDowCfg.getForceLegacyNonListCollections();
        iterableSupport = dowDowCfg.getIterableSupport();
        domNodeSupport = dowDowCfg.getDOMNodeSupport();
        jythonSupport = dowDowCfg.getJythonSupport();
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
    
    static {
        Class<?> cl;
        ObjectWrapper ow;
        try {
            cl = Class.forName("org.python.core.PyObject");
            ow = (ObjectWrapper) Class.forName(
                    "freemarker.ext.jython.JythonWrapper")
                    .getField("INSTANCE").get(null);
        } catch (Throwable e) {
            cl = null;
            ow = null;
            if (!(e instanceof ClassNotFoundException)) {
                try {
                    Logger.getLogger("freemarker.template.DefaultObjectWrapper")
                            .error("Failed to init Jython support, so it was disabled.", e);
                } catch (Throwable e2) {
                    // ignore
                }
            }
        }
        JYTHON_OBJ_CLASS = cl;
        JYTHON_WRAPPER = ow;
    }

    /**
     * Wraps the parameter object to {@link TemplateModel} interface(s). Simple types like numbers, strings, booleans
     * and dates will be wrapped into the corresponding {@code SimpleXxx} classes (like {@link SimpleNumber}).
     * {@link Map}-s, {@link List}-s, other {@link Collection}-s, arrays and {@link Iterator}-s will be wrapped into the
     * corresponding {@code SimpleXxx} or {@code DefaultXxxAdapter} classes (like {@link SimpleHash} or
     * {@link DefaultMapAdapter}), depending on {@link #getUseAdaptersForContainers()} and
     * {@link #getForceLegacyNonListCollections()}. After that, the wrapping is handled by
     * {@link #handleUnknownType(Object)}, so see more there.
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
            if (useAdaptersForContainers) {
                return DefaultArrayAdapter.adapt(obj, this);
            } else {
                obj = convertArray(obj);
                // Falls through (a strange legacy...)
            }
        }
        if (obj instanceof Collection) {
            if (useAdaptersForContainers) {
                if (obj instanceof List) {
                    return DefaultListAdapter.adapt((List<?>) obj, this);
                } else {
                    return forceLegacyNonListCollections
                            ? new SimpleSequence((Collection<?>) obj, this)
                            : DefaultNonListCollectionAdapter.adapt((Collection<?>) obj, this);
                }
            } else {
                return new SimpleSequence((Collection<?>) obj, this);
            }
        }
        if (obj instanceof Map) {
            return useAdaptersForContainers
                    ? DefaultMapAdapter.adapt((Map<?, ?>) obj, this)
                    : new SimpleHash((Map<?, ?>) obj, this);
        }
        if (obj instanceof Boolean) {
            return obj.equals(Boolean.TRUE) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
        if (obj instanceof Iterator) {
            return useAdaptersForContainers
                    ? DefaultIteratorAdapter.adapt((Iterator<?>) obj, this)
                    : new SimpleCollection((Iterator<?>) obj, this);
        }
        if (useAdapterForEnumerations && obj instanceof Enumeration) {
            return DefaultEnumerationAdapter.adapt((Enumeration<?>) obj, this);
        }        
        if (iterableSupport && obj instanceof Iterable) {
            return DefaultIterableAdapter.adapt((Iterable<?>) obj, this);
        }
        
        return handleUnknownType(obj);
    }
    
    /**
     * Called for an object that isn't considered to be of a "basic" Java type, like for an application specific type,
     * or for a W3C DOM node. In its default implementation, W3C {@link Node}-s will be wrapped as {@link NodeModel}-s
     * (allows DOM tree traversal), Jython objects will be delegated to the {@code JythonWrapper}, others will be
     * wrapped using {@link BeansWrapper#wrap(Object)}. However, these can be turned off with the
     * {@link #setDOMNodeSupport(boolean)} and {@link #setJythonSupport(boolean)}. Note that if
     * {@link #getMemberAccessPolicy()} doesn't return a {@link DefaultMemberAccessPolicy} or
     * {@link LegacyDefaultMemberAccessPolicy}, then Jython wrapper will be skipped for security reasons.
     * 
     * <p>
     * When you override this method, you should first decide if you want to wrap the object in a custom way (and if so
     * then do it and return with the result), and if not, then you should call the super method (assuming the default
     * behavior is fine with you).
     */
    protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
        if (domNodeSupport && obj instanceof Node) {
            return wrapDomNode(obj);
        }

        if (jythonSupport) {
            MemberAccessPolicy memberAccessPolicy = getMemberAccessPolicy();
            if (memberAccessPolicy instanceof DefaultMemberAccessPolicy
                    || memberAccessPolicy instanceof LegacyDefaultMemberAccessPolicy) {
                if (JYTHON_WRAPPER != null && JYTHON_OBJ_CLASS.isInstance(obj)) {
                    return JYTHON_WRAPPER.wrap(obj);
                }
            }
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
     * The getter pair of {@link #setUseAdaptersForContainers(boolean)}.
     * 
     * @since 2.3.22
     */
    public boolean getUseAdaptersForContainers() {
        return useAdaptersForContainers;
    }

    /**
     * Sets if to wrap container objects ({@link Map}-s, {@link List}-s, arrays and such) the legacy copying approach or
     * the newer adapter approach should be used. {@code true} is recommended, which is also the default when the
     * {@code incompatible_improvements} of this instance was set to {@link Configuration#VERSION_2_3_22} or higher. To
     * understand the difference, check some of the classes that implement the two approaches:
     * <ul>
     * <li>Copying approach: {@link SimpleHash}, {@link SimpleSequence}</li>
     * <li>Adapter approach: {@link DefaultMapAdapter}, {@link DefaultListAdapter}, {@link DefaultArrayAdapter},
     * {@link DefaultIteratorAdapter}</li>
     * </ul>
     * 
     * <p>
     * See also the related Version History entry under 2.3.22 in the FreeMarker Manual, which gives a breakdown of
     * the consequences.
     * 
     * <p>
     * <b>Attention:</b> For backward compatibility, currently, non-{@link List} collections (like {@link Set}-s) will
     * only be wrapped with adapter approach (with {@link DefaultNonListCollectionAdapter}) if
     * {@link #setForceLegacyNonListCollections(boolean) forceLegacyNonListCollections} was set to {@code false}.
     * Currently the default is {@code true}, but in new projects you should set it to {@code false}. See
     * {@link #setForceLegacyNonListCollections(boolean)} for more.
     * 
     * @see #setForceLegacyNonListCollections(boolean)
     * 
     * @since 2.3.22
     */
    public void setUseAdaptersForContainers(boolean useAdaptersForContainers) {
        checkModifiable();
        this.useAdaptersForContainers = useAdaptersForContainers;
    }
    
    /**
     * Getter pair of {@link #setForceLegacyNonListCollections(boolean)}; see there.
     * 
     * @since 2.3.22
     */
    public boolean getForceLegacyNonListCollections() {
        return forceLegacyNonListCollections;
    }

    /**
     * Specifies whether non-{@link List} {@link Collection}-s (like {@link Set}-s) must be wrapped by pre-fetching into
     * a {@link SimpleSequence}. The modern approach is wrapping into a {@link DefaultNonListCollectionAdapter}. This
     * setting only has effect when {@link #getUseAdaptersForContainers()} is also {@code true}, as otherwise
     * {@link SimpleSequence} will be used regardless of this. In new projects you should set this to {@code false}. At
     * least before {@code incompatible_improvements} 2.4.0 it defaults to {@code true}, because of backward
     * compatibility concerns: with {@link TemplateSequenceModel} templates could access the items by index if they
     * wanted to (the index values were defined by the iteration order). This was not very useful, or was even
     * confusing, and it conflicts with the adapter approach.
     * 
     * @see #setUseAdaptersForContainers(boolean)
     * 
     * @since 2.3.22
     */
    public void setForceLegacyNonListCollections(boolean forceLegacyNonListCollections) {
        checkModifiable();
        this.forceLegacyNonListCollections = forceLegacyNonListCollections;
    }

    /**
     * Getter pair of {@link #setIterableSupport(boolean)}; see there.
     * 
     * @since 2.3.25
     */
    public boolean getIterableSupport() {
        return iterableSupport;
    }

    /**
     * Specifies whether {@link Iterable}-s (not to be confused with {@link Iterator}-s) that don't implement any other
     * recognized Java interfaces (most notably {@link Collection}) will be recognized as listable objects
     * ({@link TemplateCollectionModel}-s), or they will be just seen as generic objects (JavaBean-s). Defaults to
     * {@code false} for backward compatibility, but in new projects you should set this to {@code true}. Before setting
     * this to {@code true} in older projects, check if you have called {@code myIterable.iterator()} directly from any
     * templates, because the Java API is only exposed to the templates if the {@link Iterable} is wrapped as generic
     * object.
     * 
     * @since 2.3.25
     */
    public void setIterableSupport(boolean iterableSupport) {
        checkModifiable();
        this.iterableSupport = iterableSupport;
    }

    /**
     * Getter pair of {@link #setDOMNodeSupport(boolean)}; see there.
     *
     * @since 2.3.31
     */
    public final boolean getDOMNodeSupport() {
        return domNodeSupport;
    }

    /**
     * Enables wrapping {@link Node}-s on a special way (as described in the "XML Processing Guide" in the Manual);
     * defaults to {@code true}.. If this is {@code true}, {@link Node}+s will be wrapped like any other generic object.
     *
     * @see #handleUnknownType(Object)
     *
     * @since 2.3.31
     */
    public void setDOMNodeSupport(boolean domNodeSupport) {
        checkModifiable();
        this.domNodeSupport = domNodeSupport;
    }

    /**
     * Getter pair of {@link #setJythonSupport(boolean)}; see there.
     *
     * @since 2.3.31
     */
    public final boolean getJythonSupport() {
        return jythonSupport;
    }

    /**
     * Enables wrapping Jython objects in a special way; defaults to {@code true}. If this is {@code false}, they will
     * be wrapped like any other generic object. Note that Jython wrapping is legacy feature, and might by disabled by
     * the selected {@link MemberAccessPolicy}, even if this is {@code true}; see {@link #handleUnknownType(Object)}.
     *
     * @see #handleUnknownType(Object)
     *
     * @since 2.3.31
     */
    public void setJythonSupport(boolean jythonSupport) {
        checkModifiable();
        this.jythonSupport = jythonSupport;
    }

    /**
     * Returns the lowest version number that is equivalent with the parameter version.
     * 
     * @since 2.3.22
     */
    protected static Version normalizeIncompatibleImprovementsVersion(Version incompatibleImprovements) {
        _TemplateAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        Version bwIcI = BeansWrapper.normalizeIncompatibleImprovementsVersion(incompatibleImprovements);
        return incompatibleImprovements.intValue() < _VersionInts.V_2_3_22
                || bwIcI.intValue() >= _VersionInts.V_2_3_22
                ? bwIcI : Configuration.VERSION_2_3_22;
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
        
        return "useAdaptersForContainers=" + useAdaptersForContainers
                + ", forceLegacyNonListCollections=" + forceLegacyNonListCollections
                + ", iterableSupport=" + iterableSupport
                + ", domNodeSupport=" + domNodeSupport
                + ", jythonSupport=" + jythonSupport
                + bwProps;
    }
    
}
