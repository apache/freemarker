/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

import javax.servlet.ServletContext;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core._CoreLogs;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLoaderSession;
import org.apache.freemarker.core.util._CollectionUtil;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResult;
import org.apache.freemarker.core.templateresolver.TemplateLoadingSource;
import org.apache.freemarker.core.templateresolver.impl.URLTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl._TemplateLoaderUtils;
import org.slf4j.Logger;

/**
 * A {@link TemplateLoader} that uses streams reachable through {@link ServletContext#getResource(String)} as its source
 * of templates.  
 */
public class WebAppTemplateLoader implements TemplateLoader {

    private static final Logger LOG = _CoreLogs.TEMPLATE_RESOLVER;

    private final ServletContext servletContext;
    private final String subdirPath;

    private Boolean urlConnectionUsesCaches = false;

    private boolean attemptFileAccess = true;

    /**
     * Creates a template loader that will use the specified servlet context to load the resources. It will use
     * the base path of <code>"/"</code> meaning templates will be resolved relative to the servlet context root
     * location.
     * 
     * @param servletContext
     *            the servlet context whose {@link ServletContext#getResource(String)} will be used to load the
     *            templates.
     */
    public WebAppTemplateLoader(ServletContext servletContext) {
        this(servletContext, "/");
    }

    /**
     * Creates a template loader that will use the specified servlet context to load the resources. It will use the
     * specified base path, which is interpreted relatively to the context root (does not mater if you start it with "/"
     * or not). Path components should be separated by forward slashes independently of the separator character used by
     * the underlying operating system.
     * 
     * @param servletContext
     *            the servlet context whose {@link ServletContext#getResource(String)} will be used to load the
     *            templates.
     * @param subdirPath
     *            the base path to template resources.
     */
    public WebAppTemplateLoader(ServletContext servletContext, String subdirPath) {
        _NullArgumentException.check("servletContext", servletContext);
        _NullArgumentException.check("subdirPath", subdirPath);

        subdirPath = subdirPath.replace('\\', '/');
        if (!subdirPath.endsWith("/")) {
            subdirPath += "/";
        }
        if (!subdirPath.startsWith("/")) {
            subdirPath = "/" + subdirPath;
        }
        this.subdirPath = subdirPath;
        this.servletContext = servletContext;
    }

    /**
     * Getter pair of {@link #setURLConnectionUsesCaches(Boolean)}.
     * 
     * @since 2.3.21
     */
    public Boolean getURLConnectionUsesCaches() {
        return urlConnectionUsesCaches;
    }

    /**
     * It does the same as {@link URLTemplateLoader#setURLConnectionUsesCaches(Boolean)}; see there.
     * 
     * @since 2.3.21
     */
    public void setURLConnectionUsesCaches(Boolean urlConnectionUsesCaches) {
        this.urlConnectionUsesCaches = urlConnectionUsesCaches;
    }

    /**
     * Show class name and some details that are useful in template-not-found errors.
     * 
     * @since 2.3.21
     */
    @Override
    public String toString() {
        return _TemplateLoaderUtils.getClassNameForToString(this)
                + "(subdirPath=" + _StringUtil.jQuote(subdirPath)
                + ", servletContext={contextPath=" + _StringUtil.jQuote(getContextPath())
                + ", displayName=" + _StringUtil.jQuote(servletContext.getServletContextName()) + "})";
    }

    /** Gets the context path if we are on Servlet 2.5+, or else returns failure description string. */
    private String getContextPath() {
        try {
            Method m = servletContext.getClass().getMethod("getContextPath", _CollectionUtil.EMPTY_CLASS_ARRAY);
            return (String) m.invoke(servletContext, _CollectionUtil.EMPTY_OBJECT_ARRAY);
        } catch (Throwable e) {
            return "[can't query before Serlvet 2.5]";
        }
    }

    /**
     * Getter pair of {@link #setAttemptFileAccess(boolean)}.
     * 
     * @since 2.3.23
     */
    public boolean getAttemptFileAccess() {
        return attemptFileAccess;
    }

