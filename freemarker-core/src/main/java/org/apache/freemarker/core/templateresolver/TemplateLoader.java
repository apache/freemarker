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

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;

import java.io.IOException;
import java.io.Serializable;

/**
 * FreeMarker loads template "files" through objects that implement this interface, thus the templates need not be real
 * files, and can come from any kind of data source (like classpath, servlet context, database, etc). While FreeMarker
 * provides a few template loader implementations out-of-the-box, it's normal for embedding frameworks to use their own
 * implementations.
 * 
 * <p>
 * The {@link TemplateLoader} used by FreeMaker is specified by the {@link Configuration#getTemplateLoader()
 * templateLoader} configuration setting.
 * 
 * <p>
 * Implementations of this interface should be thread-safe.
 * 
 * <p>
 * Implementations should override {@link Object#toString()} to show information about from where the
 * {@link TemplateLoader} loads the templates. For example, for a template loader that loads template from database
 * table {@code toString} could return something like
 * {@code "MyDatabaseTemplateLoader(user=\"cms\", table=\"mail_templates\")"}. This string will be shown in
 * {@link TemplateNotFoundException} exception messages, next to the template name.
 * 
 * <p>
 * For those who has to dig deeper, note that the {@link TemplateLoader} is actually stored inside the
 * {@link DefaultTemplateResolver} of the {@link Configuration}, and is normally only accessed directly by the
 * {@link DefaultTemplateResolver}, and templates are get via the {@link DefaultTemplateResolver} API-s.
 */
public interface TemplateLoader {

    /**
     * Creates a new session, or returns {@code null} if the template loader implementation doesn't support sessions.
     * See {@link TemplateLoaderSession} for more information about sessions.
     */
    TemplateLoaderSession createSession();
    
    /**
     * Loads the template content together with meta-data such as the version (usually the last modification time).
     * 
     * @param name
     *            The name (template root directory relative path) of the template, already localized and normalized by
     *            the {@link org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver cache}. It is completely up to the loader implementation to
     *            interpret the name, however it should expect to receive hierarchical paths where path components are
     *            separated by a slash (not backslash). Backslashes (or any other OS specific separator character) are
     *            not considered as separators by FreeMarker, and thus they will not be replaced with slash before
     *            passing to this method, so it's up to the template loader to handle them (say, by throwing an
     *            exception that tells the user that the path (s)he has entered is invalid, as (s)he must use slash --
     *            typical mistake of Windows users). The passed names are always considered relative to some
     *            loader-defined root location (often referred as the "template root directory"), and will never start
     *            with a slash, nor will they contain a path component consisting of either a single or a double dot --
     *            these are all resolved by the template cache before passing the name to the loader. As a side effect,
     *            paths that trivially reach outside template root directory, such as {@code ../my.f3ah}, will be
     *            rejected by the template cache, so they never reach the template loader. Note again, that if the path
     *            uses backslash as path separator instead of slash as (the template loader should not accept that), the
     *            normalization will not properly happen, as FreeMarker (the cache) recognizes only the slashes as
     *            separators.
     * @param ifSourceDiffersFrom
     *            If we only want to load the template if its source differs from this. {@code null} if you want the
     *            template to be loaded unconditionally. If this is {@code null} then the
     *            {@code ifVersionDiffersFrom} parameter must be {@code null} too. See
     *            {@link TemplateLoadingResult#getSource()} for more about sources.
     * @param ifVersionDiffersFrom
     *            If we only want to load the template if its version (which is usually the last modification time)
     *            differs from this. {@code null} if {@code ifSourceDiffersFrom} is {@code null}, or if the backing
     *            storage from which the {@code ifSourceDiffersFrom} template source comes from doesn't store a version.
     *            See {@link TemplateLoadingResult#getVersion()} for more about versions.
     * 
     * @return Not {@code null}.
     */
    TemplateLoadingResult load(String name, TemplateLoadingSource ifSourceDiffersFrom, Serializable ifVersionDiffersFrom,
            TemplateLoaderSession session) throws IOException;
    
    /**
     * Invoked by {@link Configuration#clearTemplateCache()} to instruct this template loader to throw away its current
     * state (some kind of cache usually) and start afresh. For most {@link TemplateLoader} implementations this does
     * nothing.
     */
    void resetState();

}
