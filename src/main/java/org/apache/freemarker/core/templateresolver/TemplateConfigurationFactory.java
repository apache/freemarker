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

import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateConfiguration;

/**
 * Creates (or returns) {@link TemplateConfiguration}-s for template sources.
 * 
 * @since 2.3.24
 */
public abstract class TemplateConfigurationFactory {

    /**
     * Returns (maybe creates) the {@link TemplateConfiguration} for the given template source.
     * 
     * @param sourceName
     *            The name (path) that was used for {@link TemplateLoader#load}. See
     *            {@link Template#getSourceName()} for details.
     * @param templateLoadingSource
     *            The object returned by {@link TemplateLoadingResult#getSource()}.
     * 
     * @return The {@link TemplateConfiguration} to apply, or {@code null} if the there's no {@link TemplateConfiguration} for
     *         this template source.
     * 
     * @throws IOException
     *             Typically, if there factory needs further I/O to find out more about the template source, but that
     *             fails.
     * @throws TemplateConfigurationFactoryException
     *             If there's a problem that's specific to the factory logic.
     */
    public abstract TemplateConfiguration get(String sourceName, TemplateLoadingSource templateLoadingSource)
            throws IOException, TemplateConfigurationFactoryException;

}
