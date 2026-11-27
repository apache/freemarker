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
 * The {@link TemplateNumberFormat} behind {@link ScalePreservingTemplateNumberFormatFactory}. Not thread-safe, as it
 * adjusts the fraction digits of the wrapped {@link NumberFormat} for each {@link BigDecimal}.
 */
class ScalePreservingTemplateNumberFormat extends TemplateNumberFormat {

    private final NumberFormat javaNumberFormat;
    private final TemplateNumberFormat nonBigDecimalNumberFormat;

    /**
     * @param javaNumberFormat
     *         Formats the {@link BigDecimal}-s; must not be shared, as its fraction digits will be modified.
     * @param nonBigDecimalNumberFormat
     *         Formats all other numbers.
     */
    ScalePreservingTemplateNumberFormat(
            NumberFormat javaNumberFormat, TemplateNumberFormat nonBigDecimalNumberFormat) {
        this.javaNumberFormat = javaNumberFormat;
        this.nonBigDecimalNumberFormat = nonBigDecimalNumberFormat;
    }

    @Override
    public String formatToPlainText(TemplateNumberModel numberModel)
            throws TemplateValueFormatException, TemplateModelException {
        Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
        return number instanceof BigDecimal
                ? formatBigDecimal((BigDecimal) number)
                : nonBigDecimalNumberFormat.formatToPlainText(numberModel);
    }

    /**
     * Overridden to keep the markup output of the non-{@link BigDecimal} format.
     */
    @Override
    public Object format(TemplateNumberModel numberModel)
            throws TemplateValueFormatException, TemplateModelException {
        Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
        return number instanceof BigDecimal
                ? formatBigDecimal((BigDecimal) number)
                : nonBigDecimalNumberFormat.format(numberModel);
    }

    private String formatBigDecimal(BigDecimal number) throws UnformattableValueException {
        // NumberFormat treats a negative fraction digit count as 0, which is also what we need for a
        // negative scale (as then there are no fraction digits to preserve).
        int scale = number.scale();
        javaNumberFormat.setMinimumFractionDigits(scale);
        javaNumberFormat.setMaximumFractionDigits(scale);
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
        return "scale preserving number format (non-BigDecimal-s are formatted with: "
                + nonBigDecimalNumberFormat.getDescription() + ")";
    }

}
