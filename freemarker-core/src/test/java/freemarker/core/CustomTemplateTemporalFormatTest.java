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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.test.TemplateTest;

/**
 * Like {@link JavaTemplateTemporalFormatTest}, but this one contains the tests that utilize {@link TemplateTest}.
 */
public class CustomTemplateTemporalFormatTest extends TemplateTest {

    @Before
    public void setup() {
        Configuration cfg = getConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_33);
        cfg.setLocale(Locale.US);
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+01:00"));

        cfg.setCustomTemporalFormats(ImmutableMap.of(
                "epoch", EpochMillisTemplateTemporalFormatFactory.INSTANCE,
                "loc", LocAndTZSensitiveTemplateTemporalFormatFactory.INSTANCE,
                "div", EpochMillisDivTemplateTemporalFormatFactory.INSTANCE,
                "htmlIso", HTMLISOTemplateTemporalFormatFactory.INSTANCE));
    }

    @Test
    public void testCustomFormat() throws Exception {
        addToDataModel("d", OffsetDateTime.of(
                1970, 1, 2,
                10, 17, 36, 789000000,
                ZoneOffset.ofHours(0)));
        assertOutput(
                "${d?string.@epoch} ${d?string.@epoch} <#setting locale='de_DE'>${d?string.@epoch}",
                "123456789 123456789 123456789");

        getConfiguration().setDateTimeFormat("@epoch");
        assertOutput(
                "${d} ${d?string} <#setting locale='de_DE'>${d}",
                "123456789 123456789 123456789");

        getConfiguration().setDateTimeFormat("@htmlIso");
        assertOutput(
                "${d} ${d?string} <#setting locale='de_DE'>${d}",
                "1970-01-02<span class='T'>T</span>10:17:36.789Z "
                        + "1970-01-02T10:17:36.789Z "
                        + "1970-01-02<span class='T'>T</span>10:17:36.789Z");
    }
}
