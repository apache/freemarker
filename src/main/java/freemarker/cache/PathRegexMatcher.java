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

import java.io.IOException;
import java.util.regex.Pattern;

import freemarker.template.utility.StringUtil;

/**
 * Matches the whole template source name (also known as template source path) with the given regular expression.
 * Note that the template source name is relative to the template storage root defined by the {@link TemplateLoader};
 * it's not the full path of a file on the file system.
 * 
 * @since 2.3.24
 */
public class PathRegexMatcher extends TemplateSourceMatcher {
    
    private final Pattern pattern;
    
    /**
     * @param regex
     *            Glob with the syntax defined by {@link StringUtil#globToRegularExpression(String)}. Must not
     *            start with {@code /}.
     */
    public PathRegexMatcher(String regex) {
        if (regex.startsWith("/")) {
            throw new IllegalArgumentException("Absolute template paths need no inital \"/\"; remove it from: " + regex);
        }
        pattern = Pattern.compile(regex);
    }

    @Override
    public boolean matches(String sourceName, Object templateSource) throws IOException {
        return pattern.matcher(sourceName).matches();
    }

}
