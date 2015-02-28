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

package freemarker.ext.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import freemarker.core.BugException;
import freemarker.core._DelayedFTLTypeDescription;
import freemarker.core._DelayedShortClassName;
import freemarker.core._TemplateModelException;
import freemarker.ext.util.IdentityHashMap;
import freemarker.ext.util.ModelCache;
import freemarker.ext.util.ModelFactory;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.log.Logger;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.ObjectWrapperAndUnwrapper;
import freemarker.template.SimpleObjectWrapper;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelAdapter;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.RichObjectWrapper;
import freemarker.template.utility.UndeclaredThrowableException;
import freemarker.template.utility.WriteProtectable;

/**
 * {@link ObjectWrapper} that is able to expose the Java API of arbitrary Java objects. This is also the superclass of
 * {@link DefaultObjectWrapper}. Note that instances of this class generally should be created with a
 * {@link BeansWrapperBuilder}, not with its public constructors.
 * 
 * <p>
 * As of 2.3.22, using {@link BeansWrapper} unextended is not recommended. Instead, {@link DefaultObjectWrapper} with
 * its {@code incompatibleImprovements} property set to 2.3.22 (or higher) is the recommended {@link ObjectWrapper}.
 * 
 * <p>
 * This class is only thread-safe after you have finished calling its setter methods, and then safely published it (see
 * JSR 133 and related literature). When used as part of {@link Configuration}, of course it's enough if that was safely
 * published and then left unmodified. Using {@link BeansWrapperBuilder} also guarantees thread safety.
 */
public class BeansWrapper implements RichObjectWrapper, WriteProtectable
{
    private static final Logger LOG = Logger.getLogger("freemarker.beans");

    /**
     * @deprecated Use {@link ObjectWrapperAndUnwrapper#CANT_UNWRAP_TO_TARGET_CLASS} instead. It's not a public field
     *             anyway.
     */
    static final Object CAN_NOT_UNWRAP = ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
    
    private static final Class ITERABLE_CLASS;
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
    // Introspection cache:
    
    private final Object sharedIntrospectionLock;
    
    /** 
     * {@link Class} to class info cache.
     * This object is possibly shared with other {@link BeansWrapper}-s!
     * 
     * <p>To write this, always use {@link #replaceClassIntrospector(ClassIntrospectorBuilder)}.
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
    private volatile boolean writeProtected;
    
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
     * @deprecated Use {@link BeansWrapperBuilder} or, in rare cases, {@link #BeansWrapper(Version)} instead.
     */
    public BeansWrapper() {
        this(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        // Attention! Don't change fields here, as the instance is possibly already visible to other threads.  
    }
    
    /**
     * Use {@link BeansWrapperBuilder} instead of the public constructors if possible.
     * The main disadvantage of using the public constructors is that the instances won't share caches. So unless having
     * a private cache is your goal, don't use them. See 
     * 
     * @param incompatibleImprovements
     *   Sets which of the non-backward-compatible improvements should be enabled. Not {@code null}. This version number
     *   is the same as the FreeMarker version number with which the improvements were implemented.
     *    
     *   <p>For new projects, it's recommended to set this to the FreeMarker version that's used during the development.
     *   For released products that are still actively developed it's a low risk change to increase the 3rd
     *   version number further as FreeMarker is updated, but of course you should always check the list of effects
     *   below. Increasing the 2nd or 1st version number possibly mean substantial changes with higher risk of breaking
     *   the application, but again, see the list of effects below.
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
     *       Several glitches were fixed in <em>overloaded</em> method selection. This usually just gets
     *       rid of errors (like ambiguity exceptions and numerical precision loses due to bad overloaded method
     *       choices), still, as in some cases the method chosen can be a different one now (that was the point of
     *       the reworking after all), it can mean a change in the behavior of the application. The most important
     *       change is that the treatment of {@code null} arguments were fixed, as earlier they were only seen
     *       applicable to parameters of type {@code Object}. Now {@code null}-s are seen to be applicable to any
     *       non-primitive parameters, and among those the one with the most specific type will be preferred (just
     *       like in Java), which is hence never the one with the {@code Object} parameter type. For more details
     *       about overloaded method selection changes see the version history in the FreeMarker Manual.
     *     </li>
     *   </ul>
     *   
     *   <p>Note that the version will be normalized to the lowest version where the same incompatible
     *   {@link BeansWrapper} improvements were already present, so {@link #getIncompatibleImprovements()} might returns
     *   a lower version than what you have specified.
     *
     * @since 2.3.21
     */
    public BeansWrapper(Version incompatibleImprovements) {
        this(new BeansWrapperConfiguration(incompatibleImprovements) {}, false);
        // Attention! Don't don anything here, as the instance is possibly already visible to other threads through the
        // model factory callbacks.
    }
    
    private static volatile boolean ftmaDeprecationWarnLogged;
    
    /**
     * Same as {@link #BeansWrapper(BeansWrapperConfiguration, boolean, boolean)} with {@code true}
     * {@code finalizeConstruction} argument.
     * 
     * @since 2.3.21
     */
    protected BeansWrapper(BeansWrapperConfiguration bwConf, boolean writeProtected) {
        this(bwConf, writeProtected, true);
    }
    
    /**
     * Initializes the instance based on the the {@link BeansWrapperConfiguration} specified.
     * 
     * @param writeProtected Makes the instance's configuration settings read-only via
     *     {@link WriteProtectable#writeProtect()}; this way it can use the shared class introspection cache.
     *     
     * @param finalizeConstruction Decides if the construction is finalized now, or the caller will do some more
     *     adjustments on the instance and then call {@link #finalizeConstruction(boolean)} itself. 
     * 
     * @since 2.3.22
     */
    protected BeansWrapper(BeansWrapperConfiguration bwConf, boolean writeProtected, boolean finalizeConstruction) {
        // Backward-compatibility hack for "finetuneMethodAppearance" overrides to work:
        if (bwConf.getMethodAppearanceFineTuner() == null) {
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
                    LOG.warn("Overriding " + BeansWrapper.class.getName() + ".finetuneMethodAppearance is deprecated "
                            + "and will be banned sometimes in the future. Use setMethodAppearanceFineTuner instead.");
                    ftmaDeprecationWarnLogged = true;
                }
                bwConf = (BeansWrapperConfiguration) bwConf.clone(false);
                bwConf.setMethodAppearanceFineTuner(new MethodAppearanceFineTuner() {

                    public void process(
                            MethodAppearanceDecisionInput in, MethodAppearanceDecision out) {
                        BeansWrapper.this.finetuneMethodAppearance(in.getContainingClass(), in.getMethod(), out);
                    }
                    
                });
            }
        }
        
