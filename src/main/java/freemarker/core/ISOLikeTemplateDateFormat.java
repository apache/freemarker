/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.util.Date;
import java.util.TimeZone;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.CalendarFieldsToDateConverter;
import freemarker.template.utility.DateUtil.DateParseException;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;
import freemarker.template.utility.StringUtil;

abstract class ISOLikeTemplateDateFormat  extends TemplateDateFormat {
    
    private static final String XS_LESS_THAN_SECONDS_ACCURACY_ERROR_MESSAGE
            = "Less than seconds accuracy isn't allowed by the XML Schema format";
    private final ISOLikeLocalizedTemplateDateFormatFactory factory; 
    protected final int dateType;
    protected final boolean zonelessInput;
    protected final TimeZone timeZone;
    protected final Boolean forceUTC;
    protected final Boolean showZoneOffset;
    protected final int accuracy;

    /**
     * @param formatDesc The value of the ..._format setting, like "iso nz".
     * @param parsingStart The index of the char in the {@code settingValue} that directly after the prefix that has
     *     indicated the exact formatter class (like "iso" or "xs") 
     */
    public ISOLikeTemplateDateFormat(
            final String formatDesc, int parsingStart,
            int dateType, boolean zonelessInput,
            TimeZone timeZone,
            ISOLikeLocalizedTemplateDateFormatFactory factory)
            throws InvalidFormatDescriptorException, UnknownDateTypeFormattingUnsupportedException {
        this.factory = factory;
        if (dateType == TemplateDateModel.UNKNOWN) {
            throw new UnknownDateTypeFormattingUnsupportedException();
        }
        
        this.dateType = dateType;
        this.zonelessInput = zonelessInput;
        
        final int ln = formatDesc.length();
        boolean afterSeparator = false;
        int i = parsingStart;
        int accuracy = DateUtil.ACCURACY_MILLISECONDS;
        Boolean showZoneOffset = null;
        Boolean forceUTC = Boolean.FALSE;
        while (i < ln) {
            final char c = formatDesc.charAt(i++);
            if (c == '_' || c == ' ') {
                afterSeparator = true;
            } else {
                if (!afterSeparator) {
                    throw new InvalidFormatDescriptorException(
                            "Missing space or \"_\" before \"" + c + "\" (at char pos. " + i + ").", formatDesc);
                }
                
                switch (c) {
                case 'h':
                case 'm':
                case 's':
                    if (accuracy != DateUtil.ACCURACY_MILLISECONDS) {
                        throw new InvalidFormatDescriptorException(
                                "Character \"" + c + "\" is unexpected as accuracy was already specified earlier "
                                + "(at char pos. " + i + ").",
                                formatDesc);
                    }
                    switch (c) {
                    case 'h':
                        if (isXSMode()) {
                            throw new InvalidFormatDescriptorException(
                                    XS_LESS_THAN_SECONDS_ACCURACY_ERROR_MESSAGE, formatDesc);
                        }
                        accuracy = DateUtil.ACCURACY_HOURS;
                        break;
                    case 'm':
                        if (i < ln && formatDesc.charAt(i) == 's') {
                            i++;
                            accuracy = DateUtil.ACCURACY_MILLISECONDS_FORCED;
                        } else {
                            if (isXSMode()) {
                                throw new InvalidFormatDescriptorException(
                                        XS_LESS_THAN_SECONDS_ACCURACY_ERROR_MESSAGE, formatDesc);
                            }
                            accuracy = DateUtil.ACCURACY_MINUTES;
                        }
                        break;
                    case 's':
                        accuracy = DateUtil.ACCURACY_SECONDS;
                        break;
                    }
                    break;
                case 'f':
                    if (i < ln && formatDesc.charAt(i) == 'u') {
                        checkForceUTCNotSet(forceUTC, formatDesc);
                        i++;
                        forceUTC = Boolean.TRUE;
                        break;
                    }
                    // Falls through
                case 'n':
                    if (showZoneOffset != null) {
                        throw new InvalidFormatDescriptorException(
                                "Character \"" + c + "\" is unexpected as zone offset visibility was already "
                                + "specified earlier. (at char pos. " + i + ").", formatDesc);
                    }
                    switch (c) {
                    case 'n':
                        if (i < ln && formatDesc.charAt(i) == 'z') {
                            i++;
                            showZoneOffset = Boolean.FALSE;
                        } else {
                            throw new InvalidFormatDescriptorException(
                                    "\"n\" must be followed by \"z\" (at char pos. " + i + ").", formatDesc);
                        }
                        break;
                    case 'f':
                        if (i < ln && formatDesc.charAt(i) == 'z') {
                            i++;
                            showZoneOffset = Boolean.TRUE;
                        } else {
                            throw new InvalidFormatDescriptorException(
                                    "\"f\" must be followed by \"z\" (at char pos. " + i + ").", formatDesc);
                        }
                        break;
                    }
                    break;
                case 'u':
                    checkForceUTCNotSet(forceUTC, formatDesc);
                    forceUTC = null;  // means UTC will be used except for zonelessInput
                    break;
                default:
                    throw new InvalidFormatDescriptorException(
                            "Unexpected character, " + StringUtil.jQuote(String.valueOf(c))
                            + ". Expected the beginning of one of: h, m, s, ms, nz, fz, u"
                            + " (at char pos. " + i + ").",
                            formatDesc);
                } // switch
                afterSeparator = false;
            } // else
        } // while
        
        this.accuracy = accuracy;
        this.showZoneOffset = showZoneOffset;
        this.forceUTC = forceUTC;
        this.timeZone = timeZone;
    }

    private void checkForceUTCNotSet(Boolean fourceUTC, String formatDesc) throws InvalidFormatDescriptorException {
        if (fourceUTC != Boolean.FALSE) {
            throw new InvalidFormatDescriptorException(
                    "The UTC usage option was already set earlier.", formatDesc);
        }
    }
    
    @Override
    public final String format(TemplateDateModel dateModel) throws TemplateModelException {
        final Date date = dateModel.getAsDate();
        return format(
                date,
                dateType != TemplateDateModel.TIME,
                dateType != TemplateDateModel.DATE,
                showZoneOffset == null
                        ? !zonelessInput
                        : showZoneOffset.booleanValue(),
                accuracy,
                (forceUTC == null ? !zonelessInput : forceUTC.booleanValue()) ? DateUtil.UTC : timeZone,
                factory.getISOBuiltInCalendar());
    }
    
    protected abstract String format(Date date,
            boolean datePart, boolean timePart, boolean offsetPart,
            int accuracy,
            TimeZone timeZone,
            DateToISO8601CalendarFactory calendarFactory);

    @Override
    public final Date parse(String s) throws java.text.ParseException {
        CalendarFieldsToDateConverter calToDateConverter = factory.getCalendarFieldsToDateCalculator();
        TimeZone tz = forceUTC != Boolean.FALSE ? DateUtil.UTC : timeZone;
        if (dateType == TemplateDateModel.DATE) {
            return parseDate(s, tz, calToDateConverter);
        } else if (dateType == TemplateDateModel.TIME) {
            return parseTime(s, tz, calToDateConverter);
        } else if (dateType == TemplateDateModel.DATETIME) {
            return parseDateTime(s, tz, calToDateConverter);
        } else {
            throw new BugException("Unexpected date type: " + dateType);
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
            case TemplateDateModel.DATETIME: return getDateTimeDescription();
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
    
    /**
     * Always returns {@code null} (there's no markup format).
     */
    @Override
    public <MO extends TemplateMarkupOutputModel> MO format(TemplateDateModel dateModel,
            MarkupOutputFormat<MO> outputFormat) throws UnformattableNumberException, TemplateModelException {
        return null;
    }

    protected abstract boolean isXSMode();

}
