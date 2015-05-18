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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import freemarker.template.utility.SecurityUtilities;

/**
 * A {@link TemplateLoader} that uses files in a specified directory as the
 * source of templates. If contains security checks that will prevent it
 * serving templates outside the template directory (like <code>&lt;include /etc/passwd&gt;</code>.
 * It compares canonical paths for this, so templates that are symbolically
 * linked into the template directory from outside of it won't work either.
 */
public class FileTemplateLoader implements TemplateLoader
{
    private static final boolean SEP_IS_SLASH = File.separatorChar == '/';
    public final File baseDir;
    private final String canonicalPath;

    /**
     * Creates a new file template cache that will use the current directory (the value of the system property
     * <code>user.dir</code> as the base directory for loading templates. It will not allow access to template files
     * that are accessible through symlinks that point outside the base directory.
     * 
     * @deprecated Relying on what the current directory is is a bad practice; use
     *             {@link FileTemplateLoader#FileTemplateLoader(File)} instead.
     */
    public FileTemplateLoader()
    throws
    	IOException
    {
        this(new File(SecurityUtilities.getSystemProperty("user.dir")));
    }

    /**
     * Creates a new file template loader that will use the specified directory
     * as the base directory for loading templates. It will not allow access to
     * template files that are accessible through symlinks that point outside 
     * the base directory.
     * @param baseDir the base directory for loading templates
     */
    public FileTemplateLoader(final File baseDir)
    throws
        IOException
    {
        this(baseDir, false);
    }

    /**
     * Creates a new file template loader that will use the specified directory
     * as the base directory for loading templates.
     * @param baseDir the base directory for loading templates
     * @param allowLinking if true, it will allow 
     */
    public FileTemplateLoader(final File baseDir, final boolean allowLinking)
    throws
    	IOException
    {
        try {
            Object[] retval = (Object[]) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws IOException {
                    if (!baseDir.exists()) {
                        throw new FileNotFoundException(baseDir + " does not exist.");
                    }
                    if (!baseDir.isDirectory()) {
                        throw new IOException(baseDir + " is not a directory.");
                    }
                    Object[] retval = new Object[2];
                    if(allowLinking) {
                        retval[0] = baseDir;
                        retval[1] = null;
                    }
                    else {
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
            this.canonicalPath = (String) retval[1]; 
        }
        catch(PrivilegedActionException e)
        {
            throw (IOException)e.getException();
        }
    }
    
    public Object findTemplateSource(final String name)
    throws
    	IOException
    {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws IOException {
                    File source = new File(baseDir, SEP_IS_SLASH ? name : 
                        name.replace('/', File.separatorChar));
                    if(!source.isFile()) {
                        return null;
                    }
                    // Security check for inadvertently returning something 
                    // outside the template directory when linking is not 
                    // allowed.
                    if(canonicalPath != null) {
                        String normalized = source.getCanonicalPath();
                        if (!normalized.startsWith(canonicalPath)) {
                            throw new SecurityException(source.getAbsolutePath() 
                                    + " resolves to " + normalized + " which " + 
                                    " doesn't start with " + canonicalPath);
                        }
                    }
                    return source;
                }
            });
        }
        catch(PrivilegedActionException e)
        {
            throw (IOException)e.getException();
        }
    }
    
    public long getLastModified(final Object templateSource)
    {
        return ((Long)(AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return new Long(((File)templateSource).lastModified());
            }
        }))).longValue();
        
        
    }
    
    public Reader getReader(final Object templateSource, final String encoding)
    throws
        IOException
    {
        try
        {
            return (Reader)AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run()
                throws 
                    IOException
                {
                    if (!(templateSource instanceof File)) {
                        throw new IllegalArgumentException(
                                "templateSource wasn't a File, but a: " + 
                                templateSource.getClass().getName());
                    }
                    return new InputStreamReader(new FileInputStream(
                            (File)templateSource), encoding);
                }
            });
        }
        catch(PrivilegedActionException e)
        {
            throw (IOException)e.getException();
        }
    }
    
    public void closeTemplateSource(Object templateSource)
    {
        // Do nothing.
    }
    
    /**
     * Returns the base directory in which the templates are searched. This comes from the constructor argument, but
     * it's possibly a canonicalized version of that. 
     *  
     * @since 2.3.21
     */
    public File getBaseDirectory() {
        return baseDir;
    }

    /**
     * Show class name and some details that are useful in template-not-found errors.
     * 
     * @since 2.3.21
     */
    public String toString() {
        // We don't StringUtil.jQuote paths here, because on Windows there will be \\-s then that some may find
        // confusing.
        return TemplateLoaderUtils.getClassNameForToString(this) + "(baseDir=\"" + baseDir
                + "\", canonicalPath=\"" + canonicalPath + "\")";
    }
    
}