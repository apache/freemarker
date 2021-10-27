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

import static freemarker.core.ISOTemplateTemporalFormatFactory.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Format factory related to {@link someJava8Temporal?string.xs}, {@link someJava8Temporal?string.xs_...}, etc.
 */
class XSTemplateTemporalFormatFactory extends TemplateTemporalFormatFactory {

    static final XSTemplateTemporalFormatFactory INSTANCE = new XSTemplateTemporalFormatFactory();

    private XSTemplateTemporalFormatFactory() {
        // Not meant to be called from outside
    }

    @Override
    public TemplateTemporalFormat get(String params, Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone, Environment env) throws
            TemplateValueFormatException {
        if (!params.isEmpty()) {
            // TODO [FREEMARKER-35]
            throw new InvalidFormatParametersException("xs currently doesn't support parameters for Java 8 temporal types");
        }

        return getXSFormatter(temporalClass, timeZone);
    }

    private static ISOLikeTemplateTemporalTemporalFormat getXSFormatter(Class<? extends Temporal> temporalClass, TimeZone timeZone) {
        final DateTimeFormatter dateTimeFormatter;
        final String description;
        if (temporalClass == LocalTime.class || temporalClass == OffsetTime.class) {
            dateTimeFormatter = ISO8601_TIME_FORMAT;
            description = "W3C XML Schema time";
        } else if (temporalClass == Year.class) {
            dateTimeFormatter = ISO8601_YEAR_FORMAT;
            description = "W3C XML Schema year";
        } else if (temporalClass == YearMonth.class) {
            dateTimeFormatter = ISO8601_YEARMONTH_FORMAT;
            description = "W3C XML Schema year-month";
        } else if (temporalClass == LocalDate.class) {
            dateTimeFormatter = ISO8601_DATE_FORMAT;
            description = "W3C XML Schema date";
        } else {
            Class<? extends Temporal> normTemporalClass =
                    _CoreTemporalUtils.normalizeSupportedTemporalClass(temporalClass);
            if (normTemporalClass != temporalClass) {
                return getXSFormatter(normTemporalClass, timeZone);
            } else {
                dateTimeFormatter = ISO8601_DATE_TIME_FORMAT;
                description = "W3C XML Schema date-time";
            }
        }
        return new ISOLikeTemplateTemporalTemporalFormat(dateTimeFormatter, temporalClass, timeZone, description);
    }

}
