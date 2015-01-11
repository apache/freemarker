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
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import freemarker.cache.MultiTemplateLoader.MultiSource;
import freemarker.core.BugException;
import freemarker.core.Environment;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.NullArgumentException;
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
    private static final Logger LOG = Logger.getLogger("freemarker.cache");

    /** Maybe {@code null}. */
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
        this(createLegacyDefaultTemplateLoader());
    }

    /**
     * Creates the default {@link TemplateLoader} used in 2.3.0-compatible mode.
     */
    protected static TemplateLoader createLegacyDefaultTemplateLoader() {
        try {
            return new FileTemplateLoader();
        } catch(Exception e) {
            LOG.warn("Couldn't create legacy default TemplateLoader which accesses the current directory. "
                    + "(Use new Configuration(Configuration.VERSION_2_3_21) or higher to avoid this.)", e);
            return null;
        }
    }
    
    /**
     * @deprecated Use {@link #TemplateCache(TemplateLoader, CacheStorage, Configuration)} instead.
     */
    public TemplateCache(TemplateLoader templateLoader)
    {
        this(templateLoader, (Configuration) null);
    }

    /**
     * @deprecated Use {@link #TemplateCache(TemplateLoader, CacheStorage, Configuration)} instead.
     */
    public TemplateCache(TemplateLoader templateLoader, CacheStorage cacheStorage)
    {
        this(templateLoader, cacheStorage, null);
    }

    /**
     * Creates an instance with the default {@link CacheStorage}.
     * 
     * @since 2.3.21
     */
    public TemplateCache(TemplateLoader defaultTemplateLoader, Configuration config) {
        this(defaultTemplateLoader, new SoftCacheStorage(), config);
    }
    
    /**
     * @param templateLoader The {@link TemplateLoader} to use. Can't be {@code null}.
     * @param cacheStorage The {@link CacheStorage} to use. Can't be {@code null}.
     * @param config The {@link Configuration} this cache will be used for. Can be {@code null} for backward
     *          compatibility, as it can be set with {@link #setConfiguration(Configuration)} later.
     *          
     * @since 2.3.21
     */
    public TemplateCache(TemplateLoader templateLoader, CacheStorage cacheStorage, Configuration config)
    {
        this.templateLoader = templateLoader;
        if(cacheStorage == null) 
        {
            throw new IllegalArgumentException("storage == null");
        }
        this.storage = cacheStorage;
        isStorageConcurrent = cacheStorage instanceof ConcurrentCacheStorage &&
            ((ConcurrentCacheStorage)cacheStorage).isConcurrent();

        this.config = config;
    }

    /**
     * Sets the configuration object to which this cache belongs. This
     * method is called by the configuration itself to establish the
     * relation, and should not be called by users.
     * 
     * @deprecated Use the {@link #TemplateCache(TemplateLoader, CacheStorage, Configuration)} constructor.
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
        if (name == null) throw new NullArgumentException("name");
        if (locale == null) throw new NullArgumentException("locale");
        if (encoding == null) throw new NullArgumentException("encoding");
        name = normalizeName(name);
        if(name == null) return null;
        
        return templateLoader != null ? getTemplate(templateLoader, name, locale, encoding, parseAsFTL) : null;
    }    
    
    private Template getTemplate(TemplateLoader loader, String name, Locale locale, String encoding, boolean parse)
    throws IOException
    {
        boolean debug = LOG.isDebugEnabled();
        final String debugName = debug
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
                        LOG.debug(debugName + " cached copy not yet stale; using cached.");
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
                    throw new BugException("t is " + t.getClass().getName());
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
                        LOG.debug(debugName + " no source found.");
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
                        LOG.debug(debugName + ": using cached since " + 
                                newlyFoundSource + " hasn't changed.");
                    }
                    storeCached(tk, cachedTemplate);
                    return (Template)cachedTemplate.templateOrException;
                }
                else {
                    if(debug && !sourceEquals) {
                        LOG.debug("Updating source because: " + 
                            "sourceEquals=" + sourceEquals + 
                            ", newlyFoundSource=" + StringUtil.jQuoteNoXSS(newlyFoundSource) + 
                            ", cached.source=" + StringUtil.jQuoteNoXSS(cachedTemplate.source));
                    }
                    if(debug && !lastModifiedNotChanged) {
                        LOG.debug("Updating source because: " + 
                            "lastModifiedNotChanged=" + lastModifiedNotChanged + 
                            ", cached.lastModified=" + cachedTemplate.lastModified + 
                            " != source.lastModified=" + lastModified);
                    }
                    // Update the source
                    cachedTemplate.source = newlyFoundSource;
                }
            }
            else {
                if(debug) {
                    LOG.debug("Couldn't find template in cache for " + debugName + "; will try to load it.");
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
                LOG.debug("Loading template for " + debugName + " from " + StringUtil.jQuoteNoXSS(newlyFoundSource));
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
                    reader.close();
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
    public long getDelay()
    {
        // synchronized was moved here so that we don't advertise that it's thread-safe, as it's not.
        synchronized (this) {
            return delay;
        }
    }

    /**
     * Sets the delay in milliseconds between checking for newer versions of a
     * template sources.
     * @param delay the new value of the delay
     */
    public void setDelay(long delay)
    {
        // synchronized was moved here so that we don't advertise that it's thread-safe, as it's not.
        synchronized (this) {
            this.delay = delay;
        }
    }

    /**
     * Returns if localized template lookup is enabled or not.
     */
    public boolean getLocalizedLookup()
    {
        // synchronized was moved here so that we don't advertise that it's thread-safe, as it's not.
        synchronized (this) {
            return localizedLookup;
        }
    }

    /**
     * Setis if localized template lookup is enabled or not.
     */
    public void setLocalizedLookup(boolean localizedLookup)
    {
        // synchronized was moved here so that we don't advertise that it's thread-safe, as it's not.
        synchronized (this) {
            this.localizedLookup = localizedLookup;
        }
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
            boolean debug = LOG.isDebugEnabled();
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
            LOG.debug(debugName + " was removed from the cache, if it was there");
        }
    }

    private String buildDebugName(String name, Locale locale, String encoding,
            boolean parse) {
        return StringUtil.jQuoteNoXSS(name) + "["
                + StringUtil.jQuoteNoXSS(locale) + "," + encoding
                + (parse ? ",parsed]" : ",unparsed]");
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
    public static String getFullTemplatePath(Environment env, String currentTemplateDir, String targetTemplatePath) {
        if (!env.isClassicCompatible()) {
            if (targetTemplatePath.indexOf("://") > 0) {
                return targetTemplatePath;
            } else if (targetTemplatePath.startsWith("/"))  {
                int schemeSepIdx = currentTemplateDir.indexOf("://");
                if (schemeSepIdx > 0) {
                    return currentTemplateDir.substring(0, schemeSepIdx + 2) + targetTemplatePath;
                } else {
                    return targetTemplatePath.substring(1);
                }
            } else {
                return currentTemplateDir + targetTemplatePath;
            }
        } else {
            return targetTemplatePath;
        }
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
            return modifyForConfIcI(templateLoader.findTemplateSource(path));
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
            return modifyForConfIcI(templateLoader.findTemplateSource(path));
        }
        String basePath = concatPath(tokpath, 0, lastAsterisk);
        String resourcePath = concatPath(tokpath, lastAsterisk + 1, tokpath.size());
        if(resourcePath.endsWith("/"))
        {
            resourcePath = resourcePath.substring(0, resourcePath.length() - 1);
        }
        StringBuffer buf = new StringBuffer(path.length()).append(basePath);
        int l = basePath.length();
        boolean debug = LOG.isDebugEnabled();
        for(;;)
        {
            String fullPath = buf.append(resourcePath).toString();
            if(debug)
            {
                LOG.debug("Trying to find template source "
                        + StringUtil.jQuoteNoXSS(fullPath));
            }
            Object templateSource = modifyForConfIcI(templateLoader.findTemplateSource(fullPath));
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

    /**
     * If IcI >= 2.3.21, sets {@link URLTemplateSource#setUseCaches(boolean)} to {@code false} for sources that come
     * from a {@link TemplateLoader} where {@link URLConnection} cache usage wasn't set explicitly.  
     */
    private Object modifyForConfIcI(Object templateSource) {
        if (templateSource == null) return null;
        
        if (config.getIncompatibleImprovements().intValue() < _TemplateAPI.VERSION_INT_2_3_21) {
            return templateSource;
        }
        
        if (templateSource instanceof URLTemplateSource) {
            URLTemplateSource urlTemplateSource = (URLTemplateSource) templateSource;
            if (urlTemplateSource.getUseCaches() == null) {  // It was left unset
                urlTemplateSource.setUseCaches(false);
            }
        } else if (templateSource instanceof MultiSource) {
            modifyForConfIcI(((MultiSource) templateSource).getWrappedSource());
        }
        return templateSource;
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
                Boolean.valueOf(!parse).hashCode();
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
