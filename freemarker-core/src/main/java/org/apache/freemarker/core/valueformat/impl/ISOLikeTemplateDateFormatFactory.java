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

package org.apache.freemarker.core.valueformat.impl;

import org.apache.freemarker.core.CustomStateKey;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.util._DateUtils.CalendarFieldsToDateConverter;
import org.apache.freemarker.core.util._DateUtils.DateToISO8601CalendarFactory;
import org.apache.freemarker.core.util._DateUtils.TrivialCalendarFieldsToDateConverter;
import org.apache.freemarker.core.util._DateUtils.TrivialDateToISO8601CalendarFactory;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;

abstract class ISOLikeTemplateDateFormatFactory extends TemplateDateFormatFactory {
    
    private static final CustomStateKey<TrivialDateToISO8601CalendarFactory> DATE_TO_CAL_CONVERTER_KEY
            = new CustomStateKey<TrivialDateToISO8601CalendarFactory>() {
        @Override
        protected TrivialDateToISO8601CalendarFactory create() {
            return new TrivialDateToISO8601CalendarFactory();
        }
    };
    private static final CustomStateKey<TrivialCalendarFieldsToDateConverter> CAL_TO_DATE_CONVERTER_KEY
            = new CustomStateKey<TrivialCalendarFieldsToDateConverter>() {
        @Override
        protected TrivialCalendarFieldsToDateConverter create() {
            return new TrivialCalendarFieldsToDateConverter();
        }
    };
    
    protected ISOLikeTemplateDateFormatFactory() { }

    public DateToISO8601CalendarFactory getISOBuiltInCalendar(Environment env) {
        return env.getCustomState(DATE_TO_CAL_CONVERTER_KEY);
    }

    public CalendarFieldsToDateConverter getCalendarFieldsToDateCalculator(Environment env) {
        return env.getCustomState(CAL_TO_DATE_CONVERTER_KEY);
    }
    
}
