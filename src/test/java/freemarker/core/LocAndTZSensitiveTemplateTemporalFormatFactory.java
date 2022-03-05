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
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

public class LocAndTZSensitiveTemplateTemporalFormatFactory extends TemplateTemporalFormatFactory {

    public static final LocAndTZSensitiveTemplateTemporalFormatFactory
            INSTANCE = new LocAndTZSensitiveTemplateTemporalFormatFactory();
    
    private LocAndTZSensitiveTemplateTemporalFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public TemplateTemporalFormat get(
            String params, Class<? extends Temporal> temporalClass,
            Locale locale, TimeZone timeZone,
            Environment env)
            throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        TemplateFormatUtil.checkHasNoParameters(params);
        return new LocAndTZSensitiveTemplateTemporalFormat(locale, timeZone);
    }

    private static class LocAndTZSensitiveTemplateTemporalFormat extends TemplateTemporalFormat {

        private final Locale locale;
        private final TimeZone timeZone;
        
        public LocAndTZSensitiveTemplateTemporalFormat(Locale locale, TimeZone timeZone) {
            this.locale = locale;
            this.timeZone = timeZone;
        }

        @Override
        public String formatToPlainText(TemplateTemporalModel temporalModel)
                throws UnformattableValueException, TemplateModelException {
            Temporal temporal = TemplateFormatUtil.getNonNullTemporal(temporalModel);
            long epochMillis;
            try {
                epochMillis = temporal.query(Instant::from).toEpochMilli();
            } catch (Exception e) {
                throw new UnformattableValueException("Can't extract epoch millis from " + temporal.getClass().getName() + " object.");
            }
            return epochMillis + "@" + locale + ":" + timeZone.getID();
        }

        @Override
        public Object parse(String s, MissingTimeZoneParserPolicy missingTimeZoneParserPolicy) throws TemplateValueFormatException {
            throw new ParsingNotSupportedException("Parsing is not implement for this test class");
        }

        @Override
        public boolean isLocaleBound() {
            return true;
        }

        @Override
        public boolean isTimeZoneBound() {
            return true;
        }

        @Override
        public String getDescription() {
            return "millis since the epoch";
        }
        
    }

}
