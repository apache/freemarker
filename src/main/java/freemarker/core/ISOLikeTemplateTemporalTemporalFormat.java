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
import static freemarker.template.utility.TemporalUtils.*;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.TimeZone;
import java.util.regex.Pattern;

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
    private final TemporalQuery<? extends Temporal> temporalQuery;
    private final Class<? extends Temporal> temporalClass;
    private final DateTimeFormatter parserExtendedDateTimeFormatter;
    private final DateTimeFormatter parserBasicDateTimeFormatter;
    private final boolean localTemporalClass;

    ISOLikeTemplateTemporalTemporalFormat(
            DateTimeFormatter dateTimeFormatter,
            DateTimeFormatter parserExtendedDateTimeFormatter,
            DateTimeFormatter parserBasicDateTimeFormatter,
            Class<? extends Temporal> temporalClass, TimeZone zone, String formatString) {
        temporalClass = normalizeSupportedTemporalClass(temporalClass);
        this.dateTimeFormatter = dateTimeFormatter;
        this.parserExtendedDateTimeFormatter = parserExtendedDateTimeFormatter;
        this.parserBasicDateTimeFormatter = parserBasicDateTimeFormatter;
        this.temporalQuery = TemporalUtils.getTemporalQuery(temporalClass);
        this.instantConversion = temporalClass == Instant.class;
        this.temporalClass = temporalClass;
        this.localTemporalClass = isLocalTemporalClass(temporalClass);
        this.zoneId = temporalClass == Instant.class ? zone.toZoneId() : null;
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
    public Object parse(String s) throws TemplateValueFormatException {
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
                throw new UnparsableValueException(
                        "Failed to parse value " + jQuote(s) + " with format " + jQuote(description)
                                + ", and target class " + temporalClass.getSimpleName() + ": "
                                + "Character \"T\" must be used to separate the date and time part.");
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

        DateTimeFormatter parserDateTimeFormatter = parserBasicDateTimeFormatter == null || extendedFormat
                ? parserExtendedDateTimeFormatter : parserBasicDateTimeFormatter;
        try {
            TemporalAccessor parseResult = parserDateTimeFormatter.parse(s);
            if (!localTemporalClass && !parseResult.isSupported(ChronoField.OFFSET_SECONDS)) {
                // Unlike for the Java format, for ISO we require the string to contain the offset for a non-local
                // target type. We could use the default time zone, but that's really just guessing, also DST creates
                // ambiguous cases. For the Java formatter we are lenient, as the shared date-time format typically
                // misses the offset, and because we don't want a format-and-then-parse cycle to fail. But in ISO
                // format, the offset is always shown for a non-local temporal.
                throw new UnparsableValueException(
                        "Failed to parse value " + jQuote(s) + " with format " + jQuote(description)
                                + ", and target class " + temporalClass.getSimpleName() + ": "
                                + "The string must contain the time zone offset for this target class. "
                                + "(Defaulting to the current time zone is not allowed for ISO-style formats.)");

            }
            Temporal resultTemporal = parseResult.query(temporalQuery);
            if (add1Day) {
                resultTemporal = resultTemporal.plus(1, ChronoUnit.DAYS);
            }
            return resultTemporal;
        } catch (DateTimeException e) {
            throw new UnparsableValueException(
                    "Failed to parse value " + jQuote(s) + " with format " + jQuote(description)
                            + ", and target class " + temporalClass.getSimpleName() + ", "
                            + "zoneId " + jQuote(zoneId) + ".\n"
                            + "(Used this DateTimeFormatter: " + parserDateTimeFormatter + ")\n"
                            + "(Root cause message: " + e.getMessage() + ")",
                    e);
        }
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

    //!!T
    public static void main(String[] args) {
        for (String original : new String[] {"24", "24:00", "24:00:00", "24:00:00.0"}) {
            for (boolean basic : new boolean[] {false, true}) {
                for (String prefix : new String[] {"", "T"}) {
                    for (String suffix : new String[] {"", "Z", "-01", "+01"}) {
                        String s = prefix + (basic ? original.replace(":", "") : original)+ suffix;

                        int startIndex = s.indexOf("24");
                        if (!isStartOf240000(s, startIndex)) {
                            throw new AssertionError("Couldn't find end of time part in: " + s);
                        }
                    }
                }
            }
        }

        for (String original : new String[] {
                "24:", "24:01", "24:00:01", "24:00:00.1", "24:0", "24:00:x",
                "2401", "240001", "240000.1", "240"}) {
            for (String prefix : new String[] {"", "T"}) {
                for (String suffix : new String[] {"", "Z", "-01", "+01"}) {
                    String s = prefix + original + suffix;

                    int startIndex = s.indexOf("24");
                    if (isStartOf240000(s, startIndex)) {
                        throw new AssertionError("Shouldn't match: " + s);
                    }
                }
            }
        }

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
