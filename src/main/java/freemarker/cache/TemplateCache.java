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
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import freemarker.core.Environment;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.StringUtil;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * Performs caching and on-demand loading of the templates.
 * The actual template "file" loading is delegated to a {@link TemplateLoader} that you can specify in the constructor.
 * Some aspects of caching is delegated to a {@link CacheStorage} that you can also specify in the constructor.
 * 
 * <p>Typically you don't instantiate or otherwise use this class directly. The {@link Configuration} embeds an
 * instance of this class, that you access indirectly through {@link Configuration#getTemplate(String)} and other
 * {@link Configuration} API-s. Then {@link TemplateLoader} and {@link CacheStorage} can be set with
 * {@link Configuration#setTemplateLoader(TemplateLoader)} and
 * {@link Configuration#setCacheStorage(CacheStorage)}.
 *
 * @author Attila Szegedi
 */
public class TemplateCache
{
    private static final String ASTERISKSTR = "*";
    private static final String LOCALE_SEPARATOR = "_";
    private static final char ASTERISK = '*';
    private static final String CURRENT_DIR_PATH_PREFIX = "./";
    private static final String CURRENT_DIR_PATH = "/./";
    private static final String PARENT_DIR_PATH_PREFIX = "../";
    private static final String PARENT_DIR_PATH = "/../";
    private static final char SLASH = '/';
    private static final Logger logger = Logger.getLogger("freemarker.cache");

    private final TemplateLoader templateLoader;
    
    /** Here we keep our cached templates */
    private final CacheStorage storage;
    private final boolean isStorageConcurrent;
    /** The default refresh delay in milliseconds. */
    private long delay = 5000;
    /** Specifies if localized template lookup is enabled or not */
    private boolean localizedLookup = true;

    private Configuration config;

    /**
     * Returns a template cache that will first try to load a template from
     * the file system relative to the current user directory (i.e. the value
     * of the system property <code>user.dir</code>), then from the classpath.
     * 
     * @deprecated Use {@link #TemplateCache(TemplateLoader)} instead. The default loader is useless in most
     *     applications, also it can mean a security risk.
     */
    public TemplateCache()
    {
        this(createDefaultTemplateLoader());
    }

    private static TemplateLoader createDefaultTemplateLoader() {
        try {
            return new FileTemplateLoader();
        } catch(Exception e) {
            logger.warn("Could not create a file template loader for current directory", e);
            return null;
        }
    }
    
    /**
     * Creates a new template cache with a custom template loader that is used
     * to load the templates.
     * @param templateLoader the template loader to use.
     */
    public TemplateCache(TemplateLoader templateLoader)
    {
        this(templateLoader, new SoftCacheStorage());
    }
    
    /**
     * Creates a new template cache with a custom template loader that is used
     * to load the templates.
     * @param templateLoader the template loader to use.
     */
    public TemplateCache(TemplateLoader templateLoader, CacheStorage cacheStorage)
    {
        this.templateLoader = templateLoader;
        this.storage = cacheStorage;
        if(cacheStorage == null) 
        {
            throw new IllegalArgumentException("storage == null");
        }
        isStorageConcurrent = cacheStorage instanceof ConcurrentCacheStorage &&
            ((ConcurrentCacheStorage)cacheStorage).isConcurrent();
    }

    /**
     * Sets the configuration object to which this cache belongs. This
     * method is called by the configuration itself to establish the
     * relation, and should not be called by users.
     */
    public void setConfiguration(Configuration config)
    {
        this.config = config;
        clear();
    }

    public TemplateLoader getTemplateLoader()
    {
        return templateLoader;
    }
    
    public CacheStorage getCacheStorage()
    {
        return storage;
    }
    
    /**
     * Retrieves the template with the given name (and according the specified further parameters) from the
     * template cache, loading it into the cache first if it's missing/staled.
     * 
     * <p>For the meaning of the parameters see {@link Configuration#getTemplate(String, Locale, String, boolean)}.
     *
     * @return the loaded template, or {@code null} if the template was not found.
     */
    public Template getTemplate(String name, Locale locale, String encoding, boolean parseAsFTL)
    throws IOException
    {
        if (name == null) {
            throw new IllegalArgumentException("Argument \"name\" can't be null");
        }
        if (locale == null) {
            throw new IllegalArgumentException("Argument \"locale\" can't be null");
        }
        if (encoding == null) {
            throw new IllegalArgumentException("Argument \"encoding\" can't be null");
        }
        name = normalizeName(name);
        if(name == null) {
            return null;
        }
        Template result = null;
        if (templateLoader != null) {
            result = getTemplate(templateLoader, name, locale, encoding, parseAsFTL);
        }
        return result;
    }    
    
    private Template getTemplate(TemplateLoader loader, String name, Locale locale, String encoding, boolean parse)
    throws IOException
    {
        boolean debug = logger.isDebugEnabled();
        String debugName = debug
                ? buildDebugName(name, locale, encoding, parse)
                : null;
        TemplateKey tk = new TemplateKey(name, locale, encoding, parse);
        
        CachedTemplate cachedTemplate;
        if(isStorageConcurrent) {
            cachedTemplate = (CachedTemplate)storage.get(tk);
        }
        else {
            synchronized(storage) {
                cachedTemplate = (CachedTemplate)storage.get(tk);
            }
        }
        long now = System.currentTimeMillis();
        long lastModified = -1L;
        Object newlyFoundSource = null;
        boolean rethrown = false;
        try {
            if (cachedTemplate != null) {
                // If we're within the refresh delay, return the cached copy
                if (now - cachedTemplate.lastChecked < delay) {
                    if(debug) {
                        logger.debug(debugName + " cached copy not yet stale; using cached.");
                    }
                    // Can be null, indicating a cached negative lookup
                    Object t = cachedTemplate.templateOrException;
                    if(t instanceof Template || t == null) {
                        return (Template)t;
                    }
                    else if(t instanceof RuntimeException) {
                        throwLoadFailedException((RuntimeException)t);
                    }
                    else if(t instanceof IOException) {
                        rethrown = true;
                        throwLoadFailedException((IOException)t);
                    }
                    throw new RuntimeException("t is " + t.getClass().getName());
                }
                // Clone as the instance bound to the map should be treated as
                // immutable to ensure proper concurrent semantics
                cachedTemplate = cachedTemplate.cloneCachedTemplate();
                // Update the last-checked flag
                cachedTemplate.lastChecked = now;

                // Find the template source
                newlyFoundSource = findTemplateSource(name, locale);

                // Template source was removed
                if (newlyFoundSource == null) {
                    if(debug) {
                        logger.debug(debugName + " no source found.");
                    } 
                    storeNegativeLookup(tk, cachedTemplate, null);
                    return null;
                }

                // If the source didn't change and its last modified date
                // also didn't change, return the cached version.
                lastModified = loader.getLastModified(newlyFoundSource);
                boolean lastModifiedNotChanged = lastModified == cachedTemplate.lastModified;
                boolean sourceEquals = newlyFoundSource.equals(cachedTemplate.source);
                if(lastModifiedNotChanged && sourceEquals) {
                    if(debug) {
                        logger.debug(debugName + " using cached since " + 
                                newlyFoundSource + " didn't change.");
                    }
                    storeCached(tk, cachedTemplate);
                    return (Template)cachedTemplate.templateOrException;
                }
                else {
                    if(debug && !sourceEquals) {
                        logger.debug("Updating source, info for cause: " + 
                            "sourceEquals=" + sourceEquals + 
                            ", newlyFoundSource=" + StringUtil.jQuoteNoXSS(newlyFoundSource) + 
                            ", cachedTemplate.source=" + StringUtil.jQuoteNoXSS(cachedTemplate.source));
                    }
                    if(debug && !lastModifiedNotChanged) {
                        logger.debug("Updating source, info for cause: " + 
                            "lastModifiedNotChanged=" + lastModifiedNotChanged + 
                            ", cache lastModified=" + cachedTemplate.lastModified + 
                            " != file lastModified=" + lastModified);
                    }
                    // Update the source
                    cachedTemplate.source = newlyFoundSource;
                }
            }
            else {
                if(debug) {
                    logger.debug("Could not find template in cache, " +
                        "creating new one; id=[" +
                        StringUtil.jQuoteNoXSS(tk.name) + "[" +
                        StringUtil.jQuoteNoXSS(tk.locale) + "," +
                        tk.encoding + (tk.parse ? ",parsed] " : ",unparsed] ") +
                        "]");
                }
                
                // Construct a new CachedTemplate entry. Note we set the
                // cachedTemplate.lastModified to Long.MIN_VALUE. This is
                // a flag that signs it has to be explicitly queried later on.
                cachedTemplate = new CachedTemplate();
                cachedTemplate.lastChecked = now;
                newlyFoundSource = findTemplateSource(name, locale);
                if (newlyFoundSource == null) {
                    storeNegativeLookup(tk, cachedTemplate, null);
                    return null;
                }
                cachedTemplate.source = newlyFoundSource;
                cachedTemplate.lastModified = lastModified = Long.MIN_VALUE;
            }
            if(debug) {
                logger.debug("Compiling FreeMarker template " + 
                    debugName + " from " + StringUtil.jQuoteNoXSS(newlyFoundSource));
            }
            // If we get here, then we need to (re)load the template
            Object source = cachedTemplate.source;
            Template t = loadTemplate(loader, name, locale, encoding, parse, source);
            cachedTemplate.templateOrException = t;
            cachedTemplate.lastModified =
                lastModified == Long.MIN_VALUE
                    ? loader.getLastModified(source)
                    : lastModified;
            storeCached(tk, cachedTemplate);
            return t;
        }
        catch(RuntimeException e) {
            if (cachedTemplate != null) {
                storeNegativeLookup(tk, cachedTemplate, e);
            }
            throw e;
        }
        catch(IOException e) {
            if(!rethrown) {
                storeNegativeLookup(tk, cachedTemplate, e);
            }
            throw e;
        }
        finally {
            if(newlyFoundSource != null) {
                loader.closeTemplateSource(newlyFoundSource);
            }
        }
    }

    private static final Method INIT_CAUSE = getInitCauseMethod();
    
    private static final Method getInitCauseMethod() {
        try {
            return Throwable.class.getMethod("initCause", new Class[] { Throwable.class });
        } catch(NoSuchMethodException e) {
            return null;
        }
    }
    
    private void throwLoadFailedException(Exception e) throws IOException {
        IOException ioe;
        if(INIT_CAUSE != null) {
            ioe = new IOException("There was an error loading the " +
                "template on an earlier attempt; it's attached as a cause");
            try {
                INIT_CAUSE.invoke(ioe, new Object[] { e });
            } catch(RuntimeException ex) {
                throw ex;
            } catch(Exception ex) {
                throw new UndeclaredThrowableException(ex);
            }
        }
        else {
            ioe = new IOException("There was an error loading the " +
            "template on an earlier attempt: " + e.getClass().getName() + 
            ": " + e.getMessage());
        }
        throw ioe;
    }

    private void storeNegativeLookup(TemplateKey tk, 
            CachedTemplate cachedTemplate, Exception e) {
        cachedTemplate.templateOrException = e;
        cachedTemplate.source = null;
        cachedTemplate.lastModified = 0L;
        storeCached(tk, cachedTemplate);
    }

    private void storeCached(TemplateKey tk, CachedTemplate cachedTemplate) {
        if(isStorageConcurrent) {
            storage.put(tk, cachedTemplate);
        }
        else {
            synchronized(storage) {
                storage.put(tk, cachedTemplate);
            }
        }
    }

    private Template loadTemplate(TemplateLoader loader, String name, Locale locale, String encoding,
                                   boolean parse, Object source)
    throws IOException
    {
        Template template;
        Reader reader = loader.getReader(source, encoding);
        try
        {
            if(parse)
            {
                try {
                    template = new Template(name, reader, config, encoding);
                }
                catch (Template.WrongEncodingException wee) {
                    encoding = wee.specifiedEncoding;
                    reader = loader.getReader(source, encoding);
                    template = new Template(name, reader, config, encoding);
                }
                template.setLocale(locale);
            }
            else
            {
                // Read the contents into a StringWriter, then construct a single-textblock
                // template from it.
                StringWriter sw = new StringWriter();
                char[] buf = new char[4096];
                for(;;)
                {
                    int charsRead = reader.read(buf);
                    if (charsRead > 0)
                    {
                        sw.write(buf, 0, charsRead);
                    }
                    else if(charsRead == -1)
                    {
                        break;
                    }
                }
                template = Template.getPlainTextTemplate(name, sw.toString(), config);
                template.setLocale(locale);
            }
            template.setEncoding(encoding);
        }
        finally
        {
            reader.close();
        }
        return template;
    }

    /**
     * Gets the delay in milliseconds between checking for newer versions of a
     * template source.
     * @return the current value of the delay
     */
    public synchronized long getDelay()
    {
        return delay;
    }

    /**
     * Sets the delay in milliseconds between checking for newer versions of a
     * template sources.
     * @param delay the new value of the delay
     */
    public synchronized void setDelay(long delay)
    {
        this.delay = delay;
    }

    /**
     * Returns if localized template lookup is enabled or not.
     */
    public synchronized boolean getLocalizedLookup()
    {
        return localizedLookup;
    }

    /**
     * Setis if localized template lookup is enabled or not.
     */
    public synchronized void setLocalizedLookup(boolean localizedLookup)
    {
        this.localizedLookup = localizedLookup;
    }

    /**
     * Removes all entries from the cache, forcing reloading of templates
     * on subsequent {@link #getTemplate(String, Locale, String, boolean)}
     * calls. If the configured template loader is 
     * {@link StatefulTemplateLoader stateful}, then its 
     * {@link StatefulTemplateLoader#resetState()} method is invoked as well.
     */
    public void clear()
    {
        synchronized (storage) {
            storage.clear();
            if(templateLoader instanceof StatefulTemplateLoader) {
                ((StatefulTemplateLoader)templateLoader).resetState();
            }
        }
    }
    
    /**
     * Removes an entry from the cache, hence forcing the re-loading of it when
     * it's next time requested. It doesn't delete the template file itself.
     * This is to give the application finer control over cache updating than
     * {@link #setDelay(long)} alone does.
     * 
     * For the meaning of the parameters, see
     * {@link #getTemplate(String, Locale, String, boolean)}.
     */
    public void removeTemplate(
            String name, Locale locale, String encoding, boolean parse)
    throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("Argument \"name\" can't be null");
        }
        if (locale == null) {
            throw new IllegalArgumentException("Argument \"locale\" can't be null");
        }
        if (encoding == null) {
            throw new IllegalArgumentException("Argument \"encoding\" can't be null");
        }
        name = normalizeName(name);
        if(name != null && templateLoader != null) {
            boolean debug = logger.isDebugEnabled();
            String debugName = debug
                    ? buildDebugName(name, locale, encoding, parse)
                    : null;
            TemplateKey tk = new TemplateKey(name, locale, encoding, parse);
            
            if(isStorageConcurrent) {
                storage.remove(tk);
            } else {
                synchronized(storage) {
                    storage.remove(tk);
                }
            }
            logger.debug(debugName + " was removed from the cache, if it was there");
        }
    }

    private String buildDebugName(String name, Locale locale, String encoding,
            boolean parse) {
        return StringUtil.jQuoteNoXSS(name) + "["
                + StringUtil.jQuoteNoXSS(locale) + "," + encoding
                + (parse ? ",parsed] " : ",unparsed]");
    }    

    /**
     * Resolves a path-like reference to a template (like the one used in {@code #include} or {@code #import}), assuming
     * a current directory. This gives a full, even if non-normalized template name, that could be used for
     * {@link #getTemplate(String, Locale, String, boolean)}. This is mostly used when a template refers to another
     * template.
     * 
     * @param targetTemplatePath If starts with "/" or contains "://", it's an absolute path and {@code currentDir}
     *     will be ignored, otherwise it's interpreted as relative to {@code currentDir}
     * @param currentTemplateDir must end with "/", might contains "://".  
     */
    public static String getFullTemplatePath(Environment env, String currentTemplateDir, String targetTemplatePath)
    {
        if (!env.isClassicCompatible()) {
            if (targetTemplatePath.indexOf("://") >0) {
                ;
            }
            else if (targetTemplatePath.length() > 0 && targetTemplatePath.charAt(0) == '/')  {
                int protIndex = currentTemplateDir.indexOf("://");
                if (protIndex >0) {
                    targetTemplatePath = currentTemplateDir.substring(0, protIndex + 2) + targetTemplatePath;
                } else {
                    targetTemplatePath = targetTemplatePath.substring(1);
                }
            }
            else {
                targetTemplatePath = currentTemplateDir + targetTemplatePath;
            }
        }
        return targetTemplatePath;
    }

    private Object findTemplateSource(String name, Locale locale)
    throws
        IOException
    {
        if (localizedLookup) {
            int lastDot = name.lastIndexOf('.');
            String prefix = lastDot == -1 ? name : name.substring(0, lastDot);
            String suffix = lastDot == -1 ? "" : name.substring(lastDot);
            String localeName = LOCALE_SEPARATOR + locale.toString();
            StringBuffer buf = new StringBuffer(name.length() + localeName.length());
            buf.append(prefix);
            for (;;)
            {
                buf.setLength(prefix.length());
                String path = buf.append(localeName).append(suffix).toString();
                Object templateSource = acquireTemplateSource(path);
                if (templateSource != null)
                {
                    return templateSource;
                }
                int lastUnderscore = localeName.lastIndexOf('_');
                if (lastUnderscore == -1)
                    break;
                localeName = localeName.substring(0, lastUnderscore);
            }
            return null;
        }
        else
        {
            return acquireTemplateSource(name);
        }
    }

    private Object acquireTemplateSource(String path) throws IOException
    {
        int asterisk = path.indexOf(ASTERISK);
        // Shortcut in case there is no acquisition
        if(asterisk == -1)
        {
            return templateLoader.findTemplateSource(path);
        }
        StringTokenizer tok = new StringTokenizer(path, "/");
        int lastAsterisk = -1;
        List tokpath = new ArrayList();
        while(tok.hasMoreTokens())
        {
            String pathToken = tok.nextToken();
            if(pathToken.equals(ASTERISKSTR))
            {
                if(lastAsterisk != -1)
                {
                    tokpath.remove(lastAsterisk);
                }
                lastAsterisk = tokpath.size();
            }
            tokpath.add(pathToken);
        }
        if (lastAsterisk == -1) {  // if there was no real "*" step after all
            return templateLoader.findTemplateSource(path);
        }
        String basePath = concatPath(tokpath, 0, lastAsterisk);
        String resourcePath = concatPath(tokpath, lastAsterisk + 1, tokpath.size());
        if(resourcePath.endsWith("/"))
        {
            resourcePath = resourcePath.substring(0, resourcePath.length() - 1);
        }
        StringBuffer buf = new StringBuffer(path.length()).append(basePath);
        int l = basePath.length();
        boolean debug = logger.isDebugEnabled();
        for(;;)
        {
            String fullPath = buf.append(resourcePath).toString();
            if(debug)
            {
                logger.debug("Trying to find template source "
                        + StringUtil.jQuoteNoXSS(fullPath));
            }
            Object templateSource = templateLoader.findTemplateSource(fullPath);
            if(templateSource != null)
            {
                return templateSource;
            }
            if(l == 0)
            {
                return null;
            }
            l = basePath.lastIndexOf(SLASH, l - 2) + 1;
            buf.setLength(l);
        }
    }

    private String concatPath(List path, int from, int to)
    {
        StringBuffer buf = new StringBuffer((to - from) * 16);
        for(int i = from; i < to; ++i)
        {
            buf.append(path.get(i)).append('/');
        }
        return buf.toString();
    }
    
    private static String normalizeName(String name) {
        // Disallow 0 for security reasons.
        if (name.indexOf(0) != -1) return null;

        for(;;) {
            int parentDirPathLoc = name.indexOf(PARENT_DIR_PATH);
            if(parentDirPathLoc == 0) {
                // If it starts with /../, then it reaches outside the template
                // root.
                return null;
            }
            if(parentDirPathLoc == -1) {
                if(name.startsWith(PARENT_DIR_PATH_PREFIX)) {
                    // Another attempt to reach out of template root.
                    return null;
                }
                break;
            }
            int previousSlashLoc = name.lastIndexOf(SLASH, parentDirPathLoc - 1);
            name = name.substring(0, previousSlashLoc + 1) +
                   name.substring(parentDirPathLoc + PARENT_DIR_PATH.length());
        }
        for(;;) {
            int currentDirPathLoc = name.indexOf(CURRENT_DIR_PATH);
            if(currentDirPathLoc == -1) {
                if(name.startsWith(CURRENT_DIR_PATH_PREFIX)) {
                    name = name.substring(CURRENT_DIR_PATH_PREFIX.length());
                }
                break;
            }
            name = name.substring(0, currentDirPathLoc) +
                   name.substring(currentDirPathLoc + CURRENT_DIR_PATH.length() - 1);
        }
        // Editing can leave us with a leading slash; strip it.
        if(name.length() > 1 && name.charAt(0) == SLASH) {
            name = name.substring(1);
        }
        return name;
    }

    /**
     * This class holds a (name, locale) pair and is used as the key in
     * the cached templates map.
     */
    private static final class TemplateKey
    {
        private final String name;
        private final Locale locale;
        private final String encoding;
        private final boolean parse;

        TemplateKey(String name, Locale locale, String encoding, boolean parse)
        {
            this.name = name;
            this.locale = locale;
            this.encoding = encoding;
            this.parse = parse;
        }

        public boolean equals(Object o)
        {
            if (o instanceof TemplateKey)
            {
                TemplateKey tk = (TemplateKey)o;
                return
                    parse == tk.parse &&
                    name.equals(tk.name) &&
                    locale.equals(tk.locale) &&
                    encoding.equals(tk.encoding);
            }
            return false;
        }

        public int hashCode()
        {
            return
                name.hashCode() ^
                locale.hashCode() ^
                encoding.hashCode() ^
                (parse ? Boolean.FALSE : Boolean.TRUE).hashCode();
        }
    }

    /**
     * This class holds the cached template and associated information
     * (the source object, and the last-checked and last-modified timestamps).
     * It is used as the value in the cached templates map. Note: this class
     * is Serializable to allow custom 3rd party CacheStorage implementations 
     * to serialize/replicate them (see tracker issue #1926150); FreeMarker 
     * code itself doesn't rely on its serializability.
     */
    private static final class CachedTemplate implements Cloneable, Serializable
    {
        private static final long serialVersionUID = 1L;

        Object templateOrException;
        Object source;
        long lastChecked;
        long lastModified;
        
        public CachedTemplate cloneCachedTemplate() {
            try {
                return (CachedTemplate)super.clone();
            }
            catch(CloneNotSupportedException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
    }
}
