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

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _CoreLocaleUtils {

    /**
     * Returns a locale that's one less specific, or {@code null} if there's no less specific locale.
     */
    public static Locale getLessSpecificLocale(Locale locale) {
        String country = locale.getCountry();
        if (locale.getVariant().length() != 0) {
            String language = locale.getLanguage();
            return country != null ? new Locale(language, country) : new Locale(language);
        }
        if (country.length() != 0) {
            return new Locale(locale.getLanguage());
        }
        return null;
    }
    
}
