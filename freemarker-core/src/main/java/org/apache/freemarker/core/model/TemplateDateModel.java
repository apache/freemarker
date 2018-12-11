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

package org.apache.freemarker.core.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.freemarker.core.TemplateException;

/**
 * "date", "time" and "date-time" template language data types: corresponds to {@link java.util.Date}. Contrary to Java,
 * FreeMarker distinguishes date (no time part), time and date-time values.
 * 
 * <p>
 * Objects of this type should be immutable, that is, calling {@link #getAsDate()} and {@link #getDateType()} should
 * always return the same value as for the first time.
 */
public interface TemplateDateModel extends TemplateModel {
    
    /**
     * It is not known whether the date represents a date, a time, or a date-time value.
     * This often leads to exceptions in templates due to ambiguities it causes, so avoid it if possible.
     */
    int UNKNOWN = 0;

    /**
     * The date model represents a time value (no date part).
     */
    int TIME = 1;

    /**
     * The date model represents a date value (no time part).
     */
    int DATE = 2;

    /**
     * The date model represents a date-time value (also known as timestamp).
     */
    int DATE_TIME = 3;
    
    List TYPE_NAMES =
        Collections.unmodifiableList(
            Arrays.asList(
                    "UNKNOWN", "TIME", "DATE", "DATE_TIME"));
    /**
     * Returns the date value. The return value must not be {@code null}.
     */
    Date getAsDate() throws TemplateException;

    /**
     * Returns the type of the date. It can be any of {@link #TIME}, 
     * {@link #DATE}, or {@link #DATE_TIME}.
     */
    int getDateType();
    
}
