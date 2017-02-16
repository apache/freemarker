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

import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.templateresolver.impl.TemplateLoaderBasedTemplateLookupResult;

/**
 * The return value of {@link TemplateLookupStrategy#lookup(TemplateLookupContext)} and similar lookup methods. You
 * usually get one from {@link TemplateLookupContext#lookupWithAcquisitionStrategy(String)} or
 * {@link TemplateLookupContext#createNegativeLookupResult()}.
 * 
 * <p>
 * Subclass this only if you are implementing a {@link TemplateLookupContext}; if the {@link TemplateLookupContext} that
 * you are implementing uses {@link TemplateLoader}-s, consider using {@link TemplateLoaderBasedTemplateLookupResult}
 * instead of writing your own subclass.
 * 
 * @since 2.3.22
 */
public abstract class TemplateLookupResult {

    protected TemplateLookupResult() {
        // nop
    }
    
    /**
     * The source name of the template found (see {@link Template#getSourceName()}), or {@code null} if
     * {@link #isPositive()} is {@code false}.
     */
    public abstract String getTemplateSourceName();

    /**
     * Tells if the lookup has found a matching template.
     */
    public abstract boolean isPositive();
    
}
