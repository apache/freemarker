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

import java.io.IOException;
import java.io.Writer;
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
     *            The date/time/dateTime to format. Most implementations will just work with the return value of
     *            {@link TemplateDateModel#getAsDate()}, but some may format differently depending on the properties of
     *            a custom {@link TemplateDateModel} implementation.
     * 
     * @return The date/time/dateTime as text, with no escaping (like no HTML escaping). Can't be {@code null}.
     * 
     * @throws TemplateValueFormatException
     *             When a problem occurs during the formatting of the value. Notable subclass:
     *             {@link UnknownDateTypeFormattingUnsupportedException}
     * @throws TemplateModelException
     *             Exception thrown by the {@code dateModel} object when calling its methods.
     */
    public abstract String format(TemplateDateModel dateModel)
            throws TemplateValueFormatException, TemplateModelException;

    /**
     * <b>[Not yet used, might changes in 2.3.24 final]</b>
     * Formats the date/time/dateTime to markup instead of to plain text, or returns {@code null} that will make
     * FreeMarker call {@link #format(TemplateDateModel)} and escape its result. If the markup format would be just the
     * result of {@link #format(TemplateDateModel)} escaped, it should return {@code null}.
     */
    public abstract <MO extends TemplateMarkupOutputModel> MO format(
            TemplateDateModel dateModel, MarkupOutputFormat<MO> outputFormat)
                    throws TemplateValueFormatException, TemplateModelException;

    /**
     * <b>[Not yet used, might changes in 2.3.24 final]</b>
     * Same as {@link #format(TemplateDateModel, MarkupOutputFormat)}, but prints the result to a {@link Writer}
     * instead of returning it. This can be utilized for some optimizatoin. In the case where
     * {@link #format(TemplateDateModel, MarkupOutputFormat)} would return {@code null}, it returns {@code false}. It
     * writes to the {@link Writer} exactly if the return value is {@code true}.
     * 
     * <p>
     * The default implementation in {@link TemplateNumberFormat} builds on calls
     * {@link #format(TemplateDateModel, MarkupOutputFormat)} and writes its result to the {@link Writer}.
     */
    public <MO extends TemplateMarkupOutputModel> boolean format(TemplateDateModel dateModel,
            MarkupOutputFormat<MO> outputFormat, Writer out)
                    throws TemplateValueFormatException, TemplateModelException, IOException {
        MO mo = format(dateModel, outputFormat);
        if (mo == null) {
            return false;
        }
        mo.getOutputFormat().output(mo, out);
        return true;
    }

    /**
     * <b>[Unfinished - will change in 2.3.24 final]</b>.
     * 
     * TODO Thrown exceptions.
     * TODO How can one return a TemplateDateModel instead?
     * 
     * @return The interpretation of the text as {@link Date}. Can't be {@code null}.
     */
    public abstract Date parse(String s) throws TemplateValueFormatException;
    
    /**
     * Tells if this formatter should be re-created if the locale changes.
     */
    public abstract boolean isLocaleBound();

    /**
     * Tells if this formatter should be re-created if the time zone changes. Currently always {@code true}.
     */
    public abstract boolean isTimeZoneBound();
        
}
