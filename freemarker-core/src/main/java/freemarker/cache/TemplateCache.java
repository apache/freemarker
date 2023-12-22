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
import freemarker.core.TemplateConfiguration;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import freemarker.template._TemplateAPI;
import freemarker.template._VersionInts;
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
public class TemplateCache {
    
    /**
     * The default template update delay; see {@link Configuration#setTemplateUpdateDelayMilliseconds(long)}.
     * 
     * @since 2.3.23
     */
    public static final long DEFAULT_TEMPLATE_UPDATE_DELAY_MILLIS = 5000L;
    
    private static final String ASTERISKSTR = "*";
    private static final char ASTERISK = '*';
    private static final char SLASH = '/';
    private static final String LOCALE_PART_SEPARATOR = "_";
    private static final Logger LOG = Logger.getLogger("freemarker.cache");

    /** Maybe {@code null}. */
    private final TemplateLoader templateLoader;
    
    /** Here we keep our cached templates */
    private final CacheStorage storage;
    private final TemplateLookupStrategy templateLookupStrategy;
    private final TemplateNameFormat templateNameFormat;
    private final TemplateConfigurationFactory templateConfigurations;
    
    private final boolean isStorageConcurrent;
    /** {@link Configuration#setTemplateUpdateDelayMilliseconds(long)} */
    private long updateDelay = DEFAULT_TEMPLATE_UPDATE_DELAY_MILLIS;
    /** {@link Configuration#setLocalizedLookup(boolean)} */
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
    @Deprecated
    public TemplateCache() {
        this(_TemplateAPI.createDefaultTemplateLoader(Configuration.VERSION_2_3_0));
    }

    /**
     * @deprecated Use {@link #TemplateCache(TemplateLoader, CacheStorage, Configuration)} instead.
     */
    @Deprecated
    public TemplateCache(TemplateLoader templateLoader) {
        this(templateLoader, (Configuration) null);
    }

    /**
     * @deprecated Use {@link #TemplateCache(TemplateLoader, CacheStorage, Configuration)} instead.
     */
    @Deprecated
    public TemplateCache(TemplateLoader templateLoader, CacheStorage cacheStorage) {
        this(templateLoader, cacheStorage, null);
    }

    /**
     * Same as {@link #TemplateCache(TemplateLoader, CacheStorage, Configuration)} with a new {@link SoftCacheStorage}
     * as the 2nd parameter.
     * 
     * @since 2.3.21
     */
    public TemplateCache(TemplateLoader templateLoader, Configuration config) {
        this(templateLoader, _TemplateAPI.createDefaultCacheStorage(Configuration.VERSION_2_3_0), config);
    }
    
    /**
     * Same as
     * {@link #TemplateCache(TemplateLoader, CacheStorage, TemplateLookupStrategy, TemplateNameFormat, Configuration)}
     * with {@link TemplateLookupStrategy#DEFAULT_2_3_0} and {@link TemplateNameFormat#DEFAULT_2_3_0}.
     * 
     * @since 2.3.21
     */
    public TemplateCache(TemplateLoader templateLoader, CacheStorage cacheStorage, Configuration config) {
        this(templateLoader, cacheStorage,
                _TemplateAPI.getDefaultTemplateLookupStrategy(Configuration.VERSION_2_3_0),
                _TemplateAPI.getDefaultTemplateNameFormat(Configuration.VERSION_2_3_0),
                config);
    }
    
    /**
     * Same as
     * {@link TemplateCache#TemplateCache(TemplateLoader, CacheStorage, TemplateLookupStrategy, TemplateNameFormat,
     * TemplateConfigurationFactory, Configuration)} with {@code null} for {@code templateConfigurations}-s.
     * 
     * @since 2.3.22
     */
    public TemplateCache(TemplateLoader templateLoader, CacheStorage cacheStorage,
            TemplateLookupStrategy templateLookupStrategy, TemplateNameFormat templateNameFormat,
            Configuration config) {
        this(templateLoader, cacheStorage, templateLookupStrategy, templateNameFormat, null, config);
    }

