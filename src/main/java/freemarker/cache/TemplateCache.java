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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.core.BugException;
import freemarker.core.Environment;
import freemarker.core.MarkReleaserTemplateSpecifiedEncodingHandler;
import freemarker.core.TemplateConfiguration;
import freemarker.core.TemplateSpecifiedEncodingHandler;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
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
    private static final Logger LOG = LoggerFactory.getLogger("freemarker.cache");

    /** Maybe {@code null}. */
    private final TemplateLoader templateLoader;
    
    /** Here we keep our cached templates */
    private final CacheStorage cacheStorage;
    private final TemplateLookupStrategy templateLookupStrategy;
    private final TemplateNameFormat templateNameFormat;
    private final TemplateConfigurationFactory templateConfigurations;
    
    private final boolean isCacheStorageConcurrent;
    /** {@link Configuration#setTemplateUpdateDelayMilliseconds(long)} */
    private long updateDelay = DEFAULT_TEMPLATE_UPDATE_DELAY_MILLIS;
    /** {@link Configuration#setLocalizedLookup(boolean)} */
    private boolean localizedLookup = true;

    private Configuration config;

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
     *            The {@link TemplateLoader} to use. Can be {@code null}, though than you won't be able to load
     *            anything.
     * @param cacheStorage
     *            The {@link CacheStorage} to use. Can't be {@code null}.
     * @param templateLookupStrategy
     *            The {@link TemplateLookupStrategy} to use. Can't be {@code null}.
     * @param templateNameFormat
     *            The {@link TemplateNameFormat} to use. Can't be {@code null}.
     * @param templateConfigurations
     *            The {@link TemplateConfigurationFactory} to use. Can be {@code null} (then all templates will use the
     *            settings coming from the {@link Configuration} as is, except in the very rare case where a
     *            {@link TemplateLoader} itself specifies a {@link TemplateConfiguration}).
     * @param config
     *            The {@link Configuration} this cache will be used for. Can't be {@code null}.
     * 
     * @since 2.3.24
     */
    public TemplateCache(TemplateLoader templateLoader, CacheStorage cacheStorage,
            TemplateLookupStrategy templateLookupStrategy, TemplateNameFormat templateNameFormat,
            TemplateConfigurationFactory templateConfigurations,
            Configuration config) {
        this.templateLoader = templateLoader;
        
        NullArgumentException.check("cacheStorage", cacheStorage);
        this.cacheStorage = cacheStorage;
        isCacheStorageConcurrent = cacheStorage instanceof ConcurrentCacheStorage &&
                ((ConcurrentCacheStorage) cacheStorage).isConcurrent();
        
        NullArgumentException.check("templateLookupStrategy", templateLookupStrategy);
        this.templateLookupStrategy = templateLookupStrategy;

        NullArgumentException.check("templateNameFormat", templateNameFormat);
        this.templateNameFormat = templateNameFormat;

        // Can be null
        this.templateConfigurations = templateConfigurations;
        
        NullArgumentException.check("config", config);
        this.config = config;
    }
    
    /**
     * Returns the configuration for internal usage.
     */
    // [FM3] After setConfiguration was removed, this can be too.
    Configuration getConfiguration() {
        return config;
    }

    public TemplateLoader getTemplateLoader() {
        return templateLoader;
    }

    public CacheStorage getCacheStorage() {
        return cacheStorage;
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
            name = templateNameFormat.normalizeAbsoluteName(name);
        } catch (MalformedTemplateNameException e) {
            // If we don't have to emulate backward compatible behavior, then just rethrow it: 
            if (templateNameFormat != TemplateNameFormat.DEFAULT_2_3_0
                    || config.getIncompatibleImprovements().intValue() >= _TemplateAPI.VERSION_INT_2_4_0) {
                throw e;
            }
            return new MaybeMissingTemplate(null, e);
        }
        
        if (templateLoader == null) {
            return new MaybeMissingTemplate(name, "The TemplateLoader (and TemplateLoader2) was null.");
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
    
    private Template getTemplateInternal(
            final String name, final Locale locale, final Object customLookupCondition,
            final String encoding, final boolean parseAsFTL)
    throws IOException {
        final boolean debug = LOG.isDebugEnabled();
        final String debugPrefix = debug
                ? getDebugPrefix("getTemplate", name, locale, customLookupCondition, encoding, parseAsFTL)
                : null;
        final CachedResultKey cacheKey = new CachedResultKey(name, locale, customLookupCondition, encoding, parseAsFTL);
        
        CachedResult oldCachedResult;
        if (isCacheStorageConcurrent) {
            oldCachedResult = (CachedResult) cacheStorage.get(cacheKey);
        } else {
            synchronized (cacheStorage) {
                oldCachedResult = (CachedResult) cacheStorage.get(cacheKey);
            }
        }
        
        final long now = System.currentTimeMillis();
        
        boolean rethrownCachedException = false;
        boolean suppressFinallyException = false;
        TemplateLookupResult newLookupResult = null;
        CachedResult newCachedResult = null;
        TemplateLoaderSession session = null;
        try {
            if (oldCachedResult != null) {
                // If we're within the refresh delay, return the cached result
                if (now - oldCachedResult.lastChecked < updateDelay) {
                    if (debug) {
                        LOG.debug(debugPrefix + "Cached copy not yet stale; using cached.");
                    }
                    Object t = oldCachedResult.templateOrException;
                    // t can be null, indicating a cached negative lookup
                    if (t instanceof Template || t == null) {
                        return (Template) t;
                    } else if (t instanceof RuntimeException) {
                        rethrowCachedException((RuntimeException) t);
                    } else if (t instanceof IOException) {
                        rethrownCachedException = true;
                        rethrowCachedException((IOException) t);
                    }
                    throw new BugException("Unhandled class for t: " + t.getClass().getName());
                }
                // The freshness of the cache result must be checked.
                
                // Clone, as the instance in the cache store must not be modified to ensure proper concurrent behavior.
                newCachedResult = oldCachedResult.clone();
                newCachedResult.lastChecked = now;

                session = templateLoader.createSession();
                if (debug && session != null) {
                    LOG.debug(debugPrefix + "Session created.");
                }
                
                // Find the template source, load it if it doesn't correspond to the cached result.
                newLookupResult = lookupAndLoadTemplateIfChanged(
                        name, locale, customLookupCondition, oldCachedResult.source, oldCachedResult.version, session);

                // Template source was removed (TemplateLoader2ResultStatus.NOT_FOUND, or no TemplateLoader2Result)
                if (!newLookupResult.isPositive()) { 
                    if (debug) {
                        LOG.debug(debugPrefix + "No source found.");
                    } 
                    setToNegativeAndPutIntoCache(cacheKey, newCachedResult, null);
                    return null;
                }

                final TemplateLoadingResult newTemplateLoaderResult = newLookupResult.getTemplateLoaderResult();
                if (newTemplateLoaderResult.getStatus() == TemplateLoadingResultStatus.NOT_MODIFIED) {
                    // Return the cached version.
                    if (debug) {
                        LOG.debug(debugPrefix + ": Using cached template "
                                + "(source: " + newTemplateLoaderResult.getSource() + ")"
                                + " as it hasn't been changed on the backing store.");
                    }
                    putIntoCache(cacheKey, newCachedResult);
                    return (Template) newCachedResult.templateOrException;
                } else {
                    if (newTemplateLoaderResult.getStatus() != TemplateLoadingResultStatus.OPENED) {
                        // TemplateLoader2ResultStatus.NOT_FOUND was already handler earlier
                        throw new BugException("Unxpected status: " + newTemplateLoaderResult.getStatus());
                    }
                    if (debug) {
                        StringBuilder debugMsg = new StringBuilder();
                        debugMsg.append(debugPrefix)
                                .append("Reloading template instead of using the cached result because ");
                        if (newCachedResult.templateOrException instanceof Throwable) {
                            debugMsg.append("it's a cached error (retrying).");
                        } else {
                            Object newSource = newTemplateLoaderResult.getSource();
                            if (!nullSafeEquals(newSource, oldCachedResult.source)) {
                                debugMsg.append("the source has been changed: ")
                                        .append("cached.source=").append(StringUtil.jQuoteNoXSS(oldCachedResult.source))
                                        .append(", current.source=").append(StringUtil.jQuoteNoXSS(newSource));
                            } else {
                                Serializable newVersion = newTemplateLoaderResult.getVersion();
                                if (!nullSafeEquals(oldCachedResult.version, newVersion)) {
                                    debugMsg.append("the version has been changed: ")
                                            .append("cached.version=").append(oldCachedResult.version) 
                                            .append(", current.version=").append(newVersion);
                                } else {
                                    debugMsg.append("??? (unknown reason)");
                                }
                            }
                        }
                        LOG.debug(debugMsg.toString());
                    }
                }
            } else { // if there was no cached result
                if (debug) {
                    LOG.debug(debugPrefix + "No cached result was found; will try to load template.");
                }
                
                newCachedResult = new CachedResult();
                newCachedResult.lastChecked = now;
            
                session = templateLoader.createSession();
                if (debug && session != null) {
                    LOG.debug(debugPrefix + "Session created.");
                } 
                
                newLookupResult = lookupAndLoadTemplateIfChanged(
                        name, locale, customLookupCondition, null, null, session);
                
                if (!newLookupResult.isPositive()) {
                    setToNegativeAndPutIntoCache(cacheKey, newCachedResult, null);
                    return null;
                }
            }
            // We have newCachedResult and newLookupResult initialized at this point.

            TemplateLoadingResult templateLoaderResult = newLookupResult.getTemplateLoaderResult();
            newCachedResult.source = templateLoaderResult.getSource();
            
            // If we get here, then we need to (re)load the template
            if (debug) {
                LOG.debug(debugPrefix + "Reading template content (source: "
                        + StringUtil.jQuoteNoXSS(newCachedResult.source) + ")");
            }
            
            Template template = loadTemplate(
                    templateLoaderResult,
                    name, newLookupResult.getTemplateSourceName(), locale, customLookupCondition,
                    encoding, parseAsFTL);
            if (session != null) {
                session.close();
                if (debug) {
                    LOG.debug(debugPrefix + "Session closed.");
                } 
            }
            newCachedResult.templateOrException = template;
            newCachedResult.version = templateLoaderResult.getVersion();
            putIntoCache(cacheKey, newCachedResult);
            return template;
        } catch (RuntimeException e) {
            if (newCachedResult != null) {
                setToNegativeAndPutIntoCache(cacheKey, newCachedResult, e);
            }
            suppressFinallyException = true;
            throw e;
        } catch (IOException e) {
            // Rethrown cached exceptions are wrapped into IOException-s, so we only need this condition here.
            if (!rethrownCachedException) {
                setToNegativeAndPutIntoCache(cacheKey, newCachedResult, e);
            }
            suppressFinallyException = true;
            throw e;
        } finally {
            try {
                // Close streams first:
                
                if (newLookupResult != null && newLookupResult.isPositive()) {
                    TemplateLoadingResult templateLoaderResult = newLookupResult.getTemplateLoaderResult();
                    Reader reader = templateLoaderResult.getReader();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) { // [FM3] Exception e
                            if (suppressFinallyException) {
                                if (LOG.isWarnEnabled()) { 
                                    LOG.warn("Failed to close template content Reader for: " + name, e);
                                }
                            } else {
                                suppressFinallyException = true;
                                throw e;
                            }
                        }
                    } else if (templateLoaderResult.getInputStream() != null) {
                        try {
                            templateLoaderResult.getInputStream().close();
                        } catch (IOException e) { // [FM3] Exception e
                            if (suppressFinallyException) {
                                if (LOG.isWarnEnabled()) { 
                                    LOG.warn("Failed to close template content InputStream for: " + name, e);
                                }
                            } else {
                                suppressFinallyException = true;
                                throw e;
                            }
                        }
                    }
                }
            } finally {
                // Then close streams:
                
                if (session != null && !session.isClosed()) {
                    try {
                        session.close();
                        if (debug) {
                            LOG.debug(debugPrefix + "Session closed.");
                        } 
                    } catch (IOException e) { // [FM3] Exception e
                        if (suppressFinallyException) {
                            if (LOG.isWarnEnabled()) { 
                                LOG.warn("Failed to close template loader session for" + name, e);
                            }
                        } else {
                            suppressFinallyException = true;
                            throw e;
                        }
                    }
                }
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
    
    private void rethrowCachedException(Throwable e) throws IOException {
        throw newIOException("There was an error loading the " +
                "template on an earlier attempt; see cause exception.", e);
    }

    private void setToNegativeAndPutIntoCache(CachedResultKey cacheKey, CachedResult cachedResult, Exception e) {
        cachedResult.templateOrException = e;
        cachedResult.source = null;
        cachedResult.version = null;
        putIntoCache(cacheKey, cachedResult);
    }

    private void putIntoCache(CachedResultKey tk, CachedResult cachedTemplate) {
        if (isCacheStorageConcurrent) {
            cacheStorage.put(tk, cachedTemplate);
        } else {
            synchronized (cacheStorage) {
                cacheStorage.put(tk, cachedTemplate);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private Template loadTemplate(
            TemplateLoadingResult templateLoaderResult,
            final String name, final String sourceName, Locale locale, final Object customLookupCondition,
            String initialEncoding, final boolean parseAsFTL) throws IOException {
        TemplateConfiguration tc;
        {
            TemplateConfiguration cfgTC;
            try {
                cfgTC = templateConfigurations != null
                        ? templateConfigurations.get(sourceName, templateLoaderResult.getSource()) : null;
            } catch (TemplateConfigurationFactoryException e) {
                throw newIOException("Error while getting TemplateConfiguration; see cause exception.", e);
            }
            TemplateConfiguration resultTC = templateLoaderResult.getTemplateConfiguration();
            if (resultTC != null) {
                TemplateConfiguration mergedTC = new TemplateConfiguration();
                if (cfgTC != null) {
                    mergedTC.merge(cfgTC);
                }
                if (resultTC != null) {
                    mergedTC.merge(resultTC);
                }
                mergedTC.setParentConfiguration(config);
                
                tc = mergedTC;
            } else {
                tc = cfgTC;
            }
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
            Reader reader = templateLoaderResult.getReader();
            InputStream inputStream = templateLoaderResult.getInputStream();
            TemplateSpecifiedEncodingHandler templateSpecifiedEncodingHandler;
            if (reader != null) {
                if (inputStream != null) {
                    throw new IllegalStateException("For a(n) " + templateLoaderResult.getClass().getName()
                            + ", both getReader() and getInputStream() has returned non-null.");
                }
                initialEncoding = null;  // No charset decoding has happened
                templateSpecifiedEncodingHandler = TemplateSpecifiedEncodingHandler.DEFAULT; 
            } else if (inputStream != null) {
                if (parseAsFTL) {
                    // We need mark support, to restart if the charset suggested by <#ftl encoding=...> differs
                    // from that we use initially.
                    if (!inputStream.markSupported()) {
                        inputStream = new BufferedInputStream(inputStream);
                    }
                    inputStream.mark(Integer.MAX_VALUE); // Mark is released after the 1st FTL tag
                    templateSpecifiedEncodingHandler = new MarkReleaserTemplateSpecifiedEncodingHandler(inputStream);
                } else {
                    templateSpecifiedEncodingHandler = null; 
                }
                // Regarding buffering worries: On the Reader side we should only read in chunks (like through a
                // BufferedReader), so there shouldn't be a problem if the InputStream is not buffered. (Also, at least
                // on Oracle JDK and OpenJDK 7 the InputStreamReader itself has an internal ~8K buffer.)
                reader = new InputStreamReader(inputStream, initialEncoding);
            } else {
                throw new IllegalStateException("For a(n) " + templateLoaderResult.getClass().getName()
                        + ", both getReader() and getInputStream() has returned null.");
            }
            
            try {
                if (parseAsFTL) {
                    try {
                        template = new Template(name, sourceName, reader, config, tc,
                                initialEncoding, templateSpecifiedEncodingHandler);
                    } catch (Template.WrongEncodingException wee) {
                        final String templateSpecifiedEncoding = wee.getTemplateSpecifiedEncoding();
                        
                        if (inputStream != null) {
                            // We restart InputStream to re-decode it with the new charset.
                            inputStream.reset();
                            
                            // Don't close `reader`; it's an InputStreamReader that would close the wrapped InputStream.
                            reader = new InputStreamReader(inputStream, templateSpecifiedEncoding);
                        } else {
                            // Should be impossible to get here
                            throw new BugException();
                        }
                        
                        template = new Template(name, sourceName, reader, config, tc,
                                templateSpecifiedEncoding, templateSpecifiedEncodingHandler);
                    }
                } else {
                    // Read the contents into a StringWriter, then construct a single-text-block template from it.
                    final StringBuilder sb = new StringBuilder();
                    final char[] buf = new char[4096];
                    int charsRead;
                    while ((charsRead = reader.read(buf)) > 0) {
                        sb.append(buf, 0, charsRead);
                    }
                    template = Template.getPlainTextTemplate(name, sourceName, sb.toString(), config);
                    template.setEncoding(initialEncoding);
                }
            } finally {
                reader.close();
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
     * Removes all entries from the cache, forcing reloading of templates on subsequent
     * {@link #getTemplate(String, Locale, String, boolean)} calls.
     * 
     * @param resetTemplateLoader
     *            Whether to call {@link TemplateLoader#resetState()}. on the template loader.
     */
    public void clear(boolean resetTemplateLoader) {
        synchronized (cacheStorage) {
            cacheStorage.clear();
            if (templateLoader != null && resetTemplateLoader) {
                templateLoader.resetState();
            }
        }
    }
    
    /**
     * Same as {@link #clear(boolean)} with {@code true} {@code resetTemplateLoader} argument.
     */
    public void clear() {
        synchronized (cacheStorage) {
            cacheStorage.clear();
            if (templateLoader != null) {
                templateLoader.resetState();
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
        name = templateNameFormat.normalizeAbsoluteName(name);
        if (name != null && templateLoader != null) {
            boolean debug = LOG.isDebugEnabled();
            String debugPrefix = debug
                    ? getDebugPrefix("removeTemplate", name, locale, customLookupCondition, encoding, parse)
                    : null;
            CachedResultKey tk = new CachedResultKey(name, locale, customLookupCondition, encoding, parse);
            
            if (isCacheStorageConcurrent) {
                cacheStorage.remove(tk);
            } else {
                synchronized (cacheStorage) {
                    cacheStorage.remove(tk);
                }
            }
            if (debug) {
                LOG.debug(debugPrefix + "Template was removed from the cache, if it was there");
            }
        }
    }

    private String getDebugPrefix(String operation, String name, Locale locale, Object customLookupCondition, String encoding,
            boolean parse) {
        return operation + " " + StringUtil.jQuoteNoXSS(name) + "("
                + StringUtil.jQuoteNoXSS(locale)
                + (customLookupCondition != null ? ", cond=" + StringUtil.jQuoteNoXSS(customLookupCondition) : "")
                + ", " + encoding
                + (parse ? ", parsed)" : ", unparsed]")
                + ": ";
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

    /**
     * Looks up according the {@link TemplateLookupStrategy} and then starts reading the template, if it was changed
     * compared to the cached result, or if there was no cached result yet.
     */
    private TemplateLookupResult lookupAndLoadTemplateIfChanged(
            String name, Locale locale, Object customLookupCondition,
            TemplateLoadingSource cachedResultSource, Serializable cachedResultVersion,
            TemplateLoaderSession session) throws IOException {
        final TemplateLookupResult lookupResult = templateLookupStrategy.lookup(
                new TemplateCacheTemplateLookupContext(
                        name, locale, customLookupCondition,
                        cachedResultSource, cachedResultVersion,
                        session));
        if (lookupResult == null) {
            throw new NullPointerException("Lookup result shouldn't be null");
        }
        return lookupResult;
    }

    private String concatPath(List<String> pathSteps, int from, int to) {
        StringBuilder buf = new StringBuilder((to - from) * 16);
        for (int i = from; i < to; ++i) {
            buf.append(pathSteps.get(i));
            if (i < pathSteps.size() - 1) {
                buf.append('/');
            }
        }
        return buf.toString();
    }
    
    // Replace with Objects.equals in Java 7
    private static boolean nullSafeEquals(Object o1, Object o2) {
        if (o1 == o2) return true;
        if (o1 == null || o2 == null) return false;
        return o1.equals(o2);
    }
    
    /**
     * Used as cache key to look up a {@link CachedResult}. 
     */
    @SuppressWarnings("serial")
    private static final class CachedResultKey implements Serializable {
        private final String name;
        private final Locale locale;
        private final Object customLookupCondition;
        private final String encoding;
        private final boolean parse;

        CachedResultKey(String name, Locale locale, Object customLookupCondition, String encoding, boolean parse) {
            this.name = name;
            this.locale = locale;
            this.customLookupCondition = customLookupCondition;
            this.encoding = encoding;
            this.parse = parse;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CachedResultKey) {
                CachedResultKey tk = (CachedResultKey) o;
                return
                    parse == tk.parse &&
                    name.equals(tk.name) &&
                    locale.equals(tk.locale) &&
                    nullSafeEquals(customLookupCondition, tk.customLookupCondition) &&
                    encoding.equals(tk.encoding);
            }
            return false;
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
     * Hold the a cached {@link #getTemplate(String, Locale, Object, String, boolean)} result and the associated
     * information needed to check if the cached value is up to date.
     * 
     * <p>
     * Note: this class is Serializable to allow custom 3rd party CacheStorage implementations to serialize/replicate
     * them; FreeMarker code itself doesn't rely on its serializability.
     * 
     * @see CachedResultKey
     */
    private static final class CachedResult implements Cloneable, Serializable {
        private static final long serialVersionUID = 1L;

        Object templateOrException;
        TemplateLoadingSource source;
        Serializable version;
        long lastChecked;
        
        @Override
        public CachedResult clone() {
            try {
                return (CachedResult) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
    }
    
    private class TemplateCacheTemplateLookupContext extends TemplateLookupContext {

        private final TemplateLoaderSession session; 
        
        TemplateCacheTemplateLookupContext(String templateName, Locale templateLocale, Object customLookupCondition,
                TemplateLoadingSource cachedResultSource, Serializable cachedResultVersion,
                TemplateLoaderSession session) {
            super(templateName, localizedLookup ? templateLocale : null, customLookupCondition,
                    cachedResultSource, cachedResultVersion);
            this.session = session;
        }

        @Override
        public TemplateLookupResult lookupWithAcquisitionStrategy(String path) throws IOException {
            // Only one of the possible ways of making a name non-normalized, but is the easiest mistake to do:
            if (path.startsWith("/")) {
                throw new IllegalArgumentException("Non-normalized name, starts with \"/\": " + path);
            }
            
            int asterisk = path.indexOf(ASTERISK);
            // Shortcut in case there is no acquisition
            if (asterisk == -1) {
                return TemplateLookupResult.from(
                        path,
                        templateLoader.load(path, getCachedResultSource(), getCachedResultVersion(), session));
            }
            StringTokenizer pathTokenizer = new StringTokenizer(path, "/");
            int lastAsterisk = -1;
            List<String> pathSteps = new ArrayList<String>();
            while (pathTokenizer.hasMoreTokens()) {
                String pathStep = pathTokenizer.nextToken();
                if (pathStep.equals(ASTERISKSTR)) {
                    if (lastAsterisk != -1) {
                        pathSteps.remove(lastAsterisk);
                    }
                    lastAsterisk = pathSteps.size();
                }
                pathSteps.add(pathStep);
            }
            if (lastAsterisk == -1) {  // if there was no real "*" step after all
                return TemplateLookupResult.from(
                        path,
                        templateLoader.load(path, getCachedResultSource(), getCachedResultVersion(), session));
            }
            String basePath = concatPath(pathSteps, 0, lastAsterisk);
            String postAsteriskPath = concatPath(pathSteps, lastAsterisk + 1, pathSteps.size());
            StringBuilder buf = new StringBuilder(path.length()).append(basePath);
            int basePathLen = basePath.length();
            while (true) {
                String fullPath = buf.append(postAsteriskPath).toString();
                TemplateLoadingResult templateLoaderResult = templateLoader.load(
                        fullPath, getCachedResultSource(), getCachedResultVersion(), session);
                if (templateLoaderResult.getStatus() == TemplateLoadingResultStatus.OPENED) {
                    return TemplateLookupResult.from(fullPath, templateLoaderResult);
                }
                if (basePathLen == 0) {
                    return TemplateLookupResult.createNegativeResult();
                }
                basePathLen = basePath.lastIndexOf(SLASH, basePathLen - 2) + 1;
                buf.setLength(basePathLen);
            }
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
