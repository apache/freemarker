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
    
    private Object modifyForIcI(Object source) {
        // TODO Auto-generated method stub
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
    static final class MultiSource
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
        
        Object getWrappedSource() {
            return source;
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
    
    /**
     * Show class name and some details that are useful in template-not-found errors.
     * 
     * @since 2.3.21
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("MultiTemplateLoader(");
        for (int i = 0; i < loaders.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("loader").append(i + 1).append(" = ").append(loaders[i]);
        }
        sb.append(")");
        return sb.toString();
    }
    
}