    /**
     * @param templateLoader
     *            The {@link TemplateLoader} to use. Can be {@code null}, though then every request will result in
     *            {@link TemplateNotFoundException}.
     * @param cacheStorage
     *            The {@link CacheStorage} to use. Can't be {@code null}.
     * @param templateLookupStrategy
     *            The {@link TemplateLookupStrategy} to use. Can't be {@code null}.
     * @param templateNameFormat
     *            The {@link TemplateNameFormat} to use. Can't be {@code null}.
     * @param templateConfigurations
     *            The {@link TemplateConfigurationFactory} to use. Can be {@code null} (then all templates will use the
     *            settings coming from the {@link Configuration} as is).
     * @param config
     *            The {@link Configuration} this cache will be used for. Can be {@code null} for backward compatibility,
     *            as it can be set with {@link #setConfiguration(Configuration)} later.
     * 
     * @since 2.3.24
     */
    public TemplateCache(TemplateLoader templateLoader, CacheStorage cacheStorage,
            TemplateLookupStrategy templateLookupStrategy, TemplateNameFormat templateNameFormat,
            TemplateConfigurationFactory templateConfigurations,
            Configuration config) {
        this.templateLoader = templateLoader;
        
        NullArgumentException.check("cacheStorage", cacheStorage);
        this.storage = cacheStorage;
        isStorageConcurrent = cacheStorage instanceof ConcurrentCacheStorage &&
                ((ConcurrentCacheStorage) cacheStorage).isConcurrent();
        
        NullArgumentException.check("templateLookupStrategy", templateLookupStrategy);
        this.templateLookupStrategy = templateLookupStrategy;

        NullArgumentException.check("templateNameFormat", templateNameFormat);
        this.templateNameFormat = templateNameFormat;

        // Can be null
        this.templateConfigurations = templateConfigurations;
        
        this.config = config;
    }
    
    /**
     * Sets the configuration object to which this cache belongs. This
     * method is called by the configuration itself to establish the
     * relation, and should not be called by users.
     * 
     * @deprecated Use the {@link #TemplateCache(TemplateLoader, CacheStorage, Configuration)} constructor.
     */
    @Deprecated
    public void setConfiguration(Configuration config) {
        this.config = config;
        clear();
    }

    public TemplateLoader getTemplateLoader() {
        return templateLoader;
    }
    
    public CacheStorage getCacheStorage() {
        return storage;
    }
    
    /**
     * @since 2.3.22
     */
    public TemplateLookupStrategy getTemplateLookupStrategy() {
        return templateLookupStrategy;
    }
    
    /**
     * @since 2.3.22
     */
    public TemplateNameFormat getTemplateNameFormat() {
        return templateNameFormat;
    }
    
    /**
     * @since 2.3.24
     */
    public TemplateConfigurationFactory getTemplateConfigurations() {
        return templateConfigurations;
    }

    /**
     * Retrieves the template with the given name (and according the specified further parameters) from the template
     * cache, loading it into the cache first if it's missing/staled.
     * 
     * <p>
     * All parameters must be non-{@code null}, except {@code customLookupCondition}. For the meaning of the parameters
     * see {@link Configuration#getTemplate(String, Locale, String, boolean)}.
     *
     * @return A {@link MaybeMissingTemplate} object that contains the {@link Template}, or a
     *         {@link MaybeMissingTemplate} object that contains {@code null} as the {@link Template} and information
     *         about the missing template. The return value itself is never {@code null}. Note that exceptions occurring
     *         during template loading will not be classified as a missing template, so they will cause an exception to
     *         be thrown by this method instead of returning a {@link MaybeMissingTemplate}. The idea is that having a
     *         missing template is normal (not exceptional), providing that the backing storage mechanism could indeed
     *         check that it's missing.
     * 
     * @throws MalformedTemplateNameException
     *             If the {@code name} was malformed according the current {@link TemplateNameFormat}. However, if the
     *             {@link TemplateNameFormat} is {@link TemplateNameFormat#DEFAULT_2_3_0} and
     *             {@link Configuration#getIncompatibleImprovements()} is less than 2.4.0, then instead of throwing this
     *             exception, a {@link MaybeMissingTemplate} will be returned, similarly as if the template were missing
     *             (the {@link MaybeMissingTemplate#getMissingTemplateReason()} will describe the real error).
     * 
     * @throws IOException
     *             If reading the template has failed from a reason other than the template is missing. This method
     *             should never be a {@link TemplateNotFoundException}, as that condition is indicated in the return
     *             value.
     * 
     * @since 2.3.22
     */
    public MaybeMissingTemplate getTemplate(String name, Locale locale, Object customLookupCondition,
            String encoding, boolean parseAsFTL)
    throws IOException {
        NullArgumentException.check("name", name);
        NullArgumentException.check("locale", locale);
        NullArgumentException.check("encoding", encoding);
        
        try {
            name = templateNameFormat.normalizeRootBasedName(name);
        } catch (MalformedTemplateNameException e) {
            // If we don't have to emulate backward compatible behavior, then just rethrow it: 
            if (templateNameFormat != TemplateNameFormat.DEFAULT_2_3_0
                    || config.getIncompatibleImprovements().intValue() >= _VersionInts.V_2_4_0) {
                throw e;
            }
            return new MaybeMissingTemplate(null, e);
        }
        
        if (templateLoader == null) {
            return new MaybeMissingTemplate(name, "The TemplateLoader was null.");
        }
        
        Template template = getTemplateInternal(name, locale, customLookupCondition, encoding, parseAsFTL);
        return template != null ? new MaybeMissingTemplate(template) : new MaybeMissingTemplate(name, (String) null);
    }    

