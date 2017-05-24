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

package org.apache.freemarker.core;

import java.util.Map;

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.templateresolver.CacheStorage;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.TemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormatFM2;
import org.apache.freemarker.core.templateresolver.impl.FileTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.MultiTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.SoftCacheStorage;

/**
 * Implemented by FreeMarker core classes (not by you) that provide {@link Configuration}-level settings. <b>New
 * methods may be added any time in future FreeMarker versions, so don't try to implement this interface yourself!</b>
 *
 * @see ParsingAndProcessingConfiguration
 */
public interface TopLevelConfiguration extends ParsingAndProcessingConfiguration {

    /**
     * The {@link TemplateLoader} that is used to look up and load templates.
     * By providing your own {@link TemplateLoader} implementation, you can load templates from whatever kind of
     * storages, like from relational databases, NoSQL-storages, etc.
     *
     * <p>You can chain several {@link TemplateLoader}-s together with {@link MultiTemplateLoader}.
     *
     * <p>Default value: You should always set the template loader instead of relying on the default value.
     * (But if you still care what it is, before "incompatible improvements" 2.3.21 it's a {@link FileTemplateLoader}
     * that uses the current directory as its root; as it's hard tell what that directory will be, it's not very useful
     * and dangerous. Starting with "incompatible improvements" 2.3.21 the default is {@code null}.)
     */
    TemplateLoader getTemplateLoader();

    /**
     * Tells if this setting was explicitly set (otherwise its value will be the default value).
     */
    boolean isTemplateLoaderSet();

    /**
     * The {@link TemplateLookupStrategy} that is used to look up templates based on the requested name, locale and
     * custom lookup condition. Its default is {@link DefaultTemplateLookupStrategy#INSTANCE}.
     */
    TemplateLookupStrategy getTemplateLookupStrategy();

    /**
     * Tells if this setting was explicitly set (otherwise its value will be the default value).
     */
    boolean isTemplateLookupStrategySet();

    /**
     * The template name format used; see {@link TemplateNameFormat}. The default is
     * {@link DefaultTemplateNameFormatFM2#INSTANCE}, while the recommended value for new projects is
     * {@link DefaultTemplateNameFormat#INSTANCE}.
     */
    TemplateNameFormat getTemplateNameFormat();

    /**
     * Tells if this setting was explicitly set (otherwise its value will be the default value).
     */
    boolean isTemplateNameFormatSet();

    /**
     * The {@link TemplateConfigurationFactory} that will configure individual templates where their settings differ
     * from those coming from the common {@link Configuration} object. A typical use case for that is specifying the
     * {@link #getOutputFormat() outputFormat} or {@link #getSourceEncoding() sourceEncoding} for templates based on
     * their file extension or parent directory.
     * <p>
     * Note that the settings suggested by standard file extensions are stronger than that you set here. See
     * {@link #getRecognizeStandardFileExtensions()} for more information about standard file extensions.
     * <p>
     * See "Template configurations" in the FreeMarker Manual for examples.
     */
    TemplateConfigurationFactory getTemplateConfigurations();

    /**
     * Tells if this setting was explicitly set (otherwise its value will be the default value).
     */
    boolean isTemplateConfigurationsSet();

    /**
     * The map-like object used for caching templates to avoid repeated loading and parsing of the template "files".
     * Its {@link Configuration}-level default is a {@link SoftCacheStorage}.
     */
    CacheStorage getCacheStorage();

    /**
     * Tells if this setting was explicitly set (otherwise its value will be the default value).
     */
    boolean isCacheStorageSet();

    /**
     * The time in milliseconds that must elapse before checking whether there is a newer version of a template
     * "file" than the cached one. Defaults to 5000 ms.
     */
    long getTemplateUpdateDelayMilliseconds();

    /**
     * Tells if this setting was explicitly set (otherwise its value will be the default value).
     */
    boolean isTemplateUpdateDelayMillisecondsSet();

    /**
     * Returns the value of the "incompatible improvements" setting; this is the FreeMarker version number where the
     * not 100% backward compatible bug fixes and improvements that you want to enable were already implemented. In
     * new projects you should set this to the FreeMarker version that you are actually using. In older projects it's
     * also usually better to keep this high, however you better check the changes activated (find them below), at
     * least if not only the 3rd version number (the micro version) of {@code incompatibleImprovements} is increased.
     * Generally, as far as you only increase the last version number of this setting, the changes are always low
     * risk.
     * <p>
     * Bugfixes and improvements that are fully backward compatible, also those that are important security fixes,
     * are enabled regardless of the incompatible improvements setting.
     * <p>
     * An important consequence of setting this setting is that now your application will check if the stated minimum
     * FreeMarker version requirement is met. Like if you set this setting to 3.0.1, but accidentally the
     * application is deployed with FreeMarker 3.0.0, then FreeMarker will fail, telling that a higher version is
     * required. After all, the fixes/improvements you have requested aren't available on a lower version.
     * <p>
     * Note that as FreeMarker's minor (2nd) or major (1st) version number increments, it's possible that emulating
     * some of the old bugs will become unsupported, that is, even if you set this setting to a low value, it
     * silently wont bring back the old behavior anymore. Information about that will be present here.
     *
     * <p>Currently the effects of this setting are:
     * <ul>
     *   <li><p>
     *     3.0.0: This is the lowest supported value in FreeMarker 3.
     *   </li>
     * </ul>
     *
     * @return Never {@code null}.
     */
    @Override
    Version getIncompatibleImprovements();

    /**
     * Whether localized template lookup is enabled. Enabled by default.
     *
     * <p>
     * With the default {@link TemplateLookupStrategy}, localized lookup works like this: Let's say your locale setting
     * is {@code Locale("en", "AU")}, and you call {@link Configuration#getTemplate(String) cfg.getTemplate("foo.ftl")}.
     * Then FreeMarker will look for the template under these names, stopping at the first that exists:
     * {@code "foo_en_AU.ftl"}, {@code "foo_en.ftl"}, {@code "foo.ftl"}. See the description of the default value at
     * {@link #getTemplateLookupStrategy()} for a more details. If you need to generate different
     * template names, set your own a {@link TemplateLookupStrategy} implementation as the value of the
     * {@link #getTemplateLookupStrategy() templateLookupStrategy} setting.
     */
    boolean getLocalizedLookup();

    /**
     * Tells if this setting was explicitly set (otherwise its value will be the default value).
     */
    boolean isLocalizedLookupSet();

    /**
     * Shared variables are variables that are visible as top-level variables for all templates, except where the data
     * model contains a variable with the same name (which then shadows the shared variable). This setting value store
     * the variables as they were originally added, that is, without being wrapped into {@link TemplateModel} interface
     * (unless the original value was already a {@link TemplateModel}). The wrapped values can be accessed with
     * {@link Configuration#getWrappedSharedVariable(String)}.
     *
     * @return Not {@code null}; immutable {@link Map}.
     *
     * @see Configuration.Builder#setSharedVariables(Map)
     */
    Map<String, Object> getSharedVariables();

    /**
     * Tells if this setting was explicitly set (if not, the default value of the setting will be used).
     */
    boolean isSharedVariablesSet();

}
