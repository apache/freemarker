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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

abstract class BaseJavaTemplateTemporalFormatTemplateFormat extends TemplateTemporalFormat {
    private final DateTimeFormatter dateTimeFormatterWithZoneOverride;

    protected BaseJavaTemplateTemporalFormatTemplateFormat(DateTimeFormatter dateTimeFormatterWithZoneOverride) {
        this.dateTimeFormatterWithZoneOverride = dateTimeFormatterWithZoneOverride;
    }

    @Override
    public String format(TemplateTemporalModel tm)
            throws TemplateValueFormatException, TemplateModelException {
        try {
            DateTimeFormatter dateTimeFormatter = this.dateTimeFormatterWithZoneOverride;
            Temporal temporal = TemplateFormatUtil.getNonNullTemporal(tm);

            // TODO [FREEMARKER-35] Doing these on runtime is wasteful if it's know if for which format setting
            // this object is used for.
            if (temporal instanceof Instant) {
                temporal = ((Instant) temporal).atZone(dateTimeFormatter.getZone());
            } else if (temporal instanceof OffsetDateTime) {
                dateTimeFormatter = dateTimeFormatter.withZone(((OffsetDateTime) temporal).getOffset());
            } else if (temporal instanceof OffsetTime) {
                dateTimeFormatter = dateTimeFormatter.withZone(((OffsetTime) temporal).getOffset());
            } else if (temporal instanceof ZonedDateTime) {
                dateTimeFormatter = dateTimeFormatter.withZone(null);
            }

            return dateTimeFormatter.format(temporal);
        } catch (DateTimeException e) {
            throw new UnformattableValueException(e.getMessage(), e);
        }
    }
}
