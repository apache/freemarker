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
package org.apache.freemarker.core.templateresolver;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.ConfigurationException;
import org.apache.freemarker.core.ParseException;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;
import org.apache.freemarker.core.util._NullArgumentException;

/**
 * This class allows users to fully implement the template lookup, loading and caching logic,
 * in case the standard mechanism (a {@link DefaultTemplateResolver} combined with all the {@link Configuration}
 * settings like {@link Configuration#getTemplateLoader() templateLoader},
 * {@link Configuration#getTemplateConfigurations() templateConfigurations}, etc.) is not flexible enough.
 * The {@link TemplateResolver} is put into use by setting the
 * {@link Configuration#getTemplateResolver() templateResolver} configuration setting.
 * <p>
 * A custom {@link TemplateResolver} can depend on a selected set of {@link Configuration} settings; the same ones that
 * {@link DefaultTemplateResolver} depends on. These settings are collected into the
 * {@link TemplateResolverDependencies} class, and the {@link TemplateResolver} should get them in {@link #initialize()}
 * via {@link #getDependencies()}. It's possible that the custom {@link TemplateResolver} only uses some of these
 * settings, which should be reflected by the return value of the {@code supportsXxxSetting} methods (like
 * {@link #supportsTemplateLoaderSetting()}). (Note that there's no {@code supportsXxxSetting} method for
 * {@link Configuration#getTemplateLanguage() templateLanguage} and {@link Configuration#getSourceEncoding()
 * sourceEncoding}, as the must always be supported and are always exposed.) {@link TemplateResolverDependencies}
 * will also expose the {@link TemplateResolverDependencies#parse} method, which is used to create a {@link Template}
 * from its source code in the later {@link #getTemplate(String, Locale, Serializable)} calls.
 */
public abstract class TemplateResolver {

    private TemplateResolverDependencies dependencies;
    private boolean initialized;

    /**
     * Called by FreeMarker when the {@link Configuration} is built; normally you do not call it yourself.
     * This automatically calls {@link #initialize()}.
     *
     * @throws IllegalStateException If this method was already called with another {@link Configuration} instance.
     */
    public final void setDependencies(TemplateResolverDependencies dependencies) {
        _NullArgumentException.check("dependencies", dependencies);
        synchronized (this) {
            // Note that this doesn't make the TemplateResolver safely published (as par Java Memory Model). It only
            // guarantees that the configuration won't be changed once it was set.
            if (this.dependencies != null) {
                if (this.dependencies == dependencies) {
                    return;
                }
                throw new IllegalStateException(
                        "This TemplateResolver is already bound to another Configuration instance.");
            }
            this.dependencies = dependencies;

            initialize();

            initialized = true;
        } // sync.
    }

    /**
     * Returns {@code null} before {@link #initialize()}
     */
    protected TemplateResolverDependencies getDependencies() {
        return dependencies;
    }

    /**
     * You meant to initialize the instance here instead of in the constructor. This is called only once (by
     * FreeMarker at least), when the {@link #setDependencies(TemplateResolverDependencies)}  dependencies} is called.
     */
    protected abstract void initialize() throws ConfigurationException;

