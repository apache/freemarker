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

import freemarker.template.TemplateException;
import freemarker.template.utility.StringUtil;

class CustomCFormat extends CFormat {
    final static CustomCFormat INSTANCE = new CustomCFormat();

    private CustomCFormat() {
    }

    private static final TemplateNumberFormat TEMPLATE_NUMBER_FORMAT = new CTemplateNumberFormat(
            "M:INF", "M:NINF", "M:NaN",
            "M:INF", "M:NINF", "M:NaN");

    private static final DecimalFormat LEGACY_NUMBER_FORMAT_PROTOTYPE
            = (DecimalFormat) LegacyCFormat.LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_0.clone();

    static {
        DecimalFormatSymbols symbols = LEGACY_NUMBER_FORMAT_PROTOTYPE.getDecimalFormatSymbols();
        symbols.setInfinity("Infinity");
        symbols.setNaN("NaN");
        LEGACY_NUMBER_FORMAT_PROTOTYPE.setDecimalFormatSymbols(symbols);
    }

    @Override
    TemplateNumberFormat getTemplateNumberFormat(Environment env) {
        return TEMPLATE_NUMBER_FORMAT;
    }

    @Override
    NumberFormat getLegacyNumberFormat(Environment env) {
        return (NumberFormat) LEGACY_NUMBER_FORMAT_PROTOTYPE.clone();
    }

    @Override
    String formatString(String s, Environment env) throws TemplateException {
        return StringUtil.ftlQuote(s);
    }

    @Override
    String getTrueString() {
        return "TRUE";
    }

    @Override
    String getFalseString() {
        return "FALSE";
    }

    @Override
    String getNullString() {
        return "NULL";
    }

    @Override
    public String getName() {
        return "custom";
    }
}