    /**
     * Similar to {@link #getTemplate(String, Locale, Object, String, boolean)} with {@code null}
     * {@code customLookupCondition}.
     * 
     * @return {@link MaybeMissingTemplate#getTemplate()} of the
     *         {@link #getTemplate(String, Locale, Object, String, boolean)} return value.
     * 
     * @deprecated Use {@link #getTemplate(String, Locale, Object, String, boolean)}, which can return more detailed
     *             result when the template is missing.
     */
    @Deprecated
    public Template getTemplate(String name, Locale locale, String encoding, boolean parseAsFTL)
    throws IOException {
        return getTemplate(name, locale, null, encoding, parseAsFTL).getTemplate();
    }
    
    /**
     * Returns the deprecated default template loader of FreeMarker 2.3.0.
     * 
     * @deprecated The {@link TemplateLoader} should be always specified by the constructor caller.
     */
    @Deprecated
    protected static TemplateLoader createLegacyDefaultTemplateLoader() {
        return _TemplateAPI.createDefaultTemplateLoader(Configuration.VERSION_2_3_0);        
    }
    
    private Template getTemplateInternal(
            final String name, final Locale locale, final Object customLookupCondition,
            final String encoding, final boolean parseAsFTL)
    throws IOException {
        final boolean debug = LOG.isDebugEnabled();
        final String debugName = debug
                ? buildDebugName(name, locale, customLookupCondition, encoding, parseAsFTL)
                : null;
        final TemplateKey tk = new TemplateKey(name, locale, customLookupCondition, encoding, parseAsFTL);
        
        CachedTemplate cachedTemplate;
        if (isStorageConcurrent) {
            cachedTemplate = (CachedTemplate) storage.get(tk);
        } else {
            synchronized (storage) {
                cachedTemplate = (CachedTemplate) storage.get(tk);
            }
        }
        
        final long now = System.currentTimeMillis();
        
        long lastModified = -1L;
        boolean rethrown = false;
        TemplateLookupResult newLookupResult = null;
        try {
            if (cachedTemplate != null) {
                // If we're within the refresh delay, return the cached copy
                if (now - cachedTemplate.lastChecked < updateDelay) {
                    if (debug) {
                        LOG.debug(debugName + " cached copy not yet stale; using cached.");
                    }
                    // Can be null, indicating a cached negative lookup
                    Object t = cachedTemplate.templateOrException;
                    if (t instanceof Template || t == null) {
                        return (Template) t;
                    } else if (t instanceof RuntimeException) {
                        throwLoadFailedException((RuntimeException) t);
                    } else if (t instanceof IOException) {
                        rethrown = true;
                        throwLoadFailedException((IOException) t);
                    }
                    throw new BugException("t is " + t.getClass().getName());
                }
                
                // Clone as the instance bound to the map should be treated as
                // immutable to ensure proper concurrent semantics
                cachedTemplate = cachedTemplate.cloneCachedTemplate();
                // Update the last-checked flag
                cachedTemplate.lastChecked = now;

                // Find the template source
                newLookupResult = lookupTemplate(name, locale, customLookupCondition);

                // Template source was removed
                if (!newLookupResult.isPositive()) {
                    if (debug) {
                        LOG.debug(debugName + " no source found.");
                    } 
                    storeNegativeLookup(tk, cachedTemplate, null);
                    return null;
                }

                // If the source didn't change and its last modified date
                // also didn't change, return the cached version.
                final Object newLookupResultSource = newLookupResult.getTemplateSource();
                lastModified = templateLoader.getLastModified(newLookupResultSource);
                boolean lastModifiedNotChanged = lastModified == cachedTemplate.lastModified;
                boolean sourceEquals = newLookupResultSource.equals(cachedTemplate.source);
                if (lastModifiedNotChanged && sourceEquals) {
                    if (debug) {
                        LOG.debug(debugName + ": using cached since " + newLookupResultSource + " hasn't changed.");
                    }
                    storeCached(tk, cachedTemplate);
                    return (Template) cachedTemplate.templateOrException;
                } else if (debug) {
                    if (!sourceEquals) {
                        LOG.debug("Updating source because: " + 
                            "sourceEquals=" + sourceEquals + 
                            ", newlyFoundSource=" + StringUtil.jQuoteNoXSS(newLookupResultSource) + 
                            ", cached.source=" + StringUtil.jQuoteNoXSS(cachedTemplate.source));
                    } else if (!lastModifiedNotChanged) {
                        LOG.debug("Updating source because: " + 
                            "lastModifiedNotChanged=" + lastModifiedNotChanged + 
                            ", cached.lastModified=" + cachedTemplate.lastModified + 
                            " != source.lastModified=" + lastModified);
                    }
                }
            } else {
                if (debug) {
                    LOG.debug("Couldn't find template in cache for " + debugName + "; will try to load it.");
                }
                
                // Construct a new CachedTemplate entry. Note we set the
                // cachedTemplate.lastModified to Long.MIN_VALUE. This is
                // a flag that signs it has to be explicitly queried later on.
                cachedTemplate = new CachedTemplate();
                cachedTemplate.lastChecked = now;
                
                newLookupResult = lookupTemplate(name, locale, customLookupCondition);
                
                if (!newLookupResult.isPositive()) {
                    storeNegativeLookup(tk, cachedTemplate, null);
                    return null;
                }
                
                cachedTemplate.lastModified = lastModified = Long.MIN_VALUE;
            }

            Object source = newLookupResult.getTemplateSource();
            cachedTemplate.source = source;
            
            // If we get here, then we need to (re)load the template
            if (debug) {
                LOG.debug("Loading template for " + debugName + " from " + StringUtil.jQuoteNoXSS(source));
            }
            
            lastModified = lastModified == Long.MIN_VALUE ? templateLoader.getLastModified(source) : lastModified;            
            Template template = loadTemplate(
                    templateLoader, source,
                    name, newLookupResult.getTemplateSourceName(), locale, customLookupCondition,
                    encoding, parseAsFTL);
            cachedTemplate.templateOrException = template;
            cachedTemplate.lastModified = lastModified;
            storeCached(tk, cachedTemplate);
            return template;
        } catch (RuntimeException e) {
            if (cachedTemplate != null) {
                storeNegativeLookup(tk, cachedTemplate, e);
            }
            throw e;
        } catch (IOException e) {
            if (!rethrown) {
                storeNegativeLookup(tk, cachedTemplate, e);
            }
            throw e;
        } finally {
            if (newLookupResult != null && newLookupResult.isPositive()) {
                templateLoader.closeTemplateSource(newLookupResult.getTemplateSource());
            }
        }
    }

