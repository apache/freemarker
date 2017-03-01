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
 * Logical "and" operation among the given matchers.
 * 
 * @since 2.3.24
 */
public class AndMatcher extends TemplateSourceMatcher {
    
    private final TemplateSourceMatcher[] matchers;
    
    public AndMatcher(TemplateSourceMatcher... matchers) {
        if (matchers.length == 0) throw new IllegalArgumentException("Need at least 1 matcher, had 0.");
        this.matchers = matchers;
    }

    @Override
    public boolean matches(String sourceName, Object templateSource) throws IOException {
        for (TemplateSourceMatcher matcher : matchers) {
            if (!(matcher.matches(sourceName, templateSource))) return false;
        }
        return true;
    }

}
