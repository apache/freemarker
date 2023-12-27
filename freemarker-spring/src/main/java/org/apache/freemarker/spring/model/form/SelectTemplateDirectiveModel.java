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
import org.apache.freemarker.core.model.*;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;

import java.beans.PropertyEditor;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * Provides <code>TemplateModel</code> for data-binding-aware HTML '{@code select}' element.
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
 *   ...
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:input /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */
class SelectTemplateDirectiveModel extends AbstractHtmlInputElementTemplateDirectiveModel {

    public static final String NAME = "select";

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int ITEMS_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String ITEMS_PARAM_NAME = "items";

    private static final int ITEM_VALUE_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String ITEM_VALUE_PARAM_NAME = "itemValue";

    private static final int ITEM_LABEL_PARAM_IDX = NAMED_ARGS_OFFSET + 2;
    private static final String ITEM_LABEL_PARAM_NAME = "itemLabel";

    private static final int SIZE_PARAM_IDX = NAMED_ARGS_OFFSET + 3;
    private static final String SIZE_PARAM_NAME = "size";

    private static final int MULTIPLE_PARAM_IDX = NAMED_ARGS_OFFSET + 4;
    private static final String MULTIPLE_PARAM_NAME = "multiple";

    protected static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(1, false,
            StringToIndexMap.of(
                    AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                    new StringToIndexMap.Entry(ITEMS_PARAM_NAME, ITEMS_PARAM_IDX),
                    new StringToIndexMap.Entry(ITEM_VALUE_PARAM_NAME, ITEM_VALUE_PARAM_IDX),
                    new StringToIndexMap.Entry(ITEM_LABEL_PARAM_NAME, ITEM_LABEL_PARAM_IDX),
                    new StringToIndexMap.Entry(SIZE_PARAM_NAME, SIZE_PARAM_IDX),
                    new StringToIndexMap.Entry(MULTIPLE_PARAM_NAME, MULTIPLE_PARAM_IDX)),
            true);

    private TemplateModel items;
    private String itemValue;
    private String itemLabel;
    private String size;
    private TemplateModel multiple;

    protected SelectTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public boolean isNestedContentSupported() {
        return true;
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
        size = CallableUtils.getOptionalStringArgument(args, SIZE_PARAM_IDX, this);
        multiple = CallableUtils.getOptionalArgument(args, MULTIPLE_PARAM_IDX, TemplateModel.class, this);

        TagOutputter tagOut = new TagOutputter(env, out);

        tagOut.beginTag(NAME);
        writeDefaultAttributes(tagOut);

        if (isMultipleSelect()) {
            tagOut.writeAttribute("multiple", "multiple");
        }

        tagOut.writeOptionalAttributeValue("size", getDisplayString(evaluate("size", getSize())));

        Object optionItems = null;

        if (getItems() != null) {
            optionItems = objectWrapperAndUnwrapper.unwrap(getItems());
            if (optionItems != null) {
                optionItems = evaluate("items", optionItems);
            }
        }

        if (optionItems != null) {
            final String selectName = getName();
            final String valueProperty = (getItemValue() != null
                    ? ObjectUtils.getDisplayString(evaluate("itemValue", getItemValue()))
                    : null);
            final String labelProperty = (getItemLabel() != null
                    ? ObjectUtils.getDisplayString(evaluate("itemLabel", getItemLabel()))
                    : null);
            OptionOutputHelper optionOutHelper = new OptionOutputHelper(optionItems, getBindStatus(), valueProperty,
                    labelProperty) {
                @Override
                protected String processOptionValue(String resolvedValue) throws TemplateException {
                    return processFieldValue(env, selectName, resolvedValue, "option");
                }
            };
            optionOutHelper.writeOptions(tagOut);
        }

        final FormTemplateScope formTemplateScope = env.getCustomState(FORM_TEMPLATE_SCOPE_KEY);
        try {
            formTemplateScope.setCurrentTagOutputter(tagOut);
            formTemplateScope.setCurrentSelectDirective(this);
            callPlace.executeNestedContent(null, out, env);
        } finally {
            formTemplateScope.setCurrentSelectDirective(null);
            formTemplateScope.setCurrentTagOutputter(null);
        }

        tagOut.endTag();
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

    public String getSize() {
        return size;
    }

    public TemplateModel getMultiple() {
        return multiple;
    }

    private boolean isMultipleSelect() throws TemplateException {
        final TemplateModel model = getMultiple();

        if (model != null) {
            if (model instanceof TemplateBooleanModel) {
                return ((TemplateBooleanModel) model).getAsBoolean();
            } else if (model instanceof TemplateStringModel) {
                final String value = ((TemplateStringModel) model).getAsString();
                if (value == null) {
                    return false;
                }
                return ("multiple".equalsIgnoreCase(value) || Boolean.parseBoolean(value));
            }
        }

        final BindStatus status = getBindStatus();
        final Class<?> valueType = status.getValueType();

        if (valueType != null && isArrayOrCollectionOrMap(valueType)) {
            return true;
        }

        final PropertyEditor editor = status.getEditor();

        if (editor == null) {
            return false;
        }

        final Object editorValue = status.getEditor().getValue();

        if (editorValue != null && isArrayOrCollectionOrMap(editorValue.getClass())) {
            return true;
        }

        return false;
    }

    private boolean isArrayOrCollectionOrMap(Class<?> type) {
        return (type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type));
    }
}
