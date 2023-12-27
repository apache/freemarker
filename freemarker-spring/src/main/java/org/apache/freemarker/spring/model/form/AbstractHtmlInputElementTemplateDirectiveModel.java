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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.web.servlet.support.RequestContext;

import java.io.IOException;
import java.io.Writer;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractHtmlInputElementTag</code>.
 */
abstract class AbstractHtmlInputElementTemplateDirectiveModel extends AbstractHtmlElementTemplateDirectiveModel {

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int ONFOCUS_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String ONFOCUS_PARAM_NAME = "onfocus";

    private static final int ONBLUR_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String ONBLUR_PARAM_NAME = "onblur";

    private static final int ONCHANGE_PARAM_IDX = NAMED_ARGS_OFFSET + 2;
    private static final String ONCHANGE_PARAM_NAME = "onchange";

    private static final int ACCESSKEY_PARAM_IDX = NAMED_ARGS_OFFSET + 3;
    private static final String ACCESSKEY_PARAM_NAME = "accesskey";

    private static final int DISABLED_PARAM_IDX = NAMED_ARGS_OFFSET + 4;
    private static final String DISABLED_PARAM_NAME = "disabled";

    private static final int READONLY_PARAM_IDX = NAMED_ARGS_OFFSET + 5;
    private static final String READONLY_PARAM_NAME = "readonly";

    protected static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(AbstractHtmlElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                            new StringToIndexMap.Entry(ONFOCUS_PARAM_NAME, ONFOCUS_PARAM_IDX),
                            new StringToIndexMap.Entry(ONBLUR_PARAM_NAME, ONBLUR_PARAM_IDX),
                            new StringToIndexMap.Entry(ONCHANGE_PARAM_NAME, ONCHANGE_PARAM_IDX),
                            new StringToIndexMap.Entry(ACCESSKEY_PARAM_NAME, ACCESSKEY_PARAM_IDX),
                            new StringToIndexMap.Entry(DISABLED_PARAM_NAME, DISABLED_PARAM_IDX),
                            new StringToIndexMap.Entry(READONLY_PARAM_NAME, READONLY_PARAM_IDX)
                            ),
                    true
                    );

    private String onfocus;
    private String onblur;
    private String onchange;
    private String accesskey;
    private boolean disabled;
    private boolean readonly;

    protected AbstractHtmlInputElementTemplateDirectiveModel(HttpServletRequest request,
            HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {

        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);

        onfocus = CallableUtils.getOptionalStringArgument(args, ONFOCUS_PARAM_IDX, this);
        onblur = CallableUtils.getOptionalStringArgument(args, ONBLUR_PARAM_IDX, this);
        onchange = CallableUtils.getOptionalStringArgument(args, ONCHANGE_PARAM_IDX, this);
        accesskey = CallableUtils.getOptionalStringArgument(args, ACCESSKEY_PARAM_IDX, this);
        disabled = CallableUtils.getOptionalBooleanArgument(args, DISABLED_PARAM_IDX, this, false);
        readonly = CallableUtils.getOptionalBooleanArgument(args, READONLY_PARAM_IDX, this, false);
    }

    @Override
    protected void writeOptionalAttributes(TagOutputter tagOut) throws TemplateException, IOException {
        super.writeOptionalAttributes(tagOut);

        writeOptionalAttribute(tagOut, ONFOCUS_PARAM_NAME, getOnfocus());
        writeOptionalAttribute(tagOut, ONBLUR_PARAM_NAME, getOnblur());
        writeOptionalAttribute(tagOut, ONCHANGE_PARAM_NAME, getOnchange());
        writeOptionalAttribute(tagOut, ACCESSKEY_PARAM_NAME, getAccesskey());

        if (isDisabled()) {
            tagOut.writeAttribute(DISABLED_PARAM_NAME, "disabled");
        }
        if (isReadonly()) {
            writeOptionalAttribute(tagOut, READONLY_PARAM_NAME, "readonly");
        }
    }

    public String getOnfocus() {
        return onfocus;
    }

    public String getOnblur() {
        return onblur;
    }

    public String getOnchange() {
        return onchange;
    }

    public String getAccesskey() {
        return accesskey;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isReadonly() {
        return readonly;
    }

}
