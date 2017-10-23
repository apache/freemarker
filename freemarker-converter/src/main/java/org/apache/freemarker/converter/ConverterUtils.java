/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.converter;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public final class ConverterUtils {

    private ConverterUtils() {
        //
    }

    public static boolean isUpperCaseLetter(char c) {
        return Character.isUpperCase(c) && Character.isLetter(c);
    }

    public static String rightTrim(String s) {
        if (s == null) {
            return null;
        }

        int i = s.length() - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) {
            i--;
        }
        return i != -1 ? s.substring(0, i + 1) : "";
    }

    public static boolean isQuotationChar(char q) {
        return q == '\'' || q == '\"';
    }
    
    public static boolean isWhitespaceOnly(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
