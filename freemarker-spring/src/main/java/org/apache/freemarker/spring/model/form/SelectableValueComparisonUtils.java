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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.BindStatus;

/**
 * Utility to check if a value (mostly from an option item) equals to the value bound to.
 */
class SelectableValueComparisonUtils {

    private SelectableValueComparisonUtils() {
    }

    public static boolean isEqualValueBoundTo(final Object value, final BindStatus bindStatus) {
        if (value == null && bindStatus == null) {
            return true;
        }

        Object boundValue = bindStatus.getValue();

        if (ObjectUtils.nullSafeEquals(boundValue, value)) {
            return true;
        }

        final Object actualValue = bindStatus.getActualValue();

        if (actualValue != null && actualValue != boundValue && ObjectUtils.nullSafeEquals(actualValue, value)) {
            return true;
        }

        if (actualValue != null) {
            boundValue = actualValue;
        } else if (boundValue == null) {
            return false;
        }

        boolean equal = false;

        if (boundValue.getClass().isArray()) {
            equal = isValueInCollection(CollectionUtils.arrayToList(boundValue), value, bindStatus);
        } else if (boundValue instanceof Collection) {
            equal = isValueInCollection((Collection<?>) boundValue, value, bindStatus);
        } else if (boundValue instanceof Map) {
            equal = isValueInMapKeys((Map<?, ?>) boundValue, value, bindStatus);
        }

        if (!equal) {
            equal = isEqualValuesComparingWithEditorValue(boundValue, value, bindStatus.getEditor(), null);
        }

        return equal;
    }

    private static boolean isValueInCollection(Collection<?> boundCollection, Object value,
            BindStatus bindStatus) {
        try {
            if (boundCollection.contains(value)) {
                return true;
            }
        } catch (ClassCastException ignoreEx) {
        }

        return isValueInCollectionComparingWithEditorValue(boundCollection, value, bindStatus);
    }

    private static boolean isValueInMapKeys(Map<?, ?> boundMap, Object value, BindStatus bindStatus) {
        try {
            if (boundMap.containsKey(value)) {
                return true;
            }
        } catch (ClassCastException ignoreEx) {
        }

        return isValueInCollectionComparingWithEditorValue(boundMap.keySet(), value, bindStatus);
    }

    private static boolean isValueInCollectionComparingWithEditorValue(Collection<?> collection, Object value,
            BindStatus bindStatus) {

        final Map<PropertyEditor, Object> convertedValueCache = new HashMap<>(1);
        PropertyEditor editor = null;
        final boolean isValueString = (value instanceof String);

        if (!isValueString) {
            editor = bindStatus.findEditor(value.getClass());
        }

        for (Object element : collection) {
            if (editor == null && element != null && isValueString) {
                editor = bindStatus.findEditor(element.getClass());
            }

            if (isEqualValuesComparingWithEditorValue(element, value, editor, convertedValueCache)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isEqualValuesComparingWithEditorValue(Object boundValue, Object value, PropertyEditor editor,
            Map<PropertyEditor, Object> convertedValueCache) {

        String valueDisplayString = AbstractFormTemplateDirectiveModel.getDisplayString(value, editor);

        if (boundValue != null && boundValue.getClass().isEnum()) {
            Enum<?> boundEnum = (Enum<?>) boundValue;
            String enumCodeAsString = ObjectUtils.getDisplayString(boundEnum.name());

            if (enumCodeAsString.equals(valueDisplayString)) {
                return true;
            }

            String enumLabelAsString = ObjectUtils.getDisplayString(boundEnum.toString());

            if (enumLabelAsString.equals(valueDisplayString)) {
                return true;
            }
        } else if (ObjectUtils.getDisplayString(boundValue).equals(valueDisplayString)) {
            return true;
        } else if (editor != null && value instanceof String) {
            String valueAsString = (String) value;
            Object valueAsEditorValue;

            if (convertedValueCache != null && convertedValueCache.containsKey(editor)) {
                valueAsEditorValue = convertedValueCache.get(editor);
            } else {
                editor.setAsText(valueAsString);
                valueAsEditorValue = editor.getValue();

                if (convertedValueCache != null) {
                    convertedValueCache.put(editor, valueAsEditorValue);
                }
            }

            if (ObjectUtils.nullSafeEquals(boundValue, valueAsEditorValue)) {
                return true;
            }
        }

        return false;
    }
}
