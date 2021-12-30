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
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.DateUtil;

/**
 * See {@link JavaTemplateTemporalFormatFactory}.
 *
 * @since 2.3.32
 */
class JavaTemplateTemporalFormat extends TemplateTemporalFormat {

    enum PreFormatValueConversion {
        INSTANT_TO_ZONED_DATE_TIME,
        SET_ZONE_FROM_OFFSET,
        OFFSET_TIME_WITHOUT_OFFSET_ON_THE_FORMAT_EXCEPTION
    }

    enum SpecialParsing {
        OFFSET_TIME_DST_ERROR
    }

    static final String SHORT = "short";
    static final String MEDIUM = "medium";
    static final String LONG = "long";
    static final String FULL = "full";

    private static final LocalDateTime LOCAL_DATE_TIME_SAMPLE
            = LocalDateTime.of(2020, 12, 1, 1, 2, 3);

    private static final String ANY_FORMAT_STYLE = "(" + SHORT + "|" + MEDIUM + "|" + LONG + "|" + FULL + ")";
    // Matches format style patterns like "long_medium", "long", and "" (0-length string). It's a legacy from the
    // pre-Temporal code that "" means "medium", and that it's the default of the date/time-related format settings.
    private static final Pattern FORMAT_STYLE_PATTERN = Pattern.compile(
            "(?:" + ANY_FORMAT_STYLE + "(?:_" + ANY_FORMAT_STYLE + ")?)?");

