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

package org.apache.freemarker.core.valueformat.impl;

import java.util.Locale;
import java.util.TimeZone;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.valueformat.InvalidFormatParametersException;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.UnknownDateTypeFormattingUnsupportedException;

/**
 * Creates {@link TemplateDateFormat}-s that follows the W3C XML Schema date, time and dateTime syntax.
 */
public final class XSTemplateDateFormatFactory extends ISOLikeTemplateDateFormatFactory {
    
    public static final XSTemplateDateFormatFactory INSTANCE = new XSTemplateDateFormatFactory();

    private XSTemplateDateFormatFactory() {
        // Not meant to be instantiated
    }

    @Override
    public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput,
                                  Environment env) throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        // We don't cache these as creating them is cheap (only 10% speedup of ${d?string.xs} with caching)
        return new XSTemplateDateFormat(
                params, 2,
                dateType, zonelessInput,
                timeZone, this, env);
    }

}
