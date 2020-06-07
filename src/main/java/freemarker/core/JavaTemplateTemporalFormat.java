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

import java.time.Year;
import java.time.YearMonth;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

final class JavaTemplateTemporalFormat extends BaseJavaTemplateTemporalFormatTemplateFormat {
    private static final Pattern FORMAT_STYLE_PATTERN = Pattern.compile("^(short|medium|long|full)(_(short|medium|long|full))?$");

    // TODO [FREEMARKER-35] This is not right, but for now we mimic what TemporalUtils did
    private static final DateTimeFormatter SHORT_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
    private static final DateTimeFormatter MEDIUM_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
    private static final DateTimeFormatter LONG_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);
    private static final DateTimeFormatter FULL_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);

    private final String formatString;

    JavaTemplateTemporalFormat(String formatString, Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone)
            throws InvalidFormatParametersException {
        super(getDateTimeFormat(formatString, temporalClass, locale, timeZone));
        this.formatString = formatString;
    }

    private static DateTimeFormatter getDateTimeFormat(String formatString, Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone) throws
            InvalidFormatParametersException {
        DateTimeFormatter result;
        if (FORMAT_STYLE_PATTERN.matcher(formatString).matches()) {
            // TODO [FREEMARKER-35] This is not right, but for now we mimic what TemporalUtils did
            boolean isYear = Year.class.isAssignableFrom(temporalClass);
            boolean isYearMonth = YearMonth.class.isAssignableFrom(temporalClass);
            String[] formatSplt = formatString.split("_");
            if (isYear || isYearMonth) {
                String reducedPattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.valueOf(formatSplt[0].toUpperCase()), null, IsoChronology.INSTANCE, locale);
                if (isYear)
                    result = DateTimeFormatter.ofPattern(removeNonYM(reducedPattern, false));
                else
                    result = DateTimeFormatter.ofPattern(removeNonYM(reducedPattern, true));
            } else if ("short".equals(formatString))
                result =  SHORT_FORMAT;
            else if ("medium".equals(formatString))
                result =  MEDIUM_FORMAT;
            else if ("long".equals(formatString))
                result =  LONG_FORMAT;
            else if ("full".equals(formatString))
                result = FULL_FORMAT;
            else
                result = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.valueOf(formatSplt[0].toUpperCase()), FormatStyle.valueOf(formatSplt[1].toUpperCase()));
        } else {
            try {
                result = DateTimeFormatter.ofPattern(formatString);
            } catch (IllegalArgumentException e) {
                throw new InvalidFormatParametersException(e.getMessage(), e);
            }
        }
        return result.withLocale(locale).withZone(timeZone.toZoneId());
    }

    // TODO [FREEMARKER-35] This override should be unecessary. Move logic here into getDateTimeFormat somehow.
    @Override
    public String format(TemplateTemporalModel tm) throws TemplateValueFormatException, TemplateModelException {
        return super.format(tm);
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

    // TODO [FREEMARKER-35] This is not right, but for now we mimic what TemporalUtils did
    private static String removeNonYM(String pattern, boolean withMonth) {
        boolean separator = false;
        boolean copy = true;
        StringBuilder newPattern = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == '\'')
                separator = !separator;
            if (!separator && Character.isAlphabetic(c))
                copy = c == 'y' || c == 'u' || (withMonth && (c == 'M' || c == 'L'));
            if (copy)
                newPattern.append(c);
        }
        return newPattern.toString();
    }

}
