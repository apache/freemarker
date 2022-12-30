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
import freemarker.template.Version;

/**
 * Corresponds to the behavior of {@code ?c} if
 * {@link Configuration#Configuration(Version) incompatible_improvements} is less than
 * {@linkplain Configuration#VERSION_2_3_21 2.3.21}.
 * The only good reason for using this is strict backward-compatibility.
 *
 * <p><b>Experimental class!</b> This class is too new, and might will change over time. Therefore, for now
 * constructor and most methods are not exposed outside FreeMarker, and so you can't create a custom implementation.
 * The class itself and some members are exposed as they are needed for configuring FreeMarker.
 *
 * @see Default2321CFormat
 * @see JSONCFormat
 *
 * @since 2.3.32
 */
public final class Default230CFormat extends AbstractLegacyCFormat {
    public static final Default230CFormat INSTANCE = new Default230CFormat();
    public static final String NAME = "default 2.3.0";

    /**
     * "c" number format as it was before Incompatible Improvements 2.3.21.
     */
    static final DecimalFormat LEGACY_NUMBER_FORMAT_PROTOTYPE = new DecimalFormat(
            "0.################",
            new DecimalFormatSymbols(Locale.US));
    static {
        LEGACY_NUMBER_FORMAT_PROTOTYPE.setGroupingUsed(false);
        LEGACY_NUMBER_FORMAT_PROTOTYPE.setDecimalSeparatorAlwaysShown(false);
    }

    private Default230CFormat() {
    }

    @Override
    NumberFormat getLegacyNumberFormat(Environment env) {
        // Note: DecimalFormat-s aren't thread-safe, so you must clone the static field value.
        return (NumberFormat) LEGACY_NUMBER_FORMAT_PROTOTYPE.clone();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
