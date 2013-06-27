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

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import freemarker.core._ConcurrentMapFactory;
import freemarker.ext.util.IdentityHashMap;
import freemarker.ext.util.ModelCache;
import freemarker.ext.util.ModelFactory;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.log.Logger;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.Collections12;
import freemarker.template.utility.SecurityUtilities;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * Utility class that provides generic services to reflection classes.
 * It handles all polymorphism issues in the {@link #wrap(Object)} and {@link #unwrap(TemplateModel)} methods.
 * @author Attila Szegedi
 */
public class BeansWrapper implements ObjectWrapper
{
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
    private static final Class NUMBER_CLASS = Number.class;
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
    
    // When this property is true, some things are stricter. This is mostly to
    // catch anomalous things in development that can otherwise be valid situations
    // for our users.
    private static final boolean DEVELOPMENT = "true".equals(SecurityUtilities.getSystemProperty("freemarker.development"));
    
    private static final Constructor ENUMS_MODEL_CTOR = enumsModelCtor();

    private static final Logger logger = Logger.getLogger("freemarker.beans");
    
    private static final Set UNSAFE_METHODS = createUnsafeMethodsSet();
    
    static final Object GENERIC_GET_KEY = new Object();
    private static final Object CONSTRUCTORS = new Object();
    private static final Object ARGTYPES = new Object();

    private static final boolean javaRebelAvailable = isJavaRebelAvailable();
    
    /**
     * The default instance of BeansWrapper
     */
    private static final BeansWrapper INSTANCE = new BeansWrapper();

    /**
     * Used for synchronization by {@link #genericClassIntrospectionCache},
     * {@link #staticModels} and {@link #enumModels} (and by any similar future
     * fields). The primary goal of using a common monitor is to prevent
     * deadlocks when these objects call each other.
     */
    private final Object sharedClassIntrospectionCacheLock = new Object();
    
    /**
     * This is called the "generic" class introspection cache because when the
     * public API simply says "introspection cache" it refers to the sum of all
     * caches. The other less generic caches are in {@link #staticModels}
     * and {@link #enumModels}.
     */
    private final Map/*<Class, Map<String, Object>>*/ genericClassIntrospectionCache
            = _ConcurrentMapFactory.newMaybeConcurrentHashMap();
    private final boolean isGenericClassIntrospectionCacheConcurrentMap
            = _ConcurrentMapFactory.isConcurrent(genericClassIntrospectionCache);
    private final Set/*<String>*/ genericClassIntrospectionCacheClassNames
            = new HashSet();
    private final Set/*<Class>*/ genericClassIntrospectionsInProgress
            = new HashSet();

    private final StaticModels staticModels = new StaticModels(this);
    private final ClassBasedModelFactory enumModels = createEnumModels(this);

    private final ModelCache modelCache = new BeansModelCache(this);
    
    private final BooleanModel FALSE = new BooleanModel(Boolean.FALSE, this);
    private final BooleanModel TRUE = new BooleanModel(Boolean.TRUE, this);

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

    private int exposureLevel = EXPOSE_SAFE;
    private TemplateModel nullModel = null;
    private boolean methodsShadowItems = true;
    private boolean exposeFields = false;
    private int defaultDateType = TemplateDateModel.UNKNOWN;

    private ObjectWrapper outerIdentity = this;
    private boolean simpleMapWrapper;
    private boolean strict = false;
    
    // I have commented this out, as it won't be in 2.3.20 yet.
    //private Version overloadedMethodSelection;
    
    /**
     * Creates a new instance of BeansWrapper. The newly created instance
     * will use the null reference as its null object, it will use
     * {@link #EXPOSE_SAFE} method exposure level, and will not cache
     * model instances.
     */
    public BeansWrapper() {
        if(javaRebelAvailable) {
            JavaRebelIntegration.registerWrapper(this);
        }
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
     * know what are you doing.
     */
    public void setStrict(boolean strict) {
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
     * By default the BeansWrapper wraps classes implementing
     * java.util.Map using {@link MapModel}. Setting this flag will
     * cause it to use a {@link SimpleMapModel} instead. The biggest
     * difference is that when using a {@link SimpleMapModel}, the
     * map will be visible as <code>TemplateHashModelEx</code>,
     * and the subvariables will be the content of the map,
     * without the other methods and properties of the map object.
     * @param simpleMapWrapper enable simple map wrapping
     */
    public void setSimpleMapWrapper(boolean simpleMapWrapper)
    {
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
        if(exposureLevel < EXPOSE_ALL || exposureLevel > EXPOSE_NOTHING)
        {
            throw new IllegalArgumentException("Illegal exposure level " + exposureLevel);
        }
        this.exposureLevel = exposureLevel;
    }
    
    int getExposureLevel()
    {
        return exposureLevel;
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
        this.exposeFields = exposeFields;
    }
    
    /**
     * Returns whether exposure of public instance fields of classes is 
     * enabled. See {@link #setExposeFields(boolean)} for details.
     * @return true if public instance fields are exposed, false otherwise.
     */
    public boolean isExposeFields()
    {
        return exposeFields;
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
    public synchronized void setMethodsShadowItems(boolean methodsShadowItems)
    {
        this.methodsShadowItems = methodsShadowItems;
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
    public synchronized void setDefaultDateType(int defaultDateType) {
        this.defaultDateType = defaultDateType;
    }

    /**
     * Returns the default date type. See {@link #setDefaultDateType(int)} for
     * details.
     * @return the default date type
     */
    protected int getDefaultDateType() {
        return defaultDateType;
    }
    
    /**
     * Sets whether this wrapper caches model instances. Default is false.
     * When set to true, calling {@link #wrap(Object)} multiple times for
     * the same object will likely return the same model (although there is
     * no guarantee as the cache items can be cleared anytime).
     */
    public void setUseCache(boolean useCache)
    {
        modelCache.setUseCache(useCache);
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
        this.nullModel = nullModel;
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
     */
    public static final BeansWrapper getDefaultInstance()
    {
        return INSTANCE;
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
        if(object == null)
            return nullModel;
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
            return ((Boolean)object).booleanValue() ? TRUE : FALSE; 
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
        final Object obj = unwrapInternal(model, hint);
        if(obj == CAN_NOT_UNWRAP) {
          throw new TemplateModelException("Can not unwrap model of type " + 
              model.getClass().getName() + " to type " + hint.getName());
        }
        return obj;
    }
    
    Object unwrapInternal(TemplateModel model, Class hint) 
    throws TemplateModelException
    {
        return unwrap(model, hint, null);
    }

    private Object unwrap(TemplateModel model, Class hint, Map recursionStops) 
    throws TemplateModelException
    {
        if(model == null || model == nullModel) {
            return null;
        }
        
        boolean isBoolean = Boolean.TYPE == hint;
        boolean isChar = Character.TYPE == hint;
        
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
            if(adapted instanceof Number && ((hint.isPrimitive() && !isChar && 
                    !isBoolean) || NUMBER_CLASS.isAssignableFrom(hint))) {
                Number number = convertUnwrappedNumber(hint,
                        (Number)adapted);
                if(number != null) {
                    return number;
                }
            }
        }
        
        if(model instanceof WrapperTemplateModel) {
            Object wrapped = ((WrapperTemplateModel)model).getWrappedObject();
            if(hint.isInstance(wrapped)) {
                return wrapped;
            }
            // Attempt numeric conversion 
            if(wrapped instanceof Number && ((hint.isPrimitive() && !isChar && 
                    !isBoolean) || NUMBER_CLASS.isAssignableFrom(hint))) {
                Number number = convertUnwrappedNumber(hint,
                        (Number)wrapped);
                if(number != null) {
                    return number;
                }
            }
        }
        
        // Translation of generic template models to POJOs. First give priority
        // to various model interfaces based on the hint class. This helps us
        // select the appropriate interface in multi-interface models when we
        // know what is expected as the return type.

        if(STRING_CLASS == hint) {
            if(model instanceof TemplateScalarModel) {
                return ((TemplateScalarModel)model).getAsString();
            }
            // String is final, so no other conversion will work
            return CAN_NOT_UNWRAP;
        }

        // Primitive numeric types & Number.class and its subclasses
        if((hint.isPrimitive() && !isChar && !isBoolean) 
                || NUMBER_CLASS.isAssignableFrom(hint)) {
            if(model instanceof TemplateNumberModel) {
                Number number = convertUnwrappedNumber(hint, 
                        ((TemplateNumberModel)model).getAsNumber());
                if(number != null) {
                    return number;
                }
            }
        }
        
        if(isBoolean || BOOLEAN_CLASS == hint) {
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
                if(recursionStops != null) {
                    Object retval = recursionStops.get(model);
                    if(retval != null) {
                        return retval;
                    }
                } else {
                    recursionStops = 
                        new IdentityHashMap();
                }
                TemplateSequenceModel seq = (TemplateSequenceModel)model;
                Class componentType = hint.getComponentType();
                Object array = Array.newInstance(componentType, seq.size());
                recursionStops.put(model, array);
                try {
                    int size = seq.size();
                    for (int i = 0; i < size; i++) {
                        Object val = unwrap(seq.get(i), componentType, 
                                recursionStops);
                        if(val == CAN_NOT_UNWRAP) {
                            return CAN_NOT_UNWRAP;
                        }
                        Array.set(array, i, val);
                    }
                } finally {
                    recursionStops.remove(model);
                }
                return array;
            }
            // array classes are final, no other conversion will work
            return CAN_NOT_UNWRAP;
        }
        
        // Allow one-char strings to be coerced to characters
        if(isChar || hint == CHARACTER_CLASS) {
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

        // Last ditch effort - is maybe the model itself instance of the 
        // required type?
        if(hint.isInstance(model)) {
            return model;
        }
        
        return CAN_NOT_UNWRAP;
    }

    private static Number convertUnwrappedNumber(Class hint, Number number)
    {
        if(hint == Integer.TYPE || hint == Integer.class) {
            return number instanceof Integer ? (Integer)number : 
                new Integer(number.intValue());
        }
        if(hint == Long.TYPE || hint == Long.class) {
            return number instanceof Long ? (Long)number : 
                new Long(number.longValue());
        }
        if(hint == Float.TYPE || hint == Float.class) {
            return number instanceof Float ? (Float)number : 
                new Float(number.floatValue());
        }
        if(hint == Double.TYPE 
                || hint == Double.class) {
            return number instanceof Double ? (Double)number : 
                new Double(number.doubleValue());
        }
        if(hint == Byte.TYPE || hint == Byte.class) {
            return number instanceof Byte ? (Byte)number : 
                new Byte(number.byteValue());
        }
        if(hint == Short.TYPE || hint == Short.class) {
            return number instanceof Short ? (Short)number : 
                new Short(number.shortValue());
        }
        if(hint == BigInteger.class) {
            return number instanceof BigInteger ? number : 
                new BigInteger(number.toString());
        }
        if(hint == BigDecimal.class) {
            if(number instanceof BigDecimal) {
                return number;
            }
            if(number instanceof BigInteger) {
                return new BigDecimal((BigInteger)number);
            }
            if(number instanceof Long) {
                // Because we can't represent long accurately as a 
                // double
                return new BigDecimal(number.toString());
            }
            return new BigDecimal(number.doubleValue());
        }
        // Handle nonstandard Number subclasses as well as directly 
        // java.lang.Number too
        if(hint.isInstance(number)) {
            return number;
        }
        return null;
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

    public Object newInstance(Class clazz, List arguments)
    throws
        TemplateModelException
    {
        try
        {
            Object ctors = getClassIntrospectionData(clazz).get(CONSTRUCTORS);
            if(ctors == null)
            {
                throw new TemplateModelException("Class " + clazz.getName() + 
                        " has no public constructors.");
            }
            Constructor ctor = null;
            Object[] objargs;
            if(ctors instanceof SimpleMemberModel)
            {
                SimpleMemberModel smm = (SimpleMemberModel)ctors;
                ctor = (Constructor)smm.getMember();
                objargs = smm.unwrapArguments(arguments, this);
            }
            else if(ctors instanceof OverloadedMethods)
            {
                OverloadedMethods overloadedConstructors = (OverloadedMethods) ctors; 
                MemberAndArguments maa = 
                    overloadedConstructors.getMemberAndArguments(arguments);
                objargs = maa.getArgs();
                ctor = (Constructor)maa.getMember();
            }
            else
            {
                // Cannot happen
                throw new Error();
            }
            return ctor.newInstance(objargs);
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
     * Gets the class introspection data from {@link #genericClassIntrospectionCache},
     * automatically creating the genericClassIntrospectionCache entry if it's missing.
     * 
     * @return A {@link Map} where each key is a property/method name, each
     *     value is a {@link MethodDescriptor} or a {@link PropertyDescriptor}
     *     assigned to that property/method.
     */
    Map getClassIntrospectionData(Class clazz) {
        if (isGenericClassIntrospectionCacheConcurrentMap) {
            Map introspData = (Map) genericClassIntrospectionCache.get(clazz);
            if (introspData != null) return introspData;
        }
        
        String className;
        synchronized (sharedClassIntrospectionCacheLock) {
            Map introspData = (Map) genericClassIntrospectionCache.get(clazz);
            if (introspData != null) return introspData;
            
            className = clazz.getName();
            if (genericClassIntrospectionCacheClassNames.contains(className)) {
                onSameNameClassesDetected(className);
            }
            
            while (introspData == null
                    && genericClassIntrospectionsInProgress.contains(clazz)) {
                // Another thread is already introspecting this class;
                // waiting for its result.
                try {
                    sharedClassIntrospectionCacheLock.wait();
                    introspData = (Map) genericClassIntrospectionCache.get(clazz);
                } catch (InterruptedException e) {
                    throw new RuntimeException(
                            "Class inrospection data lookup aborded: " + e);
                }
            }
            if (introspData != null) return introspData;
            
            // This will be the thread that introspects this class.
            genericClassIntrospectionsInProgress.add(clazz);
        }
        try {
            Map introspData = createClassIntrospectionData(clazz);
            synchronized (sharedClassIntrospectionCacheLock) {
                genericClassIntrospectionCache.put(clazz, introspData);
                genericClassIntrospectionCacheClassNames.add(className);
            }
            return introspData;
        } finally {
            synchronized (sharedClassIntrospectionCacheLock) {
                genericClassIntrospectionsInProgress.remove(clazz);
                sharedClassIntrospectionCacheLock.notifyAll();
            }
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
        synchronized (sharedClassIntrospectionCacheLock) {
            removeFromGenericClassIntrospectionCache(clazz);
            staticModels.removeFromCache(clazz);
            if (enumModels != null) enumModels.removeFromCache(clazz);
        }
    }

    /**
     * Removes all class introspection data from the cache.
     * Use this if you want to free up memory on the expense of recreating
     * the cache entries for the classes that will be used later in templates.
     * 
     * @since 2.3.20
     */
    public void clearClassIntrospecitonCache() {
        synchronized (sharedClassIntrospectionCacheLock) {
            clearGenericClassIntrospectionCache();
            staticModels.clearCache();
            if (enumModels != null) enumModels.clearCache();
        }
    }
    
    void onSameNameClassesDetected(String className) {
        // TODO: This behavior should be pluggable, as in environments where
        // some classes are often reloaded or multiple versions of the
        // same class is normal (OSGi), this will drop the cache contents
        // too often. 
        if(logger.isInfoEnabled()) {
            logger.info(
                    "Detected multiple classes with the same name, \"" + className + 
                    "\". Assuming it was a class-reloading. Clearing BeansWrapper " +
                    "caches to release old data.");
        }
        clearClassIntrospecitonCache();
    }
    
    Object getSharedClassIntrospectionCacheLock() {
        return sharedClassIntrospectionCacheLock;
    }

    private void removeFromGenericClassIntrospectionCache(Class clazz) {
        synchronized (sharedClassIntrospectionCacheLock) {
            genericClassIntrospectionCache.remove(clazz);
            genericClassIntrospectionCacheClassNames.remove(clazz.getName());
            modelCache.clearCache();
        }
    }
    
    private void clearGenericClassIntrospectionCache() {
        synchronized (sharedClassIntrospectionCacheLock) {
            genericClassIntrospectionCache.clear();
            genericClassIntrospectionCacheClassNames.clear();
            modelCache.clearCache();
        }
    }
    
    /**
     * Returns the number of introspected methods/properties that should
     * be available via the TemplateHashModel interface. Affected by the
     * {@link #setMethodsShadowItems(boolean)} and {@link
     * #setExposureLevel(int)} settings.
     */
    int keyCount(Class clazz)
    {
        Map map = getClassIntrospectionData(clazz);
        int count = map.size();
        if (map.containsKey(CONSTRUCTORS))
            count--;
        if (map.containsKey(GENERIC_GET_KEY))
            count--;
        if (map.containsKey(ARGTYPES))
            count--;
        return count;
    }

    /**
     * Returns the Set of names of introspected methods/properties that
     * should be available via the TemplateHashModel interface. Affected
     * by the {@link #setMethodsShadowItems(boolean)} and {@link
     * #setExposureLevel(int)} settings.
     */
    Set keySet(Class clazz)
    {
        Set set = new HashSet(getClassIntrospectionData(clazz).keySet());
        set.remove(CONSTRUCTORS);
        set.remove(GENERIC_GET_KEY);
        set.remove(ARGTYPES);
        return set;
    }
    
    /**
     * Populates a map with property and method descriptors for a specified
     * class. If any property or method descriptors specifies a read method
     * that is not accessible, replaces it with appropriate accessible method
     * from a superclass or interface.
     */
    private Map createClassIntrospectionData(Class clazz)
    {
        final Map introspData = new HashMap();

        if (exposeFields) {
            addFieldsToClassIntrospectionData(introspData, clazz);
        }
        
        final Map accessibleMethods = discoverAccessibleMethods(clazz);
        
        addGenericGetToClassIntrospectionData(introspData, accessibleMethods);
        
        if(exposureLevel != EXPOSE_NOTHING) {
            try {
                addBeanInfoToClassInrospectionData(introspData, clazz, accessibleMethods);
            } catch(IntrospectionException e) {
                logger.warn("Couldn't properly perform introspection for class " + 
                        clazz, e);
                introspData.clear();  // FIXME NBC: Don't drop everything here. 
            }
        }
        
        addConstructorsToClassIntrospectionData(introspData, clazz);
        
        if (introspData.size() > 1) {
            return introspData;
        } else if (introspData.size() == 0) {
            return Collections12.EMPTY_MAP;
        } else { // map.size() == 1
            Map.Entry e = (Map.Entry)introspData.entrySet().iterator().next();
            return Collections12.singletonMap(e.getKey(), e.getValue()); 
        }
    }

    private void addFieldsToClassIntrospectionData(Map introspData, Class clazz)
            throws SecurityException {
        Field[] fields = clazz.getFields();
        for (int i = 0; i < fields.length; i++)
        {
            Field field = fields[i];
            if((field.getModifiers() & Modifier.STATIC) == 0)
            {
                introspData.put(field.getName(), field);
            }
        }
    }

    private void addBeanInfoToClassInrospectionData(Map introspData, Class clazz,
            Map accessibleMethods) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
        
        PropertyDescriptor[] pda = beanInfo.getPropertyDescriptors();
        int pdaLength = pda != null ? pda.length : 0;
        for(int i = pdaLength - 1; i >= 0; --i) {
            addPropertyDescriptorToClassIntrospectionData(
                    pda[i], clazz, accessibleMethods,
                    introspData);
        }
        
        if(exposureLevel < EXPOSE_PROPERTIES_ONLY)
        {
            MethodAppearanceDecision decision = new MethodAppearanceDecision();  
            MethodDescriptor[] mda = beanInfo.getMethodDescriptors();
            int mdaLength = mda != null ? mda.length : 0;  
            for(int i = mdaLength - 1; i >= 0; --i)
            {
                MethodDescriptor md = mda[i];
                Method publicMethod = getAccessibleMethod(
                        md.getMethod(), accessibleMethods);
                if(publicMethod != null && isSafeMethod(publicMethod))
                {
                    decision.setDefaults(publicMethod);
                    finetuneMethodAppearance(clazz, publicMethod, decision);
                    
                    PropertyDescriptor propDesc = decision.getExposeAsProperty();
                    if (propDesc != null
                            && !(introspData.get(propDesc.getName())
                                    instanceof PropertyDescriptor))
                    {
                        addPropertyDescriptorToClassIntrospectionData(
                                propDesc, clazz, accessibleMethods,
                                introspData);
                    }
                    
                    String methodKey = decision.getExposeMethodAs();
                    if (methodKey != null)
                    {
                        Object previous = introspData.get(methodKey);
                        if(previous instanceof Method)
                        {
                            // Overloaded method - replace method with a method map
                            OverloadedMethods overloadedMethods = new OverloadedMethods(this);
                            overloadedMethods.addMember((Method)previous);
                            overloadedMethods.addMember(publicMethod);
                            introspData.put(methodKey, overloadedMethods);
                            // remove parameter type information
                            getArgTypes(introspData).remove(previous);
                        }
                        else if(previous instanceof OverloadedMethods)
                        {
                            // Already overloaded method - add new overload
                            ((OverloadedMethods)previous).addMember(publicMethod);
                        }
                        else if (decision.getMethodShadowsProperty()
                                || !(previous instanceof PropertyDescriptor))
                        {
                            // Simple method (this far)
                            introspData.put(methodKey, publicMethod);
                            getArgTypes(introspData).put(publicMethod, 
                                    publicMethod.getParameterTypes());
                        }
                    }
                }
            }
        } // end if(exposureLevel < EXPOSE_PROPERTIES_ONLY)
    }

    private void addPropertyDescriptorToClassIntrospectionData(PropertyDescriptor pd,
            Class clazz, Map accessibleMethods, Map classMap) {
        if(pd instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor ipd = 
                (IndexedPropertyDescriptor)pd;
            Method readMethod = ipd.getIndexedReadMethod();
            Method publicReadMethod = getAccessibleMethod(readMethod, 
                    accessibleMethods);
            if(publicReadMethod != null && isSafeMethod(publicReadMethod)) {
                try {
                    if(readMethod != publicReadMethod) {
                        ipd = new IndexedPropertyDescriptor(
                                ipd.getName(), ipd.getReadMethod(), 
                                null, publicReadMethod, 
                                null);
                    }
                    classMap.put(ipd.getName(), ipd);
                    getArgTypes(classMap).put(publicReadMethod, 
                            publicReadMethod.getParameterTypes());
                }
                catch(IntrospectionException e) {
                    logger.warn("Failed creating a publicly-accessible " +
                            "property descriptor for " + clazz.getName() + 
                            " indexed property " + pd.getName() + 
                            ", read method " + publicReadMethod, 
                            e);
                }
            }
        }
        else {
            Method readMethod = pd.getReadMethod();
            Method publicReadMethod = getAccessibleMethod(readMethod, accessibleMethods);
            if(publicReadMethod != null && isSafeMethod(publicReadMethod)) {
                try {
                    if(readMethod != publicReadMethod) {
                        pd = new PropertyDescriptor(pd.getName(), 
                                publicReadMethod, null);
                        pd.setReadMethod(publicReadMethod);
                    }
                    classMap.put(pd.getName(), pd);
                }
                catch(IntrospectionException e) {
                    logger.warn("Failed creating a publicly-accessible " +
                            "property descriptor for " + clazz.getName() + 
                            " property " + pd.getName() + ", read method " + 
                            publicReadMethod, e);
                }
            }
        }
    }

    private void addGenericGetToClassIntrospectionData(Map introspData,
            Map accessibleMethods) {
        Method genericGet = getFirstAccessibleMethod(
                MethodSignature.GET_STRING_SIGNATURE, accessibleMethods);
        if(genericGet == null)
        {
            genericGet = getFirstAccessibleMethod(
                    MethodSignature.GET_OBJECT_SIGNATURE, accessibleMethods);
        }
        if(genericGet != null)
        {
            introspData.put(GENERIC_GET_KEY, genericGet);
        }
    }
    
    private void addConstructorsToClassIntrospectionData(final Map introspData,
            Class clazz) {
        try
        {
            Constructor[] ctors = clazz.getConstructors();
            if(ctors.length == 1)
            {
                Constructor ctor = ctors[0];
                introspData.put(CONSTRUCTORS, new SimpleMemberModel(ctor, ctor.getParameterTypes()));
            }
            else if(ctors.length > 1)
            {
                OverloadedMethods ctorMap = new OverloadedMethods(this);
                for (int i = 0; i < ctors.length; i++)
                {
                    ctorMap.addMember(ctors[i]);
                }
                introspData.put(CONSTRUCTORS, ctorMap);
            }
        }
        catch(SecurityException e)
        {
            logger.warn("Canont discover constructors for class " + 
                    clazz.getName(), e);
        }
    }

    /**
     * <b>Experimental method; subject to change!</b>
     * Override this to tweak certain aspects of how methods appear in the
     * data-model. {@link BeansWrapper} will pass in all Java methods here that
     * it intends to expose in the data-model as methods (so you can do
     * <tt>obj.foo()</tt> in the template). By default this method does nothing.
     * By overriding it you can do the following tweaks:
     * <ul>
     *   <li>Hide a method that would be otherwise shown by calling
     *     {@link MethodAppearanceDecision#setExposeMethodAs(String)}
     *     with <tt>null</tt> parameter. Note that you can't un-hide methods
     *     that are not public or are considered to by unsafe
     *     (like {@link Object#wait()}) because
     *     {@link #finetuneMethodAppearance} is not called for those.</li>
     *   <li>Show the method with a different name in the data-model than its
     *     real name by calling
     *     {@link MethodAppearanceDecision#setExposeMethodAs(String)}
     *     with non-<tt>null</tt> parameter.
     *   <li>Create a fake JavaBean property for this method by calling
     *     {@link MethodAppearanceDecision#setExposeAsProperty(PropertyDescriptor)}.
     *     For example, if you have <tt>int size()</tt> in a class, but you
     *     want it to be accessed from the templates as <tt>obj.size</tt>,
     *     rather than as <tt>obj.size()</tt>, you can do that with this.
     *     The default is {@code null}, which means that no fake property is
     *     created for the method. You need not and shouldn't set this
     *     to non-<tt>null</tt> for the getter methods of real JavaBean
     *     properties, as those are automatically shown as properties anyway.
     *     The property name in the {@link PropertyDescriptor} can be anything,
     *     but the method (or methods) in it must belong to the class that
     *     is given as the <tt>clazz</tt> parameter or it must be inherited from
     *     that class, or else whatever errors can occur later.
     *     {@link IndexedPropertyDescriptor}-s are supported.
     *     If a real JavaBean property of the same name exists, it won't be
     *     replaced by the fake one. Also if a fake property of the same name
     *     was assigned earlier, it won't be replaced.
     *   <li>Prevent the method to hide a JavaBean property (fake or real) of
     *     the same name by calling
     *     {@link MethodAppearanceDecision#setMethodShadowsProperty(boolean)}
     *     with <tt>false</tt>. The default is <tt>true</tt>, so if you have
     *     both a property and a method called "foo", then in the template
     *     <tt>myObject.foo</tt> will return the method itself instead
     *     of the property value, which is often undesirable.
     * </ul>
     * 
     * <p>Note that you can expose a Java method both as a method and as a
     * JavaBean property on the same time, however you have to chose different
     * names for them to prevent shadowing. 
     * 
     * @param decision Stores how the parameter method will be exposed in the
     *   data-model after {@link #finetuneMethodAppearance} returns.
     *   This is initialized so that it reflects the default
     *   behavior of {@link BeansWrapper}.
     */
    protected void finetuneMethodAppearance(
            Class clazz, Method m, MethodAppearanceDecision decision) {
        // left everything on its default; do nothing
    }

    private static Map getArgTypes(Map classMap) {
        Map argTypes = (Map)classMap.get(ARGTYPES);
        if(argTypes == null) {
            argTypes = new HashMap();
            classMap.put(ARGTYPES, argTypes);
        }
        return argTypes;
    }
    
    static Class[] getArgTypes(Map classMap, AccessibleObject methodOrCtor) {
        return (Class[])((Map)classMap.get(ARGTYPES)).get(methodOrCtor);
    }

    private static Method getFirstAccessibleMethod(MethodSignature sig, Map accessibles)
    {
        List l = (List)accessibles.get(sig);
        if(l == null || l.isEmpty()) {
            return null;
        }
        return (Method)l.iterator().next();
    }

    private static Method getAccessibleMethod(Method m, Map accessibles)
    {
        if(m == null) {
            return null;
        }
        MethodSignature sig = new MethodSignature(m);
        List l = (List)accessibles.get(sig);
        if(l == null) {
            return null;
        }
        for (Iterator iterator = l.iterator(); iterator.hasNext();)
        {
            Method am = (Method) iterator.next();
            if(am.getReturnType() == m.getReturnType()) {
                return am;
            }
        }
        return null;
    }
    
    boolean isSafeMethod(Method method)
    {
        return exposureLevel < EXPOSE_SAFE || !UNSAFE_METHODS.contains(method);
    }
    
    /**
     * Retrieves mapping of methods to accessible methods for a class.
     * In case the class is not public, retrieves methods with same 
     * signature as its public methods from public superclasses and 
     * interfaces (if they exist). Basically upcasts every method to the 
     * nearest accessible method.
     */
    private static Map discoverAccessibleMethods(Class clazz)
    {
        Map map = new HashMap();
        discoverAccessibleMethods(clazz, map);
        return map;
    }
    
    private static void discoverAccessibleMethods(Class clazz, Map map)
    {
        if(Modifier.isPublic(clazz.getModifiers()))
        {
            try
            {
                Method[] methods = clazz.getMethods();
                for(int i = 0; i < methods.length; i++)
                {
                    Method method = methods[i];
                    MethodSignature sig = new MethodSignature(method);
                    // Contrary to intuition, a class can actually have several 
                    // different methods with same signature *but* different
                    // return types. These can't be constructed using Java the
                    // language, as this is illegal on source code level, but 
                    // the compiler can emit synthetic methods as part of 
                    // generic type reification that will have same signature 
                    // yet different return type than an existing explicitly
                    // declared method. Consider:
                    // public interface I<T> { T m(); }
                    // public class C implements I<Integer> { Integer m() { return 42; } }
                    // C.class will have both "Object m()" and "Integer m()" methods.
                    List methodList = (List)map.get(sig);
                    if(methodList == null) {
                        methodList = new LinkedList();
                        map.put(sig, methodList);
                    }
                    methodList.add(method);
                }
                return;
            }
            catch(SecurityException e)
            {
                logger.warn("Could not discover accessible methods of class " + 
                        clazz.getName() + 
                        ", attemping superclasses/interfaces.", e);
                // Fall through and attempt to discover superclass/interface 
                // methods
            }
        }

        Class[] interfaces = clazz.getInterfaces();
        for(int i = 0; i < interfaces.length; i++)
        {
            discoverAccessibleMethods(interfaces[i], map);
        }
        Class superclass = clazz.getSuperclass();
        if(superclass != null)
        {
            discoverAccessibleMethods(superclass, map);
        }
    }

    private static final class MethodSignature
    {
        private static final MethodSignature GET_STRING_SIGNATURE = 
            new MethodSignature("get", new Class[] { STRING_CLASS });
        private static final MethodSignature GET_OBJECT_SIGNATURE = 
            new MethodSignature("get", new Class[] { OBJECT_CLASS });

        private final String name;
        private final Class[] args;
        
        private MethodSignature(String name, Class[] args)
        {
            this.name = name;
            this.args = args;
        }
        
        MethodSignature(Method method)
        {
            this(method.getName(), method.getParameterTypes());
        }
        
        public boolean equals(Object o)
        {
            if(o instanceof MethodSignature)
            {
                MethodSignature ms = (MethodSignature)o;
                return ms.name.equals(name) && Arrays.equals(args, ms.args);
            }
            return false;
        }
        
        public int hashCode()
        {
            return name.hashCode() ^ args.length;
        }
    }
    
    private static final Set createUnsafeMethodsSet()
    {
        Properties props = new Properties();
        InputStream in = BeansWrapper.class.getResourceAsStream("unsafeMethods.txt");
        if(in != null)
        {
            String methodSpec = null;
            try
            {
                try
                {
                    props.load(in);
                }
                finally
                {
                    in.close();
                }
                Set set = new HashSet(props.size() * 4/3, .75f);
                Map primClasses = createPrimitiveClassesMap();
                for (Iterator iterator = props.keySet().iterator(); iterator.hasNext();)
                {
                    methodSpec = (String) iterator.next();
                    try {
                        set.add(parseMethodSpec(methodSpec, primClasses));
                    }
                    catch(ClassNotFoundException e) {
                        if(DEVELOPMENT) {
                            throw e;
                        }
                    }
                    catch(NoSuchMethodException e) {
                        if(DEVELOPMENT) {
                            throw e;
                        }
                    }
                }
                return set;
            }
            catch(Exception e)
            {
                throw new RuntimeException("Could not load unsafe method " + methodSpec + " " + e.getClass().getName() + " " + e.getMessage());
            }
        }
        return Collections.EMPTY_SET;
    }
                                                                           
    private static Method parseMethodSpec(String methodSpec, Map primClasses)
    throws
        ClassNotFoundException,
        NoSuchMethodException
    {
        int brace = methodSpec.indexOf('(');
        int dot = methodSpec.lastIndexOf('.', brace);
        Class clazz = ClassUtil.forName(methodSpec.substring(0, dot));
        String methodName = methodSpec.substring(dot + 1, brace);
        String argSpec = methodSpec.substring(brace + 1, methodSpec.length() - 1);
        StringTokenizer tok = new StringTokenizer(argSpec, ",");
        int argcount = tok.countTokens();
        Class[] argTypes = new Class[argcount];
        for (int i = 0; i < argcount; i++)
        {
            String argClassName = tok.nextToken();
            argTypes[i] = (Class)primClasses.get(argClassName);
            if(argTypes[i] == null)
            {
                argTypes[i] = ClassUtil.forName(argClassName);
            }
        }
        return clazz.getMethod(methodName, argTypes);
    }

    private static Map createPrimitiveClassesMap()
    {
        Map map = new HashMap();
        map.put("boolean", Boolean.TYPE);
        map.put("byte", Byte.TYPE);
        map.put("char", Character.TYPE);
        map.put("short", Short.TYPE);
        map.put("int", Integer.TYPE);
        map.put("long", Long.TYPE);
        map.put("float", Float.TYPE);
        map.put("double", Double.TYPE);
        return map;
    }


    /**
     * Converts any {@link BigDecimal}s in the passed array to the type of
     * the corresponding formal argument of the method.
     */
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
        }
        return bd;
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

    
    private static boolean isJavaRebelAvailable() {
        try {
            JavaRebelIntegration.testAvailability();
            return true;
        }
        catch(NoClassDefFoundError e) {
            return false;
        }
    }
    
    /**
     * <b>Experimental class; subject to change!</b>
     * Used for {@link #finetuneMethodAppearance} as output parameter; see there.
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

}
