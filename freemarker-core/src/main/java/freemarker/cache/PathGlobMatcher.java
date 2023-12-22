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
 * Matches the whole template source name (also known as template source path) with the given glob.
 * Note that the template source name is relative to the template storage root defined by the {@link TemplateLoader};
 * it's not the full path of a file on the file system.
 * 
 * <p>This glob implementation recognizes {@code **} (Ant-style directory wildcard) among others. For more details see
 * {@link StringUtil#globToRegularExpression(String, boolean)}.
 * 
 * <p>About the usage of {@code /} (slash):
 * <ul>
 *   <li>You aren't allowed to start the glob with {@code /}, because template names (template paths) never start with
 *       it. 
 *   <li>Future FreeMarker versions (compared to 2.3.24) might will support importing whole directories. Directory paths
 *       in FreeMarker should end with {@code /}. Hence, {@code foo/bar} refers to the file {bar}, while
 *       {@code foo/bar/} refers to the {bar} directory.
 * </ul>
 * 
 * <p>By default the glob is case sensitive, but this can be changed with {@link #setCaseInsensitive(boolean)} (or
 * {@link #caseInsensitive(boolean)}).
 * 
 * @since 2.3.24
 */
public class PathGlobMatcher extends TemplateSourceMatcher {
    
    private final String glob;
    
    private Pattern pattern;
    private boolean caseInsensitive;
    
    /**
     * @param glob
     *            Glob with the syntax defined by {@link StringUtil#globToRegularExpression(String, boolean)}. Must not
     *            start with {@code /}.
     */
    public PathGlobMatcher(String glob) {
        if (glob.startsWith("/")) {
            throw new IllegalArgumentException("Absolute template paths need no inital \"/\"; remove it from: " + glob);
        }
        this.glob = glob;
        buildPattern();
    }

    private void buildPattern() {
        pattern = StringUtil.globToRegularExpression(glob, caseInsensitive);
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
    public PathGlobMatcher caseInsensitive(boolean caseInsensitive) {
        setCaseInsensitive(caseInsensitive);
        return this;
    }

}
