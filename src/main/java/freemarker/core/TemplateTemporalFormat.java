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

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

/**
 * Represents a {@link Temporal} format; used in templates for formatting {@link Temporal}-s, and parsing strings to
 * {@link Temporal}-s. This is similar to Java's {@link DateTimeFormatter}, but made to fit the requirements of
 * FreeMarker. Also, it makes it possible to define formats that can't be described with {@link DateTimeFormatter}.
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
 * @since 2.3.32
 */
public abstract class TemplateTemporalFormat extends TemplateValueFormat {

    public abstract String formatToPlainText(TemplateTemporalModel temporalModel)
            throws TemplateValueFormatException, TemplateModelException;

    /**
     * Formats the model to markup instead of to plain text, if the result markup will be more than just plain text
     * escaped, otherwise falls back to formatting to plain text. If the markup result would be just the result of
     * {@link #formatToPlainText(TemplateTemporalModel)} escaped, it must return the {@link String} that
     * {@link #formatToPlainText(TemplateTemporalModel)} does.
     *
     * <p>The implementation in {@link TemplateTemporalFormat} simply calls {@link #formatToPlainText(TemplateTemporalModel)}.
     *
     * @return A {@link String} or a {@link TemplateMarkupOutputModel}; not {@code null}.
     */
    public Object format(TemplateTemporalModel temporalModel) throws TemplateValueFormatException, TemplateModelException {
        return formatToPlainText(temporalModel);
    }

    /**
     * Tells if this formatter can be used for the given locale.
     */
    public abstract boolean canBeUsedForLocale(Locale locale);

    /**
     * Tells if this formatter can be used for the given {@link TimeZone}.
     */
    public abstract boolean canBeUsedForTimeZone(TimeZone timeZone);

    /**
     * Parser a string to a {@link Temporal}, according to this format. Some format implementations may throw
     * {@link ParsingNotSupportedException} here.
     *
     * @param s
     *            The string to parse
     *
     * @return The interpretation of the text either as a {@link Temporal} or {@link TemplateTemporalModel}. Typically,
     *         a {@link Temporal}. {@link TemplateTemporalModel} is used if you have to attach some application-specific
     *         meta-information that's also extracted during {@link #formatToPlainText(TemplateTemporalModel)} (so if
     *         you format something and then parse it, you get back an equivalent result). It can't be {@code null}.
     *
     * @throws ParsingNotSupportedException If this format doesn't implement parsing.
     */
    public abstract Object parse(String s, MissingTimeZoneParserPolicy missingTimeZoneParserPolicy)
            throws TemplateValueFormatException;

}
