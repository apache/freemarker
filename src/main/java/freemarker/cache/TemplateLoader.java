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

import freemarker.template.Configuration;
import freemarker.template.TemplateNotFoundException;

/**
 * FreeMarker loads template "files" through objects that implement this interface,
 * thus the templates need not be real files, and can come from any kind of data source
 * (like classpath, servlet context, database, etc). While FreeMarker provides a few
 * {@link TemplateLoader} implementations out-of-the-box, it's normal for embedding
 * frameworks to use their own implementations.
 * 
 * <p>To set the {@link TemplateLoader} used by FreeMaker, use
 * {@link Configuration#setTemplateLoader(TemplateLoader)}.
 * 
 * <p>Implementations of this interface should be thread-safe.
 * 
 * <p>Implementations should override {@link Object#toString()} to show information about from where the
 * {@link TemplateLoader} loads the templates. For example, for a template loader that loads template from database
 * table {@code toString} could return something like
 * {@code "MyDatabaseTemplateLoader(user=\"cms\", table=\"mail_templates\")"}. This string will be shown in
 * {@link TemplateNotFoundException} exception messages, next to the template name.
 * 
 * <p>For those who has to dig deeper, note that the {@link TemplateLoader} is actually stored inside
 * the {@link TemplateCache} of the {@link Configuration}, and is normally only accessed directly
 * by the {@link TemplateCache}, and templates are get via the {@link TemplateCache} API-s.
 */
public interface TemplateLoader {
	
    /**
     * Finds the template in the backing storage and returns an object that identifies the storage location where the
     * template can be loaded from. See the return value for more information.
     *
     * @param name
     *            The name of the template, already localized and normalized by the
     *            {@link freemarker.cache.TemplateCache cache}. It is completely up to the loader implementation to
     *            interpret the name, however it should expect to receive hierarchical paths where path components are
     *            separated by a slash (not backslash). Backslashes (or any other OS specific separator character) are
     *            not considered as separators by FreeMarker, and thus they will not be replaced with slash before
     *            passing to this method, so it's up to the template loader to handle them (say, be throwing and
     *            exception that tells the user that the path (s)he has entered is invalid, as (s)he must use slash --
     *            typical mistake of Windows users). The passed names are always considered relative to some
     *            loader-defined root location (often referred as the "template root directory"), and will never start
     *            with a slash, nor will they contain a path component consisting of either a single or a double dot --
     *            these are all resolved by the template cache before passing the name to the loader. As a side effect,
     *            paths that trivially reach outside template root directory, such as <tt>../my.ftl</tt>, will be
     *            rejected by the template cache, so they never reach the template loader. Note again, that if the path
     *            uses backslash as path separator instead of slash as (the template loader should not accept that), the
     *            normalization will not properly happen, as FreeMarker (the cache) recognizes only the slashes as
     *            separators.
     *
     * @return An object representing the template source, which can be supplied in subsequent calls to
     *         {@link #getLastModified(Object)} and {@link #getReader(Object, String)}, when those are called on the
     *         same {@link TemplateLoader}. {@code null} must be returned if the source for the template doesn't exist;
     *         don't throw exception then! The exact type of this object is up to the {@link TemplateLoader}
     *         implementation. As this object is possibly used as hash key in caches, and is surly compared with another
     *         template source for equality, <b>it must have a proper {@link Object#equals(Object)} and
     *         {@link Object#hashCode()}) implementation</b>. Especially, template sources that refer to the same
     *         physical source must be equivalent, otherwise template caching can become inefficient. This is only
     *         expected from {@link Object#equals(Object)} when the compared template sources came from the same
     *         {@link TemplateLoader} instance. Also, it must not influence the equality if the source is open or
     *         closed ({@link #closeTemplateSource(Object)}). 
     * 
     * @throws IOException
     *             When an error occurs that makes it impossible to find out if the template exists, or to access the
     *             existing template. Don't throw exception if the template doesn't exist, instead return with
     *             {@code null} then!
     */
    public Object findTemplateSource(String name)
    throws
    	IOException;
        
    /**
     * Returns the time of last modification of the specified template source.
     * This method is called after <code>findTemplateSource()</code>.
     * @param templateSource an object representing a template source, obtained
     * through a prior call to {@link #findTemplateSource(String)}.
     * @return the time of last modification of the specified template source,
     * or -1 if the time is not known.
     */
    public long getLastModified(Object templateSource);
    
    /**
     * Returns the character stream of a template represented by the specified
     * template source. This method is possibly called for multiple times for the
     * same template source object, and it must always return a {@link Reader} that
     * reads the template from its beginning. Before this method is called for the
     * second time (or later), its caller must close the previously returned
     * {@link Reader}, and it must not use it anymore. That is, this method is not
     * required to support multiple concurrent readers for the same source
     * {@code templateSource} object.
     *  
     * <p>Typically, this method is called if the template is missing from the cache,
     * or if after calling {@link #findTemplateSource(String)} and {@link #getLastModified(Object)}
     * it was determined that the cached copy of the template is stale. Then, if it turns out that the
     * {@code encoding} parameter passed doesn't match the actual template content, this method will be called for a
     * second time with the correct {@code encoding} parameter value.
     *  
     * @param templateSource an object representing a template source, obtained
     * through a prior call to {@link #findTemplateSource(String)}.
     * @param encoding the character encoding used to translate source bytes
     * to characters. Some loaders may not have access to the byte
     * representation of the template stream, and instead directly obtain a 
     * character stream. These loaders should ignore the encoding parameter.
     * @return a reader representing the template character stream. It's
     * the responsibility of the caller ({@link TemplateCache} usually) to
     * {@code close()} it.
     * @throws IOException if an I/O error occurs while accessing the stream.
     */
    public Reader getReader(Object templateSource, String encoding)
    throws
        IOException;
    
    /**
     * Closes the template source. This is the last method that is called by the {@link TemplateCache} for a template
     * source, except that {@link Object#equals(Object)} is might called later too. {@link TemplateCache} ensures that
     * this method will be called on every object that is returned from {@link #findTemplateSource(String)}.
     * 
     * @param templateSource
     *            the template source that should be closed.
     */
    public void closeTemplateSource(Object templateSource)
    throws
        IOException;
}
