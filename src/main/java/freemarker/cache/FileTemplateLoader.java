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
 * serving templates outside the template directory (like <code>&lt;include /etc/passwd></code>.
 * It compares canonical paths for this, so templates that are symbolically
 * linked into the template directory from outside of it won't work either.
 * @author Attila Szegedi, szegedia at freemail dot hu
 */
public class FileTemplateLoader implements TemplateLoader
{
    private static final boolean SEP_IS_SLASH = File.separatorChar == '/';
    public final File baseDir;
    private final String canonicalPath;

    /**
     * Creates a new file template cache that will use the current directory
     * (the value of the system property <code>user.dir</code> as the base
     * directory for loading templates. It will not allow access to template
     * files that are accessible through symlinks that point outside the
     * base directory.
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
                                "templateSource is a: " + 
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
}