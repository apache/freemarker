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

package org.apache.freemarker.core;

import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.core.valueformat.TemplateValueFormat;

// TODO Should be public and moved over to core.valueformat?
final class TemplateBooleanFormat extends TemplateValueFormat {

    static final String C_FORMAT_STRING = "c";
    static final String C_FALSE = "false";
    static final String C_TRUE = "true";
    static final TemplateBooleanFormat C_INSTANCE = new TemplateBooleanFormat(C_FORMAT_STRING, C_TRUE, C_FALSE);

    /**
     * @return {@code null} if the format string was an empty string.
     */
    static TemplateBooleanFormat getInstance(String formatString) {
        return getInstanceOrValidate(formatString, false);
    }

    static void validateFormatString(String formatString) {
        getInstanceOrValidate(formatString, true);
    }

    private static TemplateBooleanFormat getInstanceOrValidate(String formatString, boolean validationOnly) {
        if (formatString.length() == 0) {
            return null;
        }
        if (formatString.equals(C_FORMAT_STRING)) {
            return C_INSTANCE;
        }

        int commaIdx = formatString.indexOf(',');
        if (commaIdx == -1) {
            throw new IllegalArgumentException(
                    "Setting value must be a string that contains two comma-separated values for true and false, "
                            + "or it must be \"" + TemplateBooleanFormat.C_FORMAT_STRING + "\", "
                            + "or it must be an empty string, but it was "
                            + _StringUtils.jQuote(formatString) + ".");
        }
        if (validationOnly) {
            return null;
        }

        String trueStringValue = formatString.substring(0, commaIdx);
        String falseStringValue = formatString.substring(commaIdx + 1);
        return new TemplateBooleanFormat(formatString, trueStringValue, falseStringValue);
    }

    private final String formatString;
    private final String trueStringValue;  // Usually deduced from booleanFormat
    private final String falseStringValue;  // Usually deduced from booleanFormat

    private TemplateBooleanFormat(String formatString, String trueStringValue, String falseStringValue) {
        this.formatString = formatString;
        this.trueStringValue = trueStringValue;
        this.falseStringValue = falseStringValue;
    }

    public String getFormatString() {
        return formatString;
    }

    /**
     * Returns the string to which {@code true} is converted to for human audience, or {@code null} if automatic
     * coercion to string is not allowed. The default value is {@code null}.
     *
     * <p>This value is deduced from the {@code "booleanFormat"} setting.
     * Confusingly, for backward compatibility (at least until 2.4) that defaults to {@code "true,false"}, yet this
     * defaults to {@code null}. That's so because {@code "true,false"} is treated exceptionally, as that default is a
     * historical mistake in FreeMarker, since it targets computer language output, not human writing. Thus it's
     * ignored.
     */
    public String getTrueStringValue() {
        return trueStringValue;
    }

    /**
     * Same as {@link #getTrueStringValue()} but with {@code false}.
     */
    public String getFalseStringValue() {
        return falseStringValue;
    }

    @Override
    public String getDescription() {
        return _StringUtils.jQuote(formatString);
    }

}
