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

import freemarker.template.TemplateException;

abstract class Interpolation extends TemplateElement {

    protected abstract String dump(boolean canonical, boolean inStringLiteral);

    @Override
    protected final String dump(boolean canonical) {
        return dump(canonical, false);
    }
    
    final String getCanonicalFormInStringLiteral() {
        return dump(true, true);
    }

    /**
     * Returns the already type-converted value that this interpolation will insert into the output.
     * 
     * @return A {@link String} or {@link TemplateMarkupOutputModel}. Not {@code null}.
     */
    protected abstract Object calculateInterpolatedStringOrMarkup(Environment env) throws TemplateException;

    @Override
    boolean isShownInStackTrace() {
        return true;
    }
    
}
