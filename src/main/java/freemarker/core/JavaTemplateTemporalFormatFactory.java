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

import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

class JavaTemplateTemporalFormatFactory extends TemplateTemporalFormatFactory {
    public static final JavaTemplateTemporalFormatFactory INSTANCE = new JavaTemplateTemporalFormatFactory();

    private final FastLRUKeyValueStore<CacheKey, JavaTemplateTemporalFormat> formatCache =
            new FastLRUKeyValueStore<>(512);

    private JavaTemplateTemporalFormatFactory() {
        // Not instantiated from outside
    }

    @Override
    public TemplateTemporalFormat get(
            String params, Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone, Environment env)
            throws TemplateValueFormatException {
        CacheKey cacheKey = new CacheKey(params, temporalClass, locale, timeZone);
        JavaTemplateTemporalFormat format = formatCache.get(cacheKey);
        if (format == null) {
            format = new JavaTemplateTemporalFormat(params, temporalClass, locale, timeZone);
            format = formatCache.putIfAbsentThenReturnStored(cacheKey, format);
        }
        // JavaTemplateTemporalFormat-s use a java.time.format.DateTimeFormatter internally, and so are thread safe,
        // and immutable, so we don't have to clone() anything (unlike in JavaTemplateDateFormatFactory):
        return format;
    }

    void clear() {
        formatCache.clear();
    }

    private static class CacheKey {
        private final String params;
        private final Class<? extends Temporal> temporalClass;
        private final Locale locale;
        private final TimeZone timeZone;

        public CacheKey(String params, Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone) {
            this.params = params;
            this.temporalClass = temporalClass;
            this.locale = locale;
            this.timeZone = timeZone;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return params.equals(cacheKey.params) && temporalClass.equals(cacheKey.temporalClass) &&
                    locale.equals(cacheKey.locale) && timeZone.equals(cacheKey.timeZone);
        }

        @Override
        public int hashCode() {
            return Objects.hash(params, temporalClass, locale, timeZone);
        }
    }
}
