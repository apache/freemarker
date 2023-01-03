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
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;

/**
 * {@value #NAME} {@link CFormat}, for outputting XML that follows the conventions of XML Schema.
 *
 * <p>Note that with this, strings formatted with {@code ?c}/{@code ?cn} aren't quoted, or escaped with backslash, since
 * those are meaningless in XML, as far as XML Schema is concerned. They are just printed without change. (Note that
 * XML-escaping is the duty of the auto-escaping facility of FreeMarker, and not of the {@link CFormat}, so that's not
 * done here either.)
 */
public final class XSCFormat extends CFormat {
    public static final String NAME = "XS";
    public static final XSCFormat INSTANCE = new XSCFormat();

    private static final TemplateNumberFormat TEMPLATE_NUMBER_FORMAT = new CTemplateNumberFormat(
            "INF", "-INF", "NaN",
            "INF", "-INF", "NaN");

    private XSCFormat() {
    }

    @Override
    public TemplateNumberFormat getTemplateNumberFormat(Environment env) {
        return TEMPLATE_NUMBER_FORMAT;
    }

    @Override
    public String formatString(String s, Environment env) throws TemplateException {
        return s; // So we don't escape here, as we assume that there's XML auto-escaping
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
        // XSD has no null literal, and you have to leave out the whole element to represent null. We can achieve that
        // here. The closet we can do is causing an empty element or attribute to be outputted. Some frameworks will
        // interpret that as null if the XSD type is not string.
        return "";
    }

    @Override
    public String getName() {
        return NAME;
    }
}
