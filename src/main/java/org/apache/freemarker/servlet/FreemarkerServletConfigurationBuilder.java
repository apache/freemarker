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

package org.apache.freemarker.servlet;

import java.io.IOException;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Configuration.ExtendableBuilder;
import org.apache.freemarker.core.TemplateExceptionHandler;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.templateresolver.TemplateLoader;

/**
 * Changes some defaults compared to {@link ExtendableBuilder}, to values that makes more sense for the
 * {@link FreemarkerServlet}.
 */
// TODO [FM3] JavaDoc the defaults when they are stable
public class FreemarkerServletConfigurationBuilder<SelfT extends FreemarkerServletConfigurationBuilder<SelfT>>
        extends Configuration.ExtendableBuilder<SelfT> {

    private final FreemarkerServlet freemarkerServlet;
    private TemplateLoader cachedDefaultTemplateLoader;

    public FreemarkerServletConfigurationBuilder(FreemarkerServlet freemarkerServlet, Version version) {
        super(version);
        this.freemarkerServlet = freemarkerServlet;
    }

    @Override
    protected TemplateExceptionHandler getDefaultTemplateExceptionHandler() {
        // TODO [FM3] Not a good default. Should depend on if we are in development mode or production mode.
        return TemplateExceptionHandler.HTML_DEBUG_HANDLER;
    }

    // TODO [FM3] Remove when this will be the ExtendableBuilder default too.
    @Override
    protected boolean getDefaultLogTemplateExceptions() {
        return false;
    }

    @Override
    public void setTemplateLoader(TemplateLoader templateLoader) {
        super.setTemplateLoader(templateLoader);
        if (cachedDefaultTemplateLoader != templateLoader) {
            // Just to make it GC-able
            cachedDefaultTemplateLoader = null;
        }
    }

    @Override
    protected TemplateLoader getDefaultTemplateLoader() {
        try {
            if (cachedDefaultTemplateLoader == null) {
                cachedDefaultTemplateLoader = freemarkerServlet.createTemplateLoader(InitParamParser.TEMPLATE_PATH_PREFIX_CLASS);
            }
            return cachedDefaultTemplateLoader;
        } catch (IOException e) {
            // It's almost impossible that this will happen
            throw new RuntimeException("Failed to create default template loader; see cause exception", e);
        }
    }
}
