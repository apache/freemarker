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

import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTemporalModel;

public class HTMLISOTemplateTemporalFormatFactory extends TemplateTemporalFormatFactory {

    public static final HTMLISOTemplateTemporalFormatFactory INSTANCE = new HTMLISOTemplateTemporalFormatFactory();

    private HTMLISOTemplateTemporalFormatFactory() {
        // Defined to decrease visibility
    }

    @Override
    public TemplateTemporalFormat get(
            String params,
            Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone,
            Environment env)
            throws TemplateValueFormatException {
        TemplateFormatUtil.checkHasNoParameters(params);
        TemplateTemporalFormat isoFormat = ISOTemplateTemporalFormatFactory.INSTANCE
                .get("", temporalClass, locale, timeZone, env);
        return new HTMLISOTemplateTemporalFormat(isoFormat);
    }

    private static class HTMLISOTemplateTemporalFormat extends TemplateTemporalFormat {

        private final TemplateTemporalFormat isoFormat;

        private HTMLISOTemplateTemporalFormat(TemplateTemporalFormat isoFormat) {
            this.isoFormat = isoFormat;
        }
        
        @Override
        public String formatToPlainText(TemplateTemporalModel temporalModel)
                throws TemplateValueFormatException, TemplateModelException {
            return isoFormat.formatToPlainText(temporalModel);
        }

        @Override
        public Object parse(String s, MissingTimeZoneParserPolicy missingTimeZoneParserPolicy) throws TemplateValueFormatException {
            throw new ParsingNotSupportedException("Parsing is not implement for this test class");
        }

        @Override
        public boolean canBeUsedForLocale(Locale locale) {
            return isoFormat.canBeUsedForLocale(locale);
        }

        @Override
        public boolean canBeUsedForTimeZone(TimeZone timeZone) {
            return isoFormat.canBeUsedForTimeZone(timeZone);
        }

        @Override
        public Object format(TemplateTemporalModel temporalModel) throws TemplateValueFormatException, TemplateModelException {
            return HTMLOutputFormat.INSTANCE.fromMarkup(
                    formatToPlainText(temporalModel).replace("T", "<span class='T'>T</span>"));
        }

        @Override
        public String getDescription() {
            return "ISO UTC HTML";
        }
        
    }

}
 