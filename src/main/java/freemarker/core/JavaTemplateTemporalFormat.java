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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;

/**
 * See {@link JavaTemplateTemporalFormatFactory}.
 *
 * @since 2.3.32
 */
class JavaTemplateTemporalFormat extends TemplateTemporalFormat {

    enum PreFormatValueConversion {
        INSTANT_TO_ZONED_DATE_TIME,
        SET_ZONE_FROM_OFFSET,
        CONVERT_TO_CURRENT_ZONE
    }

    static final String SHORT = "short";
    static final String MEDIUM = "medium";
    static final String LONG = "long";
    static final String FULL = "full";
    private static final String ANY_FORMAT_STYLE = "(" + SHORT + "|" + MEDIUM + "|" + LONG + "|" + FULL + ")";
    // Matches format style patterns like "long_medium", "long", and "" (0-length string). It's a legacy from the
    // pre-Temporal code that "" means "medium", and that it's the default of the date/time-related format settings.
    private static final Pattern FORMAT_STYLE_PATTERN = Pattern.compile(
            "(?:" + ANY_FORMAT_STYLE + "(?:_" + ANY_FORMAT_STYLE + ")?)?");

    private final DateTimeFormatter dateTimeFormatter;
    private final ZoneId zoneId;
    private final String formatString;
    private final PreFormatValueConversion preFormatValueConversion;

    JavaTemplateTemporalFormat(String formatString, Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone)
            throws InvalidFormatParametersException {
        temporalClass = _CoreTemporalUtils.normalizeSupportedTemporalClass(temporalClass);

        Matcher formatStylePatternMatcher = FORMAT_STYLE_PATTERN.matcher(formatString);
        final boolean isFormatStyleString = formatStylePatternMatcher.matches();
        FormatStyle timePartFormatStyle;

        DateTimeFormatter dateTimeFormatter;
        if (isFormatStyleString) {
            FormatStyle datePartFormatStyle;
            {
                String group1 = formatStylePatternMatcher.group(1);
                datePartFormatStyle = group1 != null
                        ? FormatStyle.valueOf(group1.toUpperCase(Locale.ROOT))
                        : FormatStyle.MEDIUM;
                String group2 = formatStylePatternMatcher.group(2);
                timePartFormatStyle = group2 != null
                        ? FormatStyle.valueOf(group2.toUpperCase(Locale.ROOT))
                        : datePartFormatStyle;
            }

            if (temporalClass == LocalDateTime.class || temporalClass == ZonedDateTime.class
                    || temporalClass == OffsetDateTime.class || temporalClass == Instant.class) {
                dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(datePartFormatStyle, timePartFormatStyle);
            } else if (temporalClass == LocalTime.class || temporalClass == OffsetTime.class) {
                dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(timePartFormatStyle);
            } else if (temporalClass == LocalDate.class) {
                dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(datePartFormatStyle);
            } else {
                throw new InvalidFormatParametersException(
                        "Format styles (like " + StringUtil.jQuote(formatString) + ") is not supported for "
                        + temporalClass.getName() + " values.");
            }
        } else {
            timePartFormatStyle = null;
            try {
                dateTimeFormatter = DateTimeFormatter.ofPattern(formatString);
            } catch (IllegalArgumentException e) {
                throw new InvalidFormatParametersException(e.getMessage(), e);
            }
        }

        if (isLocalTemporalClass(temporalClass)) {
            this.preFormatValueConversion = null;
        } else {
            PreFormatValueConversion preFormatValueConversion;
            nonLocalFormatAttempt: while (true) {
                if (showsZone(dateTimeFormatter)) {
                    if (temporalClass == Instant.class) {
                        preFormatValueConversion = PreFormatValueConversion.INSTANT_TO_ZONED_DATE_TIME;
                    } else if (isFormatStyleString &&
                            (temporalClass == OffsetDateTime.class || temporalClass == OffsetTime.class)) {
                        preFormatValueConversion = PreFormatValueConversion.SET_ZONE_FROM_OFFSET;
                    } else {
                        preFormatValueConversion = null;
                    }
                } else {
                    if (temporalClass == OffsetTime.class && timeZone.useDaylightTime()) {
                        if (isFormatStyleString) {
                            // To find the closest style that already shows the offset
                            timePartFormatStyle = getMoreVerboseStyle(timePartFormatStyle);
                        }
                        if (timePartFormatStyle == null) {
                            throw new InvalidFormatParametersException(
                                    "The format must show the time offset, as the current FreeMarker time zone, "
                                            + StringUtil.jQuote(timeZone.getID()) +
                                            ", may uses Daylight Saving Time, and thus "
                                            + "it's not possible to convert the value to the local time in that zone, "
                                            + "since we don't know the day.");
                        }
                        dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(timePartFormatStyle);
                        formatString = timePartFormatStyle.name().toLowerCase(Locale.ROOT);
                        continue nonLocalFormatAttempt;
                    } else {
                        preFormatValueConversion = PreFormatValueConversion.CONVERT_TO_CURRENT_ZONE;
                    }
                }
                break nonLocalFormatAttempt;
            };
            this.preFormatValueConversion = preFormatValueConversion;
        }

        this.dateTimeFormatter = dateTimeFormatter.withLocale(locale);
        this.formatString = formatString;
        this.zoneId = timeZone.toZoneId();
    }

    private static FormatStyle getMoreVerboseStyle(FormatStyle style) {
        switch (style) {
            case SHORT:
                return FormatStyle.MEDIUM;
            case MEDIUM:
                return FormatStyle.LONG;
            case LONG:
                return FormatStyle.FULL;
            default:
                return null;
        }
    }

    @Override
    public String formatToPlainText(TemplateTemporalModel tm) throws TemplateValueFormatException, TemplateModelException {
        DateTimeFormatter dateTimeFormatter = this.dateTimeFormatter;
        Temporal temporal = TemplateFormatUtil.getNonNullTemporal(tm);

        if (preFormatValueConversion == PreFormatValueConversion.INSTANT_TO_ZONED_DATE_TIME) {
            temporal = ((Instant) temporal).atZone(zoneId);
        } else if (preFormatValueConversion == PreFormatValueConversion.CONVERT_TO_CURRENT_ZONE) {
            if (temporal instanceof Instant) {
                temporal = ((Instant) temporal).atZone(zoneId);
            } else if (temporal instanceof OffsetDateTime) {
                temporal = ((OffsetDateTime) temporal).atZoneSameInstant(zoneId);
            } else if (temporal instanceof ZonedDateTime) {
                temporal = ((ZonedDateTime) temporal).withZoneSameInstant(zoneId);
            } else if (temporal instanceof OffsetTime) {
                // Because of logic in the constructor, this is only reached if the zone never uses Daylight Saving.
                temporal = ((OffsetTime) temporal).withOffsetSameInstant(zoneId.getRules().getOffset(Instant.EPOCH));
            } else {
                throw new InvalidFormatParametersException(
                        "Don't know how to convert value of type " + temporal.getClass().getName() + " to the current "
                                + "FreeMarker time zone, " + StringUtil.jQuote(zoneId.getId()) + ", which is "
                                + "needed to format with " + StringUtil.jQuote(formatString) + ".");
            }
        } else if (preFormatValueConversion == PreFormatValueConversion.SET_ZONE_FROM_OFFSET) {
            // Formats like "long" want a time zone field, but oddly, they don't treat the zoneOffset as such.
            if (temporal instanceof OffsetDateTime) {
                OffsetDateTime offsetDateTime = (OffsetDateTime) temporal;
                temporal = ZonedDateTime.of(offsetDateTime.toLocalDateTime(), offsetDateTime.getOffset());
            } else if (temporal instanceof OffsetTime) {
                // There's no ZonedTime class, so we must manipulate the format.
                dateTimeFormatter = dateTimeFormatter.withZone(((OffsetTime) temporal).getOffset());
            } else {
                throw new IllegalArgumentException(
                        "Formatter was created for OffsetTime or OffsetDateTime, but value was a "
                                + ClassUtil.getShortClassNameOfObject(temporal));
            }
        }

        try {
            return dateTimeFormatter.format(temporal);
        } catch (DateTimeException e) {
            throw new UnformattableValueException(e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return formatString;
    }

    /**
     * Tells if this formatter should be re-created if the locale changes.
     */
    @Override
    public boolean isLocaleBound() {
        return true;
    }

    /**
     * Tells if this formatter should be re-created if the time zone changes.
     */
    @Override
    public boolean isTimeZoneBound() {
        return true;
    }

    private static final ZonedDateTime SHOWS_ZONE_SAMPLE_TEMPORAL_1 = ZonedDateTime.of(
            LocalDateTime.of(2011, 1, 1, 1, 1), ZoneOffset.ofHours(0));
    private static final ZonedDateTime SHOWS_ZONE_SAMPLE_TEMPORAL_2 = ZonedDateTime.of(
            LocalDateTime.of(2011, 1, 1, 1, 1), ZoneOffset.ofHours(1));

    private boolean showsZone(DateTimeFormatter dateTimeFormatter) {
        return !dateTimeFormatter.format(SHOWS_ZONE_SAMPLE_TEMPORAL_1)
                .equals(dateTimeFormatter.format(SHOWS_ZONE_SAMPLE_TEMPORAL_2));
    }

    private static boolean isLocalTemporalClass(Class<? extends Temporal> normalizedTemporalClass) {
        return normalizedTemporalClass == LocalDateTime.class
                || normalizedTemporalClass == LocalTime.class
                || normalizedTemporalClass == LocalDate.class
                || normalizedTemporalClass == Year.class
                || normalizedTemporalClass == YearMonth.class;
    }

}
