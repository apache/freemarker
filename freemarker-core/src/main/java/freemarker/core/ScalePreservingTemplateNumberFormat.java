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

import java.math.BigDecimal;
import java.text.NumberFormat;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

/**
 * The {@link TemplateNumberFormat} implementation behind {@link ScalePreservingTemplateNumberFormatFactory}; see the
 * behavior described there. Wraps a locale-specific {@link NumberFormat}, whose minimum and maximum fraction digits
 * are adjusted for each formatted number (which is also why instances aren't thread-safe, as allowed by the
 * {@link TemplateNumberFormat} contract).
 */
class ScalePreservingTemplateNumberFormat extends TemplateNumberFormat {

    private final NumberFormat javaNumberFormat;

    ScalePreservingTemplateNumberFormat(NumberFormat javaNumberFormat) {
        this.javaNumberFormat = javaNumberFormat;
    }

    @Override
    public String formatToPlainText(TemplateNumberModel numberModel)
            throws UnformattableValueException, TemplateModelException {
        Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
        if (number instanceof BigDecimal) {
            // NumberFormat treats a negative fraction digit count as 0, which is also what we need for a
            // negative scale (as then there are no fraction digits to preserve).
            int scale = ((BigDecimal) number).scale();
            javaNumberFormat.setMinimumFractionDigits(scale);
            javaNumberFormat.setMaximumFractionDigits(scale);
        } else {
            javaNumberFormat.setMinimumFractionDigits(0);
            javaNumberFormat.setMaximumFractionDigits(Integer.MAX_VALUE);
        }
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

    @Override
    public String getDescription() {
        return "scale preserving number format";
    }

}