    /**
     * Checks if the {@link TemplateResolver} was fully initialized.
     * It's a good practice to call this at the beginning of most method.
     */
    protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "TemplateResolver wasn't properly initialized; ensure that initialize() was called and did not "
                    + "throw an exception.");
        }
    }

    /**
     * Retrieves the parsed template with the given name (and according the specified further parameters), or returns a
     * result that indicates that no such template exists. The result should come from a cache most of the time
     * (avoiding I/O and template parsing), as this method is typically called frequently.
     * <p>
     * All parameters must be non-{@code null}, except {@code customLookupCondition}. For the meaning of the parameters
     * see {@link Configuration#getTemplate(String, Locale, Serializable, boolean)}. Note that {@code name} parameter
     * is not normalized; you are supposed to call {@link #normalizeRootBasedName(String)} internally.
     *
     * @return A {@link GetTemplateResult} object that contains the {@link Template}, or a
     *         {@link GetTemplateResult} object that contains {@code null} as the {@link Template} and information
     *         about the missing template. The return value itself is never {@code null}. Note that exceptions occurring
     *         during template loading mustn't be treated as a missing template, they must cause an exception to be
     *         thrown by this method instead of returning a {@link GetTemplateResult}. The idea is that having a
     *         missing template is normal (not exceptional), because of how some lookup strategies work. That the
     *         backing storage mechanism should indeed check that it's missing though, and not cover an error as such.
     * 
     * @throws MalformedTemplateNameException
     *             If the {@code name} was malformed. This is certainly originally thrown by
     *             {@link #normalizeRootBasedName(String)}; see more there.
     * 
     * @throws IOException
     *             If reading the template has failed from a reason other than the template is missing. This method
     *             should never be a {@link TemplateNotFoundException}, as that condition is indicated in the return
     *             value.
     */
    public abstract GetTemplateResult getTemplate(String name, Locale locale, Serializable customLookupCondition)
            throws MalformedTemplateNameException, ParseException, IOException;

    /**
     * Clears the cache of templates, to enforce re-loading templates when they are get next time; this is an optional
     * operation.
     * <p>
     * Note that if the {@link TemplateResolver} implementation uses {@link TemplateLoader}-s, it should also call
     * {@link TemplateLoader#resetState()} on them.
     * <p>
     * This method is thread-safe and can be called while the engine processes templates.
     * 
     * @throws UnsupportedOperationException If the {@link TemplateResolver} implementation doesn't support this
     *        operation.
     */
    public abstract void clearTemplateCache() throws UnsupportedOperationException;

    /**
     * Removes a template from the template cache, hence forcing the re-loading of it when it's next time requested;
     * this is an optional operation. This is to give the application finer control over cache updating than the
     * {@link Configuration#getTemplateUpdateDelayMilliseconds() templateUpdateDelayMilliseconds} setting alone gives.
     * <p>
     * For the meaning of the parameters, see {@link #getTemplate(String, Locale, Serializable)}
     * <p>
     * This method is thread-safe and can be called while the engine processes templates.
     * 
     * @throws UnsupportedOperationException If the {@link TemplateResolver} implementation doesn't support this
     *        operation.
     */
    public abstract void removeTemplateFromCache(String name, Locale locale, Serializable customLookupCondition)
            throws IOException, UnsupportedOperationException;

    /**
     * Converts a name to a template root directory based name, so that it can be used to find a template without
     * knowing what (like which template) has referred to it. The rules depend on the name format, but a typical example
     * is converting "t.ftl" with base "sub/contex.ftl" to "sub/t.ftl".
     * <p>
     * Some implementations, notably {@link DefaultTemplateResolver}, delegate this task to a
     * {@link TemplateNameFormat}.
     * 
     * @param baseName
     *            Maybe a file name, maybe a directory name. The meaning of file name VS directory name depends on the
     *            name format, but typically, something like "foo/bar/" is a directory name, and something like
     *            "foo/bar" is a file name, and thus in the last case the effective base is "foo/" (i.e., the directory
     *            that contains the file). Not {@code null}.
     * @param targetName
     *            The name to convert. This usually comes from a template that refers to another template by name. It
     *            can be a relative name, or an absolute name. (In typical name formats absolute names start with
     *            {@code "/"} or maybe with an URL scheme, and all others are relative). Not {@code null}.
     * 
     * @return The path in template root directory relative format, or even an absolute name (where the root directory
     *         is not the real root directory of the file system, but the imaginary directory that exists to store the
     *         templates). The standard implementations shipped with FreeMarker always return a root relative path
     *         (except if the name starts with an URI schema, in which case a full URI is returned).
     */
    public abstract String toRootBasedName(String baseName, String targetName) throws MalformedTemplateNameException;

    /**
     * Normalizes a template root directory based name (relative to the root or absolute), so that equivalent names
     * become equivalent according {@link String#equals(Object)} too. The rules depend on the name format, but typical
     * examples are "sub/../t.ftl" to "t.ftl", "sub/./t.ftl" to "sub/t.ftl" and "/t.ftl" to "t.ftl".
     * <p>
     * Some implementations, notably {@link DefaultTemplateResolver}, delegates this check to a
     * {@link TemplateNameFormat}. The standard {@link TemplateNameFormat} implementations shipped with FreeMarker
     * always return a root relative path (except if the name starts with an URI schema, in which case a full URI is
     * returned), for example, "/foo.ftl" becomes to "foo.ftl".
     * 
     * @param name
     *            The root based name (a name that's either absolute or relative to the root). Not {@code null}.
     * 
     * @return The normalized root based name. Not {@code null}.
     */
    public abstract String normalizeRootBasedName(String name) throws MalformedTemplateNameException;

    /**
     * Converts a root based name to an absolute name, which is useful if you need to pass a name to something that
     * doesn't necessary resolve relative paths relative to the root (like the {@code #include} directive).
     * 
     * @param name
     *            The root based name (a name that's either absolute or relative to the root). Not {@code null}.
     */
    // TODO [FM3] This is the kind of complication why normalized template names should just be absolute paths. 
    public abstract String rootBasedNameToAbsoluteName(String name) throws MalformedTemplateNameException;
    
    /**
     * Tells whether the {@link TemplateResolver} implementation depends on the
     * {@link Configuration#getTemplateLoader() templateLoader} {@link Configuration}. If it returns {@code false}
     * then this {@link TemplateResolver} must not call {@link TemplateResolverDependencies#getTemplateLoader()}, or
     * else that will throw {@link IllegalStateException}. Furthermore if the user sets the {@code templateLoader} in
     * the {@link Configuration} to non-{@code null} value (the default is {@code null}), then the
     * {@link Configuration} constructor will throw an exception to tell the user that the {@code templateLoader}
     * setting is not supported by this {@link TemplateResolver} class. Some may feel tempted to return {@code true}
     * to avoid such error, but consider that as the user has explicitly set this setting, they certainly expect it
     * have an effect, and will be frustrated when it doesn't have any.
     */
    public abstract boolean supportsTemplateLoaderSetting();

    /**
     * Works like {@link #supportsTemplateLoaderSetting()}, but for the
     * {@link Configuration#getTemplateCacheStorage() templateCacheStorage} setting.
     */
    public abstract boolean supportsTemplateCacheStorageSetting();

    /**
     * Works like {@link #supportsTemplateLoaderSetting()}, but for the
     * {@link Configuration#getTemplateLookupStrategy() templateLookupStrategy} setting.
     */
    public abstract boolean supportsTemplateLookupStrategySetting();

    /**
     * Works like {@link #supportsTemplateLoaderSetting()}, but for the
     * {@link Configuration#getTemplateNameFormat() templateNameFormat} setting.
     */
    public abstract boolean supportsTemplateNameFormatSetting();

    /**
     * Works like {@link #supportsTemplateLoaderSetting()}, but for the
     * {@link Configuration#getTemplateConfigurations() templateConfigurations} setting.
     */
    public abstract boolean supportsTemplateConfigurationsSetting();

    /**
     * Works like {@link #supportsTemplateUpdateDelayMillisecondsSetting()}, but for the
     * {@link Configuration#getTemplateUpdateDelayMilliseconds() templateUpdateDelayMilliseconds} setting.
     */
    public abstract boolean supportsTemplateUpdateDelayMillisecondsSetting();

    /**
     * Works like {@link #supportsTemplateLoaderSetting()}, but for the
     * {@link Configuration#getLocalizedTemplateLookup() localizedTemplateLookup} setting.
     */
    public abstract boolean supportsLocalizedTemplateLookupSetting();

}
