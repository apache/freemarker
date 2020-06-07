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

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

/**
 * Represents a {@link Temporal} format; used in templates for formatting and parsing with that format. This is
 * similar to Java's {@link DateTimeFormatter}, but made to fit the requirements of FreeMarker. Also, it makes easier to
 * define formats that can't be represented with {@link DateTimeFormatter}.
 *
 * <p>
 * Implementations need not be thread-safe if the {@link TemplateTemporalFormatFactory} doesn't recycle them among
 * different {@link Environment}-s. As far as FreeMarker's concerned, instances are bound to a single
 * {@link Environment}, and {@link Environment}-s are thread-local objects.
 *
 * @since 2.3.31
 */
public abstract class TemplateTemporalFormat extends TemplateValueFormat {

    public abstract String format(TemplateTemporalModel temporalModel) throws TemplateValueFormatException, TemplateModelException;

    /**
     * Tells if this formatter should be re-created if the locale changes.
     */
    public abstract boolean isLocaleBound();

    /**
     * Tells if this formatter should be re-created if the time zone changes.
     */
    public abstract boolean isTimeZoneBound();

}
