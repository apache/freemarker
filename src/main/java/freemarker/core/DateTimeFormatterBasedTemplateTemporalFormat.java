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

import static freemarker.core.MissingTimeZoneParserPolicy.*;
import static freemarker.core._TemporalUtils.*;
import static freemarker.template.utility.StringUtil.*;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Common logic among our {@link TemplateTemporalFormat}-s that are based on {@link TemplateTemporalFormat}.
 */
abstract class DateTimeFormatterBasedTemplateTemporalFormat extends TemplateTemporalFormat {
    protected final Class<? extends Temporal> temporalClass;
    protected final boolean isLocalTemporalClass;
    protected final TimeZone timeZone;
    protected final ZoneId zoneId;

    public DateTimeFormatterBasedTemplateTemporalFormat(
            Class<? extends Temporal> temporalClass, TimeZone timeZone) {
        this.temporalClass = Objects.requireNonNull(_TemporalUtils.normalizeSupportedTemporalClass(temporalClass));
        this.isLocalTemporalClass = isLocalTemporalClass(this.temporalClass);
        if (isLocalTemporalClass) {
            this.zoneId = null;
            this.timeZone = null;
        } else {
            this.timeZone = Objects.requireNonNull(timeZone);
            this.zoneId = timeZone.toZoneId();
        }
    }

    /**
     * Called from {@link TemplateTemporalFormat#parse(String, MissingTimeZoneParserPolicy)}, when that has figured
     * out the {@link DateTimeFormatter} to use, this method will deal with the time zone related matters, and some
     * more (like converting parsing exceptions).
     */
    protected Temporal parse(
            String s, MissingTimeZoneParserPolicy missingTimeZoneParserPolicy,
            DateTimeFormatter parserDateTimeFormatter) throws UnparsableValueException {
        try {
            TemporalAccessor parseResult = parserDateTimeFormatter.parse(s);

            if (isLocalTemporalClass
                    || parseResult.isSupported(ChronoField.OFFSET_SECONDS)
                    || parseResult.isSupported(ChronoField.INSTANT_SECONDS)) {
                return parseResult.query(_TemporalUtils.getTemporalQuery(temporalClass));
            }

            if (missingTimeZoneParserPolicy == ASSUME_CURRENT_TIME_ZONE ||
                    missingTimeZoneParserPolicy == FALL_BACK_TO_LOCAL_TEMPORAL) {
                boolean fallbackToLocal = missingTimeZoneParserPolicy == FALL_BACK_TO_LOCAL_TEMPORAL;
                Class<? extends Temporal> localFallbackTemporalClass;
                localFallbackTemporalClass = tryGetLocalTemporalClassForNonLocal(temporalClass);
                if (localFallbackTemporalClass == null) {
                    throw newUnparsableValueException(
                            s, parserDateTimeFormatter,
                            "String contains no zone, nor offset, and no local variant exists for target type "
                                    + temporalClass.getName(),
                            null);
                }
                if (!fallbackToLocal && temporalClass == OffsetTime.class) {
                    throw newUnparsableValueException(
                            s, parserDateTimeFormatter,
                            "It's not possible to parse a string that contains no zone, nor offset, to OffsetTime. "
                                    + "We don't know the day, and hence can't account for Daylight Saving Time, "
                                    + "and thus we can't use the current time zone."
                                    + temporalClass.getName(),
                            null);
                }

                Temporal resultLocalTemporal = parseResult.query(
                        getTemporalQuery(localFallbackTemporalClass));
                if (fallbackToLocal) {
                    return resultLocalTemporal;
                }

                if (resultLocalTemporal instanceof LocalDateTime) {
                    ZonedDateTime zonedDateTime = ((LocalDateTime) resultLocalTemporal).atZone(zoneId);
                    if (temporalClass == ZonedDateTime.class) {
                        return zonedDateTime;
                    } else if (temporalClass == OffsetDateTime.class) {
                        return zonedDateTime.toOffsetDateTime();
                    } else if (temporalClass == Instant.class) {
                        return zonedDateTime.toInstant();
                    }
                }
                throw new BugException("Unexpected case: "
                        + "temporalClass=" + temporalClass + ", "
                        + "missingTimeZoneParserPolicy=" + missingTimeZoneParserPolicy);
            } else if (missingTimeZoneParserPolicy == FAIL) {
                throw newUnparsableValueException(
                        s, parserDateTimeFormatter,
                        _MessageUtil.FAIL_MISSING_TIME_ZONE_PARSER_POLICY_ERROR_DETAIL, null);
            }
            throw new AssertionError();
        } catch (DateTimeException|ArithmeticException e) {
            throw newUnparsableValueException(s, parserDateTimeFormatter, e.getMessage(), e);
        }
    }

    protected UnparsableValueException newUnparsableValueException(
            String s, DateTimeFormatter dateTimeFormatter,
            String cause, Exception e) {
        StringBuilder message = new StringBuilder();

        message.append("Failed to parse value ").append(jQuote(s))
                .append(" with format ").append(jQuote(getDescription()))
                .append(", and target class ").append(temporalClass.getSimpleName());
        if (dateTimeFormatter != null) {
            message.append(", ").append("locale ").append(jQuote(dateTimeFormatter.getLocale()));
        }
        if (zoneId != null) {
            message.append(", ").append("zoneId ").append(jQuote(zoneId));
        }
        message.append(".");
        if (dateTimeFormatter != null) {
            message.append("\n(DateTimeFormatter used: ").append(jQuote(dateTimeFormatter)).append(")");
        }
        message.append("\nCause: ").append(cause);

        return new UnparsableValueException(
                message.toString(),
                e);
    }

    @Override
    public final boolean canBeUsedForTimeZone(TimeZone timeZone) {
        return this.timeZone == null || this.timeZone.equals(timeZone);
    }
}