    private static final Map<Class<? extends Temporal>, TemporalQuery<? extends Temporal>> TEMPORAL_CLASS_TO_QUERY_MAP;
    static {
        TEMPORAL_CLASS_TO_QUERY_MAP = new IdentityHashMap<>();
        TEMPORAL_CLASS_TO_QUERY_MAP.put(Instant.class, Instant::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(LocalDate.class, LocalDate::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(LocalDateTime.class, LocalDateTime::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(LocalTime.class, LocalTime::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(OffsetDateTime.class, JavaTemplateTemporalFormat::offsetDateTimeFrom);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(OffsetTime.class, OffsetTime::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(ZonedDateTime.class, ZonedDateTime::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(Year.class, Year::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(YearMonth.class, YearMonth::from);
    }

    private final DateTimeFormatter dateTimeFormatter;
    private final TemporalQuery<? extends Temporal> temporalQuery;
    private final ZoneId zoneId;
    private final String formatString;
    private final Class<? extends Temporal> temporalClass;
    private final PreFormatValueConversion preFormatValueConversion;
    private final SpecialParsing specialParsing;

    JavaTemplateTemporalFormat(String formatString, Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone)
            throws InvalidFormatParametersException {
        this.temporalClass = _CoreTemporalUtils.normalizeSupportedTemporalClass(temporalClass);

        temporalQuery = Objects.requireNonNull(TEMPORAL_CLASS_TO_QUERY_MAP.get(temporalClass));

        final Matcher formatStylePatternMatcher = FORMAT_STYLE_PATTERN.matcher(formatString);
        final boolean isFormatStyleString = formatStylePatternMatcher.matches();
        FormatStyle timePartFormatStyle; // Maybe changes later for re-attempts
        final FormatStyle datePartFormatStyle;
        boolean formatWithZone;

        DateTimeFormatter dateTimeFormatter; // Maybe changes later for re-attempts
        if (isFormatStyleString) {
            // Set datePartFormatStyle, and timePartFormatStyle; both will be non-null
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
                        "Format styles (like " + jQuote(formatString) + ") is not supported for "
                        + temporalClass.getName() + " values.");
            }
        } else {
            datePartFormatStyle = null;
            timePartFormatStyle = null;

            try {
                dateTimeFormatter = DateUtil.dateTimeFormatterFromSimpleDateFormatPattern(formatString, locale);
            } catch (IllegalArgumentException e) {
                throw new InvalidFormatParametersException(e.getMessage(), e);
            }
        }

        // Handling of time zone related edge cases
        if (isLocalTemporalClass(temporalClass)) {
            this.preFormatValueConversion = null;
            this.specialParsing = null;
            formatWithZone = false;
            if (isFormatStyleString && (temporalClass == LocalTime.class || temporalClass == LocalDateTime.class)) {
                // The localized pattern possibly contains the time zone (for most locales, LONG and FULL does), so they
                // fail with local temporals that have a time part. To work this issue around, we decrease the verbosity
                // of the time style until formatting succeeds. (See also: JDK-8085887)
                localFormatAttempt: while (true) {
                    try {
                        dateTimeFormatter.format(LOCAL_DATE_TIME_SAMPLE); // We only care if it throws exception or not
                        break localFormatAttempt; // It worked
                    } catch (DateTimeException e) {
                        timePartFormatStyle = getLessVerboseStyle(timePartFormatStyle);
                        if (timePartFormatStyle == null) {
                            throw e;
                        }

                        String timePartFormatString = timePartFormatStyle.name().toLowerCase(Locale.ROOT);
                        if (temporalClass == LocalDateTime.class) {
                            dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
                                    datePartFormatStyle, timePartFormatStyle);
                            formatString = datePartFormatStyle == timePartFormatStyle
                                    ? timePartFormatString
                                    : datePartFormatStyle.name().toLowerCase(Locale.ROOT) + "_" + timePartFormatString;
                        } else {
                            dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(timePartFormatStyle);
                            formatString = timePartFormatString;
                        }
                    }
                }
            }
        } else { // is non-local temporal
            PreFormatValueConversion preFormatValueConversion;
            SpecialParsing specialParsing;
            if (showsZone(dateTimeFormatter)) {
                if (temporalClass == Instant.class) {
                    preFormatValueConversion = PreFormatValueConversion.INSTANT_TO_ZONED_DATE_TIME;
                    specialParsing = null;
                } else if (isFormatStyleString &&
                        (temporalClass == OffsetDateTime.class || temporalClass == OffsetTime.class)) {
                    preFormatValueConversion = PreFormatValueConversion.SET_ZONE_FROM_OFFSET;
                    specialParsing = null;
                } else {
                    preFormatValueConversion = null;
                    specialParsing = null;
                }
                formatWithZone = false;
            } else { // Doesn't show zone
                if (temporalClass == OffsetTime.class) {
                    // We give up, but delay the exception until the format is actually used, just in case
                    // format creation is triggered without actually using it.
                    preFormatValueConversion = PreFormatValueConversion.OFFSET_TIME_WITHOUT_OFFSET_ON_THE_FORMAT_EXCEPTION;
                    specialParsing = SpecialParsing.OFFSET_TIME_DST_ERROR;
                    formatWithZone = false;
                } else {
                    // As no zone is shown, but our temporal class is not local, we tell the formatter convert to
                    // the current time zone. Also, when parsing, that same time zone will be assumed.
                    preFormatValueConversion = null;
                    specialParsing = null;
                    formatWithZone = true;
                }
            }
            this.preFormatValueConversion = preFormatValueConversion;
            this.specialParsing = specialParsing;
        }

        dateTimeFormatter = dateTimeFormatter.withLocale(locale);
        if (formatWithZone) {
            dateTimeFormatter = dateTimeFormatter.withZone(timeZone.toZoneId());
        }
        this.dateTimeFormatter = dateTimeFormatter;
        this.formatString = formatString;
        this.zoneId = timeZone.toZoneId();
    }

    @Override
    public String formatToPlainText(TemplateTemporalModel tm) throws TemplateValueFormatException, TemplateModelException {
        DateTimeFormatter dateTimeFormatter = this.dateTimeFormatter;
        Temporal temporal = TemplateFormatUtil.getNonNullTemporal(tm);

        if (preFormatValueConversion != null) {
            switch (preFormatValueConversion) {
                case INSTANT_TO_ZONED_DATE_TIME:
                    // Typical date-time formats will fail with "UnsupportedTemporalTypeException: Unsupported field:
                    // YearOfEra" if we leave the value as Instant. (But parse(String, Instant::from) has no similar
                    // issue.)
                    temporal = ((Instant) temporal).atZone(zoneId);
                    break;
                case SET_ZONE_FROM_OFFSET:
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
                    break;
                case OFFSET_TIME_WITHOUT_OFFSET_ON_THE_FORMAT_EXCEPTION:
                    throw newOffsetTimeWithoutOffsetOnTheFormatException();
                default:
                    throw new BugException();
            }
        }

        try {
            return dateTimeFormatter.format(temporal);
        } catch (DateTimeException e) {
            throw new UnformattableValueException(e.getMessage(), e);
        }
    }

    private static InvalidFormatParametersException newOffsetTimeWithoutOffsetOnTheFormatException() {
        return new InvalidFormatParametersException(
                "The time format must show the time offset when dealing with offset-time type, because in case the "
                        + "current FreeMarker time zone uses Daylight Saving Time, it's impossible to convert between "
                        + "offset-time, and the local-time, since we don't know the day.");
    }

    @Override
    public Object parse(String s) throws TemplateValueFormatException {
        if (specialParsing != null) {
            switch (specialParsing) {
                case OFFSET_TIME_DST_ERROR:
                    throw newOffsetTimeWithoutOffsetOnTheFormatException();
                default:
                    throw new BugException();
            }
        }

        try {
            return dateTimeFormatter.parse(s, temporalQuery);
        } catch (DateTimeParseException e) {
            throw new UnparsableValueException(
                    "Failed to parse value " + jQuote(s) + " with format " + jQuote(formatString)
                            + ", and target class " + temporalClass.getSimpleName() + ", "
                            + "locale " + jQuote(dateTimeFormatter.getLocale()) + ", "
                            + "zoneId " + jQuote(zoneId) + ".\n"
                            + "(Used this DateTimeFormatter: " + dateTimeFormatter + ")\n"
                            + "(Root cause message: " + e.getMessage() + ")",
                    e);
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

    private static OffsetDateTime offsetDateTimeFrom(TemporalAccessor temporal) {
        return ZonedDateTime.from(temporal).toOffsetDateTime();
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

    private static FormatStyle getLessVerboseStyle(FormatStyle style) {
        switch (style) {
            case FULL:
                return FormatStyle.LONG;
            case LONG:
                return FormatStyle.MEDIUM;
            case MEDIUM:
                return FormatStyle.SHORT;
            default:
                return null;
        }
    }

}
