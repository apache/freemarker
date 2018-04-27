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

import java.beans.PropertyEditor;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.freemarker.core.TemplateException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.BindStatus;

/**
 * Helper to write &lt;option&gt; HTML element.
 */
class OptionOutputHelper {

    private Object optionItems;
    private BindStatus bindStatus;
    private String valueProperty;
    private String labelProperty;

    public OptionOutputHelper(Object optionItems, BindStatus bindStatus, String valueProperty, String labelProperty) {
        this.optionItems = optionItems;
        this.bindStatus = bindStatus;
        this.valueProperty = valueProperty;
        this.labelProperty = labelProperty;
    }

    public void writeOptions(TagOutputter tagOut) throws TemplateException, IOException {
        if (optionItems.getClass().isArray()) {
            writeOptionsFromArray(tagOut, optionItems);
        } else if (optionItems instanceof Collection) {
            writeOptionsFromCollection(tagOut, (Collection<?>) optionItems);
        } else if (optionItems instanceof Map) {
            writeOptionsFromMap(tagOut, (Map<?, ?>) optionItems);
        } else if (optionItems instanceof Class && ((Class<?>) optionItems).isEnum()) {
            writeOptionsFromEnum(tagOut, (Class<?>) optionItems);
        } else {
            throw new TemplateException(
                    "Type [" + optionItems.getClass().getName() + "] is not valid for option items");
        }
    }

    private void writeOptionsFromArray(TagOutputter tagOut, Object array) throws TemplateException, IOException {
        writeOptionsFromCollection(tagOut, CollectionUtils.arrayToList(array));
    }

    private void writeOptionsFromCollection(TagOutputter tagOut, Collection<?> collection)
            throws TemplateException, IOException {
        for (Object item : collection) {
            BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
            Object value;

            if (valueProperty != null) {
                value = wrapper.getPropertyValue(valueProperty);
            } else if (item instanceof Enum) {
                value = ((Enum<?>) item).name();
            } else {
                value = item;
            }

            Object label = (labelProperty != null ? wrapper.getPropertyValue(labelProperty) : item);

            writeOption(tagOut, item, value, label);
        }
    }

    private void writeOptionsFromMap(TagOutputter tagOut, Map<?, ?> map) throws TemplateException, IOException {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            Object renderValue = (valueProperty != null
                    ? PropertyAccessorFactory.forBeanPropertyAccess(key).getPropertyValue(valueProperty)
                    : key);
            Object renderLabel = (labelProperty != null
                    ? PropertyAccessorFactory.forBeanPropertyAccess(value).getPropertyValue(labelProperty)
                    : value);
            writeOption(tagOut, key, renderValue, renderLabel);
        }
    }

    private void writeOptionsFromEnum(TagOutputter tagOut, Class<?> enumClazz) throws TemplateException, IOException {
        writeOptionsFromCollection(tagOut, CollectionUtils.arrayToList(enumClazz.getEnumConstants()));
    }

    private void writeOption(TagOutputter tagOut, Object item, Object value, Object label)
            throws TemplateException, IOException {
        tagOut.beginTag("option");
        writeCommonAttributes(tagOut);

        String valueDisplayString = getDisplayString(value);
        String labelDisplayString = getDisplayString(label);

        valueDisplayString = processOptionValue(valueDisplayString);

        tagOut.writeAttribute("value", valueDisplayString);

        if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
            tagOut.writeAttribute("selected", "selected");
        }

        if (isOptionDisabled()) {
            tagOut.writeAttribute("disabled", "disabled");
        }

        tagOut.appendValue(labelDisplayString);
        tagOut.endTag();
    }

    private String getDisplayString(Object value) throws TemplateException {
        PropertyEditor editor = (value != null) ? bindStatus.findEditor(value.getClass()) : null;

        if (editor != null && !(value instanceof String)) {
            try {
                editor.setValue(value);
                String text = editor.getAsText();

                if (text != null) {
                    return ObjectUtils.getDisplayString(text);
                }
            } catch (Throwable ignore) {
            }
        }

        return ObjectUtils.getDisplayString(value);

    }

    private boolean isOptionSelected(Object resolvedValue) throws TemplateException {
        return SelectableValueComparisonUtils.isEqualValueBoundTo(resolvedValue, bindStatus);
    }

    protected boolean isOptionDisabled() throws TemplateException {
        return false;
    }

    protected String processOptionValue(String resolvedValue) throws TemplateException {
        return resolvedValue;
    }

    protected void writeCommonAttributes(TagOutputter tagOut) throws TemplateException {
    }
}
