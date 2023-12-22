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

import freemarker.template.utility.DateUtil.CalendarFieldsToDateConverter;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;
import freemarker.template.utility.DateUtil.TrivialCalendarFieldsToDateConverter;
import freemarker.template.utility.DateUtil.TrivialDateToISO8601CalendarFactory;

abstract class ISOLikeTemplateDateFormatFactory extends TemplateDateFormatFactory {
    
    private static final Object DATE_TO_CAL_CONVERTER_KEY = new Object();
    private static final Object CAL_TO_DATE_CONVERTER_KEY = new Object();
    
    protected ISOLikeTemplateDateFormatFactory() { }

    public DateToISO8601CalendarFactory getISOBuiltInCalendar(Environment env) {
        DateToISO8601CalendarFactory r = (DateToISO8601CalendarFactory) env.getCustomState(DATE_TO_CAL_CONVERTER_KEY);
        if (r == null) {
            r = new TrivialDateToISO8601CalendarFactory();
            env.setCustomState(DATE_TO_CAL_CONVERTER_KEY, r);
        }
        return r;
    }

    public CalendarFieldsToDateConverter getCalendarFieldsToDateCalculator(Environment env) {
        CalendarFieldsToDateConverter r = (CalendarFieldsToDateConverter) env.getCustomState(CAL_TO_DATE_CONVERTER_KEY);
        if (r == null) {
            r = new TrivialCalendarFieldsToDateConverter();
            env.setCustomState(CAL_TO_DATE_CONVERTER_KEY, r);
        }
        return r;
    }
    
}
