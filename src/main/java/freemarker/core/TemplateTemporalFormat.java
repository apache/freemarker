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

import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

/**
 * Represents a {@link Temporal} format; used in templates for formatting {@link Temporal}-s, and parsing strings to
 * {@link Temporal}-s. This is similar to Java's {@link DateTimeFormatter}, but made to fit the requirements of
 * FreeMarker. Also, it allows defining formats that can't be described with Java's {@link DateTimeFormatter}.
 *
 * <p>{@link TemplateTemporalFormat} instances are usually created by a {@link TemplateTemporalFormatFactory}.
 *
 * <p>
 * Implementations need not be thread-safe if the {@link TemplateTemporalFormatFactory} doesn't recycle them among
 * different {@link Environment}-s. The code outside the {@link TemplateTemporalFormatFactory} will not try to reuse
 * {@link TemplateTemporalFormat} instances in multiple {@link Environment}-s, and an {@link Environment} is only used
 * in a single thread.
 *
 * @see TemplateDateFormat
 *
 * @since 2.3.33
 */
public abstract class TemplateTemporalFormat extends TemplateValueFormat {

    /**
     * Formats the value to plain text (string that contains no markup or escaping).
     *
     * @param temporalModel
     *            The temporal value to format; not {@code null}. Most implementations will just work with the return
     *            value of {@link TemplateDateModel#getAsDate()}, but some may format differently depending on the
     *            properties of a custom {@link TemplateDateModel} implementation.
     *
     * @return The {@link Temporal} value as plain text (not markup), with no escaping (like no HTML escaping);
     *             can't be {@code null}.
     *
     * @throws TemplateValueFormatException
     *             If any problem occurs while parsing/getting the format. Notable subclass:
     *             {@link UnformattableValueException}.
     * @throws TemplateModelException
     *             Exception thrown by the {@code temporalModel} object when calling its methods.
     */
    public abstract String formatToPlainText(TemplateTemporalModel temporalModel)
            throws TemplateValueFormatException, TemplateModelException;

    /**
     * Formats the value to markup instead of to plain text, but only if the result markup will be more than just plain
     * text escaped, otherwise falls back to formatting to plain text. If the markup result would be just the result of
     * {@link #formatToPlainText(TemplateTemporalModel)} escaped, then it must return the {@link String} that
     * {@link #formatToPlainText(TemplateTemporalModel)} would.
     *
     * <p>The implementation in {@link TemplateTemporalFormat} simply calls
     * {@link #formatToPlainText(TemplateTemporalModel)}.
     *
     * @return A {@link String} (assumed to be plain text, not markup), or a {@link TemplateMarkupOutputModel};
     *             not {@code null}.
     */
    public Object format(TemplateTemporalModel temporalModel) throws TemplateValueFormatException, TemplateModelException {
        return formatToPlainText(temporalModel);
    }

    /**
     * Tells if this formatter can be used for the parameter {@link Locale}. Meant to be used for cache entry
     * invalidation.
     *
     * @param locale Not {@code null}
     */
    public abstract boolean canBeUsedForLocale(Locale locale);

    /**
     * Tells if this formatter can be used for the parameter {@link TimeZone}. Meant to be used for cache entry
     * invalidation.
     *
     * @param timeZone Not {@code null}
     */
    public abstract boolean canBeUsedForTimeZone(TimeZone timeZone);

    /**
     * Parses a string to a {@link Temporal}, according to this format. This is optional functionality; some
     * implementations may throw {@link ParsingNotSupportedException} here.
     *
     * @param s
     *            The string to parse
     * @param missingTimeZoneParserPolicy
     *            See {@link MissingTimeZoneParserPolicy}; shouldn't be {@code null}, unless you are sure
     *            that the target type is a local temporal type, or that the input string contains zone offset,
     *            time zone, or distance from the UTC epoch. The implementation must accept {@code null} if the
     *            policy is not actually needed.
     *
     * @return The text converted to either {@link Temporal}, or to {@link TemplateTemporalModel}; not {@code null}.
     *         Typically, the result should be a {@link Temporal}. Converting to {@link TemplateTemporalModel} should
     *         only be done if you need to store additional data next to the {@link Temporal}, which is then also used
     *         by {@link #formatToPlainText(TemplateTemporalModel)} (so if you format something and then parse it, you
     *         get back an equivalent object).
     *
     * @throws ParsingNotSupportedException If this format doesn't implement parsing.
     */
    public abstract Object parse(String s, MissingTimeZoneParserPolicy missingTimeZoneParserPolicy)
            throws TemplateValueFormatException;

}