    private static final Method INIT_CAUSE = getInitCauseMethod();
    
    private static final Method getInitCauseMethod() {
        try {
            return Throwable.class.getMethod("initCause", new Class[] { Throwable.class });
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
    
    /**
     * Creates an {@link IOException} that has a cause exception.
     */
    // [Java 6] Remove
    private IOException newIOException(String message, Throwable cause) {
        if (cause == null) {
            return new IOException(message);
        }
        
        IOException ioe;
        if (INIT_CAUSE != null) {
            ioe = new IOException(message);
            try {
                INIT_CAUSE.invoke(ioe, cause);
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new UndeclaredThrowableException(ex);
            }
        } else {
            ioe = new IOException(message + "\nCaused by: " + cause.getClass().getName() + 
            ": " + cause.getMessage());
        }
        return ioe;
    }
    
    private void throwLoadFailedException(Throwable e) throws IOException {
        throw newIOException("There was an error loading the " +
                "template on an earlier attempt; see cause exception.", e);
    }

    private void storeNegativeLookup(TemplateKey tk, 
            CachedTemplate cachedTemplate, Exception e) {
        cachedTemplate.templateOrException = e;
        cachedTemplate.source = null;
        cachedTemplate.lastModified = 0L;
        storeCached(tk, cachedTemplate);
    }

    private void storeCached(TemplateKey tk, CachedTemplate cachedTemplate) {
        if (isStorageConcurrent) {
            storage.put(tk, cachedTemplate);
        } else {
            synchronized (storage) {
                storage.put(tk, cachedTemplate);
            }
        }
    }

    private Template loadTemplate(
            final TemplateLoader templateLoader, final Object source,
            final String name, final String sourceName, Locale locale, final Object customLookupCondition,
            String initialEncoding, final boolean parseAsFTL) throws IOException {
        final TemplateConfiguration tc;
        try {
            tc = templateConfigurations != null ? templateConfigurations.get(sourceName, source) : null;
        } catch (TemplateConfigurationFactoryException e) {
            throw newIOException("Error while getting TemplateConfiguration; see cause exception.", e);
        }
        if (tc != null) {
            // TC.{encoding,locale} is stronger than the cfg.getTemplate arguments by design.
            if (tc.isEncodingSet()) {
                initialEncoding = tc.getEncoding();
            }
            if (tc.isLocaleSet()) {
                locale = tc.getLocale();
            }
        }
        
        Template template;
        {
            if (parseAsFTL) {
                try {
                    try (Reader reader = templateLoader.getReader(source, initialEncoding)) {
                        template = new Template(name, sourceName, reader, config, tc, initialEncoding);
                    }
                } catch (Template.WrongEncodingException wee) {
                    String actualEncoding = wee.getTemplateSpecifiedEncoding();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Initial encoding \"" + initialEncoding + "\" was incorrect, re-reading with \""
                                + actualEncoding + "\". Template: " + sourceName);
                    }

                    try (Reader reader = templateLoader.getReader(source, actualEncoding)) {
                        template = new Template(name, sourceName, reader, config, tc, actualEncoding);
                    }
                }
            } else {
                // Read the contents into a StringWriter, then construct a single-text-block template from it.
                final StringWriter sw = new StringWriter();
                final char[] buf = new char[4096];
                try (Reader reader = templateLoader.getReader(source, initialEncoding)) {
                    fetchChars:
                    while (true) {
                        int charsRead = reader.read(buf);
                        if (charsRead > 0) {
                            sw.write(buf, 0, charsRead);
                        } else if (charsRead < 0) {
                            break fetchChars;
                        }
                    }
                }
                template = Template.getPlainTextTemplate(name, sourceName, sw.toString(), config);
                template.setEncoding(initialEncoding);
            }
        }

        if (tc != null) {
            tc.apply(template);
        }
        
        template.setLocale(locale);
        template.setCustomLookupCondition(customLookupCondition);
        return template;
    }

