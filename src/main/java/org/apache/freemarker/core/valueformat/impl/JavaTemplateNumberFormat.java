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
package org.apache.freemarker.core.valueformat.impl;

import java.text.NumberFormat;

import org.apache.freemarker.core.valueformat.TemplateFormatUtil;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;
import org.apache.freemarker.core.valueformat.UnformattableValueException;

final class JavaTemplateNumberFormat extends TemplateNumberFormat {
    
    private final String formatString;
    private final NumberFormat javaNumberFormat;

    public JavaTemplateNumberFormat(NumberFormat javaNumberFormat, String formatString) {
        this.formatString = formatString;
        this.javaNumberFormat = javaNumberFormat;
    }

    @Override
    public String formatToPlainText(TemplateNumberModel numberModel) throws UnformattableValueException, TemplateModelException {
        Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
        try {
            return javaNumberFormat.format(number);
        } catch (ArithmeticException e) {
            throw new UnformattableValueException(
                    "This format can't format the " + number + " number. Reason: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isLocaleBound() {
        return true;
    }

    public NumberFormat getJavaNumberFormat() {
        return javaNumberFormat;
    }

    @Override
    public String getDescription() {
        return formatString;
    }

}
