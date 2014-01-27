/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.ext.beans;

import java.beans.PropertyDescriptor;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.WeakHashMap;

import freemarker.core.BugException;
import freemarker.ext.util.IdentityHashMap;
import freemarker.ext.util.ModelCache;
import freemarker.ext.util.ModelFactory;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.log.Logger;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleObjectWrapper;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.NullArgumentException;
import freemarker.template.utility.UndeclaredThrowableException;
import freemarker.template.utility.WriteProtectable;

/**
 * {@link ObjectWrapper} that is able to expose the Java API of arbitrary Java objects. This is also the superclass of
 * {@link DefaultObjectWrapper}. Note that instances of this class generally should be made by
 * {@link #getInstance(Version)} and its overloads, not with its constructor.
 * 
 * <p>This class is only thread-safe after you have finished calling its setter methods, and then safely published
 * it (see JSR 133 and related literature). When used as part of {@link Configuration}, of course it's enough if that
 * was safely published and then left unmodified. 
 */
public class BeansWrapper implements ObjectWrapper, WriteProtectable
{
    private static final Logger LOG = Logger.getLogger("freemarker.beans");

    static final Object CAN_NOT_UNWRAP = new Object();
    private static final Class BIGINTEGER_CLASS = java.math.BigInteger.class;
    private static final Class BOOLEAN_CLASS = Boolean.class;
    private static final Class CHARACTER_CLASS = Character.class;
    private static final Class COLLECTION_CLASS = Collection.class;
    private static final Class DATE_CLASS = Date.class;
    private static final Class HASHADAPTER_CLASS = HashAdapter.class;
    private static final Class ITERABLE_CLASS;
    private static final Class LIST_CLASS = List.class;
    private static final Class MAP_CLASS = Map.class;
    private static final Class OBJECT_CLASS = Object.class;
    private static final Class SEQUENCEADAPTER_CLASS = SequenceAdapter.class;
    private static final Class SET_CLASS = Set.class;
    private static final Class SETADAPTER_CLASS = SetAdapter.class;
    private static final Class STRING_CLASS = String.class;
    static {
        Class iterable;
        try {
            iterable = Class.forName("java.lang.Iterable");
        }
        catch(ClassNotFoundException e) {
            // We're running on a pre-1.5 JRE
            iterable = null;
        }
        ITERABLE_CLASS = iterable;
    }
    
    private static final Constructor ENUMS_MODEL_CTOR = enumsModelCtor();
    
    /**
     * At this level of exposure, all methods and properties of the
     * wrapped objects are exposed to the template.
     */
    public static final int EXPOSE_ALL = 0;
    
    /**
     * At this level of exposure, all methods and properties of the wrapped
     * objects are exposed to the template except methods that are deemed
     * not safe. The not safe methods are java.lang.Object methods wait() and
     * notify(), java.lang.Class methods getClassLoader() and newInstance(),
     * java.lang.reflect.Method and java.lang.reflect.Constructor invoke() and
     * newInstance() methods, all java.lang.reflect.Field set methods, all 
     * java.lang.Thread and java.lang.ThreadGroup methods that can change its 
     * state, as well as the usual suspects in java.lang.System and
     * java.lang.Runtime.
     */
    public static final int EXPOSE_SAFE = 1;
    
    /**
     * At this level of exposure, only property getters are exposed.
     * Additionally, property getters that map to unsafe methods are not
     * exposed (i.e. Class.classLoader and Thread.contextClassLoader).
     */
    public static final int EXPOSE_PROPERTIES_ONLY = 2;

    /**
     * At this level of exposure, no bean properties and methods are exposed.
     * Only map items, resource bundle items, and objects retrieved through
     * the generic get method (on objects of classes that have a generic get
     * method) can be retrieved through the hash interface. You might want to 
     * call {@link #setMethodsShadowItems(boolean)} with <tt>false</tt> value to
     * speed up map item retrieval.
     */
    public static final int EXPOSE_NOTHING = 3;

    // -----------------------------------------------------------------------------------------------------------------
    // Instance cache:
    
    /**
     * @deprecated Don't use this outside FreeMarker; will be soon removed.
     */
    protected final Object _preJava5Sync = _BeansAPI.JVM_USES_JSR133 ? null : new Object(); 
    
    private final static WeakHashMap/*<ClassLoader, Map<PropertyAssignments, WeakReference<BeansWrapper>>*/
            INSTANCE_CACHE = new WeakHashMap();
    private final static ReferenceQueue INSTANCE_CACHE_REF_QUEUE = new ReferenceQueue();
    
    // -----------------------------------------------------------------------------------------------------------------
    // Introspection cache:
    
    private final Object sharedInrospectionLock;
    
    /** 
     * {@link Class} to class info cache.
     * This object is possibly shared with other {@link BeansWrapper}-s!
     * 
     * <p>To write this, always use {@link #setClassIntrospector(ClassIntrospector.PropertyAssignments)}.
     * 
     * <p>When reading this, it's good idea to synchronize on sharedInrospectionLock when it doesn't hurt overall
     * performance. In theory that's not needed, but apps might fail to keep the rules.
     */
    private ClassIntrospector classIntrospector;
    
    /**
     * {@link String} class name to {@link StaticModel} cache.
     * This object only belongs to a single {@link BeansWrapper}.
     * This has to be final as {@link #getStaticModels()} might returns it any time and then it has to remain a good
     * reference.
     */
    private final StaticModels staticModels;
    
    /**
     * {@link String} class name to {@link EnumerationModel} cache.
     * This object only belongs to a single {@link BeansWrapper}.
     * This has to be final as {@link #getStaticModels()} might returns it any time and then it has to remain a good
     * reference.
     */
    private final ClassBasedModelFactory enumModels;
    
    /**
     * Object to wrapped object cache; not used by default.
     * This object only belongs to a single {@link BeansWrapper}.
     */
    private final ModelCache modelCache;

    private final BooleanModel falseModel;
    private final BooleanModel trueModel;
    
    // -----------------------------------------------------------------------------------------------------------------

    // Why volatile: In principle it need not be volatile, but we want to catch modification attempts even if the
    // object was published improperly to other threads. After all, the main goal of WriteProtectable is protecting
    // things from buggy user code.
    private volatile boolean readOnly;
    
    private TemplateModel nullModel = null;
    private int defaultDateType; // initialized by PropertyAssignments.apply
    private ObjectWrapper outerIdentity = this;
    private boolean methodsShadowItems = true;
    private boolean simpleMapWrapper;  // initialized by PropertyAssignments.apply
    private boolean strict;  // initialized by PropertyAssignments.apply
    
    private final Version incompatibleImprovements;
    