    /**
     * Gets the delay in milliseconds between checking for newer versions of a
     * template source.
     * @return the current value of the delay
     */
    public long getDelay() {
        // synchronized was moved here so that we don't advertise that it's thread-safe, as it's not.
        synchronized (this) {
            return updateDelay;
        }
    }

    /**
     * Sets the delay in milliseconds between checking for newer versions of a
     * template sources.
     * @param delay the new value of the delay
     */
    public void setDelay(long delay) {
        // synchronized was moved here so that we don't advertise that it's thread-safe, as it's not.
        synchronized (this) {
            this.updateDelay = delay;
        }
    }

    /**
     * Returns if localized template lookup is enabled or not.
     */
    public boolean getLocalizedLookup() {
        // synchronized was moved here so that we don't advertise that it's thread-safe, as it's not.
        synchronized (this) {
            return localizedLookup;
        }
    }

    /**
     * Setis if localized template lookup is enabled or not.
     */
    public void setLocalizedLookup(boolean localizedLookup) {
        // synchronized was moved here so that we don't advertise that it's thread-safe, as it's not.
        synchronized (this) {
            if (this.localizedLookup != localizedLookup) {
                this.localizedLookup = localizedLookup;
                clear();
            }
        }
    }

    /**
     * Removes all entries from the cache, forcing reloading of templates
     * on subsequent {@link #getTemplate(String, Locale, String, boolean)}
     * calls. If the configured template loader is 
     * {@link StatefulTemplateLoader stateful}, then its 
     * {@link StatefulTemplateLoader#resetState()} method is invoked as well.
     */
    public void clear() {
        synchronized (storage) {
            storage.clear();
            if (templateLoader instanceof StatefulTemplateLoader) {
                ((StatefulTemplateLoader) templateLoader).resetState();
            }
        }
    }

