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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

import freemarker.template.TemplateDateModel;

class JavaTemplateDateFormatFactory extends TemplateDateFormatFactory {

    static final JavaTemplateDateFormatFactory INSTANCE = new JavaTemplateDateFormatFactory();

    /**
     * Exposed to unit testing.
     */
    static final int GUARANTEED_RECENT_ENTRIES = 512;

    private final FastLRUKeyValueStore<CacheKey, DateFormat> dateFormatCache =
            new FastLRUKeyValueStore<>(GUARANTEED_RECENT_ENTRIES);

    private JavaTemplateDateFormatFactory() {
        // Can't be instantiated
    }

    /**
     * @param zonelessInput
     *         Has no effect in this implementation.
     */
    @Override
    public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput,
            Environment env) throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        CacheKey cacheKey = new CacheKey(dateType, params, locale, timeZone);

        DateFormat dateFormat = dateFormatCache.get(cacheKey);
        if (dateFormat == null) {
            dateFormat = getJavaDateFormatNoCache(dateType, params, cacheKey);
            dateFormat = dateFormatCache.putIfAbsentThenReturnStored(cacheKey, dateFormat);
        }

        // Must clone, as SimpleDateFormat is not thread safe, not even if you don't call setters on it:
        return new JavaTemplateDateFormat((DateFormat) dateFormat.clone());
    }

    private DateFormat getJavaDateFormatNoCache(int dateType, String nameOrPattern, CacheKey cacheKey) throws
            UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        DateFormat dateFormat = getJavaDateFormatNoCacheNoCommonAdjustments(dateType, nameOrPattern, cacheKey.locale);
        dateFormat.setTimeZone(cacheKey.timeZone);
        return dateFormat;
    }

    private DateFormat getJavaDateFormatNoCacheNoCommonAdjustments(
            int dateType, String nameOrPattern, Locale locale)
            throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        StringTokenizer tok = new StringTokenizer(nameOrPattern, "_");
        int tok1Style = tok.hasMoreTokens() ? parseDateStyleToken(tok.nextToken()) : DateFormat.DEFAULT;
        if (tok1Style != -1) {
            switch (dateType) {
                case TemplateDateModel.UNKNOWN: {
                    throw new UnknownDateTypeFormattingUnsupportedException();
                }
                case TemplateDateModel.TIME: {
                    return DateFormat.getTimeInstance(tok1Style, locale);
                }
                case TemplateDateModel.DATE: {
                    return DateFormat.getDateInstance(tok1Style, locale);
                }
                case TemplateDateModel.DATETIME: {
                    int tok2Style = tok.hasMoreTokens() ? parseDateStyleToken(tok.nextToken()) : tok1Style;
                    if (tok2Style != -1) {
                        return DateFormat.getDateTimeInstance(tok1Style, tok2Style, locale);
                    }
                    break;
                }
            }
        }

        try {
            return new SimpleDateFormat(nameOrPattern, locale);
        } catch (IllegalArgumentException e) {
            final String msg = e.getMessage();
            throw new InvalidFormatParametersException(
                    msg != null ? msg : "Invalid SimpleDateFormat pattern", e);
        }
    }


    /**
     * Used for unit testing.
     */
    void clear() {
        dateFormatCache.clear();
    }

    /**
     * Used for unit testing.
     */
    int getSize() {
        return dateFormatCache.size();
    }

    private static final class CacheKey {
        private final int dateType;
        private final String pattern;
        private final Locale locale;
        private final TimeZone timeZone;

        CacheKey(int dateType, String pattern, Locale locale, TimeZone timeZone) {
            this.dateType = dateType;
            this.pattern = pattern;
            this.locale = locale;
            this.timeZone = timeZone;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CacheKey) {
                CacheKey fk = (CacheKey) o;
                return dateType == fk.dateType && fk.pattern.equals(pattern) && fk.locale.equals(locale)
                        && fk.timeZone.equals(timeZone);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return dateType ^ pattern.hashCode() ^ locale.hashCode() ^ timeZone.hashCode();
        }
    }

    private int parseDateStyleToken(String token) {
        if ("short".equals(token)) {
            return DateFormat.SHORT;
        }
        if ("medium".equals(token)) {
            return DateFormat.MEDIUM;
        }
        if ("long".equals(token)) {
            return DateFormat.LONG;
        }
        if ("full".equals(token)) {
            return DateFormat.FULL;
        }
        return -1;
    }

}