    /**
     * Creates a new instance with the incompatible-improvements-version specified in
     * {@link Configuration#DEFAULT_INCOMPATIBLE_IMPROVEMENTS}.
     * 
     * @deprecated Use {@link #getInstance(Version)}, {@link #getInstance(Version, boolean)} or
     *     {@link #getInstance(PropertyAssignments)}, or in rare cases {@link #BeansWrapper(Version)} instead.
     */
    public BeansWrapper() {
        this(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        // Attention! Don't don anything here, as the instance is possibly already visible to other threads.  
    }
    
    /**
     * Use {@link #getInstance(Version)} or {@link #getInstance(Version, boolean)} or
     * {@link #getInstance(PropertyAssignments)} instead if possible.
     * Instances created with this constructor won't share the class introspection caches with other instances. That's
     * also why you may want to use it instead of {@code getInstance} (you don't want to use common caches).
     * 
     * @param incompatibleImprovements
     *   Sets which of the non-backward-compatible improvements should be enabled. Not {@code null}. This version number
     *   is the same as the FreeMarker version number with which the improvements were implemented.
     *    
     *   <p>For new projects, it's recommended to set this to the FreeMarker version that's used during the development.
     *   For released products that are still actively developed it's a low risk change to increase the 3rd
     *   version number further as FreeMarker is updated, but of course you should always check the list of effects
     *   below. Increasing the 2nd or 1st version number can mean substantial changes with higher risk of breaking
     *   the application.
     *   
     *   <p>The reason it's separate from {@link Configuration#setIncompatibleImprovements(Version)} is that
     *   {@link ObjectWrapper} objects are often shared among multiple {@link Configuration}-s, so the two version
     *   numbers are technically independent. But it's recommended to keep those two version numbers the same.
     * 
     *   <p>The changes enabled by {@code incompatibleImprovements} are:
     *   <ul>
     *     <li>
     *       <p>2.3.0: No changes; this is the starting point, the version used in older projects.
     *     </li>
     *     <li>
     *       <p>2.3.21 (or higher):
     *       Several glitches were fixed in overloaded method selection. This usually just gets
     *       rid of errors (like ambiguity exceptions and numerical precision loses due to bad overloaded method
     *       choices), still, as in some cases the method chosen can be a different one now (that was the point of
     *       the reworking after all), it can mean a change in the behavior of the application. The most important
     *       change is that the treatment of {@code null} arguments were fixed, as earlier they were only seen
     *       applicable to parameters of type {@code Object}. Now {@code null}-s are seen to be applicable to any
     *       non-primitive parameters, and among those the one with the most specific type will be preferred (just
     *       like in Java), which is hence never the one with the {@code Object} parameter type. For more details
     *       about overloaded method selection changes see the version History in the FreeMarker Manual.
     *     </li>
     *   </ul>
     *
     * @since 2.3.21
     */
    public BeansWrapper(Version incompatibleImprovements) {
        this(new PropertyAssignments(incompatibleImprovements), false);
        // Attention! Don't don anything here, as the instance is possibly already visible to other threads.  
    }
    
    private static volatile boolean ftmaDeprecationWarnLogged;
    
    /**
     * @param readOnly makes the instance read-only via {@link WriteProtectable#writeProtect()}; this way it can use the shared
     *     introspection cache.
     * 
     * @since 2.3.21
     */
    protected BeansWrapper(PropertyAssignments pa, boolean readOnly) {
        // Backward-compatibility hack for "finetuneMethodAppearance" overrides to work:
        if (pa.getMethodAppearanceFineTuner() == null) {
            Class thisClass = this.getClass();
            boolean overridden = false;
            boolean testFailed = false;
            try {
                while (!overridden
                        && thisClass != DefaultObjectWrapper.class
                        && thisClass != BeansWrapper.class
                        && thisClass != SimpleObjectWrapper.class) {
                    try {
                        thisClass.getDeclaredMethod("finetuneMethodAppearance",
                                new Class[] { Class.class, Method.class, MethodAppearanceDecision.class });
                        overridden = true;
                    } catch (NoSuchMethodException e) {
                        thisClass = thisClass.getSuperclass();
                    }
                }
            } catch (Throwable e) {
                // The security manager sometimes doesn't allow this
                LOG.info("Failed to check if finetuneMethodAppearance is overidden in " + thisClass.getName()
                        + "; acting like if it was, but this way it won't utilize the shared class introspection "
                        + "cache.",
                        e);
                overridden = true;
                testFailed = true;
            }
            if (overridden) {
                if (!testFailed && !ftmaDeprecationWarnLogged) {
                    LOG.warn("Overriding BeansWrapper.finetuneMethodAppearance is deprecated and will be banned in the "
                            + "future. Use BeansWrapper.setMethodAppearanceFineTuner instead.");
                    ftmaDeprecationWarnLogged = true;
                }
                pa = (PropertyAssignments) pa.clone(false);
                pa.setMethodAppearanceFineTuner(new MethodAppearanceFineTuner() {

                    public void process(
                            MethodAppearanceDecisionInput in, MethodAppearanceDecision out) {
                        BeansWrapper.this.finetuneMethodAppearance(in.getContainingClass(), in.getMethod(), out);
                    }
                    
                });
            }
        }
        
        this.incompatibleImprovements = pa.getIncompatibleImprovements();  // normalized
        
        simpleMapWrapper = pa.isSimpleMapWrapper();
        defaultDateType = pa.getDefaultDateType();
        outerIdentity = pa.getOuterIdentity() != null ? pa.getOuterIdentity() : this;
        strict = pa.isStrict();
        
        if (!readOnly) {
            // As this is not a read-only BeansWrapper, the classIntrospector will be possibly replaced for a few times,
            // but we need to use the same sharedInrospectionLock forever, because that's what the model factories
            // synchronize on, even during the classIntrospector is being replaced.
            sharedInrospectionLock = new Object();
            classIntrospector = new ClassIntrospector(pa.classIntrospectorPropertyAssignments, sharedInrospectionLock);
        } else {
            // As in this read-only BeansWrapper, the classIntrospector is never replaced, and since it's shared by
            // other BeansWrapper instances, we use the lock belonging to the shared ClassIntrospector.
            classIntrospector = ClassIntrospector.getInstance(pa.classIntrospectorPropertyAssignments);
            sharedInrospectionLock = classIntrospector.getSharedLock(); 
        }
        
        falseModel = new BooleanModel(Boolean.FALSE, this);
        trueModel = new BooleanModel(Boolean.TRUE, this);
        
        staticModels = new StaticModels(BeansWrapper.this);
        enumModels = createEnumModels(BeansWrapper.this);
        modelCache = new BeansModelCache(BeansWrapper.this);
        setUseCache(pa.useModelCache);

        if (readOnly) {
            writeProtect();
        }
        
        // Attention! At this point, the BeansWrapper must be fully initialized, as when the model factories are
        // registered below, the BeansWrapper can immediately get concurrent callbacks. That those other threads will
        // see consistent image of the BeansWrapper is ensured that callback are always sync-ed on
        // classIntrospector.sharedLock, and so is classIntrospector.registerModelFactory(...).
        
        registerModelFactories();
    }
    
    /**
     * Returns an unconfigurable (read-only) {@link BeansWrapper} instance that's already configured as specified in the
     * argument; this is preferred over using the constructors. The returned instance is often, but not always a
     * VM-wide (or rather, Web-Application-wide) singleton. Note that other overloads of this method allows you to
     * configure more properties.
     * 
     * <p>Note that what this method documentation says about {@link BeansWrapper} also applies to
     * {@link DefaultObjectWrapper}.
     * 
     * <p>The main benefit of using this method instead of the constructors is that this way the internal
     * object wrapping-related caches will be reused/shared when appropriate. As the caches are expensive to build and
     * take memory, if multiple independent components use FreeMarker in an application, this can mean some resource
     * savings.
     * 
     * <p>The object wrapping-related caches are:
     * <ul>
     *   <li><p>Class introspection cache: Stores information about classes that once had to be wrapped. The cache is
     *     stored in the static fields of certain FreeMarker classes. Thus, if you have two {@link BeansWrapper}
     *     instances, they might share the same class introspection cache. But if you have two
     *     {@code freemarker.jar}-s (typically, in two Web Application's {@code WEB-INF/lib} directories), those won't
     *     share their caches (as they don't share the same FreeMarker classes).
     *     Also, currently there's a separate cache for each permutation of the property values that influence class
     *     introspection: {@link PropertyAssignments#setExposeFields(boolean) expose_fields} and
     *     {@link PropertyAssignments#setExposureLevel(int) exposure_level}. So only {@link BeansWrapper} where those
     *     properties are the same may share class introspection caches among each other.
     *   </li>
     *   <li><p>Model caches: These are local to a {@link BeansWrapper}. {@code getInstance} returns the same
     *     {@link BeansWrapper} instance for equivalent properties (unless the existing instance was garbage collected
     *     and thus a new one had to be created), hence these caches will be re-used too. {@link BeansWrapper} instances
     *     are cached in the static fields of FreeMarker here too, but there's a separate cache for each
     *     Thread Context Class Loader, which in a servlet container practically means a separate cache for each Web
     *     Application (each servlet context). (This is like so because when resolving class names to classes FreeMarker
     *     will turn to the Thread Context Class Loader first, and only then to the defining class loader of FreeMarker,
     *     so the result of the resolution can be different for different Thread Context Class Loaders.) The caches
     *     local to a {@link BeansWrapper} are:
     *     <ul>
     *       <li><p>
     *         Static model caches: These are used by the hash returned by {@link #getEnumModels()} and
     *         {@link #getStaticModels()}, for caching {@link TemplateModel}-s for the static methods/fields
     *         and Java 5 enums that were accessed through them. To use said hashes, you have to put them
     *         explicitly into the data-model or expose them to template explicitly otherwise, so in most applications
     *         these cache are unused. 
     *       </li>
     *       <li><p>
     *         Instance model cache: By default off (see {@link #setUseCache(boolean)}). Caches the
     *         {@link TemplateModel}-s for all Java objects that were accessed from templates.
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * <p>Note that if you set {@link PropertyAssignments#setMethodAppearanceFineTuner(MethodAppearanceFineTuner)} to
     * non-{@code null}, you will always get a new instance, and the class introspection cache won't be shared.
     * 
     * @param incompatibleImprovements See the corresponding parameter of {@link BeansWrapper#BeansWrapper(Version)}.
     *     Not {@code null}.
     *     Note that the version will be normalized to the lowest version where the same incompatible
     *     {@link BeansWrapper} improvements were already present, so for the returned instance
     *     {@link #getIncompatibleImprovements()} might returns a lower version than what you have specified.
     * 
     * @since 2.3.21
     */
    public static BeansWrapper getInstance(Version incompatibleImprovements) {
        return getInstance(new PropertyAssignments(incompatibleImprovements));
    }
    
    /**
     * Same as {@link #getInstance(Version)}, but also specifies the {@code simpleMapWrapper} property of the desired
     * instance. Without this, that will be set to its default.
     *     
     * @param simpleMapsWrapper See {@link BeansWrapper#setSimpleMapWrapper(boolean)}.
     * 
     * @since 2.3.21
     */
    public static BeansWrapper getInstance(Version incompatibleImprovements, boolean simpleMapsWrapper) {
        PropertyAssignments pa = new PropertyAssignments(incompatibleImprovements);
        pa.setSimpleMapWrapper(simpleMapsWrapper);
        return getInstance(pa);
    }
    
    /**
     * Same as {@link #getInstance(Version)}, but you can specify more properties of the desired instance.
     *     
     * @param pa Stores what the values of the JavaBean properties of the returned instance will be. Not {@code null}.
     * 
     * @since 2.3.21
     */
    public static BeansWrapper getInstance(PropertyAssignments pa) {
        return _BeansAPI.getBeansWrapperSubclassInstance(
                pa, INSTANCE_CACHE, INSTANCE_CACHE_REF_QUEUE, BeansWrapperFactory.INSTANCE);
    }
    
    private static class BeansWrapperFactory implements _BeansAPI.BeansWrapperSubclassFactory {
        
        private static final BeansWrapperFactory INSTANCE = new BeansWrapperFactory(); 

        public BeansWrapper create(PropertyAssignments pa) {
            return new BeansWrapper(pa, true);
        }
        
    }

    /** For unit testing only */
    static void clearInstanceCache() {
        synchronized (INSTANCE_CACHE) {
            INSTANCE_CACHE.clear();
        }
    }

    /** For unit testing only */
    static Map getInstanceCache() {
        return INSTANCE_CACHE;
    }
    
    /**
     * Makes the JavaBean properties of this object read-only.
     * 
     * @since 2.3.21
     */
    public void writeProtect() {
        readOnly = true;
    }

    /**
     * @since 2.3.21
     */
    public boolean isWriteProtected() {
        return readOnly;
    }
    
    Object getSharedInrospectionLock() {
        return sharedInrospectionLock;
    }
    
    /**
     * If this object is already read-only according to {@link WriteProtectable}, throws {@link IllegalStateException},
     * otherwise does nothing.
     * 
     * @since 2.3.21
     */
    protected void checkModifiable() {
        if (readOnly) throw new IllegalStateException(
                "Can't modify the " + this.getClass().getName() + " object, as it was set to read-only.");
    }

    /**
     * @see #setStrict(boolean)
     */
    public boolean isStrict() {
    	return strict;
    }
    
    /**
     * Specifies if an attempt to read a bean property that doesn't exist in the
     * wrapped object should throw an {@link InvalidPropertyException}.
     * 
     * <p>If this property is <tt>false</tt> (the default) then an attempt to read
     * a missing bean property is the same as reading an existing bean property whose
     * value is <tt>null</tt>. The template can't tell the difference, and thus always
     * can use <tt>?default('something')</tt> and <tt>?exists</tt> and similar built-ins
     * to handle the situation.
     *
     * <p>If this property is <tt>true</tt> then an attempt to read a bean propertly in
     * the template (like <tt>myBean.aProperty</tt>) that doesn't exist in the bean
     * object (as opposed to just holding <tt>null</tt> value) will cause
     * {@link InvalidPropertyException}, which can't be suppressed in the template
     * (not even with <tt>myBean.noSuchProperty?default('something')</tt>). This way
     * <tt>?default('something')</tt> and <tt>?exists</tt> and similar built-ins can be used to
     * handle existing properties whose value is <tt>null</tt>, without the risk of
     * hiding typos in the property names. Typos will always cause error. But mind you, it
     * goes against the basic approach of FreeMarker, so use this feature only if you really
     * know what you are doing.
     */
    public void setStrict(boolean strict) {
        checkModifiable();
    	this.strict = strict;
    }

    /**
     * When wrapping an object, the BeansWrapper commonly needs to wrap
     * "sub-objects", for example each element in a wrapped collection.
     * Normally it wraps these objects using itself. However, this makes
     * it difficult to delegate to a BeansWrapper as part of a custom
     * aggregate ObjectWrapper. This method lets you set the ObjectWrapper
     * which will be used to wrap the sub-objects.
     * @param outerIdentity the aggregate ObjectWrapper
     */
    public void setOuterIdentity(ObjectWrapper outerIdentity)
    {
        checkModifiable();
        this.outerIdentity = outerIdentity;
    }

    /**
     * By default returns <tt>this</tt>.
     * @see #setOuterIdentity(ObjectWrapper)
     */
    public ObjectWrapper getOuterIdentity()
    {
        return outerIdentity;
    }

    /**
     * When set to {@code true}, the keys in {@link Map}-s won't mix with the method names when looking at them
     * from templates. The default is {@code false} for backward-compatibility, but is not recommended.
     * 
     * <p>When this is {@code false}, {@code myMap.foo} or {@code myMap['foo']} either returns the method {@code foo},
     * or calls {@code Map.get("foo")}. If both exists (the method and the {@link Map} key), one will hide the other,
     * depending on the {@link #isMethodsShadowItems()}, which default to {@code true} (the method
     * wins). Some frameworks use this so that you can call {@code myMap.get(nonStringKey)} from templates [*], but it
     * comes on the cost of polluting the key-set with the method names, and risking methods accidentally hiding
     * {@link Map} entries (or the other way around). Thus, this setup is not recommended.
     * (Technical note: {@link Map}-s will be wrapped into {@link MapModel} in this case.)  
     *
     * <p>When this is {@code true}, {@code myMap.foo} or {@code myMap['foo']} always calls {@code Map.get("foo")}.
     * The methods of the {@link Map} object aren't visible from templates in this case. This, however, spoils the
     * {@code myMap.get(nonStringKey)} workaround. But now you can use {@code myMap(nonStringKey)} instead, that is, you
     * can use the map itself as the {@code get} method. 
     * (Technical note: {@link Map}-s will be wrapped into {@link SimpleMapModel} in this case.)
     * 
     * <p>*: For historical reasons, FreeMarker 2.3.X doesn't support non-string keys with the {@code []} operator,
     *       hence the workarounds. This will be likely fixed in FreeMarker 2.4.0. Also note that the method- and
     *       the "field"-namespaces aren't separate in FreeMarker, hence {@code myMap.get} can return the {@code get}
     *       method.
     */
    public void setSimpleMapWrapper(boolean simpleMapWrapper)
    {
        checkModifiable();
        this.simpleMapWrapper = simpleMapWrapper;
    }

    /**
     * Tells whether Maps are exposed as simple maps, without access to their
     * method. See {@link #setSimpleMapWrapper(boolean)} for details.
     * @return true if Maps are exposed as simple hashes, false if they're
     * exposed as full JavaBeans.
     */
    public boolean isSimpleMapWrapper()
    {
        return simpleMapWrapper;
    }

    // I have commented this out, as it won't be in 2.3.20 yet.
    /*
    /**
     * Tells which non-backward-compatible overloaded method selection fixes to apply;
     * see {@link #setOverloadedMethodSelection(Version)}.
     * /
    public Version getOverloadedMethodSelection() {
        return overloadedMethodSelection;
    }

    /**
     * Sets which non-backward-compatible overloaded method selection fixes to apply.
     * This has similar logic as {@link Configuration#setIncompatibleImprovements(Version)},
     * but only applies to this aspect.
     * 
     * Currently significant values:
     * <ul>
     *   <li>2.3.21: Completetlly rewritten overloaded method selection, fixes several issues with the old one.</li>
     * </ul>
     * /
    public void setOverloadedMethodSelection(Version version) {
        overloadedMethodSelection = version;
    }
    */
    
    /**
     * Sets the method exposure level. By default, set to <code>EXPOSE_SAFE</code>.
     * @param exposureLevel can be any of the <code>EXPOSE_xxx</code>
     * constants.
     */
    public void setExposureLevel(int exposureLevel)
    {
        checkModifiable();
     
        if (classIntrospector.getExposureLevel() != exposureLevel) {
            ClassIntrospector.PropertyAssignments pa = classIntrospector.getPropertyAssignments();
            pa.setExposureLevel(exposureLevel);
            setClassIntrospector(pa);
        }
    }
    
    /**
     * @since 2.3.21
     */
    public int getExposureLevel()
    {
        return classIntrospector.getExposureLevel();
    }
    
    /**
     * Controls whether public instance fields of classes are exposed to 
     * templates.
     * @param exposeFields if set to true, public instance fields of classes 
     * that do not have a property getter defined can be accessed directly by
     * their name. If there is a property getter for a property of the same 
     * name as the field (i.e. getter "getFoo()" and field "foo"), then 
     * referring to "foo" in template invokes the getter. If set to false, no
     * access to public instance fields of classes is given. Default is false.
     */
    public void setExposeFields(boolean exposeFields)
    {
        checkModifiable();
        
        if (classIntrospector.getExposeFields() != exposeFields) {
            ClassIntrospector.PropertyAssignments pa = classIntrospector.getPropertyAssignments();
            pa.setExposeFields(exposeFields);
            setClassIntrospector(pa);
        }
    }
    
    /**
     * Returns whether exposure of public instance fields of classes is 
     * enabled. See {@link #setExposeFields(boolean)} for details.
     * @return true if public instance fields are exposed, false otherwise.
     */
    public boolean isExposeFields()
    {
        return classIntrospector.getExposeFields();
    }
    
    public MethodAppearanceFineTuner getMethodAppearanceFineTuner() {
        return classIntrospector.getMethodAppearanceFineTuner();
    }

    /**
     * Used to tweak certain aspects of how methods appear in the data-model;
     * see {@link MethodAppearanceFineTuner} for more.
     */
    public void setMethodAppearanceFineTuner(MethodAppearanceFineTuner methodAppearanceFineTuner) {
        checkModifiable();
        
        if (classIntrospector.getMethodAppearanceFineTuner() != methodAppearanceFineTuner) {
            ClassIntrospector.PropertyAssignments pa = classIntrospector.getPropertyAssignments();
            pa.setMethodAppearanceFineTuner(methodAppearanceFineTuner);
            setClassIntrospector(pa);
        }
    }

    MethodSorter getMethodSorter() {
        return classIntrospector.getMethodSorter();
    }

    void setMethodSorter(MethodSorter methodSorter) {
        checkModifiable();
        
        if (classIntrospector.getMethodSorter() != methodSorter) {
            ClassIntrospector.PropertyAssignments pa = classIntrospector.getPropertyAssignments();
            pa.setMethodSorter(methodSorter);
            setClassIntrospector(pa);
        }
    }
    
    /**
     * Tells if this instance acts like if its class introspection cache is sharable with other {@link BeansWrapper}-s.
     * A restricted cache denies certain too "antisocial" operations, like {@link #clearClassIntrospecitonCache()}.
     * The value depends on how the instance
     * was created; with a public constructor (then this is {@code false}), or with {@link #getInstance(Version)} or
     * its overloads (then it's {@code true}). Note that in the last case it's possible that the introspection cache
     * will not be actually shared, but this will {@code true} even then. 
     * 
     * @since 2.3.21
     */
    public boolean isClassIntrospectionCacheRestricted() {
        return classIntrospector.getHasSharedInstanceRestrictons();
    }
    
    /** 
     * Replaces the value of {@link #classIntrospector}, but first it unregisters
     * the model factories in the old {@link #classIntrospector}.
     */
    private void setClassIntrospector(ClassIntrospector.PropertyAssignments pa) {
        checkModifiable();
        
        final ClassIntrospector newCI = new ClassIntrospector(pa, sharedInrospectionLock);
        final ClassIntrospector oldCI;
        
        // In principle this need not be synchronized, but as apps might publish the configuration improperly, or
        // even modify the wrapper after publishing. This doesn't give 100% protection from those violations,
        // as classIntrospector reading aren't everywhere synchronized for performance reasons. It still decreases the
        // chance of accidents, because some ops on classIntrospector are synchronized, and because it will at least
        // push the new value into the common shared memory.
        synchronized (sharedInrospectionLock) {
            oldCI = classIntrospector;
            if (oldCI != null) {
                // Note that after unregistering the model factory might still gets some callback from the old
                // classIntrospector
                if (staticModels != null) {
                    oldCI.unregisterModelFactory(staticModels);
                    staticModels.clearCache();
                }
                if (enumModels != null) {
                    oldCI.unregisterModelFactory(enumModels);
                    enumModels.clearCache();
                }
                if (modelCache != null) {
                    oldCI.unregisterModelFactory(modelCache);
                    modelCache.clearCache();
                }
                if (trueModel != null) {
                    trueModel.clearMemberCache();
                }
                if (falseModel != null) {
                    falseModel.clearMemberCache();
                }
            }
            
            classIntrospector = newCI;
            
            registerModelFactories();
        }
    }

    private void registerModelFactories() {
        if (staticModels != null) {
            classIntrospector.registerModelFactory(staticModels);
        }
        if (enumModels != null) {
            classIntrospector.registerModelFactory(enumModels);
        }
        if (modelCache != null) {
            classIntrospector.registerModelFactory(modelCache);
        }
    }

    /**
     * Sets whether methods shadow items in beans. When true (this is the
     * default value), <code>${object.name}</code> will first try to locate
     * a bean method or property with the specified name on the object, and
     * only if it doesn't find it will it try to call
     * <code>object.get(name)</code>, the so-called "generic get method" that
     * is usually used to access items of a container (i.e. elements of a map).
     * When set to false, the lookup order is reversed and generic get method
     * is called first, and only if it returns null is method lookup attempted.
     */
    public void setMethodsShadowItems(boolean methodsShadowItems)
    {
        // This sync is here as this method was originally synchronized, but was never truly thread-safe, so I don't
        // want to advertise it in the javadoc, nor I wanted to break any apps that work because of this accidentally.
        synchronized (this) {
            checkModifiable();
            this.methodsShadowItems = methodsShadowItems;
        }
    }
    
    boolean isMethodsShadowItems()
    {
        return methodsShadowItems;
    }
    
    /**
     * Sets the default date type to use for date models that result from
     * a plain <tt>java.util.Date</tt> instead of <tt>java.sql.Date</tt> or
     * <tt>java.sql.Time</tt> or <tt>java.sql.Timestamp</tt>. Default value is 
     * {@link TemplateDateModel#UNKNOWN}.
     * @param defaultDateType the new default date type.
     */
    public void setDefaultDateType(int defaultDateType) {
        // This sync is here as this method was originally synchronized, but was never truly thread-safe, so I don't
        // want to advertise it in the javadoc, nor I wanted to break any apps that work because of this accidentally.
        synchronized (this) {
            checkModifiable();
            
            this.defaultDateType = defaultDateType;
        }
    }

    /**
     * Returns the default date type. See {@link #setDefaultDateType(int)} for
     * details.
     * @return the default date type
     */
    public int getDefaultDateType() {
        return defaultDateType;
    }
    
    /**
     * Sets whether this wrapper caches the {@link TemplateModel}-s created for the Java objects that has wrapped with
     * this object wrapper. Default is {@code false}.
     * When set to {@code true}, calling {@link #wrap(Object)} multiple times for
     * the same object will likely return the same model (although there is
     * no guarantee as the cache items can be cleared any time).
     */
    public void setUseCache(boolean useCache)
    {
        checkModifiable();
        modelCache.setUseCache(useCache);
    }

    /**
     * @since 2.3.21
     */
    public boolean getUseCache()
    {
        return modelCache.getUseCache();
    }
    
    /**
     * Sets the null model. This model is returned from the
     * {@link #wrap(Object)} method whenever the underlying object 
     * reference is null. It defaults to null reference, which is dealt 
     * with quite strictly on engine level, however you can substitute an 
     * arbitrary (perhaps more lenient) model, such as 
     * {@link freemarker.template.TemplateScalarModel#EMPTY_STRING}.
     */
    public void setNullModel(TemplateModel nullModel)
    {
        checkModifiable();
        this.nullModel = nullModel;
    }
    
    /**
     * Returns the version given with {@link #BeansWrapper(Version)}, normalized to the lowest version where a change
     * has occurred. Thus, this is not necessarily the same version than that was given to the constructor.
     * 
     * @since 2.3.21
     */
    public Version getIncompatibleImprovements() {
        return incompatibleImprovements;
    }
    
    boolean is2321Bugfixed() {
        return is2321Bugfixed(getIncompatibleImprovements());
    }

    static boolean is2321Bugfixed(Version version) {
        return version.intValue() >= 2003021;
    }
    
    /** 
     * Returns the lowest version number that is equivalent with the parameter version.
     * @since 2.3.21
     */
    protected static Version normalizeIncompatibleImprovementsVersion(Version incompatibleImprovements) {
        NullArgumentException.check("version", incompatibleImprovements);
        // Warning! If you add new results here, you must update getInstance in ObjectWrapper and DefaultObjectWrapper,
        // as they will throw exception on unsupported version!
        return is2321Bugfixed(incompatibleImprovements) ? _TemplateAPI.VERSION_2_3_21 : _TemplateAPI.VERSION_2_3_0;
    }
    
    /**
     * Returns the default instance of the wrapper. This instance is used
     * when you construct various bean models without explicitly specifying
     * a wrapper. It is also returned by 
     * {@link freemarker.template.ObjectWrapper#BEANS_WRAPPER}
     * and this is the sole instance that is used by the JSP adapter.
     * You can modify the properties of the default instance (caching,
     * exposure level, null model) to affect its operation. By default, the
     * default instance is not caching, uses the <code>EXPOSE_SAFE</code>
     * exposure level, and uses null reference as the null model.
     * 
     * @deprecated Use {@link #getInstance(Version)}; as the instance returned is not read-only, it's dangerous to use.
     */
    public static final BeansWrapper getDefaultInstance()
    {
        return BeansWrapperSingletonHolder.INSTANCE;
    }

    /**
     * Wraps the object with a template model that is most specific for the object's
     * class. Specifically:
     * <ul>
     * <li>if the object is null, returns the {@link #setNullModel(TemplateModel) null model},</li>
     * <li>if the object is a Number returns a {@link NumberModel} for it,</li>
     * <li>if the object is a Date returns a {@link DateModel} for it,</li>
     * <li>if the object is a Boolean returns 
     * {@link freemarker.template.TemplateBooleanModel#TRUE} or 
     * {@link freemarker.template.TemplateBooleanModel#FALSE}</li>
     * <li>if the object is already a TemplateModel, returns it unchanged,</li>
     * <li>if the object is an array, returns a {@link ArrayModel} for it
     * <li>if the object is a Map, returns a {@link MapModel} for it
     * <li>if the object is a Collection, returns a {@link CollectionModel} for it
     * <li>if the object is an Iterator, returns a {@link IteratorModel} for it
     * <li>if the object is an Enumeration, returns a {@link EnumerationModel} for it
     * <li>if the object is a String, returns a {@link StringModel} for it
     * <li>otherwise, returns a generic {@link BeanModel} for it.
     * </ul>
     */
    public TemplateModel wrap(Object object) throws TemplateModelException
    {
        if(object == null) return nullModel;
        return modelCache.getInstance(object);
    }

    /**
     * @deprecated override {@link #getModelFactory(Class)} instead. Using this
     * method will now bypass wrapper caching (if it's enabled) and always 
     * result in creation of a new wrapper. This method will be removed in 2.4
     * @param object
     * @param factory
     */
    protected TemplateModel getInstance(Object object, ModelFactory factory)
    {
        return factory.create(object, this);
    }

    private final ModelFactory BOOLEAN_FACTORY = new ModelFactory() {
        public TemplateModel create(Object object, ObjectWrapper wrapper) {
            return ((Boolean)object).booleanValue() ? trueModel : falseModel; 
        }
    };

    private static final ModelFactory ITERATOR_FACTORY = new ModelFactory() {
        public TemplateModel create(Object object, ObjectWrapper wrapper) {
            return new IteratorModel((Iterator)object, (BeansWrapper)wrapper); 
        }
    };

    private static final ModelFactory ENUMERATION_FACTORY = new ModelFactory() {
        public TemplateModel create(Object object, ObjectWrapper wrapper) {
            return new EnumerationModel((Enumeration)object, (BeansWrapper)wrapper); 
        }
    };

    protected ModelFactory getModelFactory(Class clazz) {
        if(Map.class.isAssignableFrom(clazz)) {
            return simpleMapWrapper ? SimpleMapModel.FACTORY : MapModel.FACTORY;
        }
        if(Collection.class.isAssignableFrom(clazz)) {
            return CollectionModel.FACTORY;
        }
        if(Number.class.isAssignableFrom(clazz)) {
            return NumberModel.FACTORY;
        }
        if(Date.class.isAssignableFrom(clazz)) {
            return DateModel.FACTORY;
        }
        if(Boolean.class == clazz) { // Boolean is final 
            return BOOLEAN_FACTORY;
        }
        if(ResourceBundle.class.isAssignableFrom(clazz)) {
            return ResourceBundleModel.FACTORY;
        }
        if(Iterator.class.isAssignableFrom(clazz)) {
            return ITERATOR_FACTORY;
        }
        if(Enumeration.class.isAssignableFrom(clazz)) {
            return ENUMERATION_FACTORY;
        }
        if(clazz.isArray()) {
            return ArrayModel.FACTORY;
        }
        return StringModel.FACTORY;
    }

    /**
     * Attempts to unwrap a model into underlying object. Generally, this
     * method is the inverse of the {@link #wrap(Object)} method. In addition
     * it will unwrap arbitrary {@link TemplateNumberModel} instances into
     * a number, arbitrary {@link TemplateDateModel} instances into a date,
     * {@link TemplateScalarModel} instances into a String, arbitrary 
     * {@link TemplateBooleanModel} instances into a Boolean, arbitrary 
     * {@link TemplateHashModel} instances into a Map, arbitrary 
     * {@link TemplateSequenceModel} into a List, and arbitrary 
     * {@link TemplateCollectionModel} into a Set. All other objects are 
     * returned unchanged.
     * @throws TemplateModelException if an attempted unwrapping fails.
     */
    public Object unwrap(TemplateModel model) throws TemplateModelException
    {
        return unwrap(model, OBJECT_CLASS);
    }
    
    /**
     * Attempts to unwrap a model into an object of the desired class. 
     * Generally, this method is the inverse of the {@link #wrap(Object)} 
     * method. It recognizes a wide range of hint classes - all Java built-in
     * primitives, primitive wrappers, numbers, dates, sets, lists, maps, and
     * native arrays.
     * @param model the model to unwrap
     * @param hint the class of the unwrapped result
     * @return the unwrapped result of the desired class
     * @throws TemplateModelException if an attempted unwrapping fails.
     */
    public Object unwrap(TemplateModel model, Class hint) 
    throws TemplateModelException
    {
        final Object obj = tryUnwrap(model, hint);
        if(obj == CAN_NOT_UNWRAP) {
          throw new TemplateModelException("Can not unwrap model of type " + 
              model.getClass().getName() + " to type " + hint.getName());
        }
        return obj;
    }

    /**
     * Same as {@link #tryUnwrap(TemplateModel, Class, int, boolean)} with 0 and <tt>false</tt> last arguments.
     */
    Object tryUnwrap(TemplateModel model, Class hint) throws TemplateModelException
    {
        return tryUnwrap(model, hint, 0, false);
    }
    
    /**
     * @param targetNumTypes Used when unwrapping for overloaded methods and so the hint is too generic; 0 otherwise.
     *        This will be ignored if the hint is already a concrete numerical type. (With overloaded methods the hint
     *        is often {@link Number} or {@link Object}, because the unwrapping has to happen before choosing the
     *        concrete overloaded method.)
     * @param overloadedMode Set true {@code true} when unwrapping for an overloaded method parameter
     * @return {@link #CAN_NOT_UNWRAP} or the unwrapped object.
     */
    Object tryUnwrap(TemplateModel model, Class hint, int targetNumTypes, boolean overloadedMode) 
    throws TemplateModelException
    {
        Object res = tryUnwrap(model, hint, overloadedMode, null);
        if (targetNumTypes != 0
                && (targetNumTypes & OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT) != 0
                && res instanceof Number) {
            return OverloadedNumberUtil.addFallbackType((Number) res, targetNumTypes);
        } else {
            return res;
        }
    }

    /**
     * See {@try #tryUnwrap(TemplateModel, Class, int, boolean)}.
     */
    private Object tryUnwrap(TemplateModel model, Class hint, boolean overloadedMode,  Map recursionStops) 
    throws TemplateModelException
    {
        
        if(model == null || model == nullModel) {
            return null;
        }
        
        if (is2321Bugfixed() && hint.isPrimitive()) {
            hint = ClassUtil.primitiveClassToBoxingClass(hint);            
        }
        
        // This is for transparent interop with other wrappers (and ourselves)
        // Passing the hint allows i.e. a Jython-aware method that declares a
        // PyObject as its argument to receive a PyObject from a JythonModel
        // passed as an argument to TemplateMethodModelEx etc.
        if(model instanceof AdapterTemplateModel) {
            Object adapted = ((AdapterTemplateModel)model).getAdaptedObject(
                    hint);
            if(hint.isInstance(adapted)) {
                return adapted;
            }
            // Attempt numeric conversion 
            if(adapted instanceof Number && ClassUtil.isNumerical(hint)) {
                Number number = forceUnwrappedNumberToType((Number) adapted, hint, is2321Bugfixed());
                if(number != null) return number;
            }
        }
        
        if(model instanceof WrapperTemplateModel) {
            Object wrapped = ((WrapperTemplateModel)model).getWrappedObject();
            if(hint.isInstance(wrapped)) {
                return wrapped;
            }
            // Attempt numeric conversion 
            if(wrapped instanceof Number && ClassUtil.isNumerical(hint)) {
                Number number = forceUnwrappedNumberToType((Number) wrapped, hint, is2321Bugfixed());
                if(number != null) {
                    return number;
                }
            }
        }
        
        // Translation of generic template models to POJOs. First give priority
        // to various model interfaces based on the hint class. This helps us
        // select the appropriate interface in multi-interface models when we
        // know what is expected as the return type.

        // Java 5: Also should check for CharSequence at the end
        if(STRING_CLASS == hint) {
            if(model instanceof TemplateScalarModel) {
                return ((TemplateScalarModel)model).getAsString();
            }
            // String is final, so no other conversion will work
            return CAN_NOT_UNWRAP;
        }

        // Primitive numeric types & Number.class and its subclasses
        if(ClassUtil.isNumerical(hint)) {
            if(model instanceof TemplateNumberModel) {
                Number number = forceUnwrappedNumberToType(
                        ((TemplateNumberModel)model).getAsNumber(), hint, is2321Bugfixed());
                if(number != null) {
                    return number;
                }
            }
        }
        
        if(Boolean.TYPE == hint || BOOLEAN_CLASS == hint) {
            if(model instanceof TemplateBooleanModel) {
                return ((TemplateBooleanModel)model).getAsBoolean() 
                ? Boolean.TRUE : Boolean.FALSE;
            }
            // Boolean is final, no other conversion will work
            return CAN_NOT_UNWRAP;
        }

        if(MAP_CLASS == hint) {
            if(model instanceof TemplateHashModel) {
                return new HashAdapter((TemplateHashModel)model, this);
            }
        }
        
        if(LIST_CLASS == hint) {
            if(model instanceof TemplateSequenceModel) {
                return new SequenceAdapter((TemplateSequenceModel)model, this);
            }
        }
        
        if(SET_CLASS == hint) {
            if(model instanceof TemplateCollectionModel) {
                return new SetAdapter((TemplateCollectionModel)model, this);
            }
        }
        
        if(COLLECTION_CLASS == hint 
                || ITERABLE_CLASS == hint) {
            if(model instanceof TemplateCollectionModel) {
                return new CollectionAdapter((TemplateCollectionModel)model, 
                        this);
            }
            if(model instanceof TemplateSequenceModel) {
                return new SequenceAdapter((TemplateSequenceModel)model, this);
            }
        }
        
        // TemplateSequenceModels can be converted to arrays
        if(hint.isArray()) {
            if(model instanceof TemplateSequenceModel) {
                return unwrapSequenceToArray((TemplateSequenceModel) model, hint, true, recursionStops);
            }
            // array classes are final, no other conversion will work
            return CAN_NOT_UNWRAP;
        }
        
        // Allow one-char strings to be coerced to characters
        if(Character.TYPE == hint || hint == CHARACTER_CLASS) {
            if(model instanceof TemplateScalarModel) {
                String s = ((TemplateScalarModel)model).getAsString();
                if(s.length() == 1) {
                    return new Character(s.charAt(0));
                }
            }
            // Character is final, no other conversion will work
            return CAN_NOT_UNWRAP;
        }

        if(DATE_CLASS.isAssignableFrom(hint)) {
            if(model instanceof TemplateDateModel) {
                Date date = ((TemplateDateModel)model).getAsDate();
                if(hint.isInstance(date)) {
                    return date;
                }
            }
        }
        
        // Translation of generic template models to POJOs. Since hint was of
        // no help initially, now use an admittedly arbitrary order of 
        // interfaces. Note we still test for isInstance and isAssignableFrom
        // to guarantee we return a compatible value. 
        if(model instanceof TemplateNumberModel) {
            Number number = ((TemplateNumberModel)model).getAsNumber();
            if(hint.isInstance(number)) {
                return number;
            }
        }
        if(model instanceof TemplateDateModel) {
            Date date = ((TemplateDateModel)model).getAsDate();
            if(hint.isInstance(date)) {
                return date;
            }
        }
        if(model instanceof TemplateScalarModel && 
                hint.isAssignableFrom(STRING_CLASS)) {
            return ((TemplateScalarModel)model).getAsString();
        }
        if(model instanceof TemplateBooleanModel && 
                hint.isAssignableFrom(BOOLEAN_CLASS)) {
            return ((TemplateBooleanModel)model).getAsBoolean() 
            ? Boolean.TRUE : Boolean.FALSE;
        }
        if(model instanceof TemplateHashModel && hint.isAssignableFrom(
                HASHADAPTER_CLASS)) {
            return new HashAdapter((TemplateHashModel)model, this);
        }
        if(model instanceof TemplateSequenceModel 
                && hint.isAssignableFrom(SEQUENCEADAPTER_CLASS)) {
            return new SequenceAdapter((TemplateSequenceModel)model, this);
        }
        if(model instanceof TemplateCollectionModel && 
                hint.isAssignableFrom(SETADAPTER_CLASS)) {
            return new SetAdapter((TemplateCollectionModel)model, this);
        }
        if (overloadedMode && is2321Bugfixed()) {
            /** Because List-s are convertible to arrays later */
            if(model instanceof TemplateSequenceModel 
                    && hint.isAssignableFrom(Object[].class)) {
                return new SequenceAdapter((TemplateSequenceModel)model, this);
            }
        }

        // Last ditch effort - is maybe the model itself instance of the 
        // required type?
        if(hint.isInstance(model)) {
            return model;
        }
        
        return CAN_NOT_UNWRAP;
    }

    /**
     * @param tryOnly if <tt>true</true>, if the conversion of an item fails, the method returns {@link #CAN_NOT_UNWRAP}
     *     instead of throwing a {@link TemplateModelException}.
     */
    Object unwrapSequenceToArray(TemplateSequenceModel seq, Class arrayClass, boolean tryOnly, Map recursionStops)
            throws TemplateModelException {
        if(recursionStops != null) {
            Object retval = recursionStops.get(seq);
            if(retval != null) {
                return retval;
            }
        } else {
            recursionStops = new IdentityHashMap();
        }
        Class componentType = arrayClass.getComponentType();
        Object array = Array.newInstance(componentType, seq.size());
        recursionStops.put(seq, array);
        try {
            int size = seq.size();
            for (int i = 0; i < size; i++) {
                final TemplateModel seqItem = seq.get(i);
                Object val = tryUnwrap(seqItem, componentType, false, recursionStops);
                if(val == CAN_NOT_UNWRAP) {
                    if (tryOnly) {
                        return CAN_NOT_UNWRAP;
                    } else {
                        throw new TemplateModelException(
                                "Failed to convert " + ClassUtil.getFTLTypeDescription(seq)
                                + " object to " + ClassUtil.getShortClassNameOfObject(array) + " at index " + i
                                + ". The type of the failing item was: " + ClassUtil.getFTLTypeDescription(seqItem));
                    }
                }
                Array.set(array, i, val);
            }
        } finally {
            recursionStops.remove(seq);
        }
        return array;
    }
    
    Object listToArray(List list, Class arrayClass, Map recursionStops)
            throws TemplateModelException {
        if (list instanceof SequenceAdapter) {
            return unwrapSequenceToArray(
                    ((SequenceAdapter) list).getTemplateSequenceModel(),
                    arrayClass, false,
                    recursionStops);
        }
        
        if(recursionStops != null) {
            Object retval = recursionStops.get(list);
            if(retval != null) {
                return retval;
            }
        } else {
            recursionStops = new IdentityHashMap();
        }
        Class componentType = arrayClass.getComponentType();
        Object array = Array.newInstance(componentType, list.size());
        recursionStops.put(list, array);
        try {
            int i = 0;
            for (Iterator it = list.iterator(); it.hasNext();) {
                Object listItem = it.next();
                // TODO: Component type conversion: numerical types, char -> String, Java 5 CharSequence -> String
                try {
                    Array.set(array, i++, listItem);
                } catch (IllegalArgumentException e) {
                    throw new TemplateModelException(
                            "Failed to convert " + ClassUtil.getShortClassNameOfObject(list)
                            + " object to " + ClassUtil.getShortClassNameOfObject(array) + " at index " + i
                            + ". The type of the failing item was: " + ClassUtil.getShortClassNameOfObject(listItem), e);
                }
            }
        } finally {
            recursionStops.remove(list);
        }
        return array;
    }

    /**
     * Converts a number to the target type aggressively (possibly with overflow or significant loss of precision).
     * @param n Non-{@code null}
     * @return {@code null} if the conversion has failed.
     */
    static Number forceUnwrappedNumberToType(final Number n, final Class targetType, boolean bugfixed) {
        // We try to order the conditions by decreasing probability.
        if (targetType == n.getClass()) {
            return n;
        } else if (targetType == Integer.TYPE || targetType == Integer.class) {
            return n instanceof Integer ? (Integer) n : new Integer(n.intValue());
        } else if (targetType == Long.TYPE || targetType == Long.class) {
            return n instanceof Long ? (Long) n : new Long(n.longValue());
        } else if (targetType == Double.TYPE || targetType == Double.class) {
            return n instanceof Double ? (Double) n : new Double(n.doubleValue());
        } else if(targetType == BigDecimal.class) {
            if(n instanceof BigDecimal) {
                return n;
            } else if (n instanceof BigInteger) {
                return new BigDecimal((BigInteger) n);
            } else if (n instanceof Long) {
                // Because we can't represent long accurately as double
                return BigDecimal.valueOf(n.longValue());
            } else {
                return new BigDecimal(n.doubleValue());
            }
        } else if (targetType == Float.TYPE || targetType == Float.class) {
            return n instanceof Float ? (Float) n : new Float(n.floatValue());
        } else if (targetType == Byte.TYPE || targetType == Byte.class) {
            return n instanceof Byte ? (Byte) n : new Byte(n.byteValue());
        } else if (targetType == Short.TYPE || targetType == Short.class) {
            return n instanceof Short ? (Short) n : new Short(n.shortValue());
        } else if (targetType == BigInteger.class) {
            if (n instanceof BigInteger) {
                return n;
            } else if (bugfixed) {
                if (n instanceof OverloadedNumberUtil.IntegerBigDecimal) {
                    return ((OverloadedNumberUtil.IntegerBigDecimal) n).bigIntegerValue();
                } else if (n instanceof BigDecimal) {
                    return ((BigDecimal) n).toBigInteger(); 
                } else {
                    return BigInteger.valueOf(n.longValue()); 
                }
            } else {
                // This is wrong, because something like "123.4" will cause NumberFormatException instead of flooring.
                return new BigInteger(n.toString());
            }
        } else {
            final Number oriN = n instanceof OverloadedNumberUtil.NumberWithFallbackType
                    ? ((OverloadedNumberUtil.NumberWithFallbackType) n).getSourceNumber() : n; 
            if (targetType.isInstance(oriN)) {
                // Handle nonstandard Number subclasses as well as directly java.lang.Number.
                return oriN;
            } else {
                // Fails
                return null;
            }
        }
    }
    
    /**
     * Invokes the specified method, wrapping the return value. The specialty
     * of this method is that if the return value is null, and the return type
     * of the invoked method is void, {@link TemplateModel#NOTHING} is returned.
     * @param object the object to invoke the method on
     * @param method the method to invoke 
     * @param args the arguments to the method
     * @return the wrapped return value of the method.
     * @throws InvocationTargetException if the invoked method threw an exception
     * @throws IllegalAccessException if the method can't be invoked due to an
     * access restriction. 
     * @throws TemplateModelException if the return value couldn't be wrapped
     * (this can happen if the wrapper has an outer identity or is subclassed,
     * and the outer identity or the subclass throws an exception. Plain
     * BeansWrapper never throws TemplateModelException).
     */
    TemplateModel invokeMethod(Object object, Method method, Object[] args)
    throws
        InvocationTargetException,
        IllegalAccessException,
        TemplateModelException
    {
        // TODO: Java's Method.invoke truncates numbers if the target type has not enough bits to hold the value.
        // There should at least be an option to check this.
        Object retval = method.invoke(object, args);
        return 
            method.getReturnType() == Void.TYPE 
            ? TemplateModel.NOTHING
            : getOuterIdentity().wrap(retval); 
    }

   /**
     * Returns a hash model that represents the so-called class static models.
     * Every class static model is itself a hash through which you can call
     * static methods on the specified class. To obtain a static model for a
     * class, get the element of this hash with the fully qualified class name.
     * For example, if you place this hash model inside the root data model
     * under name "statics", you can use i.e. <code>statics["java.lang.
     * System"]. currentTimeMillis()</code> to call the {@link 
     * java.lang.System#currentTimeMillis()} method.
     * @return a hash model whose keys are fully qualified class names, and
     * that returns hash models whose elements are the static models of the
     * classes.
     */
    public TemplateHashModel getStaticModels()
    {
        return staticModels;
    }
    
    
    /**
     * Returns a hash model that represents the so-called class enum models.
     * Every class' enum model is itself a hash through which you can access
     * enum value declared by the specified class, assuming that class is an
     * enumeration. To obtain an enum model for a class, get the element of this
     * hash with the fully qualified class name. For example, if you place this 
     * hash model inside the root data model under name "enums", you can use 
     * i.e. <code>statics["java.math.RoundingMode"].UP</code> to access the 
     * {@link java.math.RoundingMode#UP} value.
     * @return a hash model whose keys are fully qualified class names, and
     * that returns hash models whose elements are the enum models of the
     * classes.
     * @throws UnsupportedOperationException if this method is invoked on a 
     * pre-1.5 JRE, as Java enums aren't supported there.
     */
    public TemplateHashModel getEnumModels() {
        if(enumModels == null) {
            throw new UnsupportedOperationException(
                    "Enums not supported before J2SE 5.");
        }
        return enumModels;
    }
    
    /** For Unit tests only */
    ModelCache getModelCache() {
        return modelCache;
    }

    public Object newInstance(Class clazz, List arguments)
    throws
        TemplateModelException
    {
        try
        {
            Object ctors = classIntrospector.get(clazz).get(ClassIntrospector.CONSTRUCTORS_KEY);
            if(ctors == null)
            {
                throw new TemplateModelException("Class " + clazz.getName() + 
                        " has no public constructors.");
            }
            Constructor ctor = null;
            Object[] objargs;
            if(ctors instanceof SimpleMethod)
            {
                SimpleMethod sm = (SimpleMethod)ctors;
                ctor = (Constructor)sm.getMember();
                objargs = sm.unwrapArguments(arguments, this);
                return ctor.newInstance(objargs);
            }
            else if(ctors instanceof OverloadedMethods)
            {
                OverloadedMethods overloadedConstructors = (OverloadedMethods) ctors; 
                return overloadedConstructors.getMemberAndArguments(arguments, this).invokeConstructor(this);
            }
            else
            {
                // Cannot happen
                throw new BugException();
            }
        }
        catch (TemplateModelException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new TemplateModelException(
                    "Could not create instance of class " + clazz.getName(), e);
        }
    }

    /**
     * Removes the introspection data for a class from the cache.
     * Use this if you know that a class is not used anymore in templates.
     * If the class will be still used, the cache entry will be silently
     * re-created, so this isn't a dangerous operation.
     * 
     * @since 2.3.20
     */
    public void removeFromClassIntrospectionCache(Class clazz) {
        classIntrospector.remove(clazz);
    }
    
    /**
     * Removes all classes from the introspection cache whose fully qualified name starts with the given prefix
     * followed by {@code '.'} or {@code '$'} or the end of the string. For example, {@code "com.example.action"}
     * will remove {@code com.example.action.Foo}, {@code com.example.action.shop.Foo}, but not
     * {@code com.example.actions.Foo} (note the "s" as the end of "actions"). {@code "com.example.action.Foo"} will
     * remove  {@code "com.example.action.Foo"} itself, and also nested classes like
     * {@code "com.example.action.Foo$Bar"}.
     * 
     * @since 2.3.21
     */
    public void removeFromClassIntrospectionCache(String namePrefix) {
        classIntrospector.removeFromClassIntrospectionCache(namePrefix);
    }
    
    /**
     * Removes all class introspection data from the cache;
     * consider using {@link #removeFromClassIntrospectionCache(String prefix)} instead.
     * 
     * <p>Use this if you want to free up memory on the expense of recreating
     * the cache entries for the classes that will be used later in templates. If you only need to purge certain
     * classes/packages, then use {@link #removeFromClassIntrospectionCache(String prefix)} instead.
     * 
     * @throws IllegalStateException if {@link #isClassIntrospectionCacheRestricted()} is {@code true}; for
     *     such singletons, you must use {@link #removeFromClassIntrospectionCache(String prefix)} instead.
     * 
     * @since 2.3.20
     */
    public void clearClassIntrospecitonCache() {
        classIntrospector.clearCache();
    }
    
    ClassIntrospector getClassIntrospector() {
        return classIntrospector;
    }
    
    /**
     * @deprecated Use {@link #setMethodAppearanceFineTuner(MethodAppearanceFineTuner)};
     *     no need to extend this class anymore.
     *     Soon this method will be final, so trying to override it will break your app.
     *     Note that if the {@code methodAppearanceFineTuner} property is set to non-{@code null}, this method is not
     *     called anymore.
     */
    protected void finetuneMethodAppearance(
            Class clazz, Method m, MethodAppearanceDecision decision) {
        // left everything on its default; do nothing
    }
    
    /**
     * Converts any {@link BigDecimal}s in the passed array to the type of
     * the corresponding formal argument of the method.
     */
    // Unused?
    public static void coerceBigDecimals(AccessibleObject callable, Object[] args)
    {
        Class[] formalTypes = null;
        for(int i = 0; i < args.length; ++i) {
            Object arg = args[i];
            if(arg instanceof BigDecimal) {
                if(formalTypes == null) {
                    if(callable instanceof Method) {
                        formalTypes = ((Method)callable).getParameterTypes();
                    }
                    else if(callable instanceof Constructor) {
                        formalTypes = ((Constructor)callable).getParameterTypes();
                    }
                    else {
                        throw new IllegalArgumentException("Expected method or "
                                + " constructor; callable is " + 
                                callable.getClass().getName());
                    }
                }
                args[i] = coerceBigDecimal((BigDecimal)arg, formalTypes[i]);
            }
        }
    }
    
    /**
     * Converts any {@link BigDecimal}s in the passed array to the type of
     * the corresponding formal argument of the method.
     */
    public static void coerceBigDecimals(Class[] formalTypes, Object[] args)
    {
        int typeLen = formalTypes.length;
        int argsLen = args.length;
        int min = Math.min(typeLen, argsLen);
        for(int i = 0; i < min; ++i) {
            Object arg = args[i];
            if(arg instanceof BigDecimal) {
                args[i] = coerceBigDecimal((BigDecimal)arg, formalTypes[i]);
            }
        }
        if(argsLen > typeLen) {
            Class varArgType = formalTypes[typeLen - 1];
            for(int i = typeLen; i < argsLen; ++i) {
                Object arg = args[i];
                if(arg instanceof BigDecimal) {
                    args[i] = coerceBigDecimal((BigDecimal)arg, varArgType);
                }
            }
        }
    }

    public static Object coerceBigDecimal(BigDecimal bd, Class formalType) {
        // int is expected in most situations, so we check it first
        if(formalType == Integer.TYPE || formalType == Integer.class) {
            return new Integer(bd.intValue());
        }
        else if(formalType == Double.TYPE || formalType == Double.class) {
            return new Double(bd.doubleValue());
        }
        else if(formalType == Long.TYPE || formalType == Long.class) {
            return new Long(bd.longValue());
        }
        else if(formalType == Float.TYPE || formalType == Float.class) {
            return new Float(bd.floatValue());
        }
        else if(formalType == Short.TYPE || formalType == Short.class) {
            return new Short(bd.shortValue());
        }
        else if(formalType == Byte.TYPE || formalType == Byte.class) {
            return new Byte(bd.byteValue());
        }
        else if(BIGINTEGER_CLASS.isAssignableFrom(formalType)) {
            return bd.toBigInteger();
        } else {
            return bd;
        }
    }
    
    /**
     * Returns the exact class name and the identity hash, also the values of the most often used {@link BeansWrapper}
     * configuration properties, also if which (if any) shared class introspection cache it uses.
     *  
     * @since 2.3.21
     */
    public String toString() {
        return ClassUtil.getShortClassNameOfObject(this) + "@" + System.identityHashCode(this)
                + "(" + incompatibleImprovements + ") { "
                + "simpleMapWrapper = " + simpleMapWrapper + ", "
                + "exposureLevel = " + classIntrospector.getExposureLevel() + ", "
                + "exposeFields = " + classIntrospector.getExposeFields() + ", "
                + "sharedClassIntrospCache = "
                + (classIntrospector.isShared() ? "@" + System.identityHashCode(classIntrospector) : "none")
                + ", ... "
                + " }";
    }

    private static ClassBasedModelFactory createEnumModels(BeansWrapper wrapper) {
        if(ENUMS_MODEL_CTOR != null) {
            try {
                return (ClassBasedModelFactory)ENUMS_MODEL_CTOR.newInstance(
                        new Object[] { wrapper });
            } catch(Exception e) {
                throw new UndeclaredThrowableException(e);
            }
        } else {
            return null;
        }
    }
    
    private static Constructor enumsModelCtor() {
        try {
            // Check if Enums are available on this platform
            Class.forName("java.lang.Enum");
            // If they are, return the appropriate constructor for enum models
            return Class.forName(
                "freemarker.ext.beans._EnumModels").getDeclaredConstructor(
                        new Class[] { BeansWrapper.class });
        }
        catch(Exception e) {
            // Otherwise, return null
            return null;
        }
    }

    /**
     * <b>Experimental class; subject to change!</b>
     * Used for
     * {@link MethodAppearanceFineTuner#process(MethodAppearanceDecisionInput, MethodAppearanceDecision)}
     * to store the results; see there.
     */
    static public final class MethodAppearanceDecision {
        private PropertyDescriptor exposeAsProperty;
        private String exposeMethodAs;
        private boolean methodShadowsProperty;
        
        void setDefaults(Method m) {
            exposeAsProperty = null;
            exposeMethodAs = m.getName();
            methodShadowsProperty = true;
        }
        
        public PropertyDescriptor getExposeAsProperty() {
            return exposeAsProperty;
        }
        
        public void setExposeAsProperty(PropertyDescriptor exposeAsProperty) {
            this.exposeAsProperty = exposeAsProperty;
        }
        
        public String getExposeMethodAs() {
            return exposeMethodAs;
        }
        
        public void setExposeMethodAs(String exposeAsMethod) {
            this.exposeMethodAs = exposeAsMethod;
        }
        
        public boolean getMethodShadowsProperty() {
            return methodShadowsProperty;
        }
        
        public void setMethodShadowsProperty(boolean shadowEarlierProperty) {
            this.methodShadowsProperty = shadowEarlierProperty;
        }

    }
    
    /**
     * <b>Experimental class; subject to change!</b>
     * Used for
     * {@link MethodAppearanceFineTuner#process(MethodAppearanceDecisionInput, MethodAppearanceDecision)}
     * as input parameter; see there.
     */
    static public final class MethodAppearanceDecisionInput {
        private Method method;
        private Class containingClass;
        
        void setMethod(Method method) {
            this.method = method;
        }
        
        void setContainingClass(Class containingClass) {
            this.containingClass = containingClass;
        }

        public Method getMethod() {
            return method;
        }

        public Class getContainingClass() {
            return containingClass;
        }
        
    }
    
    /**
     * Used as the parameter to {@link #getInstance(PropertyAssignments)}; see there.
     */
    static public final class PropertyAssignments implements freemarker.template.utility.PropertyAssignments, Cloneable {
        private final Version incompatibleImprovements;
        
        private ClassIntrospector.PropertyAssignments classIntrospectorPropertyAssignments;
        
        // Properties and their *defaults*:
        private boolean simpleMapWrapper = false;
        private int defaultDateType = TemplateDateModel.UNKNOWN;
        private ObjectWrapper outerIdentity = null;
        private boolean strict = false;
        private boolean useModelCache;
        // Attention!
        // - This is also used as a cache key, so non-normalized field values should be avoided.
        // - If some field has a default value, it must be set until the end of the constructor. No field that has a
        //   default can be left unset (like null).
        // - If you add a new field, review all methods in this class
        
        public PropertyAssignments(Version incompatibleImprovements) {
            NullArgumentException.check("incompatibleImprovements", incompatibleImprovements);
            _TemplateAPI.checkVersionSupported(incompatibleImprovements);
            
            incompatibleImprovements = normalizeIncompatibleImprovementsVersion(incompatibleImprovements);
            this.incompatibleImprovements = incompatibleImprovements;
            
            classIntrospectorPropertyAssignments = new ClassIntrospector.PropertyAssignments(incompatibleImprovements);
        }

        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + incompatibleImprovements.hashCode();
            result = prime * result + (simpleMapWrapper ? 1231 : 1237);
            result = prime * result + defaultDateType;
            result = prime * result + (outerIdentity != null ? outerIdentity.hashCode() : 0);
            result = prime * result + (strict ? 1231 : 1237);
            result = prime * result + (useModelCache ? 1231 : 1237);
            result = prime * result + classIntrospectorPropertyAssignments.hashCode();
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            PropertyAssignments other = (PropertyAssignments) obj;
            
            if (!incompatibleImprovements.equals(other.incompatibleImprovements)) return false;
            if (simpleMapWrapper != other.simpleMapWrapper) return false;
            if (defaultDateType != other.defaultDateType) return false;
            if (outerIdentity != other.outerIdentity) return false;
            if (strict != other.strict) return false;
            if (useModelCache != other.useModelCache) return false;
            if (!classIntrospectorPropertyAssignments.equals(other.classIntrospectorPropertyAssignments)) return false;
            
            return true;
        }
        
        protected Object clone(boolean deepCloneKey) {
            try {
                PropertyAssignments newPA = (PropertyAssignments) super.clone();
                if (deepCloneKey) {
                    newPA.classIntrospectorPropertyAssignments
                            = (ClassIntrospector.PropertyAssignments) classIntrospectorPropertyAssignments.clone();
                }
                return newPA;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e.getMessage());  // Java 5: use cause
            }
        }
        
