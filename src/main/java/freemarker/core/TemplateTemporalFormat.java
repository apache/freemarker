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

import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;
import freemarker.template.utility.TemporalUtil;

public class TemplateTemporalFormat extends TemplateValueFormat {
    private final String format;
    private final Locale locale;
    private final TimeZone timeZone;

    public TemplateTemporalFormat(String format, Locale locale, TimeZone timeZone) {
        this.format = format;
        this.locale = locale;
        this.timeZone = timeZone;
    }

    public String format(TemplateTemporalModel temporalModel) throws TemplateValueFormatException, TemplateModelException {
        return TemporalUtil.format(temporalModel.getAsTemporal(), format, locale, timeZone);
    }

    @Override
    public String getDescription() {
        return format + " " + locale.toString();
    }

    /**
     * Tells if this formatter should be re-created if the locale changes.
     */
    public boolean isLocaleBound() {
        return true;
    }

    /**
     * Tells if this formatter should be re-created if the time zone changes. Currently always {@code true}.
     */
    public boolean isTimeZoneBound() {
        return true;
    }

}
