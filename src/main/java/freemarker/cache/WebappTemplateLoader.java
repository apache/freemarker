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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;

import freemarker.log.Logger;
import freemarker.template.utility.StringUtil;

/**
 * A {@link TemplateLoader} that uses streams reachable through 
 * {@link ServletContext#getResource(String)} as its source of templates.
 * @author Attila Szegedi
 */
public class WebappTemplateLoader implements TemplateLoader
{
    private static final Logger logger = Logger.getLogger("freemarker.cache");
    
    private final ServletContext servletContext;
    private final String path;
    
    /**
     * Creates a resource template cache that will use the specified servlet
     * context to load the resources. It will use the base path of 
     * <code>"/"</code> meaning templates will be resolved relative to the 
     * servlet context root location.
     * @param servletContext the servlet context whose
     * {@link ServletContext#getResource(String)} will be used to load the
     * templates.
     */
    public WebappTemplateLoader(ServletContext servletContext) {
        this(servletContext, "/");
    }

    /**
     * Creates a template loader that will use the specified servlet
     * context to load the resources. It will use the specified base path.
     * The is interpreted as relative to the current context root (does not mater
     * if you start it with "/" or not). Path components
     * should be separated by forward slashes independently of the separator 
     * character used by the underlying operating system.
     * @param servletContext the servlet context whose
     * {@link ServletContext#getResource(String)} will be used to load the
     * templates.
     * @param path the base path to template resources.
     */
    public WebappTemplateLoader(ServletContext servletContext, String path) {
        if(servletContext == null) {
            throw new IllegalArgumentException("servletContext == null");
        }
        if(path == null) {
            throw new IllegalArgumentException("path == null");
        }
        
        path = path.replace('\\', '/');
        if(!path.endsWith("/")) {
            path += "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        this.path = path;
        this.servletContext = servletContext;
    }

    public Object findTemplateSource(String name) throws IOException {
        String fullPath = path + name;
        // First try to open as plain file (to bypass servlet container resource caches).
        try {
            String realPath = servletContext.getRealPath(fullPath);
            if (realPath != null) {
                File file = new File(realPath);
                if(!file.isFile()) {
                    return null;
                }
                if(file.canRead()) {                    
                    return file;
                }
            }
        } catch (SecurityException e) {
            ;// ignore
        }
            
        // If it fails, try to open it with servletContext.getResource.
        URL url = null;
        try {
            url = servletContext.getResource(fullPath);
        } catch(MalformedURLException e) {
            logger.warn("Could not retrieve resource " + StringUtil.jQuoteNoXSS(fullPath),
                    e);
            return null;
        }
        return url == null ? null : new URLTemplateSource(url);
    }
    
    public long getLastModified(Object templateSource) {
        if (templateSource instanceof File) {
            return ((File) templateSource).lastModified();
        } else {
            return ((URLTemplateSource) templateSource).lastModified();
        }
    }
    
    public Reader getReader(Object templateSource, String encoding)
    throws IOException {
        if (templateSource instanceof File) {
            return new InputStreamReader(
                    new FileInputStream((File) templateSource),
                    encoding);
        } else {
            return new InputStreamReader(
                    ((URLTemplateSource) templateSource).getInputStream(),
                    encoding);
        }
    }
    
    public void closeTemplateSource(Object templateSource) throws IOException {
        if (templateSource instanceof File) {
            // Do nothing.
        } else {
            ((URLTemplateSource) templateSource).close();
        }
    }
}