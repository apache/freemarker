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
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePair;
import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePairIterator;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractMultiCheckedElementTag</code>.
 */
abstract class AbstractMultiCheckedElementTemplateDirectiveModel extends AbstractCheckedElementTemplateDirectiveModel {

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int ITEMS_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String ITEMS_PARAM_NAME = "items";

    private static final int ITEM_VALUE_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String ITEM_VALUE_PARAM_NAME = "itemValue";

    private static final int ITEM_LABEL_PARAM_IDX = NAMED_ARGS_OFFSET + 2;
    private static final String ITEM_LABEL_PARAM_NAME = "itemLabel";

    private static final int ENCLOSING_ELEMENT_PARAM_IDX = NAMED_ARGS_OFFSET + 3;
    private static final String ENCLOSING_ELEMENT_PARAM_NAME = "element";

    private static final int ITEM_ELEMENT_DELIMITER_PARAM_IDX = NAMED_ARGS_OFFSET + 4;
    private static final String ITEM_ELEMENT_DELIMITER_PARAM_NAME = "delimiter";

    protected static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(1, false,
            StringToIndexMap.of(
                    AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                    new StringToIndexMap.Entry(ITEMS_PARAM_NAME, ITEMS_PARAM_IDX),
                    new StringToIndexMap.Entry(ITEM_VALUE_PARAM_NAME, ITEM_VALUE_PARAM_IDX),
                    new StringToIndexMap.Entry(ITEM_LABEL_PARAM_NAME, ITEM_LABEL_PARAM_IDX),
                    new StringToIndexMap.Entry(ENCLOSING_ELEMENT_PARAM_NAME, ENCLOSING_ELEMENT_PARAM_IDX),
                    new StringToIndexMap.Entry(ITEM_ELEMENT_DELIMITER_PARAM_NAME, ITEM_ELEMENT_DELIMITER_PARAM_IDX)),
            true);

    private static final String DEFAULT_ENCLOSING_ELEMENT_NAME = "span";

    private TemplateModel items;
    private String itemValue;
    private String itemLabel;
    private String enclosingElementName;
    private String itemElementDelimiter;

    protected AbstractMultiCheckedElementTemplateDirectiveModel(HttpServletRequest request,
            HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {
        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);

        items = CallableUtils.getArgument(args, ITEMS_PARAM_IDX, TemplateModel.class, this);
        itemValue = CallableUtils.getOptionalStringArgument(args, ITEM_VALUE_PARAM_IDX, this);
        itemLabel = CallableUtils.getOptionalStringArgument(args, ITEM_LABEL_PARAM_IDX, this);
        enclosingElementName = CallableUtils.getOptionalStringArgument(args, ENCLOSING_ELEMENT_PARAM_IDX,
                DEFAULT_ENCLOSING_ELEMENT_NAME, this);
        itemElementDelimiter = CallableUtils.getOptionalStringArgument(args, ITEM_ELEMENT_DELIMITER_PARAM_IDX, this);

        final TagOutputter tagOut = new TagOutputter(env, out);

        final String itemValue = getItemValue();
        final String itemLabel = getItemLabel();
        final String valueProperty = (itemValue != null ? ObjectUtils.getDisplayString(evaluate("itemValue", itemValue))
                : null);
        final String labelProperty = (itemLabel != null ? ObjectUtils.getDisplayString(evaluate("itemLabel", itemLabel))
                : null);

        if (items instanceof TemplateIterableModel) {
            int itemIndex = 0;

            for (TemplateModelIterator it = ((TemplateIterableModel) items).iterator(); it.hasNext(); itemIndex++) {
                final TemplateModel item = it.next();
                writeObjectEntry(env, tagOut, valueProperty, labelProperty, objectWrapperAndUnwrapper.unwrap(item),
                        itemIndex);
            }
        } else if (items instanceof TemplateHashModelEx) {
            int itemIndex = 0;

            for (KeyValuePairIterator it = ((TemplateHashModelEx) items).keyValuePairIterator(); it
                    .hasNext(); itemIndex++) {
                final KeyValuePair pair = it.next();
                final Object key = objectWrapperAndUnwrapper.unwrap(pair.getKey());
                final Object value = objectWrapperAndUnwrapper.unwrap(pair.getValue());
                writeKeyValueEntry(env, tagOut, valueProperty, labelProperty, key, value, itemIndex);
            }
        } else {
            final Object itemsObject = objectWrapperAndUnwrapper.unwrap(items);

            if (itemsObject.getClass().isArray()) {
                final Object[] itemsArray = (Object[]) itemsObject;

                for (int i = 0; i < itemsArray.length; i++) {
                    final Object item = itemsArray[i];
                    writeObjectEntry(env, tagOut, valueProperty, labelProperty, item, i);
                }
            } else if (itemsObject instanceof Collection) {
                final Collection<Object> optionCollection = (Collection<Object>) itemsObject;
                int itemIndex = 0;

                for (Object item : optionCollection) {
                    writeObjectEntry(env, tagOut, valueProperty, labelProperty, item, itemIndex++);
                }
            } else if (itemsObject instanceof Map) {
                final Map<Object, Object> optionMap = (Map<Object, Object>) itemsObject;
                int itemIndex = 0;

                for (Map.Entry<Object, Object> entry : optionMap.entrySet()) {
                    final Object key = entry.getKey();
                    final Object value = entry.getValue();
                    writeKeyValueEntry(env, tagOut, valueProperty, labelProperty, key, value, itemIndex++);
                }
            } else {
                throw new TemplateException(
                        "The 'items' template model must be TemplateIterableModel or TemplateHashModelEx, "
                                + "or represent an array, a Collection or a Map.");
            }
        }

        writeAdditionalDetails(env, tagOut);
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

    public String getEnclosingElementName() {
        return enclosingElementName;
    }

    public String getItemElementDelimiter() {
        return itemElementDelimiter;
    }

    /**
     * Appends a counter to the ID value because there will be multiple HTML elements.
     */
    @Override
    protected String resolveId(Environment env) throws TemplateException {
        Object id = evaluate(ID_PARAM_NAME, getId());

        if (id != null) {
            String idString = id.toString();
            return (StringUtils.hasText(idString)) ? TagIdGenerationUtils.getNextId(env, idString) : null;
        }

        return autogenerateId(env);
    }

    protected abstract void writeAdditionalDetails(Environment env, TagOutputter tagOut)
            throws TemplateException, IOException;

    private void writeObjectEntry(final Environment env, final TagOutputter tagOut, String valueProperty,
            String labelProperty, Object item, int itemIndex) throws TemplateException, IOException {
        final BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
        Object renderValue;

        if (valueProperty != null) {
            renderValue = wrapper.getPropertyValue(valueProperty);
        } else if (item instanceof Enum) {
            renderValue = ((Enum<?>) item).name();
        } else {
            renderValue = item;
        }

        final Object renderLabel = (labelProperty != null) ? wrapper.getPropertyValue(labelProperty) : item;
        writeElementTag(env, tagOut, item, renderValue, renderLabel, itemIndex);
    }

    private void writeKeyValueEntry(final Environment env, final TagOutputter tagOut, String valueProperty,
            String labelProperty, Object key, Object value, int itemIndex) throws TemplateException, IOException {
        final BeanWrapper keyWrapper = PropertyAccessorFactory.forBeanPropertyAccess(key);
        final BeanWrapper valueWrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);
        final Object renderValue = (valueProperty != null ? keyWrapper.getPropertyValue(valueProperty)
                : key.toString());
        final Object renderLabel = (labelProperty != null ? valueWrapper.getPropertyValue(labelProperty)
                : value.toString());
        writeElementTag(env, tagOut, key, renderValue, renderLabel, itemIndex);
    }

    private void writeElementTag(final Environment env, final TagOutputter tagOut, Object item, Object value,
            Object label, int itemIndex) throws TemplateException, IOException {
        final boolean enclosingByElem = StringUtils.hasText(getEnclosingElementName());

        if (enclosingByElem) {
            tagOut.beginTag(getEnclosingElementName());
        }

        if (itemIndex > 0) {
            Object resolvedDelimiter = evaluate("delimiter", getItemElementDelimiter());

            if (resolvedDelimiter != null) {
                tagOut.appendValue(resolvedDelimiter.toString());
            }
        }

        tagOut.beginTag("input");
        String id = resolveId(env);
        writeOptionalAttribute(tagOut, "id", id);
        writeOptionalAttribute(tagOut, "name", getName());
        writeOptionalAttributes(tagOut);
        tagOut.writeAttribute("type", getInputType());
        renderFromValue(env, item, value, tagOut);
        tagOut.endTag();

        tagOut.beginTag("label");
        tagOut.writeAttribute("for", id);
        tagOut.appendValue(getDisplayString(label, getBindStatus()));
        tagOut.endTag();

        if (enclosingByElem) {
            tagOut.endTag();
        }
    }
}
