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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;

class JavaTemplateDateFormatFactory extends TemplateDateFormatFactory {

    private static final Map JAVA_DATE_FORMATS = new HashMap();
    
    private final Locale locale; 
    
    private Map/*<TemplateDateFormat>*/[] formatCache;

    public JavaTemplateDateFormatFactory(TimeZone timeZone, Locale locale) {
        super(timeZone);
        this.locale = locale;
    }

    public boolean isLocaleBound() {
        return true;
    }

    /**
     * @param zonelessInput Has no effect in this implementation.
     */
    public TemplateDateFormat get(int dateType, boolean zonelessInput, String formatDescriptor)
            throws ParseException, TemplateModelException, UnknownDateTypeFormattingUnsupportedException {
        Map/*<TemplateDateFormat>*/[] formatCache = this.formatCache;
        if(formatCache == null) {
            formatCache = new Map[4]; // Index 0..3: values of TemplateDateModel's date type constants
            this.formatCache = formatCache; 
        }
        
        Map/*<TemplateDateFormat>*/ formatsForDateType = formatCache[dateType];
        if (formatsForDateType == null) {
            formatsForDateType = new HashMap/*<TemplateDateFormat>*/();
            formatCache[dateType] = formatsForDateType; 
        }

        TemplateDateFormat format = (TemplateDateFormat) formatsForDateType.get(formatDescriptor);
        if(format == null) {
            format = new JavaTemplateDateFormat(getJavaDateFormat(dateType, formatDescriptor));
            formatsForDateType.put(formatDescriptor, format);
        }
        return format;
    }

    private DateFormat getJavaDateFormat(int dateType, String nameOrPattern)
            throws UnknownDateTypeFormattingUnsupportedException, ParseException {

        // Get DateFormat from global cache:
        DateFormatKey cacheKey = new DateFormatKey(
                dateType, nameOrPattern, locale, getTimeZone());
        DateFormat jDateFormat;
        synchronized (JAVA_DATE_FORMATS) {
            jDateFormat = (DateFormat) JAVA_DATE_FORMATS.get(cacheKey);
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
                            if(tok2Style != -1) {
                                jDateFormat = DateFormat.getDateTimeInstance(tok1Style, tok2Style, cacheKey.locale);
                            }
                            break;
                        }
                    }
                }
                if (jDateFormat == null) {
                    try {
                        jDateFormat = new SimpleDateFormat(nameOrPattern, cacheKey.locale);
                    } catch(IllegalArgumentException e) {
                        final String msg = e.getMessage();
                        throw new ParseException(msg != null ? msg : "Illegal SimpleDateFormat pattern", 0);
                    }
                }
                jDateFormat.setTimeZone(cacheKey.timeZone);
                
                JAVA_DATE_FORMATS.put(cacheKey, jDateFormat);
            }  // if cache miss
        }  // sync
        
        // Store the value from the global cache in to the local cache:
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

        public boolean equals(Object o) {
            if (o instanceof DateFormatKey) {
                DateFormatKey fk = (DateFormatKey) o;
                return dateType == fk.dateType && fk.pattern.equals(pattern) && fk.locale.equals(locale)
                        && fk.timeZone.equals(timeZone);
            }
            return false;
        }

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
