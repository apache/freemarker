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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperConfiguration;
import freemarker.ext.dom.NodeModel;
import freemarker.log.Logger;

/**
 * The default implementation of the {@link ObjectWrapper} interface. Note that instances of this class generally should
 * be made by {@link DefaultObjectWrapperBuilder} and its overloads, not with its constructor.
 * 
 * <p>This class is only thread-safe after you have finished calling its setter methods, and then safely published
 * it (see JSR 133 and related literature). When used as part of {@link Configuration}, of course it's enough if that
 * was safely published and then left unmodified. 
 */
public class DefaultObjectWrapper extends freemarker.ext.beans.BeansWrapper {
    
    /** @deprecated Use {@link DefaultObjectWrapperBuilder} instead, but mind its performance */
    static final DefaultObjectWrapper instance = new DefaultObjectWrapper();
    
    static final private Class JYTHON_OBJ_CLASS;
    
    static final private ObjectWrapper JYTHON_WRAPPER;
    
    private boolean useAdaptersForContainers;
    private boolean useAdaptersForNonListCollections;
    
    /**
     * Creates a new instance with the incompatible-improvements-version specified in
     * {@link Configuration#DEFAULT_INCOMPATIBLE_IMPROVEMENTS}.
     * 
     * @deprecated Use {@link DefaultObjectWrapperBuilder}, or in rare cases,
     *          {@link #DefaultObjectWrapper(Version)} instead.
     */
    public DefaultObjectWrapper() {
        this(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    }
    
    /**
     * Use {@link DefaultObjectWrapperBuilder} instead if possible.
     * Instances created with this constructor won't share the class introspection caches with other instances.
     * See {@link BeansWrapper#BeansWrapper(Version)} (the superclass constructor) for more details.
     * 
     * @param incompatibleImprovements As of yet, the same as in {@link BeansWrapper#BeansWrapper(Version)}.
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
    protected DefaultObjectWrapper(BeansWrapperConfiguration bwCfg, boolean readOnly) {
        super(bwCfg, readOnly, false);
        DefaultObjectWrapperConfiguration dowDowCfg = bwCfg instanceof DefaultObjectWrapperConfiguration
                ? (DefaultObjectWrapperConfiguration) bwCfg
                : new DefaultObjectWrapperConfiguration(bwCfg.getIncompatibleImprovements()) { }; 
        useAdaptersForContainers = dowDowCfg.getUseAdaptersForContainers();
        useAdaptersForNonListCollections = dowDowCfg.getUseAdaptersForNonListCollections();
        finalizeConstruction(readOnly);
    }

    /**
     * Calls {@link BeansWrapper#BeansWrapper(BeansWrapperConfiguration, boolean)} and sets up
     * {@link DefaultObjectWrapper}-specific fields.
     * 
     * @since 2.3.22
     */
    protected DefaultObjectWrapper(DefaultObjectWrapperConfiguration dowCfg, boolean readOnly) {
        this((BeansWrapperConfiguration) dowCfg, readOnly);
    }
    
    static {
        Class cl;
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
            if(obj instanceof java.sql.Date) {
                return new SimpleDate((java.sql.Date) obj);
            }
            if(obj instanceof java.sql.Time) {
                return new SimpleDate((java.sql.Time) obj);
            }
            if(obj instanceof java.sql.Timestamp) {
                return new SimpleDate((java.sql.Timestamp) obj);
            }
            return new SimpleDate((java.util.Date) obj, getDefaultDateType());
        }
        final Class objClass = obj.getClass();
        if (objClass.isArray()) {
            if (useAdaptersForContainers) {
                return SimpleArrayAdapter.adapt(obj, this);
            } else {
                obj = convertArray(obj);
                // Falls through (a strange legacy...)
            }
        }
        if (obj instanceof Collection) {
            if (useAdaptersForContainers && obj instanceof List) {
                return SimpleListAdapter.adapt((List) obj, this);
            } else {
                return useAdaptersForNonListCollections && !(obj instanceof List)
                        ? (TemplateModel) SimpleNonListCollectionAdapter.adapt((Collection) obj, this)
                        : (TemplateModel) new SimpleSequence((Collection) obj, this);
            }
        }
        if (obj instanceof Map) {
            return useAdaptersForContainers
                    ? (TemplateModel) SimpleMapAdapter.adapt((Map) obj, this)
                    : (TemplateModel) new SimpleHash((Map) obj, this);
        }
        if (obj instanceof Boolean) {
            return obj.equals(Boolean.TRUE) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
        if (obj instanceof Iterator) {
            return new SimpleCollection((Iterator) obj, this);
        }
        return handleUnknownType(obj);
    }
    
    
    /**
     * Called if an unknown type is passed in.
     * Since 2.3, this falls back on XML wrapper and BeansWrapper functionality.
     */
    protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
        if (obj instanceof Node) {
            return wrapDomNode(obj);
        }
        if (JYTHON_WRAPPER != null  && JYTHON_OBJ_CLASS.isInstance(obj)) {
            return JYTHON_WRAPPER.wrap(obj);
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
        final int size = Array.getLength(arr);
        ArrayList list = new ArrayList(size);
        for (int i=0;i<size; i++) {
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
     * Sets if to wrap container objects ({@link Map}-s, {@link List}-s, arrays and such) the copying approach or the
     * adapter approach should be used. {@code true} is recommended, which is also the default when the
     * {@code incompatible_improvements} of this instance was set to {@link Configuration#VERSION_2_3_22} or higher. To
     * understand the difference, check some of the classes that implement the two approaches
     * <ul>
     * <li>Copying approach: {@link SimpleHash}, {@link SimpleSequence}</li>
     * <li>Adapter approach: {@link SimpleMapAdapter}, {@link SimpleListAdapter}, {@link SimpleArrayAdapter}</li>
     * </ul>
     * 
     * <p>
     * <b>Attention:</b> For backward compatibility, currently, non-{@link List} collections (like {@link Set}-s) will
     * only be wrapped with adapter approach (with {@link SimpleNonListCollectionAdapter}) if
     * {@link #setUseAdaptersForNonListCollections(boolean) useAdaptersForNonListCollections} was set to {@code true}.
     * Currently the default is {@code false}, but in new projects you should set it to {@code true}.
     * See {@link #setUseAdaptersForNonListCollections(boolean)} for more.
     * 
     * @see #setUseAdaptersForNonListCollections(boolean)
     * 
     * @since 2.3.22
     */
    public void setUseAdaptersForContainers(boolean useAdaptersForContainers) {
        checkModifiable();
        this.useAdaptersForContainers = useAdaptersForContainers;
    }
    
    /**
     * Getter pair of {@link #setUseAdaptersForNonListCollections(boolean)}; see there.
     * 
     * @since 2.3.22
     */
    public boolean getUseAdaptersForNonListCollections() {
        return useAdaptersForNonListCollections;
    }

    /**
     * Specifies if non-{@link List} {@link Collections} (like {@link Set}-s) will be wrapped with with the legacy
     * {@link SimpleSequence}, or with the more modern {@link SimpleNonListCollectionAdapter}. This meant to be used when
     * {@link #getUseAdaptersForContainers()} is also {@code true}. In new projects you should definitely set this to
     * {@code true}. At least before {@code incompatible_improvements} 2.4.0 it defaults to {@code false}, because of
     * backward compatibility concerns. Namely, in earlier versions even non-{@link List} {@link Collection}-s were
     * wrapped into a {@link TemplateSequenceModel}, which means that the templates could access the items by index if
     * they wanted to (the index values were defined by the iteration order). This was not very useful, or was even
     * confusing, and conflicts with the adapter approach.
     * 
     * @see #setUseAdaptersForContainers(boolean)
     * 
     * @since 2.3.22
     */
    public void setUseAdaptersForNonListCollections(boolean legacyNonListCollectionWrapping) {
        checkModifiable();
        this.useAdaptersForNonListCollections = legacyNonListCollectionWrapping;
    }

    /** 
     * Returns the lowest version number that is equivalent with the parameter version.
     * @since 2.3.22
     */
    protected static Version normalizeIncompatibleImprovementsVersion(Version incompatibleImprovements) {
        _TemplateAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        Version bwIcI = BeansWrapper.normalizeIncompatibleImprovementsVersion(incompatibleImprovements);
        return incompatibleImprovements.intValue() < _TemplateAPI.VERSION_INT_2_3_22
                ? bwIcI : Configuration.VERSION_2_3_22;
    }    
    
}
