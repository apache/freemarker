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
import freemarker.template.utility.StringUtil;
import freemarker.template.utility.StringUtil.JsStringEncCompatibility;
import freemarker.template.utility.StringUtil.JsStringEncQuotation;

/**
 * {@value #NAME} {@link CFormat}; to be used when generating JSON (and not JavaScript), except, in most cases
 * {@link JavaScriptOrJSONCFormat} is recommended over this.
 *
 * <p><b>Experimental class!</b> This class is too new, and might will change over time. Therefore, for now
 * most methods are not exposed outside FreeMarker. The class itself and some members are exposed as they are needed for
 * configuring FreeMarker.
 *
 * @see JavaScriptCFormat
 * @see JavaScriptOrJSONCFormat
 *
 * @since 2.3.32
 */
public final class JSONCFormat extends AbstractJSONLikeFormat {
    public static final String NAME = "JSON";
    public static final JSONCFormat INSTANCE = new JSONCFormat();

    private JSONCFormat() {
    }

    @Override
    String formatString(String s, Environment env) throws TemplateException {
        return StringUtil.jsStringEnc(s, JsStringEncCompatibility.JSON, JsStringEncQuotation.QUOTATION_MARK);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
