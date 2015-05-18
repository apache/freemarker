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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import freemarker.core._ConcurrentMapFactory;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;

/**
 * Base class for hash models keyed by Java class names. 
 */
abstract class ClassBasedModelFactory implements TemplateHashModel {
    private final BeansWrapper wrapper;
    
    private final Map/*<String,TemplateModel>*/ cache
            = _ConcurrentMapFactory.newMaybeConcurrentHashMap();
    private final boolean isCacheConcurrentMap
            = _ConcurrentMapFactory.isConcurrent(cache);
    private final Set classIntrospectionsInProgress = new HashSet();
    
    protected ClassBasedModelFactory(BeansWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        try {
            return getInternal(key);
        } catch(Exception e) {
            if (e instanceof TemplateModelException) {
                throw (TemplateModelException) e;
            } else {
                throw new TemplateModelException(e);
            }
        }
    }

    private TemplateModel getInternal(String key) throws TemplateModelException, ClassNotFoundException {
        if (isCacheConcurrentMap) {
            TemplateModel model = (TemplateModel) cache.get(key);
            if (model != null) return model;
        }

        final ClassIntrospector classIntrospector;
        int classIntrospectorClearingCounter;
        final Object sharedLock = wrapper.getSharedIntrospectionLock();
        synchronized (sharedLock) {
            TemplateModel model = (TemplateModel) cache.get(key);
            if (model != null) return model;
            
            while (model == null
                    && classIntrospectionsInProgress.contains(key)) {
                // Another thread is already introspecting this class;
                // waiting for its result.
                try {
                    sharedLock.wait();
                    model = (TemplateModel) cache.get(key);
                } catch (InterruptedException e) {
                    throw new RuntimeException(
                            "Class inrospection data lookup aborded: " + e);
                }
            }
            if (model != null) return model;
            
            // This will be the thread that introspects this class.
            classIntrospectionsInProgress.add(key);

            // While the classIntrospector should not be changed from another thread, badly written apps can do that,
            // and it's cheap to get the classIntrospector from inside the lock here:   
            classIntrospector = wrapper.getClassIntrospector();
            classIntrospectorClearingCounter = classIntrospector.getClearingCounter();
        }
        try {
            final Class clazz = ClassUtil.forName(key);
            
            // This is called so that we trigger the
            // class-reloading detector. If clazz is a reloaded class,
            // the wrapper will in turn call our clearCache method.
            // TODO: Why do we check it now and only now?
            classIntrospector.get(clazz);
            
            TemplateModel model = createModel(clazz);
            // Warning: model will be null if the class is not good for the subclass.
            // For example, EnumModels#createModel returns null if clazz is not an enum.
            
            if (model != null) {
                synchronized (sharedLock) {
                    // Save it into the cache, but only if nothing relevant has changed while we were outside the lock: 
                    if (classIntrospector == wrapper.getClassIntrospector()
                            && classIntrospectorClearingCounter == classIntrospector.getClearingCounter()) {  
                        cache.put(key, model);
                    }
                }
            }
            return model;
        } finally {
            synchronized (sharedLock) {
                classIntrospectionsInProgress.remove(key);
                sharedLock.notifyAll();
            }
        }
    }
    
    void clearCache() {
        synchronized(wrapper.getSharedIntrospectionLock()) {
            cache.clear();
        }
    }
    
    void removeFromCache(Class clazz) {
        synchronized(wrapper.getSharedIntrospectionLock()) {
            cache.remove(clazz.getName());
        }
    }

    public boolean isEmpty() {
        return false;
    }
    
    protected abstract TemplateModel createModel(Class clazz) 
    throws TemplateModelException;
    
    protected BeansWrapper getWrapper() {
        return wrapper;
    }
    
}
