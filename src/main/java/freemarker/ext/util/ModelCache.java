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

package freemarker.ext.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Map;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelAdapter;

/**
 * Internally used by various wrapper implementations to implement model
 * caching.
 * @author Attila Szegedi
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