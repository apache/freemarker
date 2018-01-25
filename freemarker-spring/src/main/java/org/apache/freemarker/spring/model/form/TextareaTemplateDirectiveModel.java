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
 * Provides <code>TemplateModel</code> for data-binding-aware HTML '{@code textarea}' element.
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
 *   &lt;@form.textarea 'user.description' rows="10" cols="80" /&gt;
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:input /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */
class TextareaTemplateDirectiveModel extends AbstractHtmlInputElementTemplateDirectiveModel {

    public static final String NAME = "textarea";

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int ROWS_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String ROWS_PARAM_NAME = "rows";

    private static final int COLS_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String COLS_PARAM_NAME = "cols";

    private static final int ONSELECT_PARAM_IDX = NAMED_ARGS_OFFSET + 2;
    private static final String ONSELECT_PARAM_NAME = "onselect";

    protected static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                            new StringToIndexMap.Entry(ROWS_PARAM_NAME, ROWS_PARAM_IDX),
                            new StringToIndexMap.Entry(COLS_PARAM_NAME, COLS_PARAM_IDX),
                            new StringToIndexMap.Entry(ONSELECT_PARAM_NAME, ONSELECT_PARAM_IDX)
                            ),
                    true
                    );

    private String rows;
    private String cols;
    private String onselect;

    protected TextareaTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
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

        rows = CallableUtils.getOptionalStringArgument(args, ROWS_PARAM_IDX, this);
        cols = CallableUtils.getOptionalStringArgument(args, COLS_PARAM_IDX, this);
        onselect = CallableUtils.getOptionalStringArgument(args, ONSELECT_PARAM_IDX, this);

        TagOutputter tagOut = new TagOutputter(out);

        tagOut.beginTag(NAME);

        writeDefaultAttributes(tagOut);

        // more optional attributes by this tag
        writeOptionalAttribute(tagOut, ROWS_PARAM_NAME, getRows());
        writeOptionalAttribute(tagOut, COLS_PARAM_NAME, getCols());
        writeOptionalAttribute(tagOut, ONSELECT_PARAM_NAME, getOnselect());

        String value = getDisplayString(getBindStatus().getValue(), getBindStatus().getEditor());
        tagOut.appendValue("\r\n" + processFieldValue(env, getName(), value, "textarea"));

        tagOut.endTag();
    }

    public String getRows() {
        return rows;
    }

    public String getCols() {
        return cols;
    }

    public String getOnselect() {
        return onselect;
    }

}
