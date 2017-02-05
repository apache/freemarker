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


package freemarker.cache;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;

/**
 * This is an abstract template loader that can load templates whose
 * location can be described by an URL. Subclasses only need to override
 * the {@link #getURL(String)} method.
 */
// TODO JUnit test
public abstract class URLTemplateLoader implements TemplateLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger("freemarker.cache");
    
    private Boolean urlConnectionUsesCaches = false;
    
    /**
     * Given a template name (plus potential locale decorations) retrieves
     * an URL that points the template source.
     * @param name the name of the sought template, including the locale
     * decorations.
     * @return an URL that points to the template source, or null if it can
     * determine that the template source does not exist.
     */
    protected abstract URL getURL(String name);
    
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
     * {@code false}, becase FreeMarker has its own template cache with its own update delay setting
     * ({@link Configuration#setTemplateUpdateDelay(int)}). If this is set to {@code null},
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
        
        // To prevent clustering issues, getLastModified(fallbackToJarLMD=false)
        long lmd = getLastModified(conn, false);
        Long version = lmd != -1 ? lmd : null;
        
        URLTemplateLoadingSource source = new URLTemplateLoadingSource(url);
        
        if (ifSourceDiffersFrom != null && ifSourceDiffersFrom.equals(source)
                && Objects.equals(ifVersionDiffersFrom, version)) {
            return TemplateLoadingResult.NOT_MODIFIED;
        }
        
        return new TemplateLoadingResult(source, version, conn.getInputStream(), null);
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
    
}