        this.incompatibleImprovements = bwConf.getIncompatibleImprovements();  // normalized
        
        simpleMapWrapper = bwConf.isSimpleMapWrapper();
        defaultDateType = bwConf.getDefaultDateType();
        outerIdentity = bwConf.getOuterIdentity() != null ? bwConf.getOuterIdentity() : this;
        strict = bwConf.isStrict();
        
        if (!writeProtected) {
            // As this is not a read-only BeansWrapper, the classIntrospector will be possibly replaced for a few times,
            // but we need to use the same sharedInrospectionLock forever, because that's what the model factories
            // synchronize on, even during the classIntrospector is being replaced.
            sharedIntrospectionLock = new Object();
            classIntrospector = new ClassIntrospector(bwConf.classIntrospectorFactory, sharedIntrospectionLock);
        } else {
            // As this is a read-only BeansWrapper, the classIntrospector is never replaced, and since it's shared by
            // other BeansWrapper instances, we use the lock belonging to the shared ClassIntrospector.
            classIntrospector = bwConf.classIntrospectorFactory.build();
            sharedIntrospectionLock = classIntrospector.getSharedLock(); 
        }
        
        falseModel = new BooleanModel(Boolean.FALSE, this);
        trueModel = new BooleanModel(Boolean.TRUE, this);
        
        staticModels = new StaticModels(BeansWrapper.this);
        enumModels = createEnumModels(BeansWrapper.this);
        modelCache = new BeansModelCache(BeansWrapper.this);
        setUseCache(bwConf.getUseModelCache());

