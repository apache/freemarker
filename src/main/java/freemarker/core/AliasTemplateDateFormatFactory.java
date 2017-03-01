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

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import freemarker.template.utility.StringUtil;

/**
 * Creates an alias to another format, so that the format can be referred to with a simple name in the template, rather
 * than as a concrete pattern or other kind of format string.
 * 
 * @since 2.3.24
 */
public final class AliasTemplateDateFormatFactory extends TemplateDateFormatFactory {

    private final String defaultTargetFormatString;
    private final Map<Locale, String> localizedTargetFormatStrings;

    /**
     * @param targetFormatString
     *            The format string this format will be an alias to.
     */
    public AliasTemplateDateFormatFactory(String targetFormatString) {
        this.defaultTargetFormatString = targetFormatString;
        localizedTargetFormatStrings = null;
    }

    /**
     * @param defaultTargetFormatString
     *            The format string this format will be an alias to if there's no locale-specific format string for the
     *            requested locale in {@code localizedTargetFormatStrings}
     * @param localizedTargetFormatStrings
     *            Maps {@link Locale}-s to format strings. If the desired locale doesn't occur in the map, a less
     *            specific locale is tried, repeatedly until only the language part remains. For example, if locale is
     *            {@code new Locale("en", "US", "Linux")}, then these keys will be attempted untol a match is found, in
     *            this order: {@code new Locale("en", "US", "Linux")}, {@code new Locale("en", "US")},
     *            {@code new Locale("en")}. If there's still no matching key, the value of the
     *            {@code targetFormatString} will be used.
     */
    public AliasTemplateDateFormatFactory(
            String defaultTargetFormatString, Map<Locale, String> localizedTargetFormatStrings) {
        this.defaultTargetFormatString = defaultTargetFormatString;
        this.localizedTargetFormatStrings = localizedTargetFormatStrings;
    }
    
    @Override
    public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput,
            Environment env) throws TemplateValueFormatException {
        TemplateFormatUtil.checkHasNoParameters(params);
        try {
            String targetFormatString;
            if (localizedTargetFormatStrings != null) {
                Locale lookupLocale = locale;
                targetFormatString = localizedTargetFormatStrings.get(lookupLocale);
                while (targetFormatString == null
                        && (lookupLocale = _CoreLocaleUtils.getLessSpecificLocale(lookupLocale)) != null) {
                    targetFormatString = localizedTargetFormatStrings.get(lookupLocale);
                }
            } else {
                targetFormatString = null;
            }
            if (targetFormatString == null) {
                targetFormatString = this.defaultTargetFormatString;
            }
            return env.getTemplateDateFormat(targetFormatString, dateType, locale, timeZone, zonelessInput);
        } catch (TemplateValueFormatException e) {
            throw new AliasTargetTemplateValueFormatException("Failed to create format based on target format string,  "
                    + StringUtil.jQuote(params) + ". Reason given: " + e.getMessage(), e);
        }
    }

}
