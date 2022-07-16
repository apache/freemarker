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

import static java.time.temporal.ChronoField.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Format factory related to {@code someJava8Temporal?string.iso}, {@code someJava8Temporal?string.iso_...}, etc.
 */
class ISOTemplateTemporalFormatFactory extends TemplateTemporalFormatFactory {

    static final ISOTemplateTemporalFormatFactory INSTANCE = new ISOTemplateTemporalFormatFactory();

    private ISOTemplateTemporalFormatFactory() {
        // Not meant to be called from outside
    }

    static final DateTimeFormatter ISO8601_DATE_FORMAT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .toFormatter(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    static final DateTimeFormatter ISO8601_DATE_TIME_FORMAT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(":")
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(":")
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalStart()
            .appendOffset("+HH:MM", "Z")
            .optionalEnd()
            .toFormatter(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    static final DateTimeFormatter ISO8601_TIME_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(":")
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(":")
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalStart()
            .appendOffset("+HH:MM", "Z")
            .optionalEnd()
            .toFormatter(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    static final DateTimeFormatter ISO8601_YEAR_MONTH_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR)
            .appendLiteral("-")
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .toFormatter(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    static final DateTimeFormatter ISO8601_YEAR_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR)
            .toFormatter()
            .withLocale(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    static final DateTimeFormatter PARSER_ISO8601_EXTENDED_DATE_TIME_FORMAT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .optionalStart()
            .appendLiteral(":")
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(":")
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .optionalEnd()
            .optionalStart()
            .appendOffset("+HH:mm", "Z")
            .optionalEnd()
            .toFormatter(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    static final DateTimeFormatter PARSER_ISO8601_BASIC_DATE_TIME_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendValue(MONTH_OF_YEAR, 2)
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .optionalStart()
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .optionalEnd()
            .optionalStart()
            .appendOffset("+HHmm", "Z")
            .optionalEnd()
            .toFormatter(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    static final DateTimeFormatter PARSER_ISO8601_EXTENDED_DATE_FORMAT = ISO8601_DATE_FORMAT;

    static final DateTimeFormatter PARSER_ISO8601_BASIC_DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendValue(MONTH_OF_YEAR, 2)
            .appendValue(DAY_OF_MONTH, 2)
            .toFormatter(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    static final DateTimeFormatter PARSER_ISO8601_EXTENDED_TIME_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .optionalStart()
            .appendLiteral(":")
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(":")
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .optionalEnd()
            .optionalStart()
            .appendOffset("+HH:mm", "Z")
            .optionalEnd()
            .toFormatter(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    static final DateTimeFormatter PARSER_ISO8601_BASIC_TIME_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .optionalStart()
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .optionalEnd()
            .optionalStart()
            .appendOffset("+HHmm", "Z")
            .optionalEnd()
            .toFormatter(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    static final DateTimeFormatter PARSER_ISO8601_EXTENDED_YEAR_MONTH_FORMAT = ISO8601_YEAR_MONTH_FORMAT;
    static final DateTimeFormatter PARSER_ISO8601_BASIC_YEAR_MONTH_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR)
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .toFormatter(Locale.ROOT)
            .withChronology(IsoChronology.INSTANCE)
            .withResolverStyle(ResolverStyle.STRICT);

    @Override
    public TemplateTemporalFormat get(
            String params, Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone, Environment env)
            throws TemplateValueFormatException {
        if (!params.isEmpty()) {
            // TODO [FREEMARKER-35]
            throw new InvalidFormatParametersException("iso currently doesn't support parameters for Java 8 temporal types");
        }

        return getISOFormatter(temporalClass, timeZone);
    }

    private static ISOLikeTemplateTemporalTemporalFormat getISOFormatter(
            Class<? extends Temporal> temporalClass, TimeZone timeZone) {
        final DateTimeFormatter dateTimeFormatter;
        final DateTimeFormatter parserExtendedDateTimeFormatter;
        final DateTimeFormatter parserBasicDateTimeFormatter;
        final String description;
        temporalClass = _TemporalUtils.normalizeSupportedTemporalClass(temporalClass);
        if (temporalClass == LocalTime.class || temporalClass == OffsetTime.class) {
            dateTimeFormatter = ISO8601_TIME_FORMAT;
            parserExtendedDateTimeFormatter = PARSER_ISO8601_EXTENDED_TIME_FORMAT;
            parserBasicDateTimeFormatter = PARSER_ISO8601_BASIC_TIME_FORMAT;
            description = "ISO 8601 (subset) time";
        } else if (temporalClass == Year.class) {
            dateTimeFormatter = ISO8601_YEAR_FORMAT;
            parserExtendedDateTimeFormatter = ISO8601_YEAR_FORMAT;
            parserBasicDateTimeFormatter = null;
            description = "ISO 8601 (subset) year";
        } else if (temporalClass == YearMonth.class) {
            dateTimeFormatter = ISO8601_YEAR_MONTH_FORMAT;
            parserExtendedDateTimeFormatter = PARSER_ISO8601_EXTENDED_YEAR_MONTH_FORMAT;
            parserBasicDateTimeFormatter = PARSER_ISO8601_BASIC_YEAR_MONTH_FORMAT;
            description = "ISO 8601 (subset) year-month";
        } else if (temporalClass == LocalDate.class) {
            dateTimeFormatter = ISO8601_DATE_FORMAT;
            parserExtendedDateTimeFormatter = PARSER_ISO8601_EXTENDED_DATE_FORMAT;
            parserBasicDateTimeFormatter = PARSER_ISO8601_BASIC_DATE_FORMAT;
            description = "ISO 8601 (subset) date";
        } else if (temporalClass == LocalDateTime.class || temporalClass == OffsetDateTime.class
                || temporalClass == ZonedDateTime.class || temporalClass == Instant.class) {
            dateTimeFormatter = ISO8601_DATE_TIME_FORMAT;
            parserExtendedDateTimeFormatter = PARSER_ISO8601_EXTENDED_DATE_TIME_FORMAT;
            parserBasicDateTimeFormatter = PARSER_ISO8601_BASIC_DATE_TIME_FORMAT;
            description = "ISO 8601 (subset) date-time";
        } else {
            throw new BugException();
        }
        return new ISOLikeTemplateTemporalTemporalFormat(
                dateTimeFormatter,
                parserExtendedDateTimeFormatter,
                parserBasicDateTimeFormatter,
                temporalClass, timeZone, description);
    }

}
