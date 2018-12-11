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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLoaderSession;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResult;
import org.apache.freemarker.core.templateresolver.TemplateLoadingSource;
import org.apache.freemarker.core.util._SecurityUtils;
import org.apache.freemarker.core.util._StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TemplateLoader} that uses files inside a specified directory as the source of templates. By default it does
 * security checks on the <em>canonical</em> path that will prevent it serving templates outside that specified
 * directory. If you want symbolic links that point outside the template directory to work, you need to disable this
 * feature by using {@link #FileTemplateLoader(File, boolean)} with {@code true} second argument, but before that,
 * check the security implications there!
 */
public class FileTemplateLoader implements TemplateLoader {
    
    /**
     * By setting this Java system property to {@code true}, you can change the default of
     * {@code #getEmulateCaseSensitiveFileSystem()}.
     */
    public static String SYSTEM_PROPERTY_NAME_EMULATE_CASE_SENSITIVE_FILE_SYSTEM
            = "org.freemarker.emulateCaseSensitiveFileSystem";
    private static final boolean EMULATE_CASE_SENSITIVE_FILE_SYSTEM_DEFAULT;
    static {
        final String s = _SecurityUtils.getSystemProperty(SYSTEM_PROPERTY_NAME_EMULATE_CASE_SENSITIVE_FILE_SYSTEM,
                "false");
        boolean emuCaseSensFS;
        try {
            emuCaseSensFS = _StringUtils.getYesNo(s);
        } catch (Exception e) {
            emuCaseSensFS = false;
        }
        EMULATE_CASE_SENSITIVE_FILE_SYSTEM_DEFAULT = emuCaseSensFS;
    }

    private static final int CASE_CHECH_CACHE_HARD_SIZE = 50;
    private static final int CASE_CHECK_CACHE__SOFT_SIZE = 1000;
    private static final boolean SEP_IS_SLASH = File.separatorChar == '/';
    
    private static final Logger LOG = LoggerFactory.getLogger(FileTemplateLoader.class);
    
    public final File baseDir;
    private final String canonicalBasePath;
    private boolean emulateCaseSensitiveFileSystem;
    private MruCacheStorage correctCasePaths;

    /**
     * Creates a new file template loader that will use the specified directory
     * as the base directory for loading templates. It will not allow access to
     * template files that are accessible through symlinks that point outside 
     * the base directory.
     * @param baseDir the base directory for loading templates
     */
    public FileTemplateLoader(final File baseDir)
    throws IOException {
        this(baseDir, false);
    }

    /**
     * Creates a new file template loader that will use the specified directory as the base directory for loading
     * templates. See the parameters for allowing symlinks that point outside the base directory.
     * 
     * @param baseDir
     *            the base directory for loading templates
     * 
     * @param disableCanonicalPathCheck
     *            If {@code true}, it will not check if the file to be loaded is inside the {@code baseDir} or not,
     *            according the <em>canonical</em> paths of the {@code baseDir} and the file to load. Note that
     *            {@link Configuration#getTemplate(String)} and (its overloads) already prevents backing out from the
     *            template directory with paths like {@code /../../../etc/password}, however, that can be circumvented
     *            with symbolic links or other file system features. If you really want to use symbolic links that point
     *            outside the {@code baseDir}, set this parameter to {@code true}, but then be very careful with
     *            template paths that are supplied by the visitor or an external system.
     */
    public FileTemplateLoader(final File baseDir, final boolean disableCanonicalPathCheck)
    throws IOException {
        try {
            Object[] retval = AccessController.doPrivileged(new PrivilegedExceptionAction<Object[]>() {
                @Override
                public Object[] run() throws IOException {
                    if (!baseDir.exists()) {
                        throw new FileNotFoundException(baseDir + " does not exist.");
                    }
                    if (!baseDir.isDirectory()) {
                        throw new IOException(baseDir + " is not a directory.");
                    }
                    Object[] retval = new Object[2];
                    if (disableCanonicalPathCheck) {
                        retval[0] = baseDir;
                        retval[1] = null;
                    } else {
                        retval[0] = baseDir.getCanonicalFile();
                        String basePath = ((File) retval[0]).getPath();
                        // Most canonical paths don't end with File.separator,
                        // but some does. Like, "C:\" VS "C:\templates".
                        if (!basePath.endsWith(File.separator)) {
                            basePath += File.separatorChar;
                        }
                        retval[1] = basePath;
                    }
                    return retval;
                }
            });
            this.baseDir = (File) retval[0];
            canonicalBasePath = (String) retval[1];
            
            setEmulateCaseSensitiveFileSystem(getEmulateCaseSensitiveFileSystemDefault());
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }
    
    private File getFile(final String name) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<File>() {
                @Override
                public File run() throws IOException {
                    File source = new File(baseDir, SEP_IS_SLASH ? name : 
                        name.replace('/', File.separatorChar));
                    if (!source.isFile()) {
                        return null;
                    }
                    // Security check for inadvertently returning something 
                    // outside the template directory when linking is not 
                    // allowed.
                    if (canonicalBasePath != null) {
                        String normalized = source.getCanonicalPath();
                        if (!normalized.startsWith(canonicalBasePath)) {
                            throw new SecurityException(source.getAbsolutePath() 
                                    + " resolves to " + normalized + " which " + 
                                    " doesn't start with " + canonicalBasePath);
                        }
                    }
                    
                    if (emulateCaseSensitiveFileSystem && !isNameCaseCorrect(source)) {
                        return null;
                    }
                    
                    return source;
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }
    
    private long getLastModified(final File templateSource) {
        return (AccessController.<Long>doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return Long.valueOf((templateSource).lastModified());
            }
        })).longValue();
    }
    
    private InputStream getInputStream(final File templateSource)
    throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                @Override
                public InputStream run() throws IOException {
                    return new FileInputStream(templateSource);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }
    
    /**
     * Called by {@link #getFile(String)} when {@link #getEmulateCaseSensitiveFileSystem()} is {@code true}.
     */
    private boolean isNameCaseCorrect(File source) throws IOException {
        final String sourcePath = source.getPath();
        synchronized (correctCasePaths) {
            if (correctCasePaths.get(sourcePath) != null) {
                return true;
            }
        }
        
        final File parentDir = source.getParentFile();
        if (parentDir != null) {
            if (!baseDir.equals(parentDir) && !isNameCaseCorrect(parentDir)) {
                return false;
            }
            
            final String[] listing = parentDir.list();
            if (listing != null) {
                final String fileName = source.getName();
                
                boolean identicalNameFound = false;
                for (int i = 0; !identicalNameFound && i < listing.length; i++) {
                    if (fileName.equals(listing[i])) {
                        identicalNameFound = true;
                    }
                }
        
                if (!identicalNameFound) {
                    // If we find a similarly named file that only differs in case, then this is a file-not-found.
                    for (final String listingEntry : listing) {
                        if (fileName.equalsIgnoreCase(listingEntry)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Emulating file-not-found because of letter case differences to the "
                                        + "real file, for: {}", sourcePath);
                            }
                            return false;
                        }
                    }
                }
            }
        }

        synchronized (correctCasePaths) {
            correctCasePaths.put(sourcePath, Boolean.TRUE);        
        }
        return true;
    }
    
    /**
     * Returns the base directory in which the templates are searched. This comes from the constructor argument, but
     * it's possibly a canonicalized version of that. 
     */
    public File getBaseDirectory() {
        return baseDir;
    }
    
    /**
     * Intended for development only, checks if the template name matches the case (upper VS lower case letters) of the
     * actual file name, and if it doesn't, it emulates a file-not-found even if the file system is case insensitive.
     * This is useful when developing application on Windows, which will be later installed on Linux, OS X, etc. This
     * check can be resource intensive, as to check the file name the directories involved, up to the
     * {@link #getBaseDirectory()} directory, must be listed. Positive results (matching case) will be cached without
     * expiration time.
     * 
     * <p>The default in {@link FileTemplateLoader} is {@code false}, but subclasses may change they by overriding
     * {@link #getEmulateCaseSensitiveFileSystemDefault()}.
     */
    public void setEmulateCaseSensitiveFileSystem(boolean emulateCaseSensitiveFileSystem) {
        // Ensure that the cache exists exactly when needed:
        if (emulateCaseSensitiveFileSystem) {
            if (correctCasePaths == null) {
                correctCasePaths = new MruCacheStorage(CASE_CHECH_CACHE_HARD_SIZE, CASE_CHECK_CACHE__SOFT_SIZE);
            }
        } else {
            correctCasePaths = null;
        }
        
        this.emulateCaseSensitiveFileSystem = emulateCaseSensitiveFileSystem;
    }

    /**
     * Getter pair of {@link #setEmulateCaseSensitiveFileSystem(boolean)}.
     */
    public boolean getEmulateCaseSensitiveFileSystem() {
        return emulateCaseSensitiveFileSystem;
    }

    /**
     * Returns the default of {@link #getEmulateCaseSensitiveFileSystem()}. In {@link FileTemplateLoader} it's
     * {@code false}, unless the {@link #SYSTEM_PROPERTY_NAME_EMULATE_CASE_SENSITIVE_FILE_SYSTEM} system property was
     * set to {@code true}, but this can be overridden here in custom subclasses. For example, if your environment
     * defines something like developer mode, you may want to override this to return {@code true} on Windows.
     */
    protected boolean getEmulateCaseSensitiveFileSystemDefault() {
        return EMULATE_CASE_SENSITIVE_FILE_SYSTEM_DEFAULT;
    }

    /**
     * Show class name and some details that are useful in template-not-found errors.
     */
    @Override
    public String toString() {
        // We don't _StringUtils.jQuote paths here, because on Windows there will be \\-s then that some may find
        // confusing.
        return _TemplateLoaderUtils.getClassNameForToString(this) + "("
                + "baseDir=\"" + baseDir + "\""
                + (canonicalBasePath != null ? ", canonicalBasePath=\"" + canonicalBasePath + "\"" : "")
                + (emulateCaseSensitiveFileSystem ? ", emulateCaseSensitiveFileSystem=true" : "")
                + ")";
    }

    @Override
    public TemplateLoaderSession createSession() {
        return null;
    }

    @Override
    public TemplateLoadingResult load(String name, TemplateLoadingSource ifSourceDiffersFrom,
            Serializable ifVersionDiffersFrom, TemplateLoaderSession session) throws IOException {
        File file = getFile(name);
        if (file == null) {
            return TemplateLoadingResult.NOT_FOUND;
        }
        
        FileTemplateLoadingSource source = new FileTemplateLoadingSource(file);
        
        long lmd = getLastModified(file);
        Long version = lmd != -1 ? lmd : null;
        
        if (ifSourceDiffersFrom != null && ifSourceDiffersFrom.equals(source) 
                && Objects.equals(ifVersionDiffersFrom, version)) {
            return TemplateLoadingResult.NOT_MODIFIED;
        }
        
        return new TemplateLoadingResult(source, version, getInputStream(file), null);
    }

    @Override
    public void resetState() {
        // Does nothing
    }
    
    @SuppressWarnings("serial")
    private static class FileTemplateLoadingSource implements TemplateLoadingSource {
        
        private final File file;

        FileTemplateLoadingSource(File file) {
            this.file = file;
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            return file.equals(((FileTemplateLoadingSource) obj).file);
        }
        
    }
    
}
