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

import java.util.Collection;

import freemarker.template.Configuration;
import freemarker.template.utility.StringUtil;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public final class _CoreStringUtils {

    private _CoreStringUtils() {
        // No meant to be instantiated
    }

    public static String toFTLIdentifierReferenceAfterDot(String name) {
        return backslashEscapeIdentifier(name);
    }

    public static String toFTLTopLevelIdentifierReference(String name) {
        return backslashEscapeIdentifier(name);
    }

    public static String toFTLTopLevelTragetIdentifier(final String name) {
        char quotationType = 0;
        scanForQuotationType: for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (!(i == 0 ? StringUtil.isFTLIdentifierStart(c) : StringUtil.isFTLIdentifierPart(c)) && c != '@') {
                if ((quotationType == 0 || quotationType == '\\')
                        && StringUtil.isBackslashEscapedFTLIdentifierCharacter(c)) {
                    quotationType = '\\';
                } else {
                    quotationType = '"';
                    break scanForQuotationType;
                }
            }
        }
        switch (quotationType) {
        case 0:
            return name;
        case '"':
            return StringUtil.ftlQuote(name);
        case '\\':
            return backslashEscapeIdentifier(name);
        default:
            throw new BugException();
        }
    }

    /*
     * Escapes an identifier. This assumes that the identifier was once accepted by the parser, thus it is properly
     * escapeable. Invalid characters that can't be escaped will be left as is. (This is actually feature because of
     * historically weirdness, like that a sole {@code *} is a valid subvariable name, which must not be escaped.)
     */
    public static String backslashEscapeIdentifier(String name) {
        StringBuilder sb = null;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (StringUtil.isBackslashEscapedFTLIdentifierCharacter(c)) {
                if (sb == null) {
                    sb = new StringBuilder(name.length() + 8);
                    sb.append(name, 0, i);
                }
                sb.append('\\');
            }
            if (sb != null) {
                sb.append(c);
            }
        }
        return sb == null ? name : sb.toString();
    }

    /**
     * @return {@link Configuration#CAMEL_CASE_NAMING_CONVENTION}, or {@link Configuration#LEGACY_NAMING_CONVENTION}
     *         or, {@link Configuration#AUTO_DETECT_NAMING_CONVENTION} when undecidable.
     */
    public static int getIdentifierNamingConvention(String name) {
        final int ln = name.length();
        for (int i = 0; i < ln; i++) {
            final char c = name.charAt(i);
            if (c == '_') {
                return Configuration.LEGACY_NAMING_CONVENTION;
            }
            if (isUpperUSASCII(c)) {
                return Configuration.CAMEL_CASE_NAMING_CONVENTION;
            }
        }
        return Configuration.AUTO_DETECT_NAMING_CONVENTION;
    }
    
    // [2.4] Won't be needed anymore
    /**
     * A deliberately very inflexible camel case to underscored converter; it must not convert improper camel case
     * names to a proper underscored name.
     */
    public static String camelCaseToUnderscored(String camelCaseName) {
        int i = 0;
        while (i < camelCaseName.length() && Character.isLowerCase(camelCaseName.charAt(i))) {
            i++;
        }
        if (i == camelCaseName.length()) {
            // No conversion needed
            return camelCaseName;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(camelCaseName.substring(0, i));
        while (i < camelCaseName.length()) {
            final char c = camelCaseName.charAt(i);
            if (isUpperUSASCII(c)) {
                sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }
    
    public static boolean isUpperUSASCII(char c) {
        return c >= 'A' && c <= 'Z';
    }

    public static String commaSeparatedJQuotedItems(Collection<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(StringUtil.jQuote(item));
        }
        return sb.toString();
    }
}
