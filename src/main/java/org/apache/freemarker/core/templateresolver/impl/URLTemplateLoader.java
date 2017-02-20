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

package org.apache.freemarker.core.templateresolver.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core._CoreLogs;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLoaderSession;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResult;
import org.apache.freemarker.core.templateresolver.TemplateLoadingSource;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.slf4j.Logger;

/**
 * This is an abstract template loader that can load templates whose location can be described by an URL. Subclasses
 * only need to override the {@link #getURL(String)}, {@link #extractNegativeResult(URLConnection)}, and perhaps the
 * {@link #prepareConnection(URLConnection)} method.
 */
// TODO JUnit test (including implementing a HTTP-based template loader to test the new protected methods)
public abstract class URLTemplateLoader implements TemplateLoader {
    
    private static final Logger LOG = _CoreLogs.TEMPLATE_RESOLVER;
    
    private Boolean urlConnectionUsesCaches = false;
    
    /**
     * Getter pair of {@link #setURLConnectionUsesCaches(Boolean)}.
     * 
     * @since 2.3.21
     */
    public Boolean getURLConnectionUsesCaches() {
        return urlConnectionUsesCaches;
    }

    /**
     * Sets if {@link URLConnection#setUseCaches(boolean)} will be called, and with what value. By default this is
     * {@code false}, because FreeMarker has its own template cache with its own update delay setting
     * ({@link Configuration#setTemplateUpdateDelayMilliseconds(long)}). If this is set to {@code null},
     * {@link URLConnection#setUseCaches(boolean)} won't be called.
     */
    public void setURLConnectionUsesCaches(Boolean urlConnectionUsesCaches) {
        this.urlConnectionUsesCaches = urlConnectionUsesCaches;
    }

    @Override
    public TemplateLoaderSession createSession() {
        return null;
    }

    @Override
    public TemplateLoadingResult load(String name, TemplateLoadingSource ifSourceDiffersFrom,
            Serializable ifVersionDiffersFrom, TemplateLoaderSession session) throws IOException {
        URL url = getURL(name);
        if (url == null) {
            return TemplateLoadingResult.NOT_FOUND;             
        }
        
        URLConnection conn = url.openConnection();
        Boolean urlConnectionUsesCaches = getURLConnectionUsesCaches();
        if (urlConnectionUsesCaches != null) {
            conn.setUseCaches(urlConnectionUsesCaches);
        }
        
        prepareConnection(conn);
        conn.connect();
        
        InputStream inputStream = null;
        Long version;
        URLTemplateLoadingSource source;
        try {
            TemplateLoadingResult negativeResult = extractNegativeResult(conn);
            if (negativeResult != null) {
                return negativeResult;
            }
            
            // To prevent clustering issues, getLastModified(fallbackToJarLMD=false)
            long lmd = getLastModified(conn, false);
            version = lmd != -1 ? lmd : null;
            
            source = new URLTemplateLoadingSource(url);
            
            if (ifSourceDiffersFrom != null && ifSourceDiffersFrom.equals(source)
                    && Objects.equals(ifVersionDiffersFrom, version)) {
                return TemplateLoadingResult.NOT_MODIFIED;
            }
            
            inputStream = conn.getInputStream();
        } catch (Throwable e) {
            try {
                if (inputStream == null) {
                    // There's no URLConnection.close(), so we do this hack. In case of HttpURLConnection we could call
                    // disconnect(), but that's perhaps too aggressive.
                    conn.getInputStream().close();
                }
            } catch (IOException e2) {
                LOG.debug("Failed to close connection inputStream", e2);
            }
            throw e;
        }
        return new TemplateLoadingResult(source, version, inputStream, null);
    }

    @Override
    public void resetState() {
        // Do nothing
    }

    /**
     * {@link URLConnection#getLastModified()} with JDK bug workarounds. Because of JDK-6956385, for files inside a jar,
     * it returns the last modification time of the jar itself, rather than the last modification time of the file
     * inside the jar.
     * 
     * @param fallbackToJarLMD
     *            Tells if the file is in side jar, then we should return the last modification time of the jar itself,
     *            or -1 (to work around JDK-6956385).
     */
    public static long getLastModified(URLConnection conn, boolean fallbackToJarLMD) throws IOException {
        if (conn instanceof JarURLConnection) {
            // There is a bug in sun's jar url connection that causes file handle leaks when calling getLastModified()
            // (see https://bugs.openjdk.java.net/browse/JDK-6956385).
            // Since the time stamps of jar file contents can't vary independent from the jar file timestamp, just use
            // the jar file timestamp
            if (fallbackToJarLMD) {
                URL jarURL = ((JarURLConnection) conn).getJarFileURL();
                if (jarURL.getProtocol().equals("file")) {
                    // Return the last modified time of the underlying file - saves some opening and closing
                    return new File(jarURL.getFile()).lastModified();
                } else {
                    // Use the URL mechanism
                    URLConnection jarConn = null;
                    try {
                        jarConn = jarURL.openConnection();
                        return jarConn.getLastModified();
                    } finally {
                        try {
                            if (jarConn != null) {
                                jarConn.getInputStream().close();
                            }
                        } catch (IOException e) {
                            LOG.warn("Failed to close URL connection for: {}", conn, e);
                        }
                    }
                }
            } else {
                return -1;
            }
        } else {
          return conn.getLastModified();
        }
    }

    /**
     * Given a template name (plus potential locale decorations) retrieves an URL that points the template source.
     * 
     * @param name
     *            the name of the sought template (including the locale decorations, or other decorations the
     *            {@link TemplateLookupStrategy} uses).
     *            
     * @return An URL that points to the template source, or null if it can be determined that the template source does
     *         not exist. For many implementations the existence of the template can't be decided at this point, and you
     *         rely on {@link #extractNegativeResult(URLConnection)} instead.
     */
    protected abstract URL getURL(String name);

    /**
     * Called before the resource if read, checks if we can immediately return a {@link TemplateLoadingResult#NOT_FOUND}
     * or {@link TemplateLoadingResult#NOT_MODIFIED}, or throw an {@link IOException}. For example, for a HTTP-based
     * storage, the HTTP response status 404 could result in {@link TemplateLoadingResult#NOT_FOUND}, 304 in
     * {@link TemplateLoadingResult#NOT_MODIFIED}, and some others, like 500 in throwing an {@link IOException}.
     * 
     * <p>Some
     * implementations rely on {@link #getURL(String)} returning {@code null} when a template is missing, in which case
     * this method is certainly not applicable.
     */
    protected abstract TemplateLoadingResult extractNegativeResult(URLConnection conn) throws IOException;

    /**
     * Called before anything that causes the connection to actually build up. This is where
     * {@link URLConnection#setIfModifiedSince(long)} and such can be called if someone overrides this.
     * The default implementation in {@link URLTemplateLoader} does nothing. 
     */
    protected void prepareConnection(URLConnection conn) {
        // Does nothing
    }

    /**
     * Can be used by subclasses to canonicalize URL path prefixes.
     * @param prefix the path prefix to canonicalize
     * @return the canonicalized prefix. All backslashes are replaced with
     * forward slashes, and a trailing slash is appended if the original
     * prefix wasn't empty and didn't already end with a slash.
     */
    protected static String canonicalizePrefix(String prefix) {
        // make it foolproof
        prefix = prefix.replace('\\', '/');
        // ensure there's a trailing slash
        if (prefix.length() > 0 && !prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix;
    }
    
}