        finalizeConstruction(writeProtected);
    }

    /**
     * Meant to be called after {@link BeansWrapper#BeansWrapper(BeansWrapperConfiguration, boolean, boolean)} when
     * its last argument was {@code false}; makes the instance read-only if necessary, then registers the model
     * factories in the class introspector. No further changes should be done after calling this, if
     * {@code writeProtected} was {@code true}. 
     * 
     * @since 2.3.22
     */
    protected void finalizeConstruction(boolean writeProtected) {
        if (writeProtected) {
            writeProtect();
        }
        
        // Attention! At this point, the BeansWrapper must be fully initialized, as when the model factories are
        // registered below, the BeansWrapper can immediately get concurrent callbacks. That those other threads will
        // see consistent image of the BeansWrapper is ensured that callbacks are always sync-ed on
        // classIntrospector.sharedLock, and so is classIntrospector.registerModelFactory(...).
        
        registerModelFactories();
    }
    
    /**
     * Makes the configuration properties (settings) of this {@link BeansWrapper} object read-only. As changing them
     * after the object has become visible to multiple threads leads to undefined behavior, it's recommended to call
     * this when you have finished configuring the object.
     * 
     * <p>Consider using {@link BeansWrapperBuilder} instead, which gives an instance that's already
     * write protected and also uses some shared caches/pools. 
     * 
     * @since 2.3.21
     */
    public void writeProtect() {
        writeProtected = true;
    }

    /**
     * @since 2.3.21
     */
    public boolean isWriteProtected() {
        return writeProtected;
    }
    
    Object getSharedIntrospectionLock() {
        return sharedIntrospectionLock;
    }
    
    /**
     * If this object is already read-only according to {@link WriteProtectable}, throws {@link IllegalStateException},
     * otherwise does nothing.
     * 
     * @since 2.3.21
     */
    protected void checkModifiable() {
        if (writeProtected) throw new IllegalStateException(
                "Can't modify the " + this.getClass().getName() + " object, as it was write protected.");
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
            ClassIntrospectorBuilder pa = classIntrospector.getPropertyAssignments();
            pa.setExposureLevel(exposureLevel);
            replaceClassIntrospector(pa);
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
            ClassIntrospectorBuilder pa = classIntrospector.getPropertyAssignments();
            pa.setExposeFields(exposeFields);
            replaceClassIntrospector(pa);
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
            ClassIntrospectorBuilder pa = classIntrospector.getPropertyAssignments();
            pa.setMethodAppearanceFineTuner(methodAppearanceFineTuner);
            replaceClassIntrospector(pa);
        }
    }

    MethodSorter getMethodSorter() {
        return classIntrospector.getMethodSorter();
    }

    void setMethodSorter(MethodSorter methodSorter) {
        checkModifiable();
        
        if (classIntrospector.getMethodSorter() != methodSorter) {
            ClassIntrospectorBuilder pa = classIntrospector.getPropertyAssignments();
            pa.setMethodSorter(methodSorter);
            replaceClassIntrospector(pa);
        }
    }
    
    /**
     * Tells if this instance acts like if its class introspection cache is sharable with other {@link BeansWrapper}-s.
     * A restricted cache denies certain too "antisocial" operations, like {@link #clearClassIntrospecitonCache()}.
     * The value depends on how the instance
     * was created; with a public constructor (then this is {@code false}), or with {@link BeansWrapperBuilder}
     * (then it's {@code true}). Note that in the last case it's possible that the introspection cache
     * will not be actually shared because there's no one to share with, but this will {@code true} even then. 
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
    private void replaceClassIntrospector(ClassIntrospectorBuilder pa) {
        checkModifiable();
        
        final ClassIntrospector newCI = new ClassIntrospector(pa, sharedIntrospectionLock);
        final ClassIntrospector oldCI;
        
        // In principle this need not be synchronized, but as apps might publish the configuration improperly, or
        // even modify the wrapper after publishing. This doesn't give 100% protection from those violations,
        // as classIntrospector reading aren't everywhere synchronized for performance reasons. It still decreases the
        // chance of accidents, because some ops on classIntrospector are synchronized, and because it will at least
        // push the new value into the common shared memory.
        synchronized (sharedIntrospectionLock) {
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
     * Sets the null model. This model is returned from the {@link #wrap(Object)} method whenever the wrapped object is
     * {@code null}. It defaults to {@code null}, which is dealt with quite strictly on engine level, however you can
     * substitute an arbitrary (perhaps more lenient) model, like an empty string. For proper working, the
     * {@code nullModel} should be an {@link AdapterTemplateModel} that returns {@code null} for
     * {@link AdapterTemplateModel#getAdaptedObject(Class)}.
     * 
     * @deprecated Changing the {@code null} model can cause a lot of confusion; don't do it.
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
        return version.intValue() >= _TemplateAPI.VERSION_INT_2_3_21;
    }
    
    /** 
     * Returns the lowest version number that is equivalent with the parameter version.
     * @since 2.3.21
     */
    protected static Version normalizeIncompatibleImprovementsVersion(Version incompatibleImprovements) {
        _TemplateAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        if (incompatibleImprovements.intValue() < _TemplateAPI.VERSION_INT_2_3_0) {
            throw new IllegalArgumentException("Version must be at least 2.3.0.");
        }
        return is2321Bugfixed(incompatibleImprovements) ? Configuration.VERSION_2_3_21 : Configuration.VERSION_2_3_0;
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
     * @deprecated Use {@link BeansWrapperBuilder} instead. The instance returned here is not read-only, so it's
     *     dangerous to use.
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
     * <li>otherwise, returns a generic {@link StringModel} for it.
     * </ul>
     */
    public TemplateModel wrap(Object object) throws TemplateModelException
    {
        if(object == null) return nullModel;
        return modelCache.getInstance(object);
    }
    
    /**
     * Wraps a Java method so that it can be called from templates, without wrapping its parent ("this") object. The
     * result is almost the same as that you would get by wrapping the parent object then getting the method from the
     * resulting {@link TemplateHashModel} by name. Except, if the wrapped method is overloaded, with this method you
     * explicitly select a an overload, while otherwise you would get a {@link TemplateMethodModelEx} that selects an
     * overload each time it's called based on the argument values.
     * 
     * @param object The object whose method will be called, or {@code null} if {@code method} is a static method.
     *          This object will be used "as is", like without unwrapping it if it's a {@link TemplateModelAdapter}.
     * @param method The method to call, which must be an (inherited) member of the class of {@code object}, as
     *          described by {@link Method#invoke(Object, Object...)}
     * 
     * @since 2.3.22
     */
    public TemplateMethodModelEx wrap(Object object, Method method) {
        return new SimpleMethodModel(object, method, method.getParameterTypes(), this);
    }
    
    /**
     * @since 2.3.22
     */
    public TemplateHashModel wrapAsAPI(Object obj) throws TemplateModelException {
        return new APIModel(obj, this);
    }

    /**
     * @deprecated override {@link #getModelFactory(Class)} instead. Using this
     * method will now bypass wrapper caching (if it's enabled) and always 
     * result in creation of a new wrapper. This method will be removed in 2.4
     * @param object The object to wrap
     * @param factory The factory that wraps the object
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
        return unwrap(model, Object.class);
    }

    /**
     * Attempts to unwrap a model into an object of the desired class. 
     * Generally, this method is the inverse of the {@link #wrap(Object)} 
     * method. It recognizes a wide range of target classes - all Java built-in
     * primitives, primitive wrappers, numbers, dates, sets, lists, maps, and
     * native arrays.
     * @param model the model to unwrap
     * @param targetClass the class of the unwrapped result; {@code Object.class} of we don't know what the expected type is.
     * @return the unwrapped result of the desired class
     * @throws TemplateModelException if an attempted unwrapping fails.
     * 
     * @see #tryUnwrapTo(TemplateModel, Class)
     */
    public Object unwrap(TemplateModel model, Class targetClass) 
    throws TemplateModelException
    {
        final Object obj = tryUnwrapTo(model, targetClass);
        if(obj == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
          throw new TemplateModelException("Can not unwrap model of type " + 
              model.getClass().getName() + " to type " + targetClass.getName());
        }
        return obj;
    }

    /**
     * @since 2.3.22
     */
    public Object tryUnwrapTo(TemplateModel model, Class targetClass) throws TemplateModelException
    {
        return tryUnwrapTo(model, targetClass, 0);
    }
    
    /**
     * @param typeFlags
     *            Used when unwrapping for overloaded methods and so the {@code targetClass} is possibly too generic.
     *            Must be 0 when unwrapping parameter values for non-overloaded methods, also if
     *            {@link #is2321Bugfixed()} is {@code false}.
     * @return {@link ObjectWrapperAndUnwrapper#CANT_UNWRAP_TO_TARGET_CLASS} or the unwrapped object.
     */
    Object tryUnwrapTo(TemplateModel model, Class targetClass, int typeFlags) 
    throws TemplateModelException
    {
        Object res = tryUnwrapTo(model, targetClass, typeFlags, null);
        if ((typeFlags & TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT) != 0
                && res instanceof Number) {
            return OverloadedNumberUtil.addFallbackType((Number) res, typeFlags);
        } else {
            return res;
        }
    }

    /**
     * See {@try #tryUnwrap(TemplateModel, Class, int, boolean)}.
     */
    private Object tryUnwrapTo(final TemplateModel model, Class targetClass, final int typeFlags, final Map recursionStops) 
    throws TemplateModelException {
        if(model == null || model == nullModel) {
            return null;
        }
        
        final boolean is2321Bugfixed = is2321Bugfixed();
        
        if (is2321Bugfixed && targetClass.isPrimitive()) {
            targetClass = ClassUtil.primitiveClassToBoxingClass(targetClass);            
        }
        
        // This is for transparent interop with other wrappers (and ourselves)
        // Passing the targetClass allows i.e. a Jython-aware method that declares a
        // PyObject as its argument to receive a PyObject from a JythonModel
        // passed as an argument to TemplateMethodModelEx etc.
        if(model instanceof AdapterTemplateModel) {
            Object wrapped = ((AdapterTemplateModel)model).getAdaptedObject(
                    targetClass);
            if (targetClass == Object.class || targetClass.isInstance(wrapped)) {
                return wrapped;
            }
            
            // Attempt numeric conversion: 
            if (targetClass != Object.class && (wrapped instanceof Number && ClassUtil.isNumerical(targetClass))) {
                Number number = forceUnwrappedNumberToType((Number) wrapped, targetClass, is2321Bugfixed);
                if(number != null) return number;
            }
        }
        
        if(model instanceof WrapperTemplateModel) {
            Object wrapped = ((WrapperTemplateModel)model).getWrappedObject();
            if (targetClass == Object.class || targetClass.isInstance(wrapped)) {
                return wrapped;
            }
            
            // Attempt numeric conversion: 
            if(targetClass != Object.class && (wrapped instanceof Number && ClassUtil.isNumerical(targetClass))) {
                Number number = forceUnwrappedNumberToType((Number) wrapped, targetClass, is2321Bugfixed);
                if(number != null) {
                    return number;
                }
            }
        }
        
        // Translation of generic template models to POJOs. First give priority
        // to various model interfaces based on the targetClass. This helps us
        // select the appropriate interface in multi-interface models when we
        // know what is expected as the return type.
        if (targetClass != Object.class) {

            // Java 5: Also should check for CharSequence at the end
            if(String.class == targetClass) {
                if(model instanceof TemplateScalarModel) {
                    return ((TemplateScalarModel)model).getAsString();
                }
                // String is final, so no other conversion will work
                return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
            }
    
            // Primitive numeric types & Number.class and its subclasses
            if(ClassUtil.isNumerical(targetClass)) {
                if(model instanceof TemplateNumberModel) {
                    Number number = forceUnwrappedNumberToType(
                            ((TemplateNumberModel)model).getAsNumber(), targetClass, is2321Bugfixed);
                    if(number != null) {
                        return number;
                    }
                }
            }
            
            if(boolean.class == targetClass || Boolean.class == targetClass) {
                if(model instanceof TemplateBooleanModel) {
                    return Boolean.valueOf(((TemplateBooleanModel) model).getAsBoolean());
                }
                // Boolean is final, no other conversion will work
                return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
            }
    
            if(Map.class == targetClass) {
                if(model instanceof TemplateHashModel) {
                    return new HashAdapter((TemplateHashModel)model, this);
                }
            }
            
            if(List.class == targetClass) {
                if(model instanceof TemplateSequenceModel) {
                    return new SequenceAdapter((TemplateSequenceModel)model, this);
                }
            }
            
            if(Set.class == targetClass) {
                if(model instanceof TemplateCollectionModel) {
                    return new SetAdapter((TemplateCollectionModel)model, this);
                }
            }
            
            if(Collection.class == targetClass || ITERABLE_CLASS == targetClass) {
                if(model instanceof TemplateCollectionModel) {
                    return new CollectionAdapter((TemplateCollectionModel)model, 
                            this);
                }
                if(model instanceof TemplateSequenceModel) {
                    return new SequenceAdapter((TemplateSequenceModel)model, this);
                }
            }
            
            // TemplateSequenceModels can be converted to arrays
            if(targetClass.isArray()) {
                if(model instanceof TemplateSequenceModel) {
                    return unwrapSequenceToArray((TemplateSequenceModel) model, targetClass, true, recursionStops);
                }
                // array classes are final, no other conversion will work
                return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
            }
            
            // Allow one-char strings to be coerced to characters
            if(char.class == targetClass || targetClass == Character.class) {
                if(model instanceof TemplateScalarModel) {
                    String s = ((TemplateScalarModel)model).getAsString();
                    if(s.length() == 1) {
                        return new Character(s.charAt(0));
                    }
                }
                // Character is final, no other conversion will work
                return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
            }
    
            if(Date.class.isAssignableFrom(targetClass) && model instanceof TemplateDateModel) {
                Date date = ((TemplateDateModel)model).getAsDate();
                if(targetClass.isInstance(date)) {
                    return date;
                }
            }
        }  //  End: if (targetClass != Object.class)
        
        // Since the targetClass was of no help initially, now we use
        // a quite arbitrary order in which we walk through the TemplateModel subinterfaces, and unwrapp them to
        // their "natural" Java correspondent. We still try exclude unwrappings that won't fit the target parameter
        // type(s). This is mostly important because of multi-typed FTL values that could be unwrapped on multiple ways.
        int itf = typeFlags; // Iteration's Type Flags. Should be always 0 for non-overloaded and when !is2321Bugfixed.
        // If itf != 0, we possibly execute the following loop body at twice: once with utilizing itf, and if it has not
        // returned, once more with itf == 0. Otherwise we execute this once with itf == 0.
        do {
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_NUMBER) != 0)
                    && model instanceof TemplateNumberModel) {
                Number number = ((TemplateNumberModel) model).getAsNumber();
                if (itf != 0 || targetClass.isInstance(number)) {
                    return number;
                }
            }
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_DATE) != 0)
                    && model instanceof TemplateDateModel) {
                Date date = ((TemplateDateModel) model).getAsDate();
                if (itf != 0 || targetClass.isInstance(date)) {
                    return date;
                }
            }
            if ((itf == 0 || (itf & (TypeFlags.ACCEPTS_STRING | TypeFlags.CHARACTER)) != 0)
                    && model instanceof TemplateScalarModel
                    && (itf != 0 || targetClass.isAssignableFrom(String.class))) {
                String strVal = ((TemplateScalarModel) model).getAsString();
                if (itf == 0 || (itf & TypeFlags.CHARACTER) == 0) {
                    return strVal;
                } else { // TypeFlags.CHAR == 1
                    if (strVal.length() == 1) {
                        if ((itf & TypeFlags.ACCEPTS_STRING) != 0) {
                            return new CharacterOrString(strVal);
                        } else {
                            return new Character(strVal.charAt(0));
                        }
                    } else if ((itf & TypeFlags.ACCEPTS_STRING) != 0) {
                        return strVal; 
                    }
                    // It had to be unwrapped to Character, but the string length wasn't 1 => Fall through
                }
            }
            // Should be earlier than TemplateScalarModel, but we keep it here until FM 2.4 or such
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_BOOLEAN) != 0)
                    && model instanceof TemplateBooleanModel
                    && (itf != 0 || targetClass.isAssignableFrom(Boolean.class))) {
                return Boolean.valueOf(((TemplateBooleanModel) model).getAsBoolean());
            }
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_MAP) != 0)
                    && model instanceof TemplateHashModel
                    && (itf != 0 || targetClass.isAssignableFrom(HashAdapter.class))) {
                return new HashAdapter((TemplateHashModel) model, this);
            }
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_LIST) != 0)
                    && model instanceof TemplateSequenceModel 
                    && (itf != 0 || targetClass.isAssignableFrom(SequenceAdapter.class))) {
                return new SequenceAdapter((TemplateSequenceModel) model, this);
            }
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_SET) != 0)
                    && model instanceof TemplateCollectionModel
                    && (itf != 0 || targetClass.isAssignableFrom(SetAdapter.class))) {
                return new SetAdapter((TemplateCollectionModel) model, this);
            }
            
            // In 2.3.21 bugfixed mode only, List-s are convertible to arrays on invocation time. Only overloaded
            // methods need this. As itf will be 0 in non-bugfixed mode and for non-overloaded method calls, it's
            // enough to check if the TypeFlags.ACCEPTS_ARRAY bit is 1:
            if ((itf & TypeFlags.ACCEPTS_ARRAY) != 0
                    && model instanceof TemplateSequenceModel) {
                return new SequenceAdapter((TemplateSequenceModel) model, this);
            }
            
            if (itf == 0) {
                break;
            }
            itf = 0; // start 2nd iteration
        } while (true);

        // Last ditch effort - is maybe the model itself is an instance of the required type?
        // Note that this will be always true for Object.class targetClass. 
        if (targetClass.isInstance(model)) {
            return model;
        }
        
        return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
    }

    /**
     * @param tryOnly
     *            If {@code true}, if the conversion of an item to the component type isn't possible, the method returns
     *            {@link ObjectWrapperAndUnwrapper#CANT_UNWRAP_TO_TARGET_CLASS} instead of throwing a
     *            {@link TemplateModelException}.
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
            final int size = seq.size();
            for (int i = 0; i < size; i++) {
                final TemplateModel seqItem = seq.get(i);
                Object val = tryUnwrapTo(seqItem, componentType, 0, recursionStops);
                if(val == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                    if (tryOnly) {
                        return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
                    } else {
                        throw new _TemplateModelException(new Object[] {
                                "Failed to convert ",  new _DelayedFTLTypeDescription(seq),
                                " object to ", new _DelayedShortClassName(array.getClass()),
                                ": Problematic sequence item at index ", new Integer(i) ," with value type: ",
                                new _DelayedFTLTypeDescription(seqItem)});
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
            boolean isComponentTypeExamined = false;
            boolean isComponentTypeNumerical = false;  // will be filled on demand
            boolean isComponentTypeList = false;  // will be filled on demand
            int i = 0;
            for (Iterator it = list.iterator(); it.hasNext();) {
                Object listItem = it.next();
                if (listItem != null && !componentType.isInstance(listItem)) {
                    // Type conversion is needed. If we can't do it, we just let it fail at Array.set later.
                    if (!isComponentTypeExamined) {
                        isComponentTypeNumerical = ClassUtil.isNumerical(componentType);
                        isComponentTypeList = List.class.isAssignableFrom(componentType);
                        isComponentTypeExamined = true;
                    }
                    if (isComponentTypeNumerical && listItem instanceof Number) {
                        listItem = forceUnwrappedNumberToType((Number) listItem, componentType, true);
                    } else if (componentType == String.class && listItem instanceof Character) {
                        listItem = String.valueOf(((Character) listItem).charValue());
                    } else if ((componentType == Character.class || componentType == char.class)
                            && listItem instanceof String) {
                        String listItemStr = (String) listItem;
                        if (listItemStr.length() == 1) {
                            // Java 5: use Character.valueOf
                            listItem = new Character(listItemStr.charAt(0));
                        }
                    } else if (componentType.isArray()) {
                        if (listItem instanceof List) {
                            listItem = listToArray((List) listItem, componentType, recursionStops);
                        } else if (listItem instanceof TemplateSequenceModel) {
                            listItem = unwrapSequenceToArray((TemplateSequenceModel) listItem, componentType, false, recursionStops);
                        }
                    } else if (isComponentTypeList && listItem.getClass().isArray()) {
                        listItem = arrayToList(listItem);
                    }
                }
                try {
                    Array.set(array, i, listItem);
                } catch (IllegalArgumentException e) {
                    throw new TemplateModelException(
                            "Failed to convert " + ClassUtil.getShortClassNameOfObject(list)
                            + " object to " + ClassUtil.getShortClassNameOfObject(array)
                            + ": Problematic List item at index " + i + " with value type: "
                            + ClassUtil.getShortClassNameOfObject(listItem), e);
                }
                i++;
            }
        } finally {
            recursionStops.remove(list);
        }
        return array;
    }
    
    /**
     * @param array Must be an array (of either a reference or primitive type)
     */
    List arrayToList(Object array) throws TemplateModelException {
        if (array instanceof Object[]) {
            // Array of any non-primitive type.
            // Note that an array of non-primitive type is always instanceof Object[].
            Object[] objArray = (Object[]) array;
            return objArray.length == 0 ? Collections.EMPTY_LIST : new NonPrimitiveArrayBackedReadOnlyList(objArray);
        } else {
            // Array of any primitive type
            return Array.getLength(array) == 0 ? Collections.EMPTY_LIST : new PrimtiveArrayBackedReadOnlyList(array);
        }
    }

    /**
     * Converts a number to the target type aggressively (possibly with overflow or significant loss of precision).
     * @param n Non-{@code null}
     * @return {@code null} if the conversion has failed.
     */
    static Number forceUnwrappedNumberToType(final Number n, final Class targetType, final boolean bugfixed) {
        // We try to order the conditions by decreasing probability.
        if (targetType == n.getClass()) {
            return n;
        } else if (targetType == int.class || targetType == Integer.class) {
            return n instanceof Integer ? (Integer) n : new Integer(n.intValue());
        } else if (targetType == long.class || targetType == Long.class) {
            return n instanceof Long ? (Long) n : new Long(n.longValue());
        } else if (targetType == double.class || targetType == Double.class) {
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
        } else if (targetType == float.class || targetType == Float.class) {
            return n instanceof Float ? (Float) n : new Float(n.floatValue());
        } else if (targetType == byte.class || targetType == Byte.class) {
            return n instanceof Byte ? (Byte) n : new Byte(n.byteValue());
        } else if (targetType == short.class || targetType == Short.class) {
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
        // [2.4]: Java's Method.invoke truncates numbers if the target type has not enough bits to hold the value.
        // There should at least be an option to check this.
        Object retval = method.invoke(object, args);
        return 
            method.getReturnType() == void.class 
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

    /**
     * Creates a new instance of the specified class using the method call logic of this object wrapper for calling the
     * constructor. Overloaded constructors and varargs are supported. Only public constructors will be called.
     * 
     * @param clazz The class whose constructor we will call.
     * @param arguments The list of {@link TemplateModel}-s to pass to the constructor after unwrapping them
     * @return The instance created; it's not wrapped into {@link TemplateModel}.
     */
    public Object newInstance(Class clazz, List/*<TemplateModel>*/ arguments)
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
                try {
                    return ctor.newInstance(objargs);
                } catch (Exception e) {
                    if (e instanceof TemplateModelException) throw (TemplateModelException) e;
                    throw _MethodUtil.newInvocationTemplateModelException(null, ctor, e);
                }
            }
            else if(ctors instanceof OverloadedMethods)
            {
                final MemberAndArguments mma = ((OverloadedMethods) ctors).getMemberAndArguments(arguments, this);
                try {
                    return mma.invokeConstructor(this);
                } catch (Exception e) {
                    if (e instanceof TemplateModelException) throw (TemplateModelException) e;
                    
                    throw _MethodUtil.newInvocationTemplateModelException(null, mma.getCallableMemberDescriptor(), e);
                }
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
                    "Error while creating new instance of class " + clazz.getName() + "; see cause exception", e);
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
     * Removes all class introspection data from the cache.
     * 
     * <p>Use this if you want to free up memory on the expense of recreating
     * the cache entries for the classes that will be used later in templates.
     * 
     * @throws IllegalStateException if {@link #isClassIntrospectionCacheRestricted()} is {@code true}.
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
        if(formalType == int.class || formalType == Integer.class) {
            return new Integer(bd.intValue());
        }
        else if(formalType == double.class || formalType == Double.class) {
            return new Double(bd.doubleValue());
        }
        else if(formalType == long.class || formalType == Long.class) {
            return new Long(bd.longValue());
        }
        else if(formalType == float.class || formalType == Float.class) {
            return new Float(bd.floatValue());
        }
        else if(formalType == short.class || formalType == Short.class) {
            return new Short(bd.shortValue());
        }
        else if(formalType == byte.class || formalType == Byte.class) {
            return new Byte(bd.byteValue());
        }
        else if(java.math.BigInteger.class.isAssignableFrom(formalType)) {
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
        final String propsStr = toPropertiesString();
        return ClassUtil.getShortClassNameOfObject(this) + "@" + System.identityHashCode(this)
                + "(" + incompatibleImprovements + ", "
                + (propsStr.length() != 0 ? propsStr + ", ..." : "")
                + ")";
    }
    
    /**
     * Returns the name-value pairs that describe the configuration of this {@link BeansWrapper}; called from
     * {@link #toString()}. The expected format is like {@code "foo=bar, baaz=wombat"}. When overriding this, you should
     * call the super method, and then insert the content before it with a following {@code ", "}, or after it with a
     * preceding {@code ", "}.
     * 
     * @since 2.3.22
     */
    protected String toPropertiesString() {
        // Start with "simpleMapWrapper", because the override in DefaultObjectWrapper expects it to be there!
        return "simpleMapWrapper=" + simpleMapWrapper + ", "
               + "exposureLevel=" + classIntrospector.getExposureLevel() + ", "
               + "exposeFields=" + classIntrospector.getExposeFields() + ", "
               + "sharedClassIntrospCache="
               + (classIntrospector.isShared() ? "@" + System.identityHashCode(classIntrospector) : "none");
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
     * {@link MethodAppearanceFineTuner#process}
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
     * {@link MethodAppearanceFineTuner#process}
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

}
