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
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Provides <code>TemplateModel</code> for data-binding-aware HTML '{@code input}' element with a '{@code type}'
 * of '{@code text}'.
 * <P>
 * This directive supports the following parameters:
 * <UL>
 * <LI><code>path</code>: The first positional parameter pointing to the bean or bean property to bind status information for.</LI>
 * <LI>
 *   ... TODO ...
 * </LI>
 * </UL>
 * </P>
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 *   &lt;#assign form=spring.form /&gt;
 *   ...
 *   &lt;@form.input 'user.firstName' /&gt;
 *   
 *   &lt;@form.input 'user.email' id="customEmailId" /&gt;
 *   
 *   ...
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:input /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */
class InputTemplateDirectiveModel extends AbstractHtmlInputElementTemplateDirectiveModel {

    public static final String NAME = "input";

    private static final int NAMED_ARGS_OFFSET =
            getLastPredefinedNamedArgumentIndex(AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT) + 1;

    private static final int SIZE_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String SIZE_PARAM_NAME = "size";

    private static final int MAXLENGTH_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String MAXLENGTH_PARAM_NAME = "maxlength";

    private static final int ALT_PARAM_IDX = NAMED_ARGS_OFFSET + 2;
    private static final String ALT_PARAM_NAME = "alt";

    private static final int ONSELECT_PARAM_IDX = NAMED_ARGS_OFFSET + 3;
    private static final String ONSELECT_PARAM_NAME = "onselect";

    private static final int AUTOCOMPLETE_PARAM_IDX = NAMED_ARGS_OFFSET + 4;
    private static final String AUTOCOMPLETE_PARAM_NAME = "autocomplete";

    protected static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                            new StringToIndexMap.Entry(SIZE_PARAM_NAME, SIZE_PARAM_IDX),
                            new StringToIndexMap.Entry(MAXLENGTH_PARAM_NAME, MAXLENGTH_PARAM_IDX),
                            new StringToIndexMap.Entry(ALT_PARAM_NAME, ALT_PARAM_IDX),
                            new StringToIndexMap.Entry(ONSELECT_PARAM_NAME, ONSELECT_PARAM_IDX),
                            new StringToIndexMap.Entry(AUTOCOMPLETE_PARAM_NAME, AUTOCOMPLETE_PARAM_IDX)
                            ),
                    true
                    );

    private String size;
    private String maxlength;
    private String alt;
    private String onselect;
    private String autocomplete;

    protected InputTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public boolean isNestedContentSupported() {
        return false;
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

        size = CallableUtils.getOptionalStringArgument(args, SIZE_PARAM_IDX, this);
        maxlength = CallableUtils.getOptionalStringArgument(args, MAXLENGTH_PARAM_IDX, this);
        alt = CallableUtils.getOptionalStringArgument(args, ALT_PARAM_IDX, this);
        onselect = CallableUtils.getOptionalStringArgument(args, ONSELECT_PARAM_IDX, this);
        autocomplete = CallableUtils.getOptionalStringArgument(args, AUTOCOMPLETE_PARAM_IDX, this);

        TagOutputter tagOut = new TagOutputter(out);

        tagOut.beginTag(NAME);

        writeDefaultAttributes(tagOut);

        if (!hasDynamicTypeAttribute()) {
            tagOut.writeAttribute("type", getType());
        }

        writeValue(env, tagOut);

        // more optional attributes by this tag
        writeOptionalAttribute(tagOut, SIZE_PARAM_NAME, getSize());
        writeOptionalAttribute(tagOut, MAXLENGTH_PARAM_NAME, getMaxlength());
        writeOptionalAttribute(tagOut, ALT_PARAM_NAME, getAlt());
        writeOptionalAttribute(tagOut, ONSELECT_PARAM_NAME, getOnselect());
        writeOptionalAttribute(tagOut, AUTOCOMPLETE_PARAM_NAME, getAutocomplete());

        tagOut.endTag();
    }

    public String getSize() {
        return size;
    }

    public String getMaxlength() {
        return maxlength;
    }

    public String getAlt() {
        return alt;
    }

    public String getOnselect() {
        return onselect;
    }

    public String getAutocomplete() {
        return autocomplete;
    }

    private boolean hasDynamicTypeAttribute() {
        return getDynamicAttributes().containsKey("type");
    }

    protected void writeValue(Environment env, TagOutputter tagOut) throws TemplateException, IOException {
        String value = getDisplayString(getBindStatus().getValue(), getBindStatus().getEditor());
        String type = hasDynamicTypeAttribute() ? (String) getDynamicAttributes().get("type") : getType();
        tagOut.writeAttribute("value", processFieldValue(env, getName(), value, type));
    }

    protected String getType() {
        return "text";
    }

}
