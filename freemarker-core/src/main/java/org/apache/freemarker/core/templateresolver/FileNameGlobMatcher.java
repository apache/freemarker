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
package org.apache.freemarker.core.templateresolver;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.freemarker.core.util._StringUtils;

/**
 * As opposed to {@link PathGlobMatcher}, it only compares the "file name" part (the part after the last {@code /}) of
 * the source name with the given glob. For example, the file name glob {@code *.f3ah} matches both {@code foo.f3ah} and
 * {@code foo/bar.f3ah}. With other words, that file name glob is equivalent with the {@code **}{@code /*.f3ah})
 * <em>path</em> glob ( {@link PathGlobMatcher}).
 */
public class FileNameGlobMatcher extends TemplateSourceMatcher {

    private final String glob;
    
    private Pattern pattern;
    private boolean caseInsensitive;
    
    /**
     * @param glob
     *            Glob with the syntax defined by {@link _StringUtils#globToRegularExpression(String, boolean)}. Must not
     *            start with {@code /}.
     */
    public FileNameGlobMatcher(String glob) {
        if (glob.indexOf('/') != -1) {
            throw new IllegalArgumentException("A file name glob can't contain \"/\": " + glob);
        }
        this.glob = glob;
        buildPattern();
    }

    private void buildPattern() {
        pattern = _StringUtils.globToRegularExpression("**/" + glob, caseInsensitive);
    }

    @Override
    public boolean matches(String sourceName, Object templateSource) throws IOException {
        return pattern.matcher(sourceName).matches();
    }
    
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }
    
    /**
     * Sets if the matching will be case insensitive (UNICODE compliant); default is {@code false}.
     */
    public void setCaseInsensitive(boolean caseInsensitive) {
        boolean lastCaseInsensitive = this.caseInsensitive;
        this.caseInsensitive = caseInsensitive;
        if (lastCaseInsensitive != caseInsensitive) {
            buildPattern();
        }
    }
    
    /**
     * Fluid API variation of {@link #setCaseInsensitive(boolean)}
     */
    public FileNameGlobMatcher caseInsensitive(boolean caseInsensitive) {
        setCaseInsensitive(caseInsensitive);
        return this;
    }
    
}
