/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;

class JavaLocalTemplateDateFormatFactory extends LocalTemplateDateFormatFactory {

    private static final Map<DateFormatKey, DateFormat> GLOBAL_FORMAT_CACHE = new HashMap<DateFormatKey, DateFormat>();
    
    private Map<String, TemplateDateFormat>[] formatCache;

    public JavaLocalTemplateDateFormatFactory(Environment env, Locale locale, TimeZone timeZone) {
        super(env, locale, timeZone);
    }

    /**
     * @param zonelessInput
     *            Has no effect in this implementation.
     */
    @Override
    public TemplateDateFormat get(int dateType, boolean zonelessInput, String params)
            throws InvalidFormatParametersException, TemplateModelException,
            UnknownDateTypeFormattingUnsupportedException {
        Map<String, TemplateDateFormat>[] formatCache = this.formatCache;
        if (formatCache == null) {
            formatCache = new Map[4]; // Index 0..3: values of TemplateDateModel's date type constants
            this.formatCache = formatCache; 
        }
        
        Map<String, TemplateDateFormat> formatsForDateType = formatCache[dateType];
        if (formatsForDateType == null) {
            formatsForDateType = new HashMap();
            formatCache[dateType] = formatsForDateType; 
        }

        TemplateDateFormat format = formatsForDateType.get(params);
        if (format == null) {
            format = new JavaTemplateDateFormat(getJavaDateFormat(dateType, params));
            formatsForDateType.put(params, format);
        }
        return format;
    }

    /**
     * Returns a "private" copy (not in the global cache) for the given format.  
     */
    private DateFormat getJavaDateFormat(int dateType, String nameOrPattern)
            throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {

        // Get DateFormat from global cache:
        DateFormatKey cacheKey = new DateFormatKey(
                dateType, nameOrPattern, getLocale(), getTimeZone());
        DateFormat jDateFormat;
        synchronized (GLOBAL_FORMAT_CACHE) {
            jDateFormat = GLOBAL_FORMAT_CACHE.get(cacheKey);
            if (jDateFormat == null) {
                // Add format to global format cache.
                StringTokenizer tok = new StringTokenizer(nameOrPattern, "_");
                int tok1Style = tok.hasMoreTokens() ? parseDateStyleToken(tok.nextToken()) : DateFormat.DEFAULT;
                if (tok1Style != -1) {
                    switch (dateType) {
                        case TemplateDateModel.UNKNOWN: {
                            throw new UnknownDateTypeFormattingUnsupportedException();
                        }
                        case TemplateDateModel.TIME: {
                            jDateFormat = DateFormat.getTimeInstance(tok1Style, cacheKey.locale);
                            break;
                        }
                        case TemplateDateModel.DATE: {
                            jDateFormat = DateFormat.getDateInstance(tok1Style, cacheKey.locale);
                            break;
                        }
                        case TemplateDateModel.DATETIME: {
                            int tok2Style = tok.hasMoreTokens() ? parseDateStyleToken(tok.nextToken()) : tok1Style;
                            if (tok2Style != -1) {
                                jDateFormat = DateFormat.getDateTimeInstance(tok1Style, tok2Style, cacheKey.locale);
                            }
                            break;
                        }
                    }
                }
                if (jDateFormat == null) {
                    try {
                        jDateFormat = new SimpleDateFormat(nameOrPattern, cacheKey.locale);
                    } catch (IllegalArgumentException e) {
                        final String msg = e.getMessage();
                        throw new InvalidFormatParametersException(
                                msg != null ? msg : "Invalid SimpleDateFormat pattern", e);
                    }
                }
                jDateFormat.setTimeZone(cacheKey.timeZone);
                
                GLOBAL_FORMAT_CACHE.put(cacheKey, jDateFormat);
            }  // if cache miss
        }  // sync
        
        return (DateFormat) jDateFormat.clone();  // For thread safety
    }

    private static final class DateFormatKey {
        private final int dateType;
        private final String pattern;
        private final Locale locale;
        private final TimeZone timeZone;

        DateFormatKey(int dateType, String pattern, Locale locale, TimeZone timeZone) {
            this.dateType = dateType;
            this.pattern = pattern;
            this.locale = locale;
            this.timeZone = timeZone;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DateFormatKey) {
                DateFormatKey fk = (DateFormatKey) o;
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

    @Override
    protected void onLocaleChanged() {
        formatCache = null;
    }

    @Override
    protected void onTimeZoneChanged() {
        formatCache = null;
    }
    
}
