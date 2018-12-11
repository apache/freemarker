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
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateLookupStrategy;

/**
 * Used as the parameter of {@link TemplateLookupStrategy#lookup(TemplateLookupContext)}.
 * You can't invoke instances of this, only receive them from FreeMarker.
 */
public abstract class TemplateLookupContext<R extends TemplateLookupResult> {
    
    private final String templateName;
    private final Locale templateLocale;
    private final Object customLookupCondition;

    /**
     * Finds the template source based on its <em>normalized</em> name; handles {@code *} steps (so called acquisition),
     * otherwise it just calls {@link TemplateLoader#load(String, TemplateLoadingSource, Serializable,
     * TemplateLoaderSession)}.
     * 
     * @param templateName
     *            Must be a normalized name, like {@code "foo/bar/baaz.f3ah"}. A name is not normalized when, among
     *            others, it starts with {@code /}, or contains {@code .} or {@code ..} path steps, or it uses
     *            backslash ({@code \}) instead of {@code /}. A normalized name might contains "*" path steps
     *            (acquisition).
     * 
     * @return The result of the lookup. Not {@code null}; check {@link TemplateLookupResult#isPositive()} to see if the
     *         lookup has found anything. Note that in a positive result the content of the template is possibly
     *         also already loaded (this is the case for {@link TemplateLoader}-s when the cached content is stale, but
     *         not for {@link TemplateLoader}-s). Hence discarding a positive result and looking for another can
     *         generate substantial extra I/O.
     */
    public abstract R lookupWithAcquisitionStrategy(String templateName) throws IOException;

    /**
     * Finds the template source based on its <em>normalized</em> name; tries localized variations going from most
     * specific to less specific, and for each variation it delegates to {@link #lookupWithAcquisitionStrategy(String)}.
     * If {@code templateLocale} is {@code null} (typically, because {@link Configuration#getLocalizedTemplateLookup()} is
     * {@code false})), then it's the same as calling {@link #lookupWithAcquisitionStrategy(String)} directly. This is
     * the default strategy of FreeMarker (at least in 2.3.x), so for more information, see
     * {@link DefaultTemplateLookupStrategy#INSTANCE}.
     */
    public abstract R lookupWithLocalizedThenAcquisitionStrategy(String templateName,
            Locale templateLocale) throws IOException;
    
    /** Default visibility to prevent extending the class from outside this package. */
    protected TemplateLookupContext(String templateName, Locale templateLocale, Object customLookupCondition) {
        this.templateName = templateName;
        this.templateLocale = templateLocale;
        this.customLookupCondition = customLookupCondition;
    }

    /**
     * The normalized name (path) of the template (relatively to the {@link TemplateLoader}). Not {@code null}. 
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * {@code null} if localized template lookup is disabled (see {@link Configuration#getLocalizedTemplateLookup()}),
     * otherwise the locale requested.
     */
    public Locale getTemplateLocale() {
        return templateLocale;
    }

    /**
     * Returns the value of the {@code customLookupCondition} parameter of
     * {@link Configuration#getTemplate(String, Locale, Serializable, boolean)}; see requirements there, such
     * as having a proper {@link Object#equals(Object)} and {@link Object#hashCode()} method. The interpretation of this
     * value is up to the custom {@link TemplateLookupStrategy}. Usually, it's used similarly to as the default lookup
     * strategy uses {@link #getTemplateLocale()}, that is, to look for a template variation that satisfies the
     * condition, and then maybe fall back to more generic template if that's missing.
     */
    public Object getCustomLookupCondition() {
        return customLookupCondition;
    }

    /**
     * Creates a not-found lookup result that then can be used as the return value of
     * {@link TemplateLookupStrategy#lookup(TemplateLookupContext)}. (In the current implementation it just always
     * returns the same static singleton, but that might need to change in the future.)
     */
    public abstract R createNegativeLookupResult();
    
}