    /**
     * Specifies that before loading templates with {@link ServletContext#getResource(String)}, it should try to load
     * the template as {@link File}; default is {@code true}, though it's not always recommended anymore. This is a
     * workaround for the case when the servlet container doesn't show template modifications after the template was
     * already loaded earlier. But it's certainly better to counter this problem by disabling the URL connection cache
     * with {@link #setURLConnectionUsesCaches(Boolean)}, which is also the default behavior with
     * {@link Configuration#setIncompatibleImprovements(org.apache.freemarker.core.Version) incompatible_improvements} 2.3.21
     * and later.
     * 
     * @since 2.3.23
     */
    public void setAttemptFileAccess(boolean attemptLoadingFromFile) {
        this.attemptFileAccess = attemptLoadingFromFile;
    }

    @Override
    public TemplateLoaderSession createSession() {
        return null;
    }

    @Override
    public TemplateLoadingResult load(String name, TemplateLoadingSource ifSourceDiffersFrom,
            Serializable ifVersionDiffersFrom, TemplateLoaderSession session) throws IOException {
        WebAppTemplateLoadingSource source = createSource(name);
        if (source == null) {
            return TemplateLoadingResult.NOT_FOUND;
        }
        
        if (source.url != null) {
            URLConnection conn = source.url.openConnection();
            Boolean urlConnectionUsesCaches = getURLConnectionUsesCaches();
            if (urlConnectionUsesCaches != null) {
                conn.setUseCaches(urlConnectionUsesCaches);
            }
            
            // To prevent clustering issues, getLastModified(fallbackToJarLMD=false)
            long lmd = URLTemplateLoader.getLastModified(conn, false);
            Long version = lmd != -1 ? lmd : null;
            
            if (ifSourceDiffersFrom != null && ifSourceDiffersFrom.equals(source)
                    && Objects.equals(ifVersionDiffersFrom, version)) {
                return TemplateLoadingResult.NOT_MODIFIED;
            }
            
            return new TemplateLoadingResult(source, version, conn.getInputStream(), null);
        } else { // source.file != null
            long lmd = source.file.lastModified();
            Long version = lmd != -1 ? lmd : null;

            if (ifSourceDiffersFrom != null && ifSourceDiffersFrom.equals(source)
                    && Objects.equals(ifVersionDiffersFrom, version)) {
                return TemplateLoadingResult.NOT_MODIFIED;
            }
            
            return new TemplateLoadingResult(source, version, new FileInputStream(source.file), null);
        }
    }
    
    private WebAppTemplateLoadingSource createSource(String name) {
        String fullPath = subdirPath + name;

        if (attemptFileAccess) {
            // First try to open as plain file (to bypass servlet container resource caches).
            try {
                String realPath = servletContext.getRealPath(fullPath);
                if (realPath != null) {
                    File file = new File(realPath);
                    if (file.canRead() && file.isFile()) {
                        return new WebAppTemplateLoadingSource(file);
                    }
                }
            } catch (SecurityException e) {
                ;// ignore
            }
        }

        // If it fails, try to open it with servletContext.getResource.
        URL url = null;
        try {
            url = servletContext.getResource(fullPath);
        } catch (MalformedURLException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not retrieve resource " + _StringUtil.jQuoteNoXSS(fullPath), e);
            }
            return null;
        }
        return url == null ? null : new WebAppTemplateLoadingSource(url);
    }

    @Override
    public void resetState() {
        // Do nothing
    }
    
    @SuppressWarnings("serial")
    private class WebAppTemplateLoadingSource implements TemplateLoadingSource {
        private final File file;
        private final URL url;
        
        WebAppTemplateLoadingSource(File file) {
            this.file = file;
            this.url = null;
        }

        WebAppTemplateLoadingSource(URL url) {
            this.file = null;
            this.url = url;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((file == null) ? 0 : file.hashCode());
            result = prime * result + ((url == null) ? 0 : url.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            WebAppTemplateLoadingSource other = (WebAppTemplateLoadingSource) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (file == null) {
                if (other.file != null)
                    return false;
            } else if (!file.equals(other.file))
                return false;
            if (url == null) {
                if (other.url != null)
                    return false;
            } else if (!url.equals(other.url))
                return false;
            return true;
        }

        private WebAppTemplateLoader getOuterType() {
            return WebAppTemplateLoader.this;
        }
        
    }

}