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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import freemarker.log.Logger;

/**
 * Deals with {@link TemplateNumberFormat}-s that just wrap a Java {@link NumberFormat}.
 */
class JavaLocalTemplateNumberFormatFactory extends LocalTemplateNumberFormatFactory {
    
    private static final Logger LOG = Logger.getLogger("freemarker.runtime");

    private static final ConcurrentHashMap<NumberFormatKey, NumberFormat> GLOBAL_NUMBER_FORMAT_CACHE
            = new ConcurrentHashMap();
    
    private static final int LEAK_ALERT_NUMBER_FORMAT_CACHE_SIZE = 1024;
    
    JavaLocalTemplateNumberFormatFactory(Environment env) {
        super(env);
    }

    @Override
    public TemplateNumberFormat get(String formatDesc)
            throws InvalidFormatDescriptorException {
        Locale locale = getLocale();
        NumberFormatKey fk = new NumberFormatKey(formatDesc, locale);
        NumberFormat jFormat = GLOBAL_NUMBER_FORMAT_CACHE.get(fk);
        if (jFormat == null) {
            if ("number".equals(formatDesc)) {
                jFormat = NumberFormat.getNumberInstance(locale);
            } else if ("currency".equals(formatDesc)) {
                jFormat = NumberFormat.getCurrencyInstance(locale);
            } else if ("percent".equals(formatDesc)) {
                jFormat = NumberFormat.getPercentInstance(locale);
            } else if ("computer".equals(formatDesc)) {
                jFormat = getEnvironment().getCNumberFormat();
            } else {
                try {
                    jFormat = new DecimalFormat(formatDesc, new DecimalFormatSymbols(locale));
                } catch (IllegalArgumentException e) {
                    String msg = e.getMessage();
                    throw new InvalidFormatDescriptorException(
                            msg != null ? msg : "Invalid DecimalFormat pattern", formatDesc, e);
                }
            }

            if (GLOBAL_NUMBER_FORMAT_CACHE.size() >= LEAK_ALERT_NUMBER_FORMAT_CACHE_SIZE) {
                boolean triggered = false;
                synchronized (JavaLocalTemplateNumberFormatFactory.class) {
                    if (GLOBAL_NUMBER_FORMAT_CACHE.size() >= LEAK_ALERT_NUMBER_FORMAT_CACHE_SIZE) {
                        triggered = true;
                        GLOBAL_NUMBER_FORMAT_CACHE.clear();
                    }
                }
                if (triggered) {
                    LOG.warn("Global Java NumberFormat cache has exceeded " + LEAK_ALERT_NUMBER_FORMAT_CACHE_SIZE
                            + " entries => cache flushed. "
                            + "Typical cause: Some template generates high variety of format pattern strings.");
                }
            }
            
            NumberFormat prevJFormat = GLOBAL_NUMBER_FORMAT_CACHE.putIfAbsent(fk, jFormat);
            if (prevJFormat != null) {
                jFormat = prevJFormat;
            }
        
            // JFormat-s aren't thread-safe; must clone it
            jFormat = (NumberFormat) jFormat.clone();
        }
        return new JavaTemplateNumberFormat(jFormat, formatDesc); 
    }

    private static final class NumberFormatKey {
        private final String pattern;
        private final Locale locale;

        NumberFormatKey(String pattern, Locale locale) {
            this.pattern = pattern;
            this.locale = locale;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NumberFormatKey) {
                NumberFormatKey fk = (NumberFormatKey) o;
                return fk.pattern.equals(pattern) && fk.locale.equals(locale);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return pattern.hashCode() ^ locale.hashCode();
        }
    }

    @Override
    protected void onLocaleChanged() {
        // No op
    }
    
}
