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

package freemarker.ext.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Map;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelAdapter;

/**
 * Internally used by various wrapper implementations to implement model
 * caching.
 */
public abstract class ModelCache
{
    private boolean useCache = false;
    private Map modelCache = null;
    private ReferenceQueue refQueue = null;
    
    protected ModelCache()
    {
    }
    
    /**
     * Sets whether this wrapper caches model instances. Default is false.
     * When set to true, calling {@link #getInstance(Object)} 
     * multiple times for the same object will return the same model.
     */
    public synchronized void setUseCache(boolean useCache)
    {
        this.useCache = useCache;
        if(useCache)
        {
            modelCache = new IdentityHashMap();
            refQueue = new ReferenceQueue();
        }
        else
        {
            modelCache = null;
            refQueue = null;
        }
    }

    /**
     * @since 2.3.21
     */
    public synchronized boolean getUseCache() {
        return useCache;
    }
    
    public TemplateModel getInstance(Object object)
    {
        if(object instanceof TemplateModel) {
            return (TemplateModel)object;
        }
        if(object instanceof TemplateModelAdapter) {
            return ((TemplateModelAdapter)object).getTemplateModel();
        }
        if(useCache && isCacheable(object)) {
            TemplateModel model = lookup(object);
            if(model == null) {
                model = create(object);
                register(model, object);
            }
            return model;
        }
        else {
            return create(object);
        }
    }
    
    protected abstract TemplateModel create(Object object);
    protected abstract boolean isCacheable(Object object);
    
    public void clearCache()
    {
        if(modelCache != null)
        {
            synchronized(modelCache)
            {
                modelCache.clear();
            }
        }
    }

    private final TemplateModel lookup(Object object)
    {
        ModelReference ref = null;
        // NOTE: we're doing minimal synchronizations -- which can lead to
        // duplicate wrapper creation. However, this has no harmful side-effects and
        // is a lesser performance hit.
        synchronized (modelCache)
        {
            ref = (ModelReference) modelCache.get(object);
        }

        if (ref != null)
            return ref.getModel();

        return null;
    }

    private final void register(TemplateModel model, Object object)
    {
        synchronized (modelCache) {
            // Remove cleared references
            for (;;) {
                ModelReference queuedRef = (ModelReference) refQueue.poll();
                if (queuedRef == null)
                    break;
                modelCache.remove(queuedRef.object);
            }
            // Register new reference
            modelCache.put(object, new ModelReference(model, object, refQueue));
        }
    }

    /**
     * A special soft reference that is registered in the modelCache.
     * When it gets cleared (that is, the model became unreachable)
     * it will remove itself from the model cache.
     */
    private static final class ModelReference extends SoftReference
    {
        Object object;

        ModelReference(TemplateModel ref, Object object, ReferenceQueue refQueue)
        {
            super(ref, refQueue);
            this.object = object;
        }

        TemplateModel getModel()
        {
            return (TemplateModel) this.get();
        }
    }

}