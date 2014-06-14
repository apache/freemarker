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
 * "date" template language data type: similar to {@link java.util.Date}; a time-zone-independent date-only, time-only
 * or date-time value. Contrary to Java, FreeMarker distinguishes values that represent only a time, only a date, or a
 * combined date and time.
 *
 * @author Attila Szegedi
 */
public interface TemplateDateModel extends TemplateModel {
    
    /**
     * It is not known whether the date represents a time-only, a date-only, or a date-time value.
     * This often leads to exceptions in templates due to ambiguities it causes, so avoid it if possible.
     */
    public static final int UNKNOWN = 0;

    /**
     * The date model represents a time-only value.
     */
    public static final int TIME = 1;

    /**
     * The date model represents a date-only value.
     */
    public static final int DATE = 2;

    /**
     * The date model represents a date-time value.
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
     * Returns the type of the date. It can be any of <tt>TIME</tt>, 
     * <tt>DATE</tt>, or <tt>DATETIME</tt>.
     */
    public int getDateType();
    
}
