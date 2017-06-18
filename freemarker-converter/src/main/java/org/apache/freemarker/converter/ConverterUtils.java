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

public final class ConverterUtils {

    private ConverterUtils() {
        //
    }

    public static String snakeCaseToCamelCase(String s) {
        if (s == null) {
            return null;
        }

        int wordEndIdx = s.indexOf('_');
        if (wordEndIdx == -1) {
            return s.toLowerCase();
        }

        StringBuilder sb = new StringBuilder(s.length());
        int wordStartIdx = 0;
        do {
            if (wordStartIdx < wordEndIdx) {
                char wordStartC = s.charAt(wordStartIdx);
                sb.append(sb.length() != 0 ? Character.toUpperCase(wordStartC) : Character.toLowerCase(wordStartC));
                sb.append(s.substring(wordStartIdx + 1, wordEndIdx).toLowerCase());
            }

            wordStartIdx = wordEndIdx + 1;
            wordEndIdx = s.indexOf('_', wordStartIdx);
            if (wordEndIdx == -1) {
                wordEndIdx = s.length();
            }
        } while (wordStartIdx < s.length());
        return sb.toString();
    }
}
