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

import java.text.DateFormat;
import java.util.Date;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;

/**
 * Represents a date/time/dateTime format; used in templates for formatting and parsing with that format. This is
 * similar to Java's {@link DateFormat}, but made to fit the requirements of FreeMarker. Also, it makes easier to define
 * formats that can't be represented with Java's existing {@link DateFormat} implementations.
 * 
 * <p>
 * Implementations need not be thread-safe if the {@link TemplateNumberFormatFactory} doesn't recycle them among
 * different {@link Environment}-s. As far as FreeMarker's concerned, instances are bound to a single
 * {@link Environment}, and {@link Environment}-s are thread-local objects.
 * 
 * @since 2.3.24
 */
public abstract class TemplateDateFormat extends TemplateValueFormat {
    
    /**
     * @param dateModel
     *            The date/time/dateTime to format; not {@code null}. Most implementations will just work with the return value of
     *            {@link TemplateDateModel#getAsDate()}, but some may format differently depending on the properties of
     *            a custom {@link TemplateDateModel} implementation.
     * 
     * @return The date/time/dateTime as text, with no escaping (like no HTML escaping); can't be {@code null}.
     * 
     * @throws TemplateValueFormatException
     *             When a problem occurs during the formatting of the value. Notable subclass:
     *             {@link UnknownDateTypeFormattingUnsupportedException}
     * @throws TemplateModelException
     *             Exception thrown by the {@code dateModel} object when calling its methods.
     */
    public abstract String formatToPlainText(TemplateDateModel dateModel)
            throws TemplateValueFormatException, TemplateModelException;

    /**
     * Formats the model to markup instead of to plain text if the result markup will be more than just plain text
     * escaped, otherwise falls back to formatting to plain text. If the markup result would be just the result of
     * {@link #formatToPlainText(TemplateDateModel)} escaped, it must return the {@link String} that
     * {@link #formatToPlainText(TemplateDateModel)} does.
     * 
     * <p>The implementation in {@link TemplateDateFormat} simply calls {@link #formatToPlainText(TemplateDateModel)}.
     * 
     * @return A {@link String} or a {@link TemplateMarkupOutputModel}; not {@code null}.
     */
    public Object format(TemplateDateModel dateModel) throws TemplateValueFormatException, TemplateModelException {
        return formatToPlainText(dateModel);
    }

    /**
     * Parsers a string to date/time/datetime, according to this format. Some format implementations may throw
     * {@link ParsingNotSupportedException} here. 
     * 
     * @param s
     *            The string to parse
     * @param dateType
     *            The expected date type of the result. Not all {@link TemplateDateFormat}-s will care about this;
     *            though those who return a {@link TemplateDateModel} instead of {@link Date} often will. When strings
     *            are parsed via {@code ?date}, {@code ?time}, or {@code ?datetime}, then this parameter is
     *            {@link TemplateDateModel#DATE}, {@link TemplateDateModel#TIME}, or {@link TemplateDateModel#DATETIME},
     *            respectively. This parameter rarely if ever {@link TemplateDateModel#UNKNOWN}, but the implementation
     *            that cares about this parameter should be prepared for that. If nothing else, it should throw
     *            {@link UnknownDateTypeParsingUnsupportedException} then.
     * 
     * @return The interpretation of the text either as a {@link Date} or {@link TemplateDateModel}. Typically, a
     *         {@link Date}. {@link TemplateDateModel} is used if you have to attach some application-specific
     *         meta-information thats also extracted during {@link #formatToPlainText(TemplateDateModel)} (so if you format
     *         something and then parse it, you get back an equivalent result). It can't be {@code null}. Known issue
     *         (at least in FTL 2): {@code ?date}/{@code ?time}/{@code ?datetime}, when not invoked as a method, can't
     *         return the {@link TemplateDateModel}, only the {@link Date} from inside it, hence the additional
     *         application-specific meta-info will be lost.
     */
    public abstract Object parse(String s, int dateType) throws TemplateValueFormatException;
    
    /**
     * Tells if this formatter should be re-created if the locale changes.
     */
    public abstract boolean isLocaleBound();

    /**
     * Tells if this formatter should be re-created if the time zone changes. Currently always {@code true}.
     */
    public abstract boolean isTimeZoneBound();
        
}
