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

package freemarker.cache;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all! This class is to
 * work around the lack of module system in Java, i.e., so that other FreeMarker packages can access things inside this
 * package that users shouldn't.
 */
public class _TemplatePathUtils {
    public static final String BACKING_OUT_FROM_ROOT_NOT_ALLOWED_MESSAGE =
            "Backing out from the template root directory is not allowed";

    /**
     * Checks if the path doesn't back out from the directory it starts from. We recognize {@code "."} and {@code ".."}
     * as the reserved navigating names. Both {@code '/'} and {@code '\'} are assumed to be name separators. The path is
     * allowed to be both absolute or relative. If it's absolute, we will still block backing out from the root (like
     * {@code "/.."}). The repeated occurence of the name separator counts as a single name separator, following the
     * logic of most shells (i.e. {@code "foo/bar"} is the same as {@code "foo///bar"})
     *
     * @param name
     *         The relative template path to check. Maybe empty, but can't be {@code null}.
     */
    public static boolean isInsideBaseDir(String name) {
        int len = name.length();
        // Trim WS from the end, as some system has bugs where it's lost by the time the OS interprets the path:
        while (len > 0 && Character.isWhitespace(name.charAt(len - 1))) {
            len--;
        }

        int level = 0;
        int lastNameStart = 0;
        for (int pos = 0; pos < len + 1; pos++) {
            if (pos == len || isPossiblyNameEnd(name.charAt(pos))) {
                int nameLength = pos - lastNameStart;
                if (nameLength == 2
                        && name.charAt(lastNameStart) == '.' && name.charAt(lastNameStart + 1) == '.') {
                    level--;
                    if (level < 0) {
                        return false;
                    }
                } else if (!(nameLength == 0 || nameLength == 1 && name.charAt(lastNameStart) == '.')) {
                    level++;
                }
                lastNameStart = pos + 1;
            }
        }
        return true;
    }

    private static boolean isPossiblyNameEnd(char c) {
        return c == '\\' || c == '/';
    }
}
