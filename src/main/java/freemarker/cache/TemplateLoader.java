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

/**
 * A template loader is an object that can find the source stream for a 
 * template, can retrieve its time of last modification as well as the stream
 * itself. A template loader is plugged into the {@link TemplateCache} to
 * provide concrete loading of the templates.
 * The implementations can be coded in a non-threadsafe manner as the natural
 * user of the template loader, {@link TemplateCache} does the necessary
 * synchronization.
 * @author Attila Szegedi, szegedia at freemail dot hu
 * @version $Id: TemplateLoader.java,v 1.18 2004/03/01 01:13:30 ddekany Exp $
 */
public interface TemplateLoader
{
    /**
     * Finds the object that acts as the source of the template with the
     * given name. This method is called by the TemplateCache when a template
     * is requested, before calling either {@link #getLastModified(Object)} or
     * {@link #getReader(Object, String)}.
     *
     * @param name the name of the template, already localized and normalized by
     * the {@link freemarker.cache.TemplateCache cache}.
     * It is completely up to the loader implementation to interpret
     * the name, however it should expect to receive hierarchical paths where
     * path components are separated by a slash (not backslash). Backslashes
     * (or any other OS specific separator character) are not considered as separators by
     * FreeMarker, and thus they will not be replaced with slash before passing to this method,
     * so it is up to the template loader to handle them (say, be throwing and exception that
     * tells the user that the path (s)he has entered is invalid, as (s)he must use slash --
     * typical mistake of Windows users).
     * The passed names are always considered relative to some loader-defined root
     * location (often reffered as the "template root direcotry"), and will never start with
     * a slash, nor will they contain a path component consisting of either a single or a double
     * dot -- these are all resolved by the template cache before passing the name to the
     * loader. As a side effect, paths that trivially reach outside template root directory,
     * such as <tt>../my.ftl</tt>, will be rejected by the template cache, so they never
     * reach the template loader. Note again, that if the path uses backslash as path separator
     * instead of slash as (the template loader should not accept that), the normalisation will
     * not properly happen, as FreeMarker (the cache) recognizes only the slashes as separators.
     *
     * @return an object representing the template source, which can be
     * supplied in subsequent calls to {@link #getLastModified(Object)} and
     * {@link #getReader(Object, String)}. Null must be returned if the source
     * for the template can not be found (do not throw <code>FileNotFoundException</code>!).
     * The returned object may will be compared with a cached template source
     * object for equality, using the <code>equals</code> method. Thus,
     * objects returned for the same physical source must be equivalent
     * according to <code>equals</code> method, otherwise template caching
     * can become very ineffective!
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
     * template source. This method is called after <code>getLastModified()</code>
     * if it is determined that a cached copy of the template is unavailable
     * or stale.
     * @param templateSource an object representing a template source, obtained
     * through a prior call to {@link #findTemplateSource(String)}.
     * @param encoding the character encoding used to translate source bytes
     * to characters. Some loaders may not have access to the byte
     * representation of the template stream, and instead directly obtain a 
     * character stream. These loaders will - quite naturally - ignore the 
     * encoding parameter.
     * @return a reader representing the template character stream. The
     * framework will call <code>close()</code>.
     * @throws IOException if an I/O error occurs while accessing the stream.
     */
    public Reader getReader(Object templateSource, String encoding)
    throws
        IOException;
    
    /**
     * Closes the template source. This is the last method that is called by
     * the TemplateCache for a templateSource. The framework guarantees that
     * this method will be called on every object that is returned from
     * {@link #findTemplateSource(String)}.
     * @param templateSource the template source that should be closed.
     */
    public void closeTemplateSource(Object templateSource)
    throws
        IOException;
}
