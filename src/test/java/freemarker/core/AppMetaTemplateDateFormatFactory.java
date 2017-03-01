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

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;

public class AppMetaTemplateDateFormatFactory extends TemplateDateFormatFactory {

    public static final AppMetaTemplateDateFormatFactory INSTANCE = new AppMetaTemplateDateFormatFactory();
    
    private AppMetaTemplateDateFormatFactory() {
        // Defined to decrease visibility
    }
    
    @Override
    public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput,
            Environment env) throws UnknownDateTypeFormattingUnsupportedException, InvalidFormatParametersException {
        TemplateFormatUtil.checkHasNoParameters(params);
        return AppMetaTemplateDateFormat.INSTANCE;
    }

    private static class AppMetaTemplateDateFormat extends TemplateDateFormat {

        private static final AppMetaTemplateDateFormat INSTANCE = new AppMetaTemplateDateFormat();
        
        private AppMetaTemplateDateFormat() { }
        
        @Override
        public String formatToPlainText(TemplateDateModel dateModel)
                throws UnformattableValueException, TemplateModelException {
            String result = String.valueOf(TemplateFormatUtil.getNonNullDate(dateModel).getTime());
            if (dateModel instanceof AppMetaTemplateDateModel) {
                result += "/" + ((AppMetaTemplateDateModel) dateModel).getAppMeta(); 
            }
            return result;
        }

        @Override
        public boolean isLocaleBound() {
            return false;
        }

        @Override
        public boolean isTimeZoneBound() {
            return false;
        }

        @Override
        public Object parse(String s, int dateType) throws UnparsableValueException {
            int slashIdx = s.indexOf('/');
            try {
                if (slashIdx != -1) {
                    return new AppMetaTemplateDateModel(
                            new Date(Long.parseLong(s.substring(0,  slashIdx))),
                            dateType,
                            s.substring(slashIdx +1));
                } else {
                    return new Date(Long.parseLong(s));
                }
            } catch (NumberFormatException e) {
                throw new UnparsableValueException("Malformed long");
            }
        }

        @Override
        public String getDescription() {
            return "millis since the epoch";
        }
        
    }
    
    public static class AppMetaTemplateDateModel implements TemplateDateModel {
        
        private final Date date;
        private final int dateType;
        private final String appMeta;

        public AppMetaTemplateDateModel(Date date, int dateType, String appMeta) {
            this.date = date;
            this.dateType = dateType;
            this.appMeta = appMeta;
        }

        public Date getAsDate() throws TemplateModelException {
            return date;
        }

        public int getDateType() {
            return dateType;
        }

        public String getAppMeta() {
            return appMeta;
        }
        
    }

}
