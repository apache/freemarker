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

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.core.CollectionAndSequence;
import freemarker.core._DelayedFTLTypeDescription;
import freemarker.core._DelayedJQuote;
import freemarker.core._TemplateModelException;
import freemarker.ext.util.ModelFactory;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.log.Logger;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateModelWithAPISupport;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.StringUtil;

/**
 * A class that will wrap an arbitrary object into {@link freemarker.template.TemplateHashModel}
 * interface allowing calls to arbitrary property getters and invocation of
 * accessible methods on the object from a template using the
 * <tt>object.foo</tt> to access properties and <tt>object.bar(arg1, arg2)</tt> to
 * invoke methods on it. You can also use the <tt>object.foo[index]</tt> syntax to
 * access indexed properties. It uses Beans {@link java.beans.Introspector}
 * to dynamically discover the properties and methods. 
 */

public class BeanModel
implements
    TemplateHashModelEx, AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport
{
    private static final Logger LOG = Logger.getLogger("freemarker.beans");
    protected final Object object;
    protected final BeansWrapper wrapper;
    
    // We use this to represent an unknown value as opposed to known value of null (JR)
    static final TemplateModel UNKNOWN = new SimpleScalar("UNKNOWN");
    
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            public TemplateModel create(Object object, ObjectWrapper wrapper)
            {
                return new BeanModel(object, (BeansWrapper)wrapper);
            }
        };

    // Cached template models that implement member properties and methods for this
    // instance. Keys are FeatureDescriptor instances (from classCache values),
    // values are either ReflectionMethodModels/ReflectionScalarModels
    private HashMap memberMap;

    /**
     * Creates a new model that wraps the specified object. Note that there are
     * specialized subclasses of this class for wrapping arrays, collections,
     * enumeration, iterators, and maps. Note also that the superclass can be
     * used to wrap String objects if only scalar functionality is needed. You
     * can also choose to delegate the choice over which model class is used for
     * wrapping to {@link BeansWrapper#wrap(Object)}.
     * @param object the object to wrap into a model.
     * @param wrapper the {@link BeansWrapper} associated with this model.
     * Every model has to have an associated {@link BeansWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public BeanModel(Object object, BeansWrapper wrapper)
    {
        // [2.4]: All models were introspected here, then the results was discareded, and get() will just do the
        // introspection again. So is this necessary? (The inrospectNow parameter was added in 2.3.21 to allow
        // lazy-introspecting BeansWrapper.trueModel|falseModel.)
        this(object, wrapper, true);
    }

    /** @since 2.3.21 */
    BeanModel(Object object, BeansWrapper wrapper, boolean inrospectNow)
    {
        this.object = object;
        this.wrapper = wrapper;
        if (inrospectNow && object != null) {
            // [2.4]: Could this be removed?
            wrapper.getClassIntrospector().get(object.getClass());
        }
    }
    
    /**
     * Uses Beans introspection to locate a property or method with name
     * matching the key name. If a method or property is found, it's wrapped
     * into {@link freemarker.template.TemplateMethodModelEx} (for a method or
     * indexed property), or evaluated on-the-fly and the return value wrapped
     * into appropriate model (for a simple property) Models for various
     * properties and methods are cached on a per-class basis, so the costly
     * introspection is performed only once per property or method of a class.
     * (Side-note: this also implies that any class whose method has been called
     * will be strongly referred to by the framework and will not become
     * unloadable until this class has been unloaded first. Normally this is not
     * an issue, but can be in a rare scenario where you create many classes on-
     * the-fly. Also, as the cache grows with new classes and methods introduced
     * to the framework, it may appear as if it were leaking memory. The
     * framework does, however detect class reloads (if you happen to be in an
     * environment that does this kind of things--servlet containers do it when
     * they reload a web application) and flushes the cache. If no method or
     * property matching the key is found, the framework will try to invoke
     * methods with signature
     * <tt>non-void-return-type get(java.lang.String)</tt>,
     * then <tt>non-void-return-type get(java.lang.Object)</tt>, or 
     * alternatively (if the wrapped object is a resource bundle) 
     * <tt>Object getObject(java.lang.String)</tt>.
     * @throws TemplateModelException if there was no property nor method nor
     * a generic <tt>get</tt> method to invoke.
     */
    public TemplateModel get(String key)
        throws
        TemplateModelException
    {
        Class clazz = object.getClass();
        Map classInfo = wrapper.getClassIntrospector().get(clazz);
        TemplateModel retval = null;
        
        try
        {
            if (wrapper.isMethodsShadowItems())
            {
                Object fd = classInfo.get(key);
                if(fd != null)
                {
                    retval = invokeThroughDescriptor(fd, classInfo);
                } else {
                    retval = invokeGenericGet(classInfo, clazz, key);
                }
            }
            else
            {
                TemplateModel model = invokeGenericGet(classInfo, clazz, key);
                final TemplateModel nullModel = wrapper.wrap(null);
                if(model != nullModel && model != UNKNOWN) 
                {
                    return model;
                }
                Object fd = classInfo.get(key);
                if(fd != null) {
                    retval = invokeThroughDescriptor(fd, classInfo);
                    if (retval == UNKNOWN && model == nullModel) {
                        // This is the (somewhat subtle) case where the generic get() returns null
                        // and we have no bean info, so we respect the fact that
                        // the generic get() returns null and return null. (JR)
                        retval = nullModel;
                    }
                }
            }
            if (retval == UNKNOWN) {
                if (wrapper.isStrict()) {
                    throw new InvalidPropertyException("No such bean property: " + key);
                } else if (LOG.isDebugEnabled()) {
                    logNoSuchKey(key, classInfo);
                }
                retval = wrapper.wrap(null);
            }
            return retval;
        }
        catch(TemplateModelException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            throw new _TemplateModelException(e, new Object [] {
                    "An error has occurred when reading existing sub-variable ", new _DelayedJQuote(key),
                    "; see cause exception! The type of the containing value was: ",
                    new _DelayedFTLTypeDescription(this)
            });
        }
    }

    private void logNoSuchKey(String key, Map keyMap)
    {
        LOG.debug("Key " + StringUtil.jQuoteNoXSS(key) + " was not found on instance of " + 
            object.getClass().getName() + ". Introspection information for " +
            "the class is: " + keyMap);
    }
    
    /**
     * Whether the model has a plain get(String) or get(Object) method
     */
    
    protected boolean hasPlainGetMethod() {
        return wrapper.getClassIntrospector().get(object.getClass()).get(ClassIntrospector.GENERIC_GET_KEY) != null;
    }
    
    private TemplateModel invokeThroughDescriptor(Object desc, Map classInfo)
        throws
        IllegalAccessException,
        InvocationTargetException,
        TemplateModelException
    {
        // See if this particular instance has a cached implementation
        // for the requested feature descriptor
        TemplateModel member;
        synchronized(this) {
            if(memberMap != null) {
                member = (TemplateModel)memberMap.get(desc);
            }
            else {
                member = null;
            }
        }

        if(member != null)
            return member;

        TemplateModel retval = UNKNOWN;
        if(desc instanceof IndexedPropertyDescriptor)
        {
            Method readMethod = 
                ((IndexedPropertyDescriptor)desc).getIndexedReadMethod(); 
            retval = member = 
                new SimpleMethodModel(object, readMethod, 
                        ClassIntrospector.getArgTypes(classInfo, readMethod), wrapper);
        }
        else if(desc instanceof PropertyDescriptor)
        {
            PropertyDescriptor pd = (PropertyDescriptor)desc;
            retval = wrapper.invokeMethod(object, pd.getReadMethod(), null);
            // (member == null) condition remains, as we don't cache these
        }
        else if(desc instanceof Field)
        {
            retval = wrapper.wrap(((Field)desc).get(object));
            // (member == null) condition remains, as we don't cache these
        }
        else if(desc instanceof Method)
        {
            Method method = (Method)desc;
            retval = member = new SimpleMethodModel(object, method, 
                    ClassIntrospector.getArgTypes(classInfo, method), wrapper);
        }
        else if(desc instanceof OverloadedMethods)
        {
            retval = member = 
                new OverloadedMethodsModel(object, (OverloadedMethods) desc, wrapper);
        }
        
        // If new cacheable member was created, cache it
        if(member != null) {
            synchronized(this) {
                if(memberMap == null) {
                    memberMap = new HashMap();
                }
                memberMap.put(desc, member);
            }
        }
        return retval;
    }
    
    void clearMemberCache() {
        synchronized(this) {
            memberMap = null;
        }
    }

    protected TemplateModel invokeGenericGet(Map keyMap, Class clazz, String key)
    throws
        IllegalAccessException,
        InvocationTargetException,
        TemplateModelException
    {
        Method genericGet = (Method)keyMap.get(ClassIntrospector.GENERIC_GET_KEY);
        if(genericGet == null)
            return UNKNOWN;

        return wrapper.invokeMethod(object, genericGet, new Object[] { key });
    }

    protected TemplateModel wrap(Object obj)
    throws TemplateModelException
    {
        return wrapper.getOuterIdentity().wrap(obj);
    }
    
    protected Object unwrap(TemplateModel model)
    throws
        TemplateModelException
    {
        return wrapper.unwrap(model);
    }

    /**
     * Tells whether the model is empty. It is empty if either the wrapped 
     * object is null, or it's a Boolean with false value.
     */
    public boolean isEmpty()
    {
        if (object instanceof String) {
            return ((String) object).length() == 0;
        }
        if (object instanceof Collection) {
            return ((Collection) object).isEmpty();
        }
        if (object instanceof Map) {
            return ((Map) object).isEmpty();
        }
        return object == null || Boolean.FALSE.equals(object);
    }
    
    /**
     * Returns the same as {@link #getWrappedObject()}; to ensure that, this method will be final starting from 2.4.
     * This behavior of {@link BeanModel} is assumed by some FreeMarker code. 
     */
    public Object getAdaptedObject(Class hint) {
        return object;  // return getWrappedObject(); starting from 2.4
    }

    public Object getWrappedObject() {
        return object;
    }
    
    public int size()
    {
        return wrapper.getClassIntrospector().keyCount(object.getClass());
    }

    public TemplateCollectionModel keys()
    {
        return new CollectionAndSequence(new SimpleSequence(keySet(), wrapper));
    }

    public TemplateCollectionModel values() throws TemplateModelException
    {
        List values = new ArrayList(size());
        TemplateModelIterator it = keys().iterator();
        while (it.hasNext()) {
            String key = ((TemplateScalarModel)it.next()).getAsString();
            values.add(get(key));
        }
        return new CollectionAndSequence(new SimpleSequence(values, wrapper));
    }
    
    /**
     * Used for {@code classic_compatbile} mode; don't use it for anything else.
     * In FreeMarker 1.7 (and also at least in 2.1) {@link BeanModel} was a {@link TemplateScalarModel}. Some internal
     * FreeMarker code tries to emulate FreeMarker classic by calling this method when a {@link TemplateScalarModel} is
     * expected.
     */
    String getAsClassicCompatibleString() {
        return object == null ? "null" : object.toString();        
    }
    
    public String toString() {
        return object.toString();
    }

    /**
     * Helper method to support TemplateHashModelEx. Returns the Set of
     * Strings which are available via the TemplateHashModel
     * interface. Subclasses that override <tt>invokeGenericGet</tt> to
     * provide additional hash keys should also override this method.
     */
    protected Set keySet()
    {
        return wrapper.getClassIntrospector().keySet(object.getClass());
    }

    public TemplateModel getAPI() throws TemplateModelException {
        return wrapper.wrapAsAPI(object);
    }
    
}