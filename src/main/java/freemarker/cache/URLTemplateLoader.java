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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

/**
 * This is an abstract template loader that can load templates whose
 * location can be described by an URL. Subclasses only need to override
 * the {@link #getURL(String)} method. Both {@link ClassTemplateLoader} and
 * {@link WebappTemplateLoader} are (quite trivial) subclasses of this class.
 * @author Attila Szegedi
 */
public abstract class URLTemplateLoader implements TemplateLoader
{
    public Object findTemplateSource(String name)
    throws
    	IOException
    {
        URL url = getURL(name);
        return url == null ? null : new URLTemplateSource(url);
    }
    
    /**
     * Given a template name (plus potential locale decorations) retrieves
     * an URL that points the template source.
     * @param name the name of the sought template, including the locale
     * decorations.
     * @return an URL that points to the template source, or null if it can
     * determine that the template source does not exist.
     */
    protected abstract URL getURL(String name);
    
    public long getLastModified(Object templateSource)
    {
        return ((URLTemplateSource) templateSource).lastModified();
    }
    
    public Reader getReader(Object templateSource, String encoding)
    throws
        IOException
    {
        return new InputStreamReader(
                ((URLTemplateSource) templateSource).getInputStream(),
                encoding);
    }
    
    public void closeTemplateSource(Object templateSource)
    throws
    	IOException
    {
        ((URLTemplateSource) templateSource).close();
    }

    /**
     * Can be used by subclasses to canonicalize URL path prefixes.
     * @param prefix the path prefix to canonicalize
     * @return the canonicalized prefix. All backslashes are replaced with
     * forward slashes, and a trailing slash is appended if the original
     * prefix wasn't empty and didn't already end with a slash.
     */
    protected static String canonicalizePrefix(String prefix)
    {
        // make it foolproof
        prefix = prefix.replace('\\', '/');
        // ensure there's a trailing slash
        if (prefix.length() > 0 && !prefix.endsWith("/"))
        {
            prefix += "/";
        }
        return prefix;
    }
}
