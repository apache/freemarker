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

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.valueformat.InvalidFormatParametersException;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.UnknownDateTypeFormattingUnsupportedException;

import java.util.Locale;
import java.util.TimeZone;

/**
 * Creates {@link TemplateDateFormat}-s that follows ISO 8601 extended format that is also compatible with the XML
 * Schema format (as far as you don't have dates in the BC era). Examples of possible outputs: {@code
 * "2005-11-27T15:30:00+02:00"}, {@code "2005-11-27"}, {@code "15:30:00Z"}. Note the {@code ":00"} in the time zone
 * offset; this is not required by ISO 8601, but included for compatibility with the XML Schema format. Regarding the
 * B.C. issue, those dates will be one year off when read back according the XML Schema format, because of a mismatch
 * between that format and ISO 8601:2000 Second Edition.
 */
public final class ISOTemplateDateFormatFactory extends ISOLikeTemplateDateFormatFactory {
    
    public static final ISOTemplateDateFormatFactory INSTANCE = new ISOTemplateDateFormatFactory();

    private ISOTemplateDateFormatFactory() {
        // Not meant to be instantiated
    }

    @Override
    public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput,
                                  Environment env) throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        // We don't cache these as creating them is cheap (only 10% speedup of ${d?string.xs} with caching)
        return new ISOTemplateDateFormat(
                params, 3,
                dateType, zonelessInput,
                timeZone, this, env);
    }

}
