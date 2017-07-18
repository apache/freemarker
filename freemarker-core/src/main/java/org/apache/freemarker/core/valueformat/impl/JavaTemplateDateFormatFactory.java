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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.valueformat.InvalidFormatParametersException;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.UnknownDateTypeFormattingUnsupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deals with {@link TemplateDateFormat}-s that wrap a Java {@link DateFormat}. The parameter string is usually a
 * {@link java.text.SimpleDateFormat} pattern, but it also recognized the names "short", "medium", "long"
 * and "full", which correspond to formats defined by {@link DateFormat} with similar names.
 *
 * <p>Note that the resulting {@link java.text.SimpleDateFormat}-s are globally cached, and threading issues are
 * addressed by cloning the cached instance before returning it. So it just makes object creation faster, but doesn't
 * eliminate it.
 */
public class JavaTemplateDateFormatFactory extends TemplateDateFormatFactory {
    
    public static final JavaTemplateDateFormatFactory INSTANCE = new JavaTemplateDateFormatFactory();

    private static final Logger LOG = LoggerFactory.getLogger(JavaTemplateDateFormatFactory.class);

    private static final ConcurrentHashMap<CacheKey, DateFormat> GLOBAL_FORMAT_CACHE = new ConcurrentHashMap<>();
    private static final int LEAK_ALERT_DATE_FORMAT_CACHE_SIZE = 1024;
    
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
        DateFormat jFormat;
        
        jFormat = GLOBAL_FORMAT_CACHE.get(cacheKey);
        if (jFormat == null) {
            // Add format to global format cache.
            StringTokenizer tok = new StringTokenizer(nameOrPattern, "_");
            int tok1Style = tok.hasMoreTokens() ? parseDateStyleToken(tok.nextToken()) : DateFormat.DEFAULT;
            if (tok1Style != -1) {
                switch (dateType) {
                    case TemplateDateModel.UNKNOWN: {
                        throw new UnknownDateTypeFormattingUnsupportedException();
                    }
                    case TemplateDateModel.TIME: {
                        jFormat = DateFormat.getTimeInstance(tok1Style, cacheKey.locale);
                        break;
                    }
                    case TemplateDateModel.DATE: {
                        jFormat = DateFormat.getDateInstance(tok1Style, cacheKey.locale);
                        break;
                    }
                    case TemplateDateModel.DATE_TIME: {
                        int tok2Style = tok.hasMoreTokens() ? parseDateStyleToken(tok.nextToken()) : tok1Style;
                        if (tok2Style != -1) {
                            jFormat = DateFormat.getDateTimeInstance(tok1Style, tok2Style, cacheKey.locale);
                        }
                        break;
                    }
                }
            }
            if (jFormat == null) {
                try {
                    jFormat = new SimpleDateFormat(nameOrPattern, cacheKey.locale);
                } catch (IllegalArgumentException e) {
                    final String msg = e.getMessage();
                    throw new InvalidFormatParametersException(
                            msg != null ? msg : "Invalid SimpleDateFormat pattern", e);
                }
            }
            jFormat.setTimeZone(cacheKey.timeZone);
            
            if (GLOBAL_FORMAT_CACHE.size() >= LEAK_ALERT_DATE_FORMAT_CACHE_SIZE) {
                boolean triggered = false;
                synchronized (JavaTemplateDateFormatFactory.class) {
                    if (GLOBAL_FORMAT_CACHE.size() >= LEAK_ALERT_DATE_FORMAT_CACHE_SIZE) {
                        triggered = true;
                        GLOBAL_FORMAT_CACHE.clear();
                    }
                }
                if (triggered) {
                    LOG.warn("Global Java DateFormat cache has exceeded {} entries => cache flushed. "
                            + "Typical cause: Some template generates high variety of format pattern strings.",
                            LEAK_ALERT_DATE_FORMAT_CACHE_SIZE);
                }
            }
            
            DateFormat prevJFormat = GLOBAL_FORMAT_CACHE.putIfAbsent(cacheKey, jFormat);
            if (prevJFormat != null) {
                jFormat = prevJFormat;
            }
        }  // if cache miss
        
        return (DateFormat) jFormat.clone();  // For thread safety
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
