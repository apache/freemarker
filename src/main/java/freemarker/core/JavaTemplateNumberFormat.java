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

import java.text.NumberFormat;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

class JavaTemplateNumberFormat extends BackwardCompatibleTemplateNumberFormat {
    
    private final String formatString;
    private final NumberFormat javaNumberFormat;

    public JavaTemplateNumberFormat(NumberFormat javaNumberFormat, String formatString) {
        this.formatString = formatString;
        this.javaNumberFormat = javaNumberFormat;
    }

    @Override
    public String formatToPlainText(TemplateNumberModel numberModel) throws UnformattableValueException, TemplateModelException {
        Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
        return format(number);
    }

    @Override
    public boolean isLocaleBound() {
        return true;
    }

    @Override
    String format(Number number) throws UnformattableValueException {
        try {
            return javaNumberFormat.format(number);
        } catch (ArithmeticException e) {
            throw new UnformattableValueException(
                    "This format can't format the " + number + " number. Reason: " + e.getMessage(), e);
        }
    }

    public NumberFormat getJavaNumberFormat() {
        return javaNumberFormat;
    }

    @Override
    public String getDescription() {
        return formatString;
    }

}
