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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core._CoreLogs;
import org.apache.freemarker.core.valueformat.InvalidFormatParametersException;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.slf4j.Logger;

/**
 * Deals with {@link TemplateNumberFormat}-s that wrap a Java {@link NumberFormat}. The parameter string is usually
 * a {@link java.text.DecimalFormat} pattern, with the extensions described in the Manual (see "Extended Jav decimal
 * format"). There are some names that aren't parsed as patterns: "number", "currency", "percent", which
 * corresponds to the predefined formats with similar name in {@link NumberFormat}-s, and also "computer" that
 * behaves like {@code someNumber?c} in templates.
 *
 * <p>Note that the resulting {@link java.text.DecimalFormat}-s are globally cached, and threading issues are
 * addressed by cloning the cached instance before returning it. So it just makes object creation faster, but doesn't
 * eliminate it.
 */
public class JavaTemplateNumberFormatFactory extends TemplateNumberFormatFactory {
    
    public static final JavaTemplateNumberFormatFactory INSTANCE = new JavaTemplateNumberFormatFactory();
    
    private static final Logger LOG = _CoreLogs.RUNTIME;

    private static final ConcurrentHashMap<CacheKey, NumberFormat> GLOBAL_FORMAT_CACHE
            = new ConcurrentHashMap<>();
    private static final int LEAK_ALERT_NUMBER_FORMAT_CACHE_SIZE = 1024;
    
    private JavaTemplateNumberFormatFactory() {
        // Not meant to be instantiated
    }

    @Override
    public TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws InvalidFormatParametersException {
        CacheKey cacheKey = new CacheKey(params, locale);
        NumberFormat jFormat = GLOBAL_FORMAT_CACHE.get(cacheKey);
        if (jFormat == null) {
            if ("number".equals(params)) {
                jFormat = NumberFormat.getNumberInstance(locale);
            } else if ("currency".equals(params)) {
                jFormat = NumberFormat.getCurrencyInstance(locale);
            } else if ("percent".equals(params)) {
                jFormat = NumberFormat.getPercentInstance(locale);
            } else if ("computer".equals(params)) {
                jFormat = env.getCNumberFormat();
            } else {
                try {
                    jFormat = ExtendedDecimalFormatParser.parse(params, locale);
                } catch (ParseException e) {
                    String msg = e.getMessage();
                    throw new InvalidFormatParametersException(
                            msg != null ? msg : "Invalid DecimalFormat pattern", e);
                }
            }

            if (GLOBAL_FORMAT_CACHE.size() >= LEAK_ALERT_NUMBER_FORMAT_CACHE_SIZE) {
                boolean triggered = false;
                synchronized (JavaTemplateNumberFormatFactory.class) {
                    if (GLOBAL_FORMAT_CACHE.size() >= LEAK_ALERT_NUMBER_FORMAT_CACHE_SIZE) {
                        triggered = true;
                        GLOBAL_FORMAT_CACHE.clear();
                    }
                }
                if (triggered) {
                    LOG.warn("Global Java NumberFormat cache has exceeded {} entries => cache flushed. "
                            + "Typical cause: Some template generates high variety of format pattern strings.",
                            LEAK_ALERT_NUMBER_FORMAT_CACHE_SIZE);
                }
            }
            
            NumberFormat prevJFormat = GLOBAL_FORMAT_CACHE.putIfAbsent(cacheKey, jFormat);
            if (prevJFormat != null) {
                jFormat = prevJFormat;
            }
        }  // if cache miss
        
        // JFormat-s aren't thread-safe; must deepClone it
        jFormat = (NumberFormat) jFormat.clone();
        
        return new JavaTemplateNumberFormat(jFormat, params);
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
