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
import java.util.Locale;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.Version;
import freemarker.template._VersionInts;
import freemarker.template.utility.StringUtil;

/**
 * Corresponds to the behavior of {@code ?c} before {@linkplain Configuration#VERSION_2_3_32 2.3.32} (when there
 * were no {@link CFormat}-s yet). This only exists for strict backward-compatibility, otherwise avoid this, mostly
 * because its number-to-string conversion sometimes do rounding, and infinity and NaN formatting has some glitches.
 * This is the default of {@link Configurable#setCFormat(CFormat) c_format} if
 * {@link Configuration#Configuration(Version) incompatible_improvements} is less than 2.3.32.
 *
 * <p>If {@link Configuration#Configuration(Version) incompatible_improvements} is at least
 * 2.3.21, then infinity is formatted as {@code INF}, and NaN as
 * {@code NaN}. If it's less, then infinity is formatted to the infinity character (U+221E), and NaN to the
 * UNICODE replacement character (U+FFFD). But, because of an old bug that we emulate, this only applies to the behavior
 * of {@code ?c}/{@code ?cn}, and not to the {@code "c"} and {@code "computer"}
 * {@link Configurable#setNumberFormat(String) number_format}.
 * The last uses the pre-2.3.21 format before {@link Configuration#Configuration(Version) incompatible_improvements}
 * 2.3.31.
 *
 * <p><b>Experimental class!</b> This class is too new, and might will change over time. Therefore, for now
 * constructor and most methods are not exposed outside FreeMarker, and so you can't create a custom implementation.
 * The class itself and some members are exposed as they are needed for configuring FreeMarker.
 *
 * @see JavaScriptOrJSONCFormat
 *
 * @since 2.3.32
 */
public final class LegacyCFormat extends CFormat {
    public static final LegacyCFormat INSTANCE = new LegacyCFormat();
    public static final String NAME = "legacy";

    /**
     * "c" number format as it was before Incompatible Improvements 2.3.21.
     */
    static final DecimalFormat LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_0 = new DecimalFormat(
            "0.################",
            new DecimalFormatSymbols(Locale.US));
    static {
        LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_0.setGroupingUsed(false);
        LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_0.setDecimalSeparatorAlwaysShown(false);
    }

    /**
     * "c" number format as it was starting from Incompatible Improvements 2.3.21.
     */
    private static final DecimalFormat LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_21 = (DecimalFormat) LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_0.clone();
    static {
        DecimalFormatSymbols symbols = LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_21.getDecimalFormatSymbols();
        symbols.setInfinity("INF");
        symbols.setNaN("NaN");
        LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_21.setDecimalFormatSymbols(symbols);
    }

    private LegacyCFormat() {
    }

    @Override
    final String formatString(String s, Environment env) throws TemplateException {
        return StringUtil.jsStringEnc(
                s, StringUtil.JsStringEncCompatibility.JAVA_SCRIPT_OR_JSON, StringUtil.JsStringEncQuotation.QUOTATION_MARK);
    }

    @Override
    final TemplateNumberFormat getTemplateNumberFormat(Environment env) {
        return getTemplateNumberFormat(env.getConfiguration().getIncompatibleImprovements().intValue());
    }

    TemplateNumberFormat getTemplateNumberFormat(int iciVersion) {
        return new LegacyCTemplateNumberFormat(getLegacyNumberFormat(iciVersion));
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
    NumberFormat getLegacyNumberFormat(Environment env) {
        // Note: DecimalFormat-s aren't thread-safe, so you must clone the static field value.
        return getLegacyNumberFormat(env.getConfiguration().getIncompatibleImprovements().intValue());
    }

    NumberFormat getLegacyNumberFormat(int iciVersion) {
        NumberFormat numberFormatPrototype;
        if (iciVersion < _VersionInts.V_2_3_21) {
            numberFormatPrototype = LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_0;
        } else {
            numberFormatPrototype = LEGACY_NUMBER_FORMAT_PROTOTYPE_2_3_21;
        }
        return (NumberFormat) numberFormatPrototype.clone();
    }

    @Override
    public String getName() {
        return NAME;
    }

    static final class LegacyCTemplateNumberFormat extends JavaTemplateNumberFormat {

        public LegacyCTemplateNumberFormat(NumberFormat numberFormat) {
            super(numberFormat, Environment.COMPUTER_FORMAT_STRING);
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
