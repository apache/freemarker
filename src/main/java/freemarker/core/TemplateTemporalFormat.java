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

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

/**
 * Represents a {@link Temporal} format; used in templates for formatting and parsing with that format. This is
 * similar to Java's {@link DateTimeFormatter}, but made to fit the requirements of FreeMarker. Also, it makes easier to
 * define formats that can't be represented with {@link DateTimeFormatter}.
 *
 * <p>
 * Implementations need not be thread-safe if the {@link TemplateTemporalFormatFactory} doesn't recycle them among
 * different {@link Environment}-s. The code outside the {@link TemplateTemporalFormatFactory} will not try to reuse
 * {@link TemplateTemporalFormat} instances in multiple {@link Environment}-s, and {@link Environment}-s are
 * thread-local objects.
 *
 * @since 2.3.32
 */
public abstract class TemplateTemporalFormat extends TemplateValueFormat {

    public abstract String formatToPlainText(TemplateTemporalModel temporalModel) throws TemplateValueFormatException, TemplateModelException;

    /**
     * Formats the model to markup instead of to plain text if the result markup will be more than just plain text
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
     * Tells if the same formatter can be used regardless of the desired locale (so for example after a
     * {@link Environment#getLocale()} change we can keep using the old instance).
     */
    public abstract boolean isLocaleBound();

    /**
     * Tells if the same formatter can be used regardless of the desired time zone (so for example after a
     * {@link Environment#getTimeZone()} change we can keep using the old instance).
     */
    public abstract boolean isTimeZoneBound();

    // TODO [FREEMARKER-35] Add parse method

}