    /**
     * Same as {@link #removeTemplate(String, Locale, Object, String, boolean)} with {@code null}
     * {@code customLookupCondition}.
     */
    public void removeTemplate(
            String name, Locale locale, String encoding, boolean parse) throws IOException {
        removeTemplate(name, locale, null, encoding, parse);
    }
    
    /**
     * Removes an entry from the cache, hence forcing the re-loading of it when it's next time requested. (It doesn't
     * delete the template file itself.) This is to give the application finer control over cache updating than
     * {@link #setDelay(long)} alone does.
     * 
     * For the meaning of the parameters, see
     * {@link Configuration#getTemplate(String, Locale, Object, String, boolean, boolean)}
     */
    public void removeTemplate(
            String name, Locale locale, Object customLookupCondition, String encoding, boolean parse)
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
        name = templateNameFormat.normalizeRootBasedName(name);
        if (name != null && templateLoader != null) {
            boolean debug = LOG.isDebugEnabled();
            String debugName = debug
                    ? buildDebugName(name, locale, customLookupCondition, encoding, parse)
                    : null;
            TemplateKey tk = new TemplateKey(name, locale, customLookupCondition, encoding, parse);
            
            if (isStorageConcurrent) {
                storage.remove(tk);
            } else {
                synchronized (storage) {
                    storage.remove(tk);
                }
            }
            if (debug) {
                LOG.debug(debugName + " was removed from the cache, if it was there");
            }
        }
    }

    private String buildDebugName(String name, Locale locale, Object customLookupCondition, String encoding,
            boolean parse) {
        return StringUtil.jQuoteNoXSS(name) + "("
                + StringUtil.jQuoteNoXSS(locale)
                + (customLookupCondition != null ? ", cond=" + StringUtil.jQuoteNoXSS(customLookupCondition) : "")
                + ", " + encoding
                + (parse ? ", parsed)" : ", unparsed]");
    }    

    /**
     * @deprecated Use {@link Environment#toFullTemplateName(String, String)} instead, as that can throw
     *             {@link MalformedTemplateNameException}, and is on a more logical place anyway.
     * 
     * @throws IllegalArgumentException
     *             If the {@code baseName} or {@code targetName} is malformed according the {@link TemplateNameFormat}
     *             in use.
     */
    @Deprecated
    public static String getFullTemplatePath(Environment env, String baseName, String targetName) {
        try {
            return env.toFullTemplateName(baseName, targetName);
        } catch (MalformedTemplateNameException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private TemplateLookupResult lookupTemplate(String name, Locale locale, Object customLookupCondition)
            throws IOException {
        final TemplateLookupResult lookupResult = templateLookupStrategy.lookup(
                new TemplateCacheTemplateLookupContext(name, locale, customLookupCondition));
        if (lookupResult == null) {
            throw new NullPointerException("Lookup result shouldn't be null");
        }
        return lookupResult;
    }

    private TemplateLookupResult lookupTemplateWithAcquisitionStrategy(String path) throws IOException {
        int asterisk = path.indexOf(ASTERISK);
        // Shortcut in case there is no acquisition
        if (asterisk == -1) {
            return TemplateLookupResult.from(path, findTemplateSource(path));
        }
        StringTokenizer tok = new StringTokenizer(path, "/");
        int lastAsterisk = -1;
        List tokpath = new ArrayList();
        while (tok.hasMoreTokens()) {
            String pathToken = tok.nextToken();
            if (pathToken.equals(ASTERISKSTR)) {
                if (lastAsterisk != -1) {
                    tokpath.remove(lastAsterisk);
                }
                lastAsterisk = tokpath.size();
            }
            tokpath.add(pathToken);
        }
        if (lastAsterisk == -1) {  // if there was no real "*" step after all
            return TemplateLookupResult.from(path, findTemplateSource(path));
        }
        String basePath = concatPath(tokpath, 0, lastAsterisk);
        String resourcePath = concatPath(tokpath, lastAsterisk + 1, tokpath.size());
        if (resourcePath.endsWith("/")) {
            resourcePath = resourcePath.substring(0, resourcePath.length() - 1);
        }
        StringBuilder buf = new StringBuilder(path.length()).append(basePath);
        int l = basePath.length();
        for (; ; ) {
            String fullPath = buf.append(resourcePath).toString();
            Object templateSource = findTemplateSource(fullPath);
            if (templateSource != null) {
                return TemplateLookupResult.from(fullPath, templateSource);
            }
            if (l == 0) {
                return TemplateLookupResult.createNegativeResult();
            }
            l = basePath.lastIndexOf(SLASH, l - 2) + 1;
            buf.setLength(l);
        }
    }

    private Object findTemplateSource(String path) throws IOException {
        final Object result = templateLoader.findTemplateSource(path);
        if (LOG.isDebugEnabled()) {
            LOG.debug("TemplateLoader.findTemplateSource(" +  StringUtil.jQuote(path) + "): "
                    + (result == null ? "Not found" : "Found"));
        }
        return modifyForConfIcI(result);
    }

    /**
     * If IcI >= 2.3.21, sets {@link URLTemplateSource#setUseCaches(boolean)} to {@code false} for sources that come
     * from a {@link TemplateLoader} where {@link URLConnection} cache usage wasn't set explicitly.  
     */
    private Object modifyForConfIcI(Object templateSource) {
        if (templateSource == null) return null;
        
        if (config.getIncompatibleImprovements().intValue() < _VersionInts.V_2_3_21) {
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

    private String concatPath(List path, int from, int to) {
        StringBuilder buf = new StringBuilder((to - from) * 16);
        for (int i = from; i < to; ++i) {
            buf.append(path.get(i)).append('/');
        }
        return buf.toString();
    }
    
    /**
     * This class holds a (name, locale) pair and is used as the key in
     * the cached templates map.
     */
    private static final class TemplateKey {
        private final String name;
        private final Locale locale;
        private final Object customLookupCondition;
        private final String encoding;
        private final boolean parse;

        TemplateKey(String name, Locale locale, Object customLookupCondition, String encoding, boolean parse) {
            this.name = name;
            this.locale = locale;
            this.customLookupCondition = customLookupCondition;
            this.encoding = encoding;
            this.parse = parse;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TemplateKey) {
                TemplateKey tk = (TemplateKey) o;
                return
                    parse == tk.parse &&
                    name.equals(tk.name) &&
                    locale.equals(tk.locale) &&
                    nullSafeEquals(customLookupCondition, tk.customLookupCondition) &&
                    encoding.equals(tk.encoding);
            }
            return false;
        }

        private boolean nullSafeEquals(Object o1, Object o2) {
            return o1 != null
                ? (o2 != null ? o1.equals(o2) : false)
                : o2 == null;
        }

        @Override
        public int hashCode() {
            return
                name.hashCode() ^
                locale.hashCode() ^
                encoding.hashCode() ^
                (customLookupCondition != null ? customLookupCondition.hashCode() : 0) ^
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
    private static final class CachedTemplate implements Cloneable, Serializable {
        private static final long serialVersionUID = 1L;

        Object templateOrException;
        Object source;
        long lastChecked;
        long lastModified;
        
        public CachedTemplate cloneCachedTemplate() {
            try {
                return (CachedTemplate) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
    }
    
    private class TemplateCacheTemplateLookupContext extends TemplateLookupContext {

        TemplateCacheTemplateLookupContext(String templateName, Locale templateLocale, Object customLookupCondition) {
            super(templateName, localizedLookup ? templateLocale : null, customLookupCondition);
        }

        @Override
        public TemplateLookupResult lookupWithAcquisitionStrategy(String name) throws IOException {
            // Only one of the possible ways of making a name non-normalized, but is the easiest mistake to do:
            if (name.startsWith("/")) {
                throw new IllegalArgumentException("Non-normalized name, starts with \"/\": " + name);
            }
            
            return TemplateCache.this.lookupTemplateWithAcquisitionStrategy(name);
        }

        @Override
        public TemplateLookupResult lookupWithLocalizedThenAcquisitionStrategy(final String templateName,
                final Locale templateLocale) throws IOException {
            
                if (templateLocale == null) {
                    return lookupWithAcquisitionStrategy(templateName);
                }
                
                int lastDot = templateName.lastIndexOf('.');
                String prefix = lastDot == -1 ? templateName : templateName.substring(0, lastDot);
                String suffix = lastDot == -1 ? "" : templateName.substring(lastDot);
                String localeName = LOCALE_PART_SEPARATOR + templateLocale.toString();
                StringBuilder buf = new StringBuilder(templateName.length() + localeName.length());
                buf.append(prefix);
                tryLocaleNameVariations: while (true) {
                    buf.setLength(prefix.length());
                    String path = buf.append(localeName).append(suffix).toString();
                    TemplateLookupResult lookupResult = lookupWithAcquisitionStrategy(path);
                    if (lookupResult.isPositive()) {
                        return lookupResult;
                    }
                    
                    int lastUnderscore = localeName.lastIndexOf('_');
                    if (lastUnderscore == -1) {
                        break tryLocaleNameVariations;
                    }
                    localeName = localeName.substring(0, lastUnderscore);
                }
                return createNegativeLookupResult();
        }
        
    }
    
    /**
     * Used for the return value of {@link TemplateCache#getTemplate(String, Locale, Object, String, boolean)}.
     * 
     * @since 2.3.22
     */
    public final static class MaybeMissingTemplate {
        
        private final Template template;
        private final String missingTemplateNormalizedName;
        private final String missingTemplateReason;
        private final MalformedTemplateNameException missingTemplateCauseException;
        
        private MaybeMissingTemplate(Template template) {
            this.template = template;
            this.missingTemplateNormalizedName = null;
            this.missingTemplateReason = null;
            this.missingTemplateCauseException = null;
        }
        
        private MaybeMissingTemplate(String normalizedName, MalformedTemplateNameException missingTemplateCauseException) {
            this.template = null;
            this.missingTemplateNormalizedName = normalizedName;
            this.missingTemplateReason = null;
            this.missingTemplateCauseException = missingTemplateCauseException;
        }
        
        private MaybeMissingTemplate(String normalizedName, String missingTemplateReason) {
            this.template = null;
            this.missingTemplateNormalizedName = normalizedName;
            this.missingTemplateReason = missingTemplateReason;
            this.missingTemplateCauseException = null;
        }
        
        /**
         * The {@link Template} if it wasn't missing, otherwise {@code null}.
         */
        public Template getTemplate() {
            return template;
        }

        /**
         * When the template was missing, this <em>possibly</em> contains the explanation, or {@code null}. If the
         * template wasn't missing (i.e., when {@link #getTemplate()} return non-{@code null}) this is always
         * {@code null}.
         */
        public String getMissingTemplateReason() {
            return missingTemplateReason != null
                    ? missingTemplateReason
                    : (missingTemplateCauseException != null
                            ? missingTemplateCauseException.getMalformednessDescription()
                            : null);
        }
        
        /**
         * When the template was missing, this <em>possibly</em> contains its normalized name. If the template wasn't
         * missing (i.e., when {@link #getTemplate()} return non-{@code null}) this is always {@code null}. When the
         * template is missing, it will be {@code null} for example if the normalization itself was unsuccessful.
         */
        public String getMissingTemplateNormalizedName() {
            return missingTemplateNormalizedName;
        }
        
    }
    
}
