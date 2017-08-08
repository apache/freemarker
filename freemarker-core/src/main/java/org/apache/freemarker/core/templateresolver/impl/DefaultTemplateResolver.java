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

import static org.apache.freemarker.core.Configuration.ExtendableBuilder.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.ConfigurationException;
import org.apache.freemarker.core.InvalidSettingValueException;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateConfiguration;
import org.apache.freemarker.core.TemplateLanguage;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.apache.freemarker.core.WrongTemplateCharsetException;
import org.apache.freemarker.core.templateresolver.CacheStorage;
import org.apache.freemarker.core.templateresolver.GetTemplateResult;
import org.apache.freemarker.core.templateresolver.MalformedTemplateNameException;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactoryException;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLoaderSession;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResult;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResultStatus;
import org.apache.freemarker.core.templateresolver.TemplateLoadingSource;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.TemplateNameFormat;
import org.apache.freemarker.core.templateresolver.TemplateResolver;
import org.apache.freemarker.core.templateresolver.TemplateResolverDependencies;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util.UndeclaredThrowableException;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of the {@link TemplateResolver} class; the default value of
 * {@link Configuration#getTemplateResolver() templateResolver} configuration setting is an instance of this.
  */
public class DefaultTemplateResolver extends TemplateResolver {
    
    /**
     * The default template update delay; see {@link Configuration#getTemplateUpdateDelayMilliseconds()}.
     */
    public static final long DEFAULT_TEMPLATE_UPDATE_DELAY_MILLIS = 5000L;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTemplateResolver.class);

    private static final String ASTERISKSTR = "*";
    private static final char ASTERISK = '*';
    private static final char SLASH = '/';
    private static final String LOCALE_PART_SEPARATOR = "_";

    /** Maybe {@code null}. */
    private TemplateLoader templateLoader;
    
    /** Here we keep our cached templates */
    private CacheStorage templateCacheStorage;
    private TemplateLookupStrategy templateLookupStrategy;
    private TemplateNameFormat templateNameFormat;
    private TemplateConfigurationFactory templateConfigurations;
    private long templateUpdateDelayMilliseconds;
    private Charset sourceEncoding;
    private TemplateLanguage templateLanguage;
    private boolean localizedTemplateLookup;

    @Override
    protected void initialize() throws ConfigurationException {
        TemplateResolverDependencies deps = getDependencies();

        this.templateLoader = deps.getTemplateLoader();
        
        this.templateCacheStorage = deps.getTemplateCacheStorage();
        checkDependencyNotNull(TEMPLATE_CACHE_STORAGE_KEY, this.templateCacheStorage);

        Long templateUpdateDelayMilliseconds = deps.getTemplateUpdateDelayMilliseconds();
        checkDependencyNotNull(TEMPLATE_UPDATE_DELAY_KEY, templateUpdateDelayMilliseconds);
        this.templateUpdateDelayMilliseconds = templateUpdateDelayMilliseconds;

        Boolean localizedTemplateLookup = deps.getLocalizedTemplateLookup();
        checkDependencyNotNull(LOCALIZED_TEMPLATE_LOOKUP_KEY, localizedTemplateLookup);
        this.localizedTemplateLookup = localizedTemplateLookup;

        this.templateLookupStrategy = deps.getTemplateLookupStrategy();
        checkDependencyNotNull(TEMPLATE_LOOKUP_STRATEGY_KEY, this.templateLookupStrategy);

        this.templateNameFormat = deps.getTemplateNameFormat();
        checkDependencyNotNull(TEMPLATE_NAME_FORMAT_KEY, this.templateNameFormat);

        // Can be null
        this.templateConfigurations = deps.getTemplateConfigurations();

        this.sourceEncoding = deps.getSourceEncoding();
        checkDependencyNotNull(SOURCE_ENCODING_KEY, this.sourceEncoding);

        this.templateLanguage = deps.getTemplateLanguage();
        checkDependencyNotNull(TEMPLATE_LANGUAGE_KEY, this.templateLanguage);
    }

    private void checkDependencyNotNull(String name, Object value) {
        if (value == null) {
            throw new InvalidSettingValueException(
                    name, null, false,
                    "This Configuration setting must be set and non-null when the TemplateResolver is a(n) "
                    + this.getClass().getName() + ".", null);
        }
    }

    /**
     * Retrieves the template with the given name (and according the specified further parameters) from the template
     * cache, loading it into the cache first if it's missing/staled.
     * 
     * <p>
     * All parameters must be non-{@code null}, except {@code customLookupCondition}. For the meaning of the parameters
     * see {@link Configuration#getTemplate(String, Locale, Serializable, boolean)}.
     *
     * @return A {@link GetTemplateResult} object that contains the {@link Template}, or a
     *         {@link GetTemplateResult} object that contains {@code null} as the {@link Template} and information
     *         about the missing template. The return value itself is never {@code null}. Note that exceptions occurring
     *         during template loading will not be classified as a missing template, so they will cause an exception to
     *         be thrown by this method instead of returning a {@link GetTemplateResult}. The idea is that having a
     *         missing template is normal (not exceptional), providing that the backing storage mechanism could indeed
     *         check that it's missing.
     * 
     * @throws MalformedTemplateNameException
     *             If the {@code name} was malformed according the current {@link TemplateNameFormat}.
     * 
     * @throws IOException
     *             If reading the template has failed from a reason other than the template is missing. This method
     *             should never be a {@link TemplateNotFoundException}, as that condition is indicated in the return
     *             value.
     */
    @Override
    public GetTemplateResult getTemplate(String name, Locale locale, Serializable customLookupCondition)
    throws IOException {
        _NullArgumentException.check("name", name);
        _NullArgumentException.check("locale", locale);

        checkInitialized();

        name = templateNameFormat.normalizeRootBasedName(name);
        
        if (templateLoader == null) {
            return new GetTemplateResult(name, "The TemplateLoader (and TemplateLoader2) was null.");
        }
        
        Template template = getTemplateInternal(name, locale, customLookupCondition);
        return template != null ? new GetTemplateResult(template) : new GetTemplateResult(name, (String) null);
    }

    @Override
    public String toRootBasedName(String baseName, String targetName) throws MalformedTemplateNameException {
        return templateNameFormat.toRootBasedName(baseName, targetName);
    }

    @Override
    public String normalizeRootBasedName(String name) throws MalformedTemplateNameException {
        return templateNameFormat.normalizeRootBasedName(name);
    }

    @Override
    public boolean supportsTemplateLoaderSetting() {
        return true;
    }

    @Override
    public boolean supportsTemplateCacheStorageSetting() {
        return true;
    }

    @Override
    public boolean supportsTemplateLookupStrategySetting() {
        return true;
    }

    @Override
    public boolean supportsTemplateNameFormatSetting() {
        return true;
    }

    @Override
    public boolean supportsTemplateConfigurationsSetting() {
        return true;
    }

    @Override
    public boolean supportsTemplateUpdateDelayMillisecondsSetting() {
        return true;
    }

    @Override
    public boolean supportsLocalizedTemplateLookupSetting() {
        return true;
    }

    private Template getTemplateInternal(
            final String name, final Locale locale, final Serializable customLookupCondition)
    throws IOException {
        final boolean debug = LOG.isDebugEnabled();
        final String debugPrefix = debug
                ? getDebugPrefix("getTemplate", name, locale, customLookupCondition)
                : null;
        final CachedResultKey cacheKey = new CachedResultKey(name, locale, customLookupCondition);
        
        CachedResult oldCachedResult = (CachedResult) templateCacheStorage.get(cacheKey);
        
        final long now = System.currentTimeMillis();
        
        boolean rethrownCachedException = false;
        boolean suppressFinallyException = false;
        TemplateLoaderBasedTemplateLookupResult newLookupResult = null;
        CachedResult newCachedResult = null;
        TemplateLoaderSession session = null;
        try {
            if (oldCachedResult != null) {
                // If we're within the refresh delay, return the cached result
                if (now - oldCachedResult.lastChecked < templateUpdateDelayMilliseconds) {
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
                    templateCacheStorage.put(cacheKey, newCachedResult);
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
                                        .append("cached.source=").append(_StringUtils.jQuoteNoXSS(oldCachedResult.source))
                                        .append(", current.source=").append(_StringUtils.jQuoteNoXSS(newSource));
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
                        + _StringUtils.jQuoteNoXSS(newCachedResult.source) + ")");
            }
            
            Template template = loadTemplate(
                    templateLoaderResult,
                    name, newLookupResult.getTemplateSourceName(), locale, customLookupCondition);
            if (session != null) {
                session.close();
                if (debug) {
                    LOG.debug(debugPrefix + "Session closed.");
                } 
            }
            newCachedResult.templateOrException = template;
            newCachedResult.version = templateLoaderResult.getVersion();
            templateCacheStorage.put(cacheKey, newCachedResult);
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
    
    private static Method getInitCauseMethod() {
        try {
            return Throwable.class.getMethod("initCause", Throwable.class);
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
        templateCacheStorage.put(cacheKey, cachedResult);
    }

    private Template loadTemplate(
            TemplateLoadingResult templateLoaderResult,
            final String name, final String sourceName, Locale locale, final Serializable customLookupCondition)
            throws IOException {
        TemplateConfiguration tc;
        {
            TemplateConfiguration cfgTC;
            try {
                cfgTC = templateConfigurations != null
                        ? templateConfigurations.get(sourceName, templateLoaderResult.getSource()) : null;
            } catch (TemplateConfigurationFactoryException e) {
                throw newIOException("Error while getting TemplateConfiguration; see cause exception.", e);
            }
            TemplateConfiguration templateLoaderResultTC = templateLoaderResult.getTemplateConfiguration();
            if (templateLoaderResultTC != null) {
                TemplateConfiguration.Builder mergedTCBuilder = new TemplateConfiguration.Builder();
                if (cfgTC != null) {
                    mergedTCBuilder.merge(cfgTC);
                }
                mergedTCBuilder.merge(templateLoaderResultTC);

                tc = mergedTCBuilder.build();
            } else {
                tc = cfgTC;
            }
        }

        if (tc != null && tc.isLocaleSet()) {
            locale = tc.getLocale();
        }
        Charset initialEncoding = tc != null && tc.isSourceEncodingSet() ? tc.getSourceEncoding()
                : this.sourceEncoding;
        TemplateLanguage templateLanguage = tc != null && tc.isTemplateLanguageSet() ? tc.getTemplateLanguage()
                : this.templateLanguage;

        Template template;
        {
            Reader reader = templateLoaderResult.getReader();
            InputStream inputStream = templateLoaderResult.getInputStream();
            InputStream markedInputStream;
            if (reader != null) {
                if (inputStream != null) {
                    throw new IllegalStateException("For a(n) " + templateLoaderResult.getClass().getName()
                            + ", both getReader() and getInputStream() has returned non-null.");
                }
                initialEncoding = null;  // No charset decoding has happened
                markedInputStream = null;
            } else if (inputStream != null) {
                if (templateLanguage.getCanSpecifyCharsetInContent()) {
                    // We need mark support, to restart if the charset suggested by <#ftl encoding=...> differs
                    // from that we use initially.
                    if (!inputStream.markSupported()) {
                        inputStream = new BufferedInputStream(inputStream);
                    }
                    inputStream.mark(Integer.MAX_VALUE); // Mark is released after the 1st FTL tag
                    markedInputStream = inputStream;
                } else {
                    markedInputStream = null;
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
                try {
                    template = getDependencies().parse(
                            templateLanguage, name, sourceName, reader, tc,
                            initialEncoding, markedInputStream);
                } catch (WrongTemplateCharsetException charsetException) {
                    final Charset templateSpecifiedEncoding = charsetException.getTemplateSpecifiedEncoding();

                    if (inputStream != null) {
                        // We restart InputStream to re-decode it with the new charset.
                        inputStream.reset();

                        // Don't close `reader`; it's an InputStreamReader that would close the wrapped InputStream.
                        reader = new InputStreamReader(inputStream, templateSpecifiedEncoding);
                    } else {
                        throw new IllegalStateException(
                                "TemplateLanguage " + _StringUtils.jQuote(templateLanguage.getName()) + " has thrown "
                                + WrongTemplateCharsetException.class.getName()
                                + ", but its canSpecifyCharsetInContent property is false.");
                    }

                    template = getDependencies().parse(
                            templateLanguage, name, sourceName, reader, tc,
                            templateSpecifiedEncoding, markedInputStream);
                }
            } finally {
                reader.close();
            }
        }

        template.setLookupLocale(locale);
        template.setCustomLookupCondition(customLookupCondition);
        return template;
    }

    /**
     * Removes all entries from the cache, forcing reloading of templates on subsequent
     * {@link #getTemplate(String, Locale, Serializable)} calls.
     * 
     * @param resetTemplateLoader
     *            Whether to call {@link TemplateLoader#resetState()}. on the template loader.
     */
    public void clearTemplateCache(boolean resetTemplateLoader) {
        synchronized (templateCacheStorage) {
            templateCacheStorage.clear();
            if (templateLoader != null && resetTemplateLoader) {
                templateLoader.resetState();
            }
        }
    }
    
    /**
     * Same as {@link #clearTemplateCache(boolean)} with {@code true} {@code resetTemplateLoader} argument.
     */
    @Override
    public void clearTemplateCache() {
        synchronized (templateCacheStorage) {
            templateCacheStorage.clear();
            if (templateLoader != null) {
                templateLoader.resetState();
            }
        }
    }

    @Override
    public void removeTemplateFromCache(
            String name, Locale locale, Serializable customLookupCondition)
    throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("Argument \"name\" can't be null");
        }
        if (locale == null) {
            throw new IllegalArgumentException("Argument \"locale\" can't be null");
        }
        name = templateNameFormat.normalizeRootBasedName(name);
        if (name != null && templateLoader != null) {
            boolean debug = LOG.isDebugEnabled();
            String debugPrefix = debug
                    ? getDebugPrefix("removeTemplate", name, locale, customLookupCondition)
                    : null;
            CachedResultKey tk = new CachedResultKey(name, locale, customLookupCondition);
            
            templateCacheStorage.remove(tk);
            if (debug) {
                LOG.debug(debugPrefix + "Template was removed from the cache, if it was there");
            }
        }
    }

    private String getDebugPrefix(String operation, String name, Locale locale, Object customLookupCondition) {
        return operation + " " + _StringUtils.jQuoteNoXSS(name) + "("
                + _StringUtils.jQuoteNoXSS(locale)
                + (customLookupCondition != null ? ", cond=" + _StringUtils.jQuoteNoXSS(customLookupCondition) : "")
                + "): ";
    }    

    /**
     * Looks up according the {@link TemplateLookupStrategy} and then starts reading the template, if it was changed
     * compared to the cached result, or if there was no cached result yet.
     */
    private TemplateLoaderBasedTemplateLookupResult lookupAndLoadTemplateIfChanged(
            String name, Locale locale, Object customLookupCondition,
            TemplateLoadingSource cachedResultSource, Serializable cachedResultVersion,
            TemplateLoaderSession session) throws IOException {
        final TemplateLoaderBasedTemplateLookupResult lookupResult = templateLookupStrategy.lookup(
                new DefaultTemplateResolverTemplateLookupContext(
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
        private final Serializable customLookupCondition;

        CachedResultKey(String name, Locale locale, Serializable customLookupCondition) {
            this.name = name;
            this.locale = locale;
            this.customLookupCondition = customLookupCondition;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CachedResultKey)) {
                return false;
            }
            CachedResultKey tk = (CachedResultKey) o;
            return
                    name.equals(tk.name) &&
                    locale.equals(tk.locale) &&
                    nullSafeEquals(customLookupCondition, tk.customLookupCondition);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + locale.hashCode();
            result = 31 * result + (customLookupCondition != null ? customLookupCondition.hashCode() : 0);
            return result;
        }

    }

    /**
     * Hold the a cached {@link #getTemplate(String, Locale, Serializable)} result and the associated
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
    
    private class DefaultTemplateResolverTemplateLookupContext extends TemplateLoaderBasedTemplateLookupContext {

        private final TemplateLoaderSession session; 
        
        DefaultTemplateResolverTemplateLookupContext(String templateName, Locale templateLocale, Object customLookupCondition,
                TemplateLoadingSource cachedResultSource, Serializable cachedResultVersion,
                TemplateLoaderSession session) {
            super(templateName, localizedTemplateLookup ? templateLocale : null, customLookupCondition,
                    cachedResultSource, cachedResultVersion);
            this.session = session;
        }

        @Override
        public TemplateLoaderBasedTemplateLookupResult lookupWithAcquisitionStrategy(String path) throws IOException {
            // Only one of the possible ways of making a name non-normalized, but is the easiest mistake to do:
            if (path.startsWith("/")) {
                throw new IllegalArgumentException("Non-normalized name, starts with \"/\": " + path);
            }
            
            int asterisk = path.indexOf(ASTERISK);
            // Shortcut in case there is no acquisition
            if (asterisk == -1) {
                return createLookupResult(
                        path,
                        templateLoader.load(path, getCachedResultSource(), getCachedResultVersion(), session));
            }
            StringTokenizer pathTokenizer = new StringTokenizer(path, "/");
            int lastAsterisk = -1;
            List<String> pathSteps = new ArrayList<>();
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
                return createLookupResult(
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
                    return createLookupResult(fullPath, templateLoaderResult);
                }
                if (basePathLen == 0) {
                    return createNegativeLookupResult();
                }
                basePathLen = basePath.lastIndexOf(SLASH, basePathLen - 2) + 1;
                buf.setLength(basePathLen);
            }
        }

        @Override
        public TemplateLoaderBasedTemplateLookupResult lookupWithLocalizedThenAcquisitionStrategy(final String templateName,
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
                    TemplateLoaderBasedTemplateLookupResult lookupResult = lookupWithAcquisitionStrategy(path);
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
    
}