        public boolean isSimpleMapWrapper() {
            return simpleMapWrapper;
        }

        /** See {@link BeansWrapper#setSimpleMapWrapper(boolean)}. */
        public void setSimpleMapWrapper(boolean simpleMapWrapper) {
            this.simpleMapWrapper = simpleMapWrapper;
        }

        public int getDefaultDateType() {
            return defaultDateType;
        }

        /** See {@link BeansWrapper#setDefaultDateType(int)}. */
        public void setDefaultDateType(int defaultDateType) {
            this.defaultDateType = defaultDateType;
        }

        public ObjectWrapper getOuterIdentity() {
            return outerIdentity;
        }

        /**
         * See {@link BeansWrapper#setOuterIdentity(ObjectWrapper)}, except here the default is {@code null} that means
         * the {@link ObjectWrapper} that you will set up with this {@link PropertyAssignments} object.
         */
        public void setOuterIdentity(ObjectWrapper outerIdentity) {
            this.outerIdentity = outerIdentity;
        }

        public boolean isStrict() {
            return strict;
        }

        /** See {@link BeansWrapper#setStrict(boolean)}. */
        public void setStrict(boolean strict) {
            this.strict = strict;
        }

        public boolean getUseModelCache() {
            return useModelCache;
        }

        /** See {@link BeansWrapper#setUseCache(boolean)} (it means the same). */
        public void setUseModelCache(boolean useModelCache) {
            this.useModelCache = useModelCache;
        }

