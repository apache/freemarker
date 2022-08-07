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

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.NullWriter;

public abstract class TemplateTemporalFormatAbstractCachingInEnvironmentTest {
    protected static final TimeZone OTHER_TIME_ZONE = TimeZone.getTimeZone("GMT+01");

    protected final Environment env;

    public TemplateTemporalFormatAbstractCachingInEnvironmentTest() {
        try {
            this.env = createEnvironment();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Environment createEnvironment() throws TemplateException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setLocale(Locale.US);
        cfg.setTimeZone(DateUtil.UTC);

        return new Template(null, "", cfg)
                .createProcessingEnvironment(null, NullWriter.INSTANCE);
    }

    static final class SettingAssignments {
        private final Class<? extends Temporal> temporalClass;
        private final String[] values;

        public SettingAssignments(Class<? extends Temporal> temporalClass, String[] values) {
            this.temporalClass = temporalClass;
            this.values = values;
        }

        public void execute(Configurable configurable, int valueIndex) {
            String value = values[valueIndex];
            if (temporalClass == Instant.class
                    || temporalClass == LocalDateTime.class
                    || temporalClass == ZonedDateTime.class
                    || temporalClass == OffsetDateTime.class) {
                configurable.setDateTimeFormat(value);
            } else if (temporalClass == LocalDate.class) {
                configurable.setDateFormat(value);
            } else if (temporalClass == LocalTime.class || temporalClass == OffsetTime.class) {
                configurable.setTimeFormat(value);
            } else if (temporalClass == Year.class) {
                configurable.setYearFormat(value);
            } else if (temporalClass == YearMonth.class) {
                configurable.setYearMonthFormat(value);
            } else {
                throw new AssertionError();
            }
        }

        public String getValue(int valueIndex) {
            return values[valueIndex];
        }

        public int numberOfValues() {
            return values.length;
        }

        public Class<? extends Temporal> getTemporalClass() {
            return temporalClass;
        }
    }

    @FunctionalInterface
    interface SettingSetter {
        void execute(Configurable configurable, String value);
    }
}
