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

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.TimeZone;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

/**
 * See {@link ToStringTemplateTemporalFormatFactory}.
 *
 * @Deprected TODO [FREEMARKER-35] I guess we shouldn't need this.
 *
 * @since 2.3.32
 */
class ToStringTemplateTemporalFormat extends TemplateTemporalFormat {

    private final ZoneId timeZone;

    ToStringTemplateTemporalFormat(TimeZone timeZone) {
        this.timeZone = timeZone.toZoneId();
    }

    @Override
    public String format(TemplateTemporalModel temporalModel) throws TemplateValueFormatException,
            TemplateModelException {
        Temporal temporal = TemplateFormatUtil.getNonNullTemporal(temporalModel);
        // TODO [FREEMARKER-35] This is not right, but for now we mimic what TemporalUtils did
        if (temporal instanceof Instant) {
            temporal = ((Instant) temporal).atZone(timeZone);
        }
        return temporal.toString();
    }

    @Override
    public boolean isLocaleBound() {
        return false;
    }

    @Override
    public boolean isTimeZoneBound() {
        return true;
    }

    @Override
    public String getDescription() {
        return "toString()";
    }
}
