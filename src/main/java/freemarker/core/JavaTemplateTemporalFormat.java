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
import java.time.ZoneId;
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

class JavaTemplateTemporalFormat extends TemplateTemporalFormat {

    enum FormatTimeConversion {
        INSTANT_TO_ZONED_DATE_TIME,
        SET_ZONE_FROM_OFFSET
    }

    private static final Pattern FORMAT_STYLE_PATTERN = Pattern.compile("(short|medium|long|full)(?:_(short|medium|long|full))?");

    private final DateTimeFormatter dateTimeFormatter;
    private final ZoneId zoneId;
    private final String formatString;
    private final FormatTimeConversion formatTimeConversion;

    JavaTemplateTemporalFormat(String formatString, Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone)
            throws InvalidFormatParametersException {
        this.formatString = formatString;

        temporalClass = _CoreTemporalUtils.normalizeSupportedTemporalClass(temporalClass);

        Matcher localizedPatternMatcher = FORMAT_STYLE_PATTERN.matcher(formatString);
        boolean isLocalizedPattern = localizedPatternMatcher.matches();
        if (temporalClass == Instant.class) {
            this.formatTimeConversion = FormatTimeConversion.INSTANT_TO_ZONED_DATE_TIME;
        } else if (isLocalizedPattern && (temporalClass == OffsetDateTime.class || temporalClass == OffsetTime.class)) {
            this.formatTimeConversion = FormatTimeConversion.SET_ZONE_FROM_OFFSET;
        } else {
            this.formatTimeConversion = null;
        }

        DateTimeFormatter dateTimeFormatter;
        if (isLocalizedPattern) {
            FormatStyle datePartFormatStyle = FormatStyle.valueOf(localizedPatternMatcher.group(1).toUpperCase(Locale.ROOT));
            String group2 = localizedPatternMatcher.group(2);
            FormatStyle timePartFormatStyle = group2 != null
                    ? FormatStyle.valueOf(group2.toUpperCase(Locale.ROOT))
                    : datePartFormatStyle;
            if (temporalClass == LocalDateTime.class || temporalClass == ZonedDateTime.class
                    || temporalClass == OffsetDateTime.class || temporalClass == Instant.class) {
                dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(datePartFormatStyle, timePartFormatStyle);
            } else if (temporalClass == LocalTime.class || temporalClass == OffsetTime.class) {
                dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(timePartFormatStyle);
            } else if (temporalClass == LocalDate.class) {
                dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(datePartFormatStyle);
            } else {
                throw new InvalidFormatParametersException(
                        "Format " + StringUtil.jQuote(formatString) + " is not supported for "
                        + temporalClass.getName());
            }
        } else {
            try {
                dateTimeFormatter = DateTimeFormatter.ofPattern(formatString);
            } catch (IllegalArgumentException e) {
                throw new InvalidFormatParametersException(e.getMessage(), e);
            }
        }
        this.dateTimeFormatter = dateTimeFormatter.withLocale(locale);

        this.zoneId = timeZone.toZoneId();
    }

    @Override
    public String format(TemplateTemporalModel tm) throws TemplateValueFormatException, TemplateModelException {
        DateTimeFormatter dateTimeFormatter = this.dateTimeFormatter;
        Temporal temporal = TemplateFormatUtil.getNonNullTemporal(tm);

        if (formatTimeConversion == FormatTimeConversion.INSTANT_TO_ZONED_DATE_TIME) {
            temporal = ((Instant) temporal).atZone(zoneId);
        } else if (formatTimeConversion == FormatTimeConversion.SET_ZONE_FROM_OFFSET) {
            if (temporal instanceof OffsetDateTime) {
                dateTimeFormatter = dateTimeFormatter.withZone(((OffsetDateTime) temporal).getOffset());
            } else if (temporal instanceof OffsetTime) {
                dateTimeFormatter = dateTimeFormatter.withZone(((OffsetTime) temporal).getOffset());
            } else {
                throw new IllegalArgumentException(
                        "Formatter was created for OffsetTime and OffsetDateTime, but value was a "
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

}
