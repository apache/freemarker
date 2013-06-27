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
 * @author Attila Szegedi
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
        
        final Object sharedLock = wrapper.getSharedClassIntrospectionCacheLock();
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
        }
        try {
            final Class clazz = ClassUtil.forName(key);
            
            // This is called so that we trigger the
            // class-reloading detector. If clazz is a reloaded class,
            // the wrapper will in turn call our clearCache method.
            // TODO: Why do we check it now and only now?
            wrapper.getClassIntrospectionData(clazz);
            
            TemplateModel model = createModel(clazz);
            // Warning: model will be null if the class is not good for the subclass.
            // For example, EnumModels#createModel returns null if clazz is not an enum.
            
            if (model != null) {
                synchronized (sharedLock) {
                    cache.put(key, model);
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
        synchronized(wrapper.getSharedClassIntrospectionCacheLock()) {
            cache.clear();
        }
    }
    
    void removeFromCache(Class clazz) {
        synchronized(wrapper.getSharedClassIntrospectionCacheLock()) {
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
