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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.SimpleCollection;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Provides <code>TemplateModel</code> for displaying errors for a particular field or object.
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
 *   &lt;@form.errors '*'; messages&gt;
 *     &lt;ul&gt;
 *       &lt;#list messages as message&gt;
 *         &lt;li&gt;${message}&lt;/li&gt;
 *       &lt;/#list&gt;
 *     &lt;/ul&gt;
 *   &lt;/@form.errors&gt;
 *
 *   &lt;!-- SNIP --&gt;
 *
 *   &lt;@form.errors 'firstName'; messages /&gt;
 *
 *   &lt;!-- SNIP --&gt;
 *
 *   &lt;@form.errors 'email' cssClass="errorEmail"; messages /&gt;
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:errors /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */

class ErrorsTemplateDirectiveModel extends AbstractHtmlElementTemplateDirectiveModel {

    public static final String NAME = "errors";

    private static final String DEFAULT_ELEMENT = "span";

    private static final String DEFAULT_DELIMITER = "<br/>";

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int ELEMENT_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String ELEMENT_PARAM_NAME = "element";

    private static final int DELIMITER_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String DELIMITER_PARAM_NAME = "delimiter";

    protected static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(AbstractHtmlElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                            new StringToIndexMap.Entry(ELEMENT_PARAM_NAME, ELEMENT_PARAM_IDX),
                            new StringToIndexMap.Entry(DELIMITER_PARAM_NAME, DELIMITER_PARAM_IDX)
                            ),
                    true);

    private String element = "span";

    private String delimiter = "<br/>";

    protected ErrorsTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    public boolean isNestedContentSupported() {
        return true;
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {
        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);

        if (!shouldRender()) {
            return;
        }

        String param = CallableUtils.getOptionalStringArgument(args, ELEMENT_PARAM_IDX, this);
        element = (StringUtils.hasText(param)) ? param.trim() : DEFAULT_ELEMENT;

        param = CallableUtils.getOptionalStringArgument(args, DELIMITER_PARAM_IDX, this);
        delimiter = (StringUtils.hasText(param)) ? param : DEFAULT_DELIMITER;

        if (!callPlace.hasNestedContent()) {
            TagOutputter tagOut = new TagOutputter(out);
            renderDefaultContent(tagOut);
            return;
        }

        if (callPlace.getNestedContentParameterCount() == 0) {
            callPlace.executeNestedContent(null, out, env);
            return;
        }

        List<String> messages = new ArrayList<String>();
        messages.addAll(Arrays.asList(getBindStatus().getErrorMessages()));
        SimpleCollection messagesModel = new SimpleCollection(messages, objectWrapperAndUnwrapper);
        final TemplateModel[] nestedContentArgs = new TemplateModel[] { messagesModel };
        callPlace.executeNestedContent(nestedContentArgs, out, env);
    }

    public String getElement() {
        return element;
    }

    public String getDelimiter() {
        return delimiter;
    }

    protected void renderDefaultContent(final TagOutputter tagOut) throws TemplateException, IOException {
        tagOut.beginTag(getElement());
        writeDefaultAttributes(tagOut);
        String delim = ObjectUtils.getDisplayString(evaluate("delimiter", getDelimiter()));
        String[] errorMessages = getBindStatus().getErrorMessages();

        for (int i = 0; i < errorMessages.length; i++) {
            String errorMessage = errorMessages[i];

            if (i > 0) {
                tagOut.appendValue(delim);
            }

            tagOut.appendValue(getDisplayString(errorMessage));
        }

        tagOut.endTag();
    }

    @Override
    protected String autogenerateId() throws TemplateException {
        String path = getPropertyPath();

        if ("".equals(path) || "*".equals(path)) {
            path = (String) getRequest().getAttribute(FormTemplateDirectiveModel.MODEL_ATTRIBUTE_VARIABLE_NAME);
        }

        return StringUtils.deleteAny(path, "[]") + ".errors";
    }

    @Override
    protected String getName() throws TemplateException {
        // suppress the 'id' and 'name' attribute.
        return null;
    }

    protected boolean shouldRender() throws TemplateException {
        final BindStatus bindStatus = getBindStatus();
        return (bindStatus != null) && bindStatus.isError();
    }
}
