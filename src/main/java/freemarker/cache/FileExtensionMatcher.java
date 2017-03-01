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

/**
 * Matches the file extension; unlike other matchers, by default case <em>insensitive</em>. A name (a path) is
 * considered to have the given extension exactly if it ends with a dot plus the extension. 
 * 
 * @since 2.3.24
 */
public class FileExtensionMatcher extends TemplateSourceMatcher {

    private final String extension;
    private boolean caseInsensitive = true;
    
    /**
     * @param extension
     *            The file extension (without the initial dot). Can't contain there characters:
     *            {@code '/'}, {@code '*'}, {@code '?'}. May contains {@code '.'}, but can't start with it.
     */
    public FileExtensionMatcher(String extension) {
        if (extension.indexOf('/') != -1) {
            throw new IllegalArgumentException("A file extension can't contain \"/\": " + extension);
        }
        if (extension.indexOf('*') != -1) {
            throw new IllegalArgumentException("A file extension can't contain \"*\": " + extension);
        }
        if (extension.indexOf('?') != -1) {
            throw new IllegalArgumentException("A file extension can't contain \"*\": " + extension);
        }
        if (extension.startsWith(".")) {
            throw new IllegalArgumentException("A file extension can't start with \".\": " + extension);
        }
        this.extension = extension;
    }

    @Override
    public boolean matches(String sourceName, Object templateSource) throws IOException {
        int ln = sourceName.length();
        int extLn = extension.length();
        if (ln < extLn + 1 || sourceName.charAt(ln - extLn - 1) != '.') {
            return false;
        }
        
        return sourceName.regionMatches(caseInsensitive, ln - extLn, extension, 0, extLn);
    }
    
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }
    
    /**
     * Sets if the matching will be case insensitive (UNICODE compliant); default is {@code true}.
     */
    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }
    
    /**
     * Fluid API variation of {@link #setCaseInsensitive(boolean)}
     */
    public FileExtensionMatcher caseInsensitive(boolean caseInsensitive) {
        setCaseInsensitive(caseInsensitive);
        return this;
    }
    
}
