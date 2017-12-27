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

package org.apache.freemarker.spring.model.form;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.util.CallableUtils;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractHtmlElementTag</code>.
 */
public abstract class AbstractHtmlElementTemplateDirectiveModel
        extends AbstractDataBoundFormElementTemplateDirectiveModel {

    protected static Map<String, String> createAttributeKeyNamePairsMap(String ... attrNames) {
        Map<String, String> map = new HashMap<>();
        for (String attrName : attrNames) {
            map.put(attrName.toUpperCase(), attrName);
        }
        return map;
    }

    private static final Map<String, String> REGISTERED_ATTRIBUTES = Collections.unmodifiableMap(
            createAttributeKeyNamePairsMap(
                    "class",
                    "style",
                    "lang",
                    "title",
                    "dir",
                    "tabindex",
                    "onclick",
                    "ondblclick",
                    "onmousedown",
                    "onmouseup",
                    "onmouseover",
                    "onmousemove",
                    "onmouseout",
                    "onkeypress",
                    "onkeyup",
                    "onkeydown")
            );

    private static final int PATH_PARAM_IDX = 0;

    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    null,
                    true
                    );

    private Map<String, Object> registeredAttributes;
    private Map<String, Object> unmodifiableRegisteredAttributes = Collections.emptyMap();
    private Map<String, Object> dynamicAttributes;
    private Map<String, Object> unmodifiableDynamicAttributes = Collections.emptyMap();

    protected AbstractHtmlElementTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    public Map<String, Object> getRegisteredAttributes() {
        return unmodifiableRegisteredAttributes;
    }

    public Map<String, Object> getDynamicAttributes() {
        return unmodifiableDynamicAttributes;
    }

    public void setRegisteredAttribute(String localName, Object value) {
        if (localName == null) {
            throw new IllegalArgumentException("Attribute name must not be null.");
        }

        if (!isRegisteredAttribute(localName, value)) {
            throw new IllegalArgumentException("Invalid attribute: " + localName + "=" + value);
        }

        if (registeredAttributes == null) {
            registeredAttributes = new LinkedHashMap<String, Object>();
            unmodifiableRegisteredAttributes = Collections.unmodifiableMap(registeredAttributes);
        }

        registeredAttributes.put(localName, value);
    }

    public void setDynamicAttribute(String localName, Object value) {
        if (localName == null) {
            throw new IllegalArgumentException("Attribute name must not be null.");
        }

        if (!isValidDynamicAttribute(localName, value)) {
            throw new IllegalArgumentException("Invalid dynamic attribute: " + localName + "=" + value);
        }

        if (dynamicAttributes == null) {
            dynamicAttributes = new LinkedHashMap<String, Object>();
            unmodifiableDynamicAttributes = Collections.unmodifiableMap(dynamicAttributes);
        }

        dynamicAttributes.put(localName, value);
    }

    protected String getPathArgument(TemplateModel[] args) throws TemplateException {
        final String path = CallableUtils.getStringArgument(args, PATH_PARAM_IDX, this);
        return path;
    }

    protected boolean isRegisteredAttribute(String localName, Object value) {
        return REGISTERED_ATTRIBUTES.containsKey(localName.toUpperCase());
    }

    protected boolean isValidDynamicAttribute(String localName, Object value) {
        return true;
    }

    protected void readRegisteredAndDynamicAttributes(TemplateModel[] args, ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper) throws TemplateException {
        final int attrsVarargsIndex = getDirectiveArgumentArrayLayout().getNamedVarargsArgumentIndex();
        final TemplateHashModelEx attrsHashModel = (TemplateHashModelEx) args[attrsVarargsIndex];

        if (!attrsHashModel.isEmptyHash()) {
            for (TemplateHashModelEx.KeyValuePairIterator attrIt = attrsHashModel.keyValuePairIterator(); attrIt.hasNext();) {
                TemplateHashModelEx.KeyValuePair pair = attrIt.next();
                TemplateModel attrNameModel = pair.getKey();
                TemplateModel attrValueModel = pair.getValue();

                if (!(attrNameModel instanceof TemplateStringModel)) {
                    throw CallableUtils.newArgumentValueException(attrsVarargsIndex,
                            "Parameter name must be a string.", this);
                }

                String attrName = ((TemplateStringModel) attrNameModel).getAsString();

                if (attrName.isEmpty()) {
                    throw CallableUtils.newArgumentValueException(attrsVarargsIndex,
                            "Attribute name must be a non-blank string.", this);
                }

                final Object attrValue = objectWrapperAndUnwrapper.unwrap(attrValueModel);

                if (isRegisteredAttribute(attrName, attrValue)) {
                    setRegisteredAttribute(attrName.toUpperCase(), attrValue);
                } else {
                    setDynamicAttribute(attrName, attrValue);
                }
            }
        }
        
        System.out.println("$$$$$ dynamicAttributes: " + this.getDynamicAttributes());
    }

    protected void writeDefaultHtmlElementAttributes(TagOutputter tagOut) throws TemplateException, IOException {
        super.writeDefaultHtmlElementAttributes(tagOut);

        for (Map.Entry<String, String> entry : REGISTERED_ATTRIBUTES.entrySet()) {
            String attrKey = entry.getKey();
            String attrName = entry.getValue();
            Object attrValue = getRegisteredAttributes().get(attrKey);
            writeOptionalAttribute(tagOut, attrName, attrValue);
        }
    }

}
