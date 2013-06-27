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

package freemarker.cache;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link TemplateLoader} that uses a set of other loaders to load the templates.
 * On every request, loaders are queried in the order of their appearance in the
 * array of loaders provided to the constructor. However, if a request for some template
 * name was already satisfied in the past by one of the loaders, that Loader is queried 
 * first (a soft affinity).
 * 
 * <p>This class is <em>not</em> thread-safe. If it's accessed from multiple
 * threads concurrently, proper synchronization must be provided by the callers.
 * Note that {@link TemplateCache}, the natural user of this class, provides the
 * necessary synchronizations when it uses this class, so then you don't have to worry
 * this.
 * 
 * @author Attila Szegedi, szegedia at freemail dot hu
 */
public class MultiTemplateLoader implements StatefulTemplateLoader
{
    private final TemplateLoader[] loaders;
    private final Map lastLoaderForName = Collections.synchronizedMap(new HashMap());
    
    /**
     * Creates a new multi template Loader that will use the specified loaders.
     * @param loaders the loaders that are used to load templates. 
     */
    public MultiTemplateLoader(TemplateLoader[] loaders)
    {
        this.loaders = (TemplateLoader[])loaders.clone();
    }
    
    public Object findTemplateSource(String name)
    throws
    	IOException
    {
        // Use soft affinity - give the loader that last found this
        // resource a chance to find it again first.
        TemplateLoader lastLoader = (TemplateLoader)lastLoaderForName.get(name);
        if(lastLoader != null)
        {
            Object source = lastLoader.findTemplateSource(name);
            if(source != null)
            {
                return new MultiSource(source, lastLoader);
            }
        }
        
        // If there is no affine loader, or it could not find the resource
        // again, try all loaders in order of appearance. If any manages
        // to find the resource, then associate it as the new affine loader 
        // for this resource.
        for(int i = 0; i < loaders.length; ++i)
        {
            TemplateLoader loader = loaders[i];
            Object source = loader.findTemplateSource(name);
            if(source != null)
            {
                lastLoaderForName.put(name, loader);
                return new MultiSource(source, loader);
            }
        }
        
        lastLoaderForName.remove(name);
        // Resource not found
        return null;
    }
    
    public long getLastModified(Object templateSource)
    {
        return ((MultiSource)templateSource).getLastModified();
    }
    
    public Reader getReader(Object templateSource, String encoding)
    throws
        IOException
    {
        return ((MultiSource)templateSource).getReader(encoding);
    }
    
    public void closeTemplateSource(Object templateSource)
    throws
        IOException
    {
        ((MultiSource)templateSource).close();
    }

    public void resetState()
    {
        lastLoaderForName.clear();
        for (int i = 0; i < loaders.length; i++) {
            TemplateLoader loader = loaders[i];
            if(loader instanceof StatefulTemplateLoader) {
                ((StatefulTemplateLoader)loader).resetState();
            }
        }
    }
    
    /**
     * Represents a template source bound to a specific template loader. It
     * serves as the complete template source descriptor used by the
     * MultiTemplateLoader class.
     */
    private static final class MultiSource
    {
        private final Object source;
        private final TemplateLoader loader;
        
        MultiSource(Object source, TemplateLoader loader)
        {
            this.source = source;
            this.loader = loader;
        }
        
        long getLastModified()
        {
            return loader.getLastModified(source);
        }
        
        Reader getReader(String encoding)
        throws
            IOException
        {
            return loader.getReader(source, encoding);
        }
        
        void close()
        throws
            IOException
        {
            loader.closeTemplateSource(source);
        }
        
        public boolean equals(Object o) {
            if(o instanceof MultiSource) {
                MultiSource m = (MultiSource)o;
                return m.loader.equals(loader) && m.source.equals(source);
            }
            return false;
        }
        
        public int hashCode() {
            return loader.hashCode() + 31 * source.hashCode();
        }
        
        public String toString() {
            return source.toString();
        }
    }
}
