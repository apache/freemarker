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

import static freemarker.template.utility.StringUtil.*;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQuery;
import java.util.TimeZone;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;
import freemarker.template.utility.TemporalUtils;

// TODO [FREEMARKER-35] These should support parameters similar to {@link ISOTemplateDateFormat},

/**
 * See {@link ISOTemplateTemporalFormatFactory}, and {@link XSTemplateTemporalFormatFactory}.
 *
 * @since 2.3.32
 */
final class ISOLikeTemplateTemporalTemporalFormat extends TemplateTemporalFormat {
    private final DateTimeFormatter dateTimeFormatter;
    private final boolean instantConversion;
    private final ZoneId zoneId;
    private final String description;
    private final TemporalQuery temporalQuery;
    private final Class<? extends Temporal> temporalClass;
    private final DateTimeFormatter parserExtendedDateTimeFormatter;
    private final DateTimeFormatter parserBasicDateTimeFormatter;

    ISOLikeTemplateTemporalTemporalFormat(
            DateTimeFormatter dateTimeFormatter,
            DateTimeFormatter parserExtendedDateTimeFormatter,
            DateTimeFormatter parserBasicDateTimeFormatter,
            Class<? extends Temporal> temporalClass, TimeZone zone, String formatString) {
        this.dateTimeFormatter = dateTimeFormatter;
        this.parserExtendedDateTimeFormatter = parserExtendedDateTimeFormatter;
        this.parserBasicDateTimeFormatter = parserBasicDateTimeFormatter;
        this.temporalQuery = TemporalUtils.getTemporalQuery(temporalClass);
        this.instantConversion = Instant.class.isAssignableFrom(temporalClass);
        this.temporalClass = temporalClass;
        this.zoneId = zone.toZoneId();
        this.description = formatString;
    }

    @Override
    public String formatToPlainText(TemplateTemporalModel tm) throws TemplateValueFormatException,
            TemplateModelException {
        Temporal temporal = TemplateFormatUtil.getNonNullTemporal(tm);

        if (instantConversion) {
            temporal = ((Instant) temporal).atZone(zoneId);
        }

        try {
            return dateTimeFormatter.format(temporal);
        } catch (DateTimeException e) {
            throw new UnformattableValueException(e.getMessage(), e);
        }
    }

    @Override
    public Object parse(String s) throws TemplateValueFormatException {
        DateTimeFormatter parserDateTimeFormatter = parserBasicDateTimeFormatter == null || isExtendedFormatString(s)
                ? parserExtendedDateTimeFormatter : parserBasicDateTimeFormatter;
        try {
            return parserDateTimeFormatter.parse(s, temporalQuery);
        } catch (DateTimeParseException e) {
            throw new UnparsableValueException(
                    "Failed to parse value " + jQuote(s) + " with format " + jQuote(description)
                            + ", and target class " + temporalClass.getSimpleName() + ", "
                            + "zoneId " + jQuote(zoneId) + ".\n"
                            + "(Used this DateTimeFormatter: " + parserDateTimeFormatter + ")\n"
                            + "(Root cause message: " + e.getMessage() + ")",
                    e);
        }
    }

    private boolean isExtendedFormatString(String s) throws UnparsableValueException {
        if (temporalClass == LocalDate.class || temporalClass == YearMonth.class) {
            return !s.isEmpty() && s.indexOf('-', 1) != -1;
        } else if (temporalClass == LocalTime.class || temporalClass == OffsetTime.class) {
            return s.indexOf(":") != -1;
        } else if (temporalClass == Year.class) {
            return false;
        } else {
            int tIndex = s.indexOf('T');
            if (tIndex < 1) {
                throw new UnparsableValueException(
                        "Failed to parse value " + jQuote(s) + " with format " + jQuote(description)
                                + ", and target class " + temporalClass.getSimpleName() + ": "
                                + "Character \"T\" must be used to separate the date and time part.");
            }
            if (s.indexOf(":", tIndex + 1) != -1) {
                return true;
            }
            // Note: false for: -5000101T00, as there the last '-' has index 0
            return s.lastIndexOf('-', tIndex - 1) > 0;
        }
    }

    private boolean temporalClassHasNoTimePart() {
        return temporalClass == LocalDate.class || temporalClass == Year.class || temporalClass == YearMonth.class;
    }

    @Override
    public boolean isLocaleBound() {
        return false;
    }

    @Override
    public boolean isTimeZoneBound() {
        return zoneId != null;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
