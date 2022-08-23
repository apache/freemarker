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
import java.util.concurrent.ConcurrentHashMap;

import freemarker.log.Logger;
import freemarker.template.TemplateDateModel;

class JavaTemplateDateFormatFactory extends TemplateDateFormatFactory {
    
    static final JavaTemplateDateFormatFactory INSTANCE = new JavaTemplateDateFormatFactory(); 
    
    private static final Logger LOG = Logger.getLogger("freemarker.runtime");

    private static final int MAX_CACHE_SIZE = 2; //!!T 512

    private final ConcurrentHashMap<CacheKey, DateFormat> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CacheKey, DateFormat> cacheRecallableEntries = new ConcurrentHashMap<>();

    private JavaTemplateDateFormatFactory() {
        // Can't be instantiated
    }
    
    /**
     * @param zonelessInput
     *            Has no effect in this implementation.
     */
    @Override
    public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput,
            Environment env) throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        return new JavaTemplateDateFormat(getJavaDateFormat(dateType, params, locale, timeZone));
    }

    /**
     * Returns a "private" copy (not in the global cache) for the given format.  
     */
    private DateFormat getJavaDateFormat(int dateType, String nameOrPattern, Locale locale, TimeZone timeZone)
            throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {

        // Get DateFormat from global cache:
        CacheKey cacheKey = new CacheKey(dateType, nameOrPattern, locale, timeZone);

        DateFormat dateFormat = getFromCache(cacheKey);
        if (dateFormat == null) {
            dateFormat = getJavaDateFormatNoCache(dateType, nameOrPattern, cacheKey);
            dateFormat = addToCacheWithLimitingSize(cacheKey, dateFormat);
        }

        // Must clone, as SimpleDateFormat is not thread safe, not even if you don't call setters on it:
        return (DateFormat) dateFormat.clone();
    }

    private DateFormat addToCacheWithLimitingSize(CacheKey cacheKey, DateFormat dateFormat) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            synchronized (JavaTemplateDateFormatFactory.class) {
                if (cache.size() >= MAX_CACHE_SIZE) {
                    cacheRecallableEntries.clear();
                    cacheRecallableEntries.putAll(cache);
                    cache.clear();
                }
            }
        }

        DateFormat prevDateFormat = cache.putIfAbsent(cacheKey, dateFormat);
        return prevDateFormat != null ? prevDateFormat : dateFormat;
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

    private DateFormat getFromCache(CacheKey cacheKey) {
        DateFormat dateFormat = cache.get(cacheKey);
        if (dateFormat != null) {
            return dateFormat;
        }

        dateFormat = cacheRecallableEntries.remove(cacheKey);
        if (dateFormat == null) {
            return null;
        }

        return addToCacheWithLimitingSize(cacheKey, dateFormat);
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
