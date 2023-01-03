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

package org.apache.freemarker.core.cformat.impl;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.cformat.CFormat;
import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.core.util._StringUtils.JsStringEncCompatibility;
import org.apache.freemarker.core.util._StringUtils.JsStringEncQuotation;

/**
 * {@value #NAME} {@link CFormat}; to be used when generating JSON (and not JavaScript), except, in most cases
 * {@link JavaScriptOrJSONCFormat} is recommended over this.
 *
 * @see JavaScriptCFormat
 * @see JavaScriptOrJSONCFormat
 */
public final class JSONCFormat extends AbstractJSONLikeFormat {
    public static final String NAME = "JSON";
    public static final JSONCFormat INSTANCE = new JSONCFormat();

    private JSONCFormat() {
    }

    @Override
    public String formatString(String s, Environment env) throws TemplateException {
        return _StringUtils.jsStringEnc(s, JsStringEncCompatibility.JSON, JsStringEncQuotation.QUOTATION_MARK);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
