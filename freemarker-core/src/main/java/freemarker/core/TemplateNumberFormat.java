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

import java.text.NumberFormat;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

/**
 * Represents a number format; used in templates for formatting and parsing with that format. This is similar to Java's
 * {@link NumberFormat}, but made to fit the requirements of FreeMarker. Also, it makes easier to define formats that
 * can't be represented with Java's existing {@link NumberFormat} implementations.
 * 
 * <p>
 * Implementations need not be thread-safe if the {@link TemplateNumberFormatFactory} doesn't recycle them among
 * different {@link Environment}-s. As far as FreeMarker's concerned, instances are bound to a single
 * {@link Environment}, and {@link Environment}-s are thread-local objects.
 * 
 * @since 2.3.24
 */
public abstract class TemplateNumberFormat extends TemplateValueFormat {

    /**
     * @param numberModel
     *            The number to format; not {@code null}. Most implementations will just work with the return value of
     *            {@link TemplateDateModel#getAsDate()}, but some may format differently depending on the properties of
     *            a custom {@link TemplateDateModel} implementation.
     *            
     * @return The number as text, with no escaping (like no HTML escaping); can't be {@code null}.
     * 
     * @throws TemplateValueFormatException
     *             If any problem occurs while parsing/getting the format. Notable subclass:
     *             {@link UnformattableValueException}.
     * @throws TemplateModelException
     *             Exception thrown by the {@code dateModel} object when calling its methods.
     */
    public abstract String formatToPlainText(TemplateNumberModel numberModel)
            throws TemplateValueFormatException, TemplateModelException;

    /**
     * Formats the model to markup instead of to plain text if the result markup will be more than just plain text
     * escaped, otherwise falls back to formatting to plain text. If the markup result would be just the result of
     * {@link #formatToPlainText(TemplateNumberModel)} escaped, it must return the {@link String} that
     * {@link #formatToPlainText(TemplateNumberModel)} does.
     * 
     * <p>
     * The implementation in {@link TemplateNumberFormat} simply calls {@link #formatToPlainText(TemplateNumberModel)}.
     * 
     * @return A {@link String} or a {@link TemplateMarkupOutputModel}; not {@code null}.
     */
    public Object format(TemplateNumberModel numberModel)
            throws TemplateValueFormatException, TemplateModelException {
        return formatToPlainText(numberModel);
    }
    
    /**
     * Tells if this formatter should be re-created if the locale changes.
     */
    public abstract boolean isLocaleBound();

    /**
     * This method is reserved for future purposes; currently it always throws {@link ParsingNotSupportedException}. We
     * don't yet support number parsing with {@link TemplateNumberFormat}-s, because currently FTL parses strings to
     * number with the {@link ArithmeticEngine} ({@link TemplateNumberFormat} were only introduced in 2.3.24). If it
     * will be support, it will be similar to {@link TemplateDateFormat#parse(String, int)}.
     */
    public final Object parse(String s) throws TemplateValueFormatException {
        throw new ParsingNotSupportedException("Number formats currently don't support parsing");
    }

}
