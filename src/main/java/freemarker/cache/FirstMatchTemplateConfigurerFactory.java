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
import freemarker.template.utility.StringUtil;

/**
 * Returns the first non-{@code null} result of the child factories, ignores all further child factories. The child
 * factories are call in the order as they were added.
 */
public class FirstMatchTemplateConfigurerFactory extends TemplateConfigurerFactory {
    
    private final TemplateConfigurerFactory[] templateConfigurerFactories;
    private boolean allowNoMatch;
    private String noMatchErrorDetails;
    
    public FirstMatchTemplateConfigurerFactory(TemplateConfigurerFactory... templateConfigurerFactories) {
        this.templateConfigurerFactories = templateConfigurerFactories;
    }

    @Override
    public TemplateConfigurer get(String sourceName, Object templateSource)
            throws IOException, TemplateConfigurerFactoryException {
        for (TemplateConfigurerFactory tcf : templateConfigurerFactories) {
            TemplateConfigurer tc = tcf.get(sourceName, templateSource); 
            if (tc != null) {
                return tc;
            }
        }
        if (!allowNoMatch) {
            throw new TemplateConfigurerFactoryException(
                    FirstMatchTemplateConfigurerFactory.class.getSimpleName()
                    + " has found no matching choice for source name "
                    + StringUtil.jQuote(sourceName) + ". "
                    + (noMatchErrorDetails != null
                            ? "Error details: " + noMatchErrorDetails 
                            : "(Set the noMatchErrorDetails property of the factory bean to give a more specific error "
                                    + "message. Set allowNoMatch to true if this shouldn't be an error.)"));
        }
        return null;
    }

    /**
     * Getter pair of {@link #setAllowNoMatch(boolean)}.
     */
    public boolean getAllowNoMatch() {
        return allowNoMatch;
    }

    /**
     * Use this to specify if having no matching choice is an error. The default is {@code false}, that is, it's an
     * error if there was no matching choice.
     * 
     * @see #setNoMatchErrorDetails(String)
     */
    public void setAllowNoMatch(boolean allowNoMatch) {
        this.allowNoMatch = allowNoMatch;
    }

    /**
     * Use this to specify the text added to the exception error message when there was no matching choice.
     * The default is {@code null} (no error details).
     * 
     * @see #setAllowNoMatch(boolean)
     */
    public String getNoMatchErrorDetails() {
        return noMatchErrorDetails;
    }

    
    public void setNoMatchErrorDetails(String noMatchErrorDetails) {
        this.noMatchErrorDetails = noMatchErrorDetails;
    }
    
    /**
     * Same as {@link #setAllowNoMatch(boolean)}, but return this object to support "fluent API" style. 
     */
    public FirstMatchTemplateConfigurerFactory allowNoMatch(boolean allow) {
        setAllowNoMatch(allow);
        return this;
    }

    /**
     * Same as {@link #setNoMatchErrorDetails(String)}, but return this object to support "fluent API" style. 
     */
    public FirstMatchTemplateConfigurerFactory noMatchErrorDetails(String message) {
        setNoMatchErrorDetails(message);
        return this;
    }

    @Override
    protected void setConfigurationOfChildren(Configuration cfg) {
        for (TemplateConfigurerFactory templateConfigurerFactory : templateConfigurerFactories) {
            templateConfigurerFactory.setConfiguration(cfg);
        }
    }
    
}
