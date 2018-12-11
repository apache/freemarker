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

package org.apache.freemarker.core.valueformat.impl;

import java.util.Date;
import java.util.TimeZone;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util._DateUtils;
import org.apache.freemarker.core.util._DateUtils.CalendarFieldsToDateConverter;
import org.apache.freemarker.core.util._DateUtils.DateParseException;
import org.apache.freemarker.core.util._DateUtils.DateToISO8601CalendarFactory;
import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.core.valueformat.InvalidFormatParametersException;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.TemplateFormatUtil;
import org.apache.freemarker.core.valueformat.UnknownDateTypeFormattingUnsupportedException;
import org.apache.freemarker.core.valueformat.UnparsableValueException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

abstract class ISOLikeTemplateDateFormat  extends TemplateDateFormat {
    
    private static final String XS_LESS_THAN_SECONDS_ACCURACY_ERROR_MESSAGE
            = "Less than seconds accuracy isn't allowed by the XML Schema format";
    private final ISOLikeTemplateDateFormatFactory factory;
    private final Environment env;
    protected final int dateType;
    protected final boolean zonelessInput;
    protected final TimeZone timeZone;
    protected final Boolean forceUTC;
    protected final Boolean showZoneOffset;
    protected final int accuracy;

    /**
     * @param formatString The value of the ..._format setting, like "iso nz".
     * @param parsingStart The index of the char in the {@code settingValue} that directly after the prefix that has
     *     indicated the exact formatter class (like "iso" or "xs") 
     */
    public ISOLikeTemplateDateFormat(
            final String formatString, int parsingStart,
            int dateType, boolean zonelessInput,
            TimeZone timeZone,
            ISOLikeTemplateDateFormatFactory factory, Environment env)
            throws InvalidFormatParametersException, UnknownDateTypeFormattingUnsupportedException {
        this.factory = factory;
        this.env = env;
        if (dateType == TemplateDateModel.UNKNOWN) {
            throw new UnknownDateTypeFormattingUnsupportedException();
        }
        
        this.dateType = dateType;
        this.zonelessInput = zonelessInput;
        
        final int ln = formatString.length();
        boolean afterSeparator = false;
        int i = parsingStart;
        int accuracy = _DateUtils.ACCURACY_MILLISECONDS;
        Boolean showZoneOffset = null;
        Boolean forceUTC = Boolean.FALSE;
        while (i < ln) {
            final char c = formatString.charAt(i++);
            if (c == '_' || c == ' ') {
                afterSeparator = true;
            } else {
                if (!afterSeparator) {
                    throw new InvalidFormatParametersException(
                            "Missing space or \"_\" before \"" + c + "\" (at char pos. " + i + ").");
                }
                
                switch (c) {
                case 'h':
                case 'm':
                case 's':
                    if (accuracy != _DateUtils.ACCURACY_MILLISECONDS) {
                        throw new InvalidFormatParametersException(
                                "Character \"" + c + "\" is unexpected as accuracy was already specified earlier "
                                + "(at char pos. " + i + ").");
                    }
                    switch (c) {
                    case 'h':
                        if (isXSMode()) {
                            throw new InvalidFormatParametersException(
                                    XS_LESS_THAN_SECONDS_ACCURACY_ERROR_MESSAGE);
                        }
                        accuracy = _DateUtils.ACCURACY_HOURS;
                        break;
                    case 'm':
                        if (i < ln && formatString.charAt(i) == 's') {
                            i++;
                            accuracy = _DateUtils.ACCURACY_MILLISECONDS_FORCED;
                        } else {
                            if (isXSMode()) {
                                throw new InvalidFormatParametersException(
                                        XS_LESS_THAN_SECONDS_ACCURACY_ERROR_MESSAGE);
                            }
                            accuracy = _DateUtils.ACCURACY_MINUTES;
                        }
                        break;
                    case 's':
                        accuracy = _DateUtils.ACCURACY_SECONDS;
                        break;
                    }
                    break;
                case 'f':
                    if (i < ln && formatString.charAt(i) == 'u') {
                        checkForceUTCNotSet(forceUTC);
                        i++;
                        forceUTC = Boolean.TRUE;
                        break;
                    }
                    // Falls through
                case 'n':
                    if (showZoneOffset != null) {
                        throw new InvalidFormatParametersException(
                                "Character \"" + c + "\" is unexpected as zone offset visibility was already "
                                + "specified earlier. (at char pos. " + i + ").");
                    }
                    switch (c) {
                    case 'n':
                        if (i < ln && formatString.charAt(i) == 'z') {
                            i++;
                            showZoneOffset = Boolean.FALSE;
                        } else {
                            throw new InvalidFormatParametersException(
                                    "\"n\" must be followed by \"z\" (at char pos. " + i + ").");
                        }
                        break;
                    case 'f':
                        if (i < ln && formatString.charAt(i) == 'z') {
                            i++;
                            showZoneOffset = Boolean.TRUE;
                        } else {
                            throw new InvalidFormatParametersException(
                                    "\"f\" must be followed by \"z\" (at char pos. " + i + ").");
                        }
                        break;
                    }
                    break;
                case 'u':
                    checkForceUTCNotSet(forceUTC);
                    forceUTC = null;  // means UTC will be used except for zonelessInput
                    break;
                default:
                    throw new InvalidFormatParametersException(
                            "Unexpected character, " + _StringUtils.jQuote(String.valueOf(c))
                            + ". Expected the beginning of one of: h, m, s, ms, nz, fz, u"
                            + " (at char pos. " + i + ").");
                } // switch
                afterSeparator = false;
            } // else
        } // while
        
        this.accuracy = accuracy;
        this.showZoneOffset = showZoneOffset;
        this.forceUTC = forceUTC;
        this.timeZone = timeZone;
    }

