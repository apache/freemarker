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

import freemarker.core.TemplateConfiguration;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Creates (or returns) {@link TemplateConfiguration}-s for template sources.
 * 
 * @since 2.3.24
 */
public abstract class TemplateConfigurationFactory {
    
    private Configuration cfg;

    /**
     * Returns (maybe creates) the {@link TemplateConfiguration} for the given template source.
     * 
     * @param sourceName
     *            The name (path) that was used for {@link TemplateLoader#findTemplateSource(String)}. See
     *            {@link Template#getSourceName()} for details.
     * @param templateSource
     *            The object returned by {@link TemplateLoader#findTemplateSource(String)}.
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
    public abstract TemplateConfiguration get(String sourceName, Object templateSource)
            throws IOException, TemplateConfigurationFactoryException;
    
    /**
     * Binds this {@link TemplateConfigurationFactory} to a {@link Configuration}. Once it's bound, it can't be bound to
     * another {@link Configuration} any more. This is automatically called by
     * {@link Configuration#setTemplateConfigurations(TemplateConfigurationFactory)}.
     */
    public final void setConfiguration(Configuration cfg) {
        if (this.cfg != null) {
            if (cfg != this.cfg) {
                throw new IllegalStateException(
                        "The TemplateConfigurationFactory is already bound to another Configuration");
            }
            return;
        } else {
            this.cfg = cfg;
            setConfigurationOfChildren(cfg);
        }
    }
    
    /**
     * Returns the configuration this object belongs to, or {@code null} if it isn't yet bound to a
     * {@link Configuration}.
     */
    public Configuration getConfiguration() {
        return cfg;
    }
    
    /**
     * Calls {@link TemplateConfiguration#setParentConfiguration(Configuration)} on each enclosed
     * {@link TemplateConfiguration} and {@link TemplateConfigurationFactory#setConfiguration(Configuration)}
     * on each enclosed {@link TemplateConfigurationFactory} objects. It only supposed to call these on the direct
     * "children" of this object, not on the children of the children.
     */
    protected abstract void setConfigurationOfChildren(Configuration cfg);

}
