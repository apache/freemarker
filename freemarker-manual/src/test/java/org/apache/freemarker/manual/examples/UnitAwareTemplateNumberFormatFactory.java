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
package org.apache.freemarker.manual.examples;

import java.util.Locale;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateValueFormatException;

/**
 * A number format that takes any other number format as parameter (specified as a string, as
 * usual in FreeMarker), then if the model is a {@link UnitAwareTemplateNumberModel}, it  shows
 * the unit after the number formatted with the other format, otherwise it just shows the formatted
 * number without unit.
 */
public class UnitAwareTemplateNumberFormatFactory extends TemplateNumberFormatFactory {

    public static final UnitAwareTemplateNumberFormatFactory INSTANCE
            = new UnitAwareTemplateNumberFormatFactory();

    private UnitAwareTemplateNumberFormatFactory() {
        // Defined to decrease visibility
    }

    @Override
    public TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws TemplateValueFormatException {
        return new UnitAwareNumberFormat(env.getTemplateNumberFormat(params, locale));
    }

    private static class UnitAwareNumberFormat extends TemplateNumberFormat {

        private final TemplateNumberFormat innerFormat;

        private UnitAwareNumberFormat(TemplateNumberFormat innerFormat) {
            this.innerFormat = innerFormat;
        }

        @Override
        public String formatToPlainText(TemplateNumberModel numberModel)
                throws TemplateException, TemplateValueFormatException {
            String innerResult = innerFormat.formatToPlainText(numberModel);
            return numberModel instanceof UnitAwareTemplateNumberModel
                    ? innerResult + " " + ((UnitAwareTemplateNumberModel) numberModel).getUnit()
                    : innerResult;
        }

        @Override
        public boolean isLocaleBound() {
            return innerFormat.isLocaleBound();
        }

        @Override
        public String getDescription() {
            return "unit-aware " + innerFormat.getDescription();
        }

    }

}
