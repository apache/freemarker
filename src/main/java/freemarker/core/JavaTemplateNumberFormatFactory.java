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
import java.text.ParseException;
import java.util.Locale;

/**
 * Deals with {@link TemplateNumberFormat}-s that just wrap a Java {@link NumberFormat}.
 */
class JavaTemplateNumberFormatFactory extends TemplateNumberFormatFactory {

    static final JavaTemplateNumberFormatFactory INSTANCE = new JavaTemplateNumberFormatFactory();

    /**
     * Exposed to unit testing.
     */
    static final int GUARANTEED_RECENT_ENTRIES = 512;

    private final FastLRUKeyValueStore<CacheKey, NumberFormat> numberFormatCache = new FastLRUKeyValueStore<>(
            GUARANTEED_RECENT_ENTRIES);

    private JavaTemplateNumberFormatFactory() {
        // Not meant to be instantiated
    }

    @Override
    public TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws InvalidFormatParametersException {
        CacheKey cacheKey = new CacheKey(params, locale);

        NumberFormat numberFormat = numberFormatCache.get(cacheKey);
        if (numberFormat == null) {
            numberFormat = getNumberFormatNoCache(params, locale, env);
            numberFormat = numberFormatCache.putIfAbsentThenReturnStored(cacheKey, numberFormat);
        }

        return new JavaTemplateNumberFormat((NumberFormat) numberFormat.clone(), params);
    }

    private NumberFormat getNumberFormatNoCache(String params, Locale locale, Environment env) throws
            InvalidFormatParametersException {
        if ("number".equals(params)) {
            return NumberFormat.getNumberInstance(locale);
        } else if ("currency".equals(params)) {
            return NumberFormat.getCurrencyInstance(locale);
        } else if ("percent".equals(params)) {
            return NumberFormat.getPercentInstance(locale);
        } else {
            try {
                return ExtendedDecimalFormatParser.parse(params, locale);
            } catch (ParseException e) {
                String msg = e.getMessage();
                throw new InvalidFormatParametersException(
                        msg != null ? msg : "Invalid DecimalFormat pattern", e);
            }
        }
    }


    /**
     * Used for unit testing.
     */
    void clear() {
        numberFormatCache.clear();
    }

    /**
     * Used for unit testing.
     */
    int getSize() {
        return numberFormatCache.size();
    }

    private static final class CacheKey {
        private final String pattern;
        private final Locale locale;

        CacheKey(String pattern, Locale locale) {
            this.pattern = pattern;
            this.locale = locale;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CacheKey) {
                CacheKey fk = (CacheKey) o;
                return fk.pattern.equals(pattern) && fk.locale.equals(locale);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return pattern.hashCode() ^ locale.hashCode();
        }
    }

}
