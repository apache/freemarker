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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.TimeZone;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

// TODO [FREEMARKER-35] These should support parameters similar to {@link ISOTemplateDateFormat},

/**
 * See {@link ISOTemplateTemporalFormatFactory}, and {@link XSTemplateTemporalFormatFactory}.
 *
 * @since 2.3.32
 */
final class ISOLikeTemplateTemporalTemporalFormat extends TemplateTemporalFormat {
    private final DateTimeFormatter dateTimeFormatter;
    private final boolean instantConversion;
    private final ZoneId zoneId;
    private final String description;

    public ISOLikeTemplateTemporalTemporalFormat(
            DateTimeFormatter dateTimeFormatter, Class<? extends Temporal> temporalClass, TimeZone zone, String description) {
        this.dateTimeFormatter = dateTimeFormatter;
        this.instantConversion = Instant.class.isAssignableFrom(temporalClass);
        if (instantConversion) {
            zoneId = zone.toZoneId();
        } else {
            zoneId = null;
        }
        this.description = description;
    }

    @Override
    public String formatToPlainText(TemplateTemporalModel tm) throws TemplateValueFormatException,
            TemplateModelException {
        Temporal temporal = TemplateFormatUtil.getNonNullTemporal(tm);

        if (instantConversion) {
            temporal = ((Instant) temporal).atZone(zoneId);
        }

        try {
            return dateTimeFormatter.format(temporal);
        } catch (DateTimeException e) {
            throw new UnformattableValueException(e.getMessage(), e);
        }
    }

    @Override
    public Object parse(String s) throws TemplateValueFormatException {
        throw new ParsingNotSupportedException("To be implemented"); // TODO [FREEMARKER-35]
    }

    @Override
    public boolean isLocaleBound() {
        return false;
    }

    @Override
    public boolean isTimeZoneBound() {
        return zoneId != null;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
