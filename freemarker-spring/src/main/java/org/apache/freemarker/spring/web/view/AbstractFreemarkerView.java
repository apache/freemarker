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
import java.io.Serializable;
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
import org.springframework.web.servlet.view.AbstractTemplateView;

public abstract class AbstractFreemarkerView extends AbstractTemplateView {

    private Configuration configuration;
    private ObjectWrapperAndUnwrapper objectWrapper;

    // TODO: how to determine locale, customLookupCondition and ignoreMissing? From viewResolver settings?
    private Locale locale;
    private Serializable customLookupCondition;
    private boolean ignoreMissing;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public ObjectWrapperAndUnwrapper getObjectWrapper() {
        return objectWrapper;
    }

    public void setObjectWrapper(ObjectWrapperAndUnwrapper objectWrapper) {
        this.objectWrapper = objectWrapper;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Serializable getCustomLookupCondition() {
        return customLookupCondition;
    }

    public void setCustomLookupCondition(Serializable customLookupCondition) {
        this.customLookupCondition = customLookupCondition;
    }

    public boolean isIgnoreMissing() {
        return ignoreMissing;
    }

    public void setIgnoreMissing(boolean ignoreMissing) {
        this.ignoreMissing = ignoreMissing;
    }

    @Override
    protected void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        getTemplate().process(createModel(model, getObjectWrapper(), request, response), response.getWriter());
    }

    protected Template getTemplate()
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getConfiguration().getTemplate(getUrl(), getLocale(), getCustomLookupCondition(), isIgnoreMissing());
    }

    protected abstract TemplateHashModel createModel(Map<String, Object> map,
            ObjectWrapperAndUnwrapper objectWrapperForModel, HttpServletRequest request, HttpServletResponse response);
}
