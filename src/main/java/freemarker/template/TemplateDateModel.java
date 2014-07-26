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

package freemarker.template;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * "date", "time" and "date-time" template language data types: corresponds to {@link java.util.Date}. Contrary to Java,
 * FreeMarker distinguishes date (no time part), time and date-time values.
 */
public interface TemplateDateModel extends TemplateModel {
    
    /**
     * It is not known whether the date represents a date, a time, or a date-time value.
     * This often leads to exceptions in templates due to ambiguities it causes, so avoid it if possible.
     */
    public static final int UNKNOWN = 0;

    /**
     * The date model represents a time value (no date part).
     */
    public static final int TIME = 1;

    /**
     * The date model represents a date value (no time part).
     */
    public static final int DATE = 2;

    /**
     * The date model represents a date-time value (also known as timestamp).
     */
    public static final int DATETIME = 3;
    
    public static final List TYPE_NAMES =
        Collections.unmodifiableList(
            Arrays.asList(
                new String[] {
                    "UNKNOWN", "TIME", "DATE", "DATETIME"
                }));
    /**
     * Returns the date value. The return value must not be {@code null}.
     */
    public Date getAsDate() throws TemplateModelException;

    /**
     * Returns the type of the date. It can be any of {@link #TIME}, 
     * {@link #DATE}, or {@link #DATETIME}.
     */
    public int getDateType();
    
}