    private void checkForceUTCNotSet(Boolean fourceUTC) throws InvalidFormatParametersException {
        if (fourceUTC != Boolean.FALSE) {
            throw new InvalidFormatParametersException(
                    "The UTC usage option was already set earlier.");
        }
    }
    
    @Override
    public final String formatToPlainText(TemplateDateModel dateModel) throws TemplateException {
        final Date date = TemplateFormatUtil.getNonNullDate(dateModel);
        return format(
                date,
                dateType != TemplateDateModel.TIME,
                dateType != TemplateDateModel.DATE,
                showZoneOffset == null
                        ? !zonelessInput
                        : showZoneOffset.booleanValue(),
                accuracy,
                (forceUTC == null ? !zonelessInput : forceUTC.booleanValue()) ? _DateUtils.UTC : timeZone,
                factory.getISOBuiltInCalendar(env));
    }
    
    protected abstract String format(Date date,
            boolean datePart, boolean timePart, boolean offsetPart,
            int accuracy,
            TimeZone timeZone,
            DateToISO8601CalendarFactory calendarFactory);

    @Override
    @SuppressFBWarnings(value = "RC_REF_COMPARISON_BAD_PRACTICE_BOOLEAN",
            justification = "Known to use the singleton Boolean-s only")
    public final Date parse(String s, int dateType) throws UnparsableValueException {
        CalendarFieldsToDateConverter calToDateConverter = factory.getCalendarFieldsToDateCalculator(env);
        TimeZone tz = forceUTC != Boolean.FALSE ? _DateUtils.UTC : timeZone;
        try {
            if (dateType == TemplateDateModel.DATE) {
                return parseDate(s, tz, calToDateConverter);
            } else if (dateType == TemplateDateModel.TIME) {
                return parseTime(s, tz, calToDateConverter);
            } else if (dateType == TemplateDateModel.DATE_TIME) {
                return parseDateTime(s, tz, calToDateConverter);
            } else {
                throw new BugException("Unexpected date type: " + dateType);
            }
        } catch (DateParseException e) {
            throw new UnparsableValueException(e.getMessage(), e);
        }
    }
    
    protected abstract Date parseDate(
            String s, TimeZone tz,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException;
    
    protected abstract Date parseTime(
            String s, TimeZone tz,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException;
    
    protected abstract Date parseDateTime(
            String s, TimeZone tz,
            CalendarFieldsToDateConverter calToDateConverter) 
            throws DateParseException;

    @Override
    public final String getDescription() {
        switch (dateType) {
            case TemplateDateModel.DATE: return getDateDescription();
            case TemplateDateModel.TIME: return getTimeDescription();
            case TemplateDateModel.DATE_TIME: return getDateTimeDescription();
            default: return "<error: wrong format dateType>";
        }
    }
    
    protected abstract String getDateDescription();
    protected abstract String getTimeDescription();
    protected abstract String getDateTimeDescription();
    
    @Override
    public final boolean isLocaleBound() {
        return false;
    }
    
    @Override
    public boolean isTimeZoneBound() {
        return true;
    }

    protected abstract boolean isXSMode();

}
