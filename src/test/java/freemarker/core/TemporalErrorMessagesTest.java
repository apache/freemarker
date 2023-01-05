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

import java.time.LocalTime;
import java.time.YearMonth;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class TemporalErrorMessagesTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        return new Configuration(Configuration.VERSION_2_3_33);
    }

    @Test
    public void testExplicitFormatString() throws TemplateException {
        addToDataModel("t", LocalTime.now());
        assertErrorContains("${t?string('yyyy-HH')}", "Failed to format temporal value", "yyyy-HH", "YearOfEra");
    }

    @Test
    public void testDefaultFormatStringBadFormatString() throws TemplateException {
        getConfiguration().setSetting("year_month_format", "ABCDEF");
        addToDataModel("t", YearMonth.now());
        assertErrorContains("${t}", "year_month", "ABCDEF");
        assertErrorContains("${t?string}", "year_month", "ABCDEF");
    }

    @Test
    public void testDefaultFormatStringIncompatibleFormatString() throws TemplateException {
        getConfiguration().setSetting("year_month_format", "yyyy-mm"); // Deliberately wrong: "mm" is minutes
        addToDataModel("t", YearMonth.now());
        // TODO [FREEMARKER-35] Should contain "local_time_format" too
        assertErrorContains("${t}", "Failed to format temporal value", "yyyy-mm", "MinuteOfHour");
        assertErrorContains("${t?string}", "Failed to format temporal value", "yyyy-mm", "MinuteOfHour");
    }

}
