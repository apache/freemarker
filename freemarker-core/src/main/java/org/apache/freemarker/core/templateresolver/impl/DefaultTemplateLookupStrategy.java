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
package org.apache.freemarker.core.templateresolver.impl;

import java.io.IOException;
import java.util.Locale;

import org.apache.freemarker.core.templateresolver.TemplateLookupContext;
import org.apache.freemarker.core.templateresolver.TemplateLookupResult;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;

/**
 * <p>
 * The default lookup strategy of FreeMarker.
 * 
 * <p>
 * Through an example: Assuming localized template lookup is enabled and that a template is requested for the name
 * {@code example.ftl} and {@code Locale("es", "ES", "Traditional_WIN")}, it will try the following template names,
 * in this order: {@code "foo_en_AU_Traditional_WIN.ftl"}, {@code "foo_en_AU_Traditional.ftl"},
 * {@code "foo_en_AU.ftl"}, {@code "foo_en.ftl"}, {@code "foo.ftl"}. It stops at the first variation where it finds
 * a template. (If the template name contains "*" steps, finding the template for the attempted localized variation
 * happens with the template acquisition mechanism.) If localized template lookup is disabled, it won't try to add any
 * locale strings, so it just looks for {@code "foo.ftl"}.
 * 
 * <p>
 * The generation of the localized name variation with the default lookup strategy, happens like this: It removes
 * the file extension (the part starting with the <em>last</em> dot), then appends {@link Locale#toString()} after
 * it, and puts back the extension. Then it starts to remove the parts from the end of the locale, considering
 * {@code "_"} as the separator between the parts. It won't remove parts that are not part of the locale string
 * (like if the requested template name is {@code foo_bar.ftl}, it won't remove the {@code "_bar"}).
 */
public class DefaultTemplateLookupStrategy extends TemplateLookupStrategy {
    
    public static final DefaultTemplateLookupStrategy INSTANCE = new DefaultTemplateLookupStrategy();
    
    private DefaultTemplateLookupStrategy() {
        //
    }
    
    @Override
    public  <R extends TemplateLookupResult> R lookup(TemplateLookupContext<R> ctx) throws IOException {
        return ctx.lookupWithLocalizedThenAcquisitionStrategy(ctx.getTemplateName(), ctx.getTemplateLocale());
    }
    
}