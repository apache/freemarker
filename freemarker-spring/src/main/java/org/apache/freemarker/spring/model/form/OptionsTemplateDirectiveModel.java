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
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Provides a convenient <code>TemplateModel</code> that allow to supply a collection that are to be rendered
 * as HTML '{@code option}' elements.
 * <P>
 * This directive supports the following parameters:
 * <UL>
 * <LI><code>items</code>: collection of option items.</LI>
 * <LI>
 *   ... TODO ...
 * </LI>
 * </UL>
 * </P>
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 *   ...
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:input /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */
class OptionsTemplateDirectiveModel extends AbstractHtmlInputElementTemplateDirectiveModel {

    public static final String NAME = "options";

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int ITEMS_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String ITEMS_PARAM_NAME = "items";

    private static final int ITEM_VALUE_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String ITEM_VALUE_PARAM_NAME = "itemValue";

    private static final int ITEM_LABEL_PARAM_IDX = NAMED_ARGS_OFFSET + 2;
    private static final String ITEM_LABEL_PARAM_NAME = "itemLabel";

    protected static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(1, false,
            StringToIndexMap.of(
                    AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                    new StringToIndexMap.Entry(ITEMS_PARAM_NAME, ITEMS_PARAM_IDX),
                    new StringToIndexMap.Entry(ITEM_VALUE_PARAM_NAME, ITEM_VALUE_PARAM_IDX),
                    new StringToIndexMap.Entry(ITEM_LABEL_PARAM_NAME, ITEM_LABEL_PARAM_IDX)),
            true);

    private TemplateModel items;
    private String itemValue;
    private String itemLabel;

    protected OptionsTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
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
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, final Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {

        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);

        items = CallableUtils.getOptionalArgument(args, ITEMS_PARAM_IDX, TemplateModel.class, this);
        itemValue = CallableUtils.getOptionalStringArgument(args, ITEM_VALUE_PARAM_IDX, this);
        itemLabel = CallableUtils.getOptionalStringArgument(args, ITEM_LABEL_PARAM_IDX, this);

        final FormTemplateScope formTemplateScope = env.getCustomState(FORM_TEMPLATE_SCOPE_KEY);
        final SelectTemplateDirectiveModel curSelectDirective = formTemplateScope.getCurrentSelectDirective();

        Object optionItems = null;

        if (getItems() != null) {
            optionItems = objectWrapperAndUnwrapper.unwrap(getItems());
            if (optionItems != null) {
                optionItems = evaluate("items", optionItems);
            }
        }

        if (optionItems != null) {
            final String selectName = curSelectDirective.getName();
            final String valueProperty = (getItemValue() != null
                    ? ObjectUtils.getDisplayString(evaluate("itemValue", getItemValue()))
                    : null);
            final String labelProperty = (getItemLabel() != null
                    ? ObjectUtils.getDisplayString(evaluate("itemLabel", getItemLabel()))
                    : null);
            OptionOutputHelper optionOutHelper = new OptionOutputHelper(optionItems, curSelectDirective.getBindStatus(),
                    valueProperty, labelProperty) {
                @Override
                protected String processOptionValue(String resolvedValue) throws TemplateException {
                    return processFieldValue(env, selectName, resolvedValue, "option");
                }
            };

            optionOutHelper.writeOptions(formTemplateScope.getCurrentTagOutputter());
        }
    }

    public TemplateModel getItems() {
        return items;
    }

    public String getItemValue() {
        return itemValue;
    }

    public String getItemLabel() {
        return itemLabel;
    }

}
