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

import static freemarker.core._TemporalUtils.*;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

// TODO [FREEMARKER-35] These should support parameters similar to {@link ISOTemplateDateFormat},

/**
 * See {@link ISOTemplateTemporalFormatFactory}, and {@link XSTemplateTemporalFormatFactory}.
 *
 * @since 2.3.32
 */
final class ISOLikeTemplateTemporalTemporalFormat extends JavaOrISOLikeTemplateTemporalFormat {
    private final DateTimeFormatter dateTimeFormatter;
    private final boolean instantConversion;
    private final String description;
    private final DateTimeFormatter parserExtendedDateTimeFormatter;
    private final DateTimeFormatter parserBasicDateTimeFormatter;

    ISOLikeTemplateTemporalTemporalFormat(
            DateTimeFormatter dateTimeFormatter,
            DateTimeFormatter parserExtendedDateTimeFormatter,
            DateTimeFormatter parserBasicDateTimeFormatter,
            Class<? extends Temporal> temporalClass, TimeZone timeZone, String formatString) {
        super(temporalClass, timeZone);
        temporalClass = normalizeSupportedTemporalClass(temporalClass);
        this.dateTimeFormatter = dateTimeFormatter;
        this.parserExtendedDateTimeFormatter = parserExtendedDateTimeFormatter;
        this.parserBasicDateTimeFormatter = parserBasicDateTimeFormatter;
        this.instantConversion = temporalClass == Instant.class;
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
            throw new UnformattableValueException(
                    "Failed to format temporal " + temporal + ". Reason: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public Object parse(String s, MissingTimeZoneParserPolicy missingTimeZoneParserPolicy) throws TemplateValueFormatException {
        final boolean extendedFormat;
        final boolean add1Day;
        if (temporalClass == LocalDate.class || temporalClass == YearMonth.class) {
            extendedFormat = s.indexOf('-', 1) != -1;
            add1Day = false;
        } else if (temporalClass == LocalTime.class || temporalClass == OffsetTime.class) {
            extendedFormat = s.indexOf(":") != -1;
            add1Day = false;
            // ISO 8601 allows hour 24 if the rest of the time is 0:
            if (isStartOf240000(s, 0)) {
                s = "00" + s.substring(2);
            }
        } else if (temporalClass == Year.class) {
            extendedFormat = false;
            add1Day = false;
        } else {
            int tIndex = s.indexOf('T');
            if (tIndex < 1) {
                throw newUnparsableValueException(
                        s, null,
                        "Character \"T\" must be used to separate the date and time part.", null);
            }
            if (s.indexOf(":", tIndex + 1) != -1) {
                extendedFormat = true;
            } else {
                // Note: false for: -5000101T00, as there the last '-' has index 0
                extendedFormat = s.lastIndexOf('-', tIndex - 1) > 0;
            }
            // ISO 8601 allows hour 24 if the rest of the time is 0:
            if (isStartOf240000(s, tIndex + 1)) {
                s = s.substring(0, tIndex + 1) + "00" + s.substring(tIndex + 3);
                add1Day = true;
            } else {
                add1Day = false;
            }
        }

        Temporal resultTemporal = parse(
                s, missingTimeZoneParserPolicy,
                parserBasicDateTimeFormatter == null || extendedFormat
                        ? parserExtendedDateTimeFormatter : parserBasicDateTimeFormatter);

        if (add1Day) {
            resultTemporal = resultTemporal.plus(1, ChronoUnit.DAYS);
        }
        return resultTemporal;
    }

    private final static Pattern ZERO_TIME_AFTER_HH = Pattern.compile("(?::?+00(?::?+00(?:.?+0+)?)?)?");

    private static boolean isStartOf240000(String s, int from) {
        if (from + 1 >= s.length() || s.charAt(from) != '2' || s.charAt(from + 1) != '4') {
            return false;
        }

        int index = from + 2;

        int indexAfterHH = index;
        // Seek for time zone start or end of string
        while (index < s.length()) {
            char c = s.charAt(index);
            boolean cIsDigit = c >= '0' && c <= '9';
            if (!(cIsDigit || c == ':' || c == '.')) {
                break;
            }
            if (cIsDigit && c != '0') {
                return false;
            }

            index++;
        }

        String timeAfterHH = s.substring(indexAfterHH, index);
        return ZERO_TIME_AFTER_HH.matcher(timeAfterHH).matches();
    }

    @Override
    public boolean canBeUsedForLocale(Locale locale) {
        return true;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
