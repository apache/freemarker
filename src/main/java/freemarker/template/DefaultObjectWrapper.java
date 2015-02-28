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
 * The default implementation of the {@link ObjectWrapper} interface. Usually, you don't need to create instances of
 * this, as an instance of this is already the default value of the
 * {@link Configuration#setObjectWrapper(ObjectWrapper) object_wrapper setting}. Then the
 * {@link #DefaultObjectWrapper(Version) incompatibleImprovements} of the {@link DefaultObjectWrapper} will be the same
 * that you have set for the {@link Configuration} itself. As of this writing, it's highly recommended to use
 * {@link Configuration#Configuration(Version) incompatibleImprovements} 2.3.22 (or higher).
 * 
 * <p>
 * If you still need to create an instance, that should be done with an {@link DefaultObjectWrapperBuilder}, not with
 * its constructor, as that allows FreeMarker to reuse singletons. For new projects, it's recommended to set
 * {@link DefaultObjectWrapperBuilder#setForceLegacyNonListCollections(boolean) forceLegacyNonListCollections} to
 * {@code false} - something that setting {@code incompatibleImprovements} to 2.3.22 won't do.
 * 
 * <p>
 * This class is only thread-safe after you have finished calling its setter methods, and then safely published it (see
 * JSR 133 and related literature). When used as part of {@link Configuration}, of course it's enough if that was safely
 * published and then left unmodified.
 */
public class DefaultObjectWrapper extends freemarker.ext.beans.BeansWrapper {
    
    /** @deprecated Use {@link DefaultObjectWrapperBuilder} instead, but mind its performance */
    static final DefaultObjectWrapper instance = new DefaultObjectWrapper();
    
    static final private Class JYTHON_OBJ_CLASS;
    
    static final private ObjectWrapper JYTHON_WRAPPER;
    
    private boolean useAdaptersForContainers;
    private boolean forceLegacyNonListCollections;
    
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
        forceLegacyNonListCollections = dowDowCfg.getForceLegacyNonListCollections();
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

    /**
     * Wraps the parameter object to {@link TemplateModel} interface(s). Simple types like numbers, strings, booleans
     * and dates will be wrapped into the corresponding {@code SimpleXxx} classes (like {@link SimpleNumber}).
     * {@link Map}-s, {@link List}-s, other {@link Collection}-s, arrays and {@link Iterator}-s will be wrapped into the
     * corresponding {@code SimpleXxx} or {@code DefaultXxxAdapter} classes (like {@link SimpleHash} or
     * {@link DefaultMapAdapter}), depending on {@link #getUseAdaptersForContainers()} and
     * {@link #getForceLegacyNonListCollections()}. After that, the wrapping is handled by
     * {@link #handleUnknownType(Object)}, so see more there.
     */
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
                return DefaultArrayAdapter.adapt(obj, this);
            } else {
                obj = convertArray(obj);
                // Falls through (a strange legacy...)
            }
        }
        if (obj instanceof Collection) {
            if (useAdaptersForContainers) {
                if (obj instanceof List) {
                    return DefaultListAdapter.adapt((List) obj, this);
                } else {
                    return forceLegacyNonListCollections
                            ? (TemplateModel) new SimpleSequence((Collection) obj, this)
                            : (TemplateModel) DefaultNonListCollectionAdapter.adapt((Collection) obj, this);
                }
            } else {
                return new SimpleSequence((Collection) obj, this);
            }
        }
        if (obj instanceof Map) {
            return useAdaptersForContainers
                    ? (TemplateModel) DefaultMapAdapter.adapt((Map) obj, this)
                    : (TemplateModel) new SimpleHash((Map) obj, this);
        }
        if (obj instanceof Boolean) {
            return obj.equals(Boolean.TRUE) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
        if (obj instanceof Iterator) {
            return useAdaptersForContainers
                    ? (TemplateModel) DefaultIteratorAdapter.adapt((Iterator) obj, this)
                    : (TemplateModel) new SimpleCollection((Iterator) obj, this);
        }
        return handleUnknownType(obj);
    }
    
    /**
     * Called for an object that aren't considered to be of a "basic" Java type, like for an application specific type,
     * or for a W3C DOM node. In its default implementation, W3C {@link Node}-s will be wrapped as {@link NodeModel}-s
     * (allows DOM tree traversal), Jython objects will be delegated to the {@code JythonWrapper}, others will be
     * wrapped using {@link BeansWrapper#wrap(Object)}.
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
     * Returns the lowest version number that is equivalent with the parameter version.
     * 
     * @since 2.3.22
     */
    protected static Version normalizeIncompatibleImprovementsVersion(Version incompatibleImprovements) {
        _TemplateAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        Version bwIcI = BeansWrapper.normalizeIncompatibleImprovementsVersion(incompatibleImprovements);
        return incompatibleImprovements.intValue() < _TemplateAPI.VERSION_INT_2_3_22
                || bwIcI.intValue() >= _TemplateAPI.VERSION_INT_2_3_22
                ? bwIcI : Configuration.VERSION_2_3_22;
    }

    /**
     * @since 2.3.22
     */
    protected String toPropertiesString() {
        String bwProps = super.toPropertiesString();
        
        // Remove simpleMapWrapper, as its irrelevant for this wrapper:
        if (bwProps.startsWith("simpleMapWrapper")) {
            int smwEnd = bwProps.indexOf(',');
            if (smwEnd != -1) {
                bwProps = bwProps.substring(smwEnd + 1).trim();
            }
        }
        
        return "useAdaptersForContainers=" + useAdaptersForContainers + ", forceLegacyNonListCollections="
                + forceLegacyNonListCollections + ", " + bwProps;
    }
    
}