        public Version getIncompatibleImprovements() {
            return incompatibleImprovements;
        }
        
        public int getExposureLevel() {
            return classIntrospectorPropertyAssignments.getExposureLevel();
        }

        /** See {@link BeansWrapper#setExposureLevel(int)}. */
        public void setExposureLevel(int exposureLevel) {
            classIntrospectorPropertyAssignments.setExposureLevel(exposureLevel);
        }

        public boolean getExposeFields() {
            return classIntrospectorPropertyAssignments.getExposeFields();
        }

        /** See {@link BeansWrapper#setExposeFields(boolean)}. */
        public void setExposeFields(boolean exposeFields) {
            classIntrospectorPropertyAssignments.setExposeFields(exposeFields);
        }

        public MethodAppearanceFineTuner getMethodAppearanceFineTuner() {
            return classIntrospectorPropertyAssignments.getMethodAppearanceFineTuner();
        }

        /**
         * See {@link BeansWrapper#setMethodAppearanceFineTuner(MethodAppearanceFineTuner)}; additionally,
         * note that currently setting this to non-{@code null} will disable class introspection cache sharing, unless
         * the value implements {@link SingletonCustomizer}.
         * See {@link BeansWrapper#getInstance(Version)} for more about these.
         */
        public void setMethodAppearanceFineTuner(MethodAppearanceFineTuner methodAppearanceFineTuner) {
            classIntrospectorPropertyAssignments.setMethodAppearanceFineTuner(methodAppearanceFineTuner);
        }

        MethodSorter getMethodSorter() {
            return classIntrospectorPropertyAssignments.getMethodSorter();
        }

        void setMethodSorter(MethodSorter methodSorter) {
            classIntrospectorPropertyAssignments.setMethodSorter(methodSorter);
        }
        
    }

}
