/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package freemarker.cache;

import java.io.IOException;

import freemarker.core.TemplateConfigurer;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Creates (or returns) {@link TemplateConfigurer}-s for template sources.
 * 
 * @since 2.3.24
 */
public abstract class TemplateConfigurerFactory {
    
    private Configuration cfg;

    /**
     * Returns (maybe creates) the {@link TemplateConfigurer} for the given template source.
     * 
     * @param sourceName
     *            The name (path) that was used for {@link TemplateLoader#findTemplateSource(String)}. See
     *            {@link Template#getSourceName()} for details.
     * @param templateSource
     *            The object returned by {@link TemplateLoader#findTemplateSource(String)}.
     * 
     * @return The {@link TemplateConfigurer} to apply, or {@code null} if the there's no {@link TemplateConfigurer} for
     *         this template source.
     * 
     * @throws IOException
     *             Typically, if there factory needs further I/O to find out more about the template source, but that
     *             fails.
     * @throws TemplateConfigurerFactoryException
     *             If there's a problem that's specific to the factory logic.
     */
    public abstract TemplateConfigurer get(String sourceName, Object templateSource)
            throws IOException, TemplateConfigurerFactoryException;
    
    /**
     * Binds this {@link TemplateConfigurerFactory} to a {@link Configuration}. Once it's bound, it can't be bound to
     * another {@link Configuration} any more. This is automatically called by
     * {@link Configuration#setTemplateConfigurers(TemplateConfigurerFactory)}.
     */
    public final void setConfiguration(Configuration cfg) {
        if (this.cfg != null) {
            if (cfg != this.cfg) {
                throw new IllegalStateException(
                        "The TemplateConfigurerFactory is already bound to another Configuration");
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
     * Calls {@link TemplateConfigurer#setParentConfiguration(Configuration)} on each enclosed
     * {@link TemplateConfigurer} and {@link TemplateConfigurerFactory#setConfiguration(Configuration)}
     * on each enclosed {@link TemplateConfigurerFactory} objects. It only supposed to call these on the direct
     * "children" of this object, not on the children of the children.
     */
    protected abstract void setConfigurationOfChildren(Configuration cfg);

}
