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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.springframework.web.servlet.support.RequestContext;

public class InputTemplateDirectiveModel extends AbstractHtmlElementTemplateDirectiveModel {

    public static final String NAME = "input";

    private static final Map<String, String> REGISTERED_ATTRIBUTES = Collections.unmodifiableMap(
            createAttributeKeyNamePairsMap(
                    "size",
                    "maxlength",
                    "alt",
                    "onselect",
                    "readonly",
                    "autocomplete"));

    protected InputTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public boolean isNestedContentSupported() {
        return false;
    }

    @Override
    protected void writeDirectiveContent(TemplateModel[] args, CallPlace callPlace, TagOutputter tagOut,
            Environment env, ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException {

        final String path = getPathArgument(args);

        try {
            readRegisteredAndDynamicAttributes(args, objectWrapperAndUnwrapper);

            tagOut.beginTag(NAME);

            writeDefaultHtmlElementAttributes(tagOut);

            if (!hasDynamicTypeAttribute()) {
                tagOut.writeAttribute("type", (String) getRegisteredAttributes().get("type"));
            }

            writeValue(tagOut);

            // custom optional attributes
            for (Map.Entry<String, String> entry : REGISTERED_ATTRIBUTES.entrySet()) {
                String attrKey = entry.getKey();
                String attrName = entry.getValue();
                Object attrValue = getRegisteredAttributes().get(attrKey);
                writeOptionalAttribute(tagOut, attrName, attrValue);
            }

            tagOut.endTag();
        } catch (IOException e) {
            throw new TemplateException(e);
        }
    }

    @Override
    protected boolean isRegisteredAttribute(String localName, Object value) {
        return super.isRegisteredAttribute(localName, value) && REGISTERED_ATTRIBUTES.containsKey(localName);
    }

    private boolean hasDynamicTypeAttribute() {
        return getDynamicAttributes().containsKey("type");
    }

    protected void writeValue(TagOutputter tagOut) throws TemplateException {
//        String value = getDisplayString(getBoundValue(), getPropertyEditor());
//        String type = hasDynamicTypeAttribute() ? (String) getDynamicAttributes().get("type") : getType();
//        tagWriter.writeAttribute("value", processFieldValue(getName(), value, type));

        //FIXME
        try {
            tagOut.writeAttribute("value", "value");
        } catch (IOException e) {
            throw new TemplateException(e);
        }
    }

}
