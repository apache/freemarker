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

package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.utility.StringUtil;
import freemarker.template.utility.StringUtil.JsStringEncCompatibility;
import freemarker.template.utility.StringUtil.JsStringEncQuotation;

/**
 * Super class of {@link CFormat}-s that merely exist to mimic old {@code ?c} behavior for backward compatibility.
 *
 * <p><b>Experimental class!</b> This class is too new, and might will change over time. Therefore, for now
 * constructor and most methods are not exposed outside FreeMarker, and so you can't create a custom implementation.
 * The class itself and some members are exposed as they are needed for configuring FreeMarker.
 *
 * @since 2.3.32
 * @see AbstractJSONLikeFormat
 */
public abstract class AbstractLegacyCFormat extends CFormat {
    // Visibility is not "protected" to avoid external implementations while this class is experimental.
    AbstractLegacyCFormat() {
    }

    @Override
    final String formatString(String s, Environment env) throws TemplateException {
        return StringUtil.jsStringEnc(
                s, JsStringEncCompatibility.JAVA_SCRIPT_OR_JSON, JsStringEncQuotation.QUOTATION_MARK);
    }

    @Override
    final TemplateNumberFormat getTemplateNumberFormat(Environment env) {
        return new LegacyCTemplateNumberFormat(env);
    }

    @Override
    String getTrueString() {
        return "true";
    }

    @Override
    String getFalseString() {
        return "false";
    }

    @Override
    final String getNullString() {
        return "null";
    }

    final class LegacyCTemplateNumberFormat extends JavaTemplateNumberFormat {

        public LegacyCTemplateNumberFormat(Environment env) {
            super(getLegacyNumberFormat(env), Environment.COMPUTER_FORMAT_STRING);
        }

        @Override
        public String formatToPlainText(TemplateNumberModel numberModel) throws UnformattableValueException,
                TemplateModelException {
            Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
            return format(number);
        }

        @Override
        public boolean isLocaleBound() {
            return false;
        }

        @Override
        String format(Number number) throws UnformattableValueException {
            if (number instanceof Integer || number instanceof Long) {
                // Accelerate these fairly common cases
                return number.toString();
            }
            return super.format(number);
        }

        @Override
        public String getDescription() {
            return "LegacyC(" + super.getDescription() + ")";
        }

    }

}
