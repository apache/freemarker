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
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;

/**
 * {@value #NAME} {@link CFormat}.
 */
public final class JavaCFormat extends CFormat {
    public static final String NAME = "Java";
    public static final JavaCFormat INSTANCE = new JavaCFormat();

    private static final TemplateNumberFormat TEMPLATE_NUMBER_FORMAT = new CTemplateNumberFormat(
            "Double.POSITIVE_INFINITY", "Double.NEGATIVE_INFINITY", "Double.NaN",
            "Float.POSITIVE_INFINITY", "Float.NEGATIVE_INFINITY", "Float.NaN");

    private JavaCFormat() {
    }

    @Override
    public TemplateNumberFormat getTemplateNumberFormat(Environment env) {
        return TEMPLATE_NUMBER_FORMAT;
    }

    @Override
    public String formatString(String s, Environment env) throws TemplateException {
        return _StringUtils.javaStringEnc(s, true);
    }

    @Override
    public String getTrueString() {
        return "true";
    }

    @Override
    public String getFalseString() {
        return "false";
    }

    @Override
    public String getNullString() {
        return "null";
    }

    @Override
    public String getName() {
        return NAME;
    }
}
