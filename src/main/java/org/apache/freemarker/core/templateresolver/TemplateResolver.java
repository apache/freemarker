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
import java.util.Locale;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.apache.freemarker.core.ast.ParseException;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;

/**
 * This class was introduced to allow users to fully implement the template lookup, loading and caching logic,
 * in case the standard mechanism ({@link DefaultTemplateResolver}) is not flexible enough. By implementing this class,
 * you can take over the duty of the following {@link Configuration} settings, and it's up to the implementation if you
 * delegate some of those duties back to the {@link Configuration} setting:
 * 
 * <ul>
 * <li>{@link Configuration#setTemplateLoader(TemplateLoader) template_loader}
 * <li>{@link Configuration#setTemplateNameFormat(TemplateNameFormat) template_name_format}
 * <li>{@link Configuration#setTemplateLookupStrategy(TemplateLookupStrategy) template_lookup_strategy}
 * <li>{@link Configuration#setCacheStorage(CacheStorage) cache_storage}
 * </ul>
 * 
 * @since 3.0.0
 */
//TODO DRAFT only [FM3]
public abstract class TemplateResolver {

    private final Configuration configuration;

    protected TemplateResolver(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Retrieves the parsed template with the given name (and according the specified further parameters), or returns a
     * result that indicates that no such template exists. The result should come from a cache most of the time
     * (avoiding I/O and template parsing), as this method is typically called frequently.
     * 
     * <p>
     * All parameters must be non-{@code null}, except {@code customLookupCondition}. For the meaning of the parameters
     * see {@link Configuration#getTemplate(String, Locale, String, boolean)}.
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
    // [FM3] These parameters will certainly be removed: String suggestedEncoding, boolean parseAsFTL
    public abstract GetTemplateResult getTemplate(String name, Locale locale, Object customLookupCondition,
            String encoding, boolean parseAsFTL)
            throws MalformedTemplateNameException, ParseException, IOException;

    /**
     * Clears the cache of templates, to enforce re-loading templates when they are get next time; this is an optional
     * operation.
     * 
     * <p>
     * Note that if the {@link TemplateResolver} implementation uses {@link TemplateLoader}-s, it should also call
     * {@link TemplateLoader#resetState()} on them.
     * 
     * <p>
     * This method is thread-safe and can be called while the engine processes templates.
     * 
     * @throws UnsupportedOperationException If the {@link TemplateResolver} implementation doesn't support this
     *        operation.
     */
    public abstract void clearTemplateCache() throws UnsupportedOperationException;

    /**
     * Removes a template from the template cache, hence forcing the re-loading of it when it's next time requested;
     * this is an optional operation. This is to give the application finer control over cache updating than
     * {@link Configuration#setTemplateUpdateDelayMilliseconds(long)} alone does.
     * 
     * <p>
     * For the meaning of the parameters, see {@link #getTemplate(String, Locale, Object, String, boolean)}
     * 
     * <p>
     * This method is thread-safe and can be called while the engine processes templates.
     * 
     * @throws UnsupportedOperationException If the {@link TemplateResolver} implementation doesn't support this
     *        operation.
     */
    public abstract void removeTemplateFromCache(String name, Locale locale, String encoding, boolean parse)
            throws IOException, UnsupportedOperationException;;

    /**
     * Converts a name to a template root directory based name, so that it can be used to find a template without
     * knowing what (like which template) has referred to it. The rules depend on the name format, but a typical example
     * is converting "t.ftl" with base "sub/contex.ftl" to "sub/t.ftl".
     * 
     * <p>
     * Some implementations, notably {@link DefaultTemplateResolver}, delegates this check to the
     * {@link TemplateNameFormat} coming from the {@link Configuration}.
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
     * 
     * <p>
     * Some implementations, notably {@link DefaultTemplateResolver}, delegates this check to the {@link TemplateNameFormat}
     * coming from the {@link Configuration}. The standard {@link TemplateNameFormat} implementations shipped with
     * FreeMarker always returns a root relative path (except if the name starts with an URI schema, in which case a
     * full URI is returned), for example, "/foo.ftl" becomes to "foo.ftl".
     * 
     * @param name
     *            The root based name. Not {@code null}.
     * 
     * @return The normalized root based name. Not {@code null}.
     */
    public abstract String normalizeRootBasedName(String name) throws MalformedTemplateNameException;

}
