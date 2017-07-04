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
package org.apache.freemarker.spring.web.view;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.ParseException;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.templateresolver.MalformedTemplateNameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.view.AbstractTemplateView;

/**
 * Adapter base class for FreeMarker template-based views, with the ability to access {@link Configuration}
 * and {@link ObjectWrapperAndUnwrapper}.
 */
public abstract class AbstractFreemarkerView extends AbstractTemplateView {

    private static Logger log = LoggerFactory.getLogger(AbstractFreemarkerView.class);

    /**
     * FreeMarker {@link Configuration} instance.
     */
    private Configuration configuration;

    /**
     * {@link ObjectWrapperAndUnwrapper} instance to use in model building.
     */
    private ObjectWrapperAndUnwrapper objectWrapper;

    /**
     * Template {@link Locale}.
     */
    private Locale locale;

    /**
     * Get FreeMarker {@link Configuration} instance.
     * @return {@link Configuration} instance
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Set FreeMarker {@link Configuration} instance.
     * @param configuration {@link Configuration} instance
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Get {@link ObjectWrapperAndUnwrapper} that is used in model building.
     * @return {@link ObjectWrapperAndUnwrapper} that is used in model building
     */
    public ObjectWrapperAndUnwrapper getObjectWrapper() {
        return objectWrapper;
    }

    /**
     * Set {@link ObjectWrapperAndUnwrapper} that is used in model building.
     * @param objectWrapper {@link ObjectWrapperAndUnwrapper} that is used in model building
     */
    public void setObjectWrapper(ObjectWrapperAndUnwrapper objectWrapper) {
        this.objectWrapper = objectWrapper;
    }

    /**
     * Get template locale.
     * @return template locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Set template locale.
     * @param locale template locale
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public boolean checkResource(Locale locale) throws Exception {
        if (this.locale == null) {
            this.locale = locale;
        }

        try {
            // Check whether the underlying resource exists by trying to get the template.
            getTemplate();
            return true;
        } catch (TemplateNotFoundException e) {
            log.debug("No view found for URL: {}", getUrl());
        } catch (MalformedTemplateNameException e) {
            throw new ApplicationContextException("Malformed template name: " + getUrl(), e);
        } catch (ParseException e) {
            throw new ApplicationContextException("Template parsing exception: " + getUrl(), e);
        } catch (IOException e) {
            throw new ApplicationContextException("Template IO exception: " + getUrl(), e);
        }

        return false;
    }

    @Override
    protected void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        getTemplate().process(createModel(model, getObjectWrapper(), request, response), response.getWriter());
    }

    /**
     * Get template from the FreeMarker {@link Configuration} instance.
     * @return template from the FreeMarker {@link Configuration} instance
     * @throws TemplateNotFoundException if template is not found
     * @throws MalformedTemplateNameException if template name is malformed
     * @throws ParseException if the template is syntactically bad
     * @throws IOException if template cannot be read due to IO error
     */
    protected Template getTemplate()
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getConfiguration().getTemplate(normalizeTemplateName(getUrl()), getLocale());
    }

    /**
     * Create model for the template.
     * @param map map as initial source for the template model
     * @param objectWrapperForModel ObjectWrapper to be used in model building
     * @param request request
     * @param response response
     * @return model for the template
     */
    protected abstract TemplateHashModel createModel(Map<String, Object> map,
            ObjectWrapperAndUnwrapper objectWrapperForModel, HttpServletRequest request, HttpServletResponse response);

    private String normalizeTemplateName(String name) {
        if (name != null) {
            return (name.startsWith("/")) ? name.substring(1) : name;
        }
        return null;
    }
}
