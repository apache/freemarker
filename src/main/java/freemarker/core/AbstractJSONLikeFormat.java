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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

/**
 * Defines the methods in {@link CFormat} that are the same for all JSON-like languages.
 *
 * <p><b>Experimental class!</b> This class is too new, and might will change over time. Therefore, for now
 * constructor and most methods are not exposed outside FreeMarker, and so you can't create a custom implementation.
 * The class itself and some members are exposed as they are needed for configuring FreeMarker.
 *
 * @since 2.3.32
 */
public abstract class AbstractJSONLikeFormat extends CFormat {
    private static final TemplateNumberFormat TEMPLATE_NUMBER_FORMAT = new CTemplateNumberFormat(
            "Infinity", "-Infinity", "NaN",
            "Infinity", "-Infinity", "NaN");

    private static final DecimalFormat LEGACY_NUMBER_FORMAT_PROTOTYPE
            = (DecimalFormat) LegacyCFormat.LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_0.clone();
    static {
        DecimalFormatSymbols symbols = LEGACY_NUMBER_FORMAT_PROTOTYPE.getDecimalFormatSymbols();
        symbols.setInfinity("Infinity");
        symbols.setNaN("NaN");
        LEGACY_NUMBER_FORMAT_PROTOTYPE.setDecimalFormatSymbols(symbols);
    }

    // Visibility is not "protected" to avoid external implementations while this class is experimental.
    AbstractJSONLikeFormat() {
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

    @Override
    final TemplateNumberFormat getTemplateNumberFormat(Environment env) {
        return TEMPLATE_NUMBER_FORMAT;
    }

    @Override
    NumberFormat getLegacyNumberFormat(Environment env) {
        return (NumberFormat) LEGACY_NUMBER_FORMAT_PROTOTYPE.clone();
    }
}
