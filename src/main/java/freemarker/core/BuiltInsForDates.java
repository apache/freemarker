/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.UnrecognizedTimeZoneException;

/**
 * A holder for built-ins that operate exclusively on date left-hand values.
 */
class BuiltInsForDates {
    
    static class dateType_if_unknownBI extends BuiltIn {
        
        private final int dateType;

        dateType_if_unknownBI(int dateType) {
            this.dateType = dateType;
        }

        TemplateModel _eval(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateDateModel) {
                TemplateDateModel tdm = (TemplateDateModel) model;
                int tdmDateType = tdm.getDateType();
                if (tdmDateType != TemplateDateModel.UNKNOWN) {
                    return tdm;
                }
                return new SimpleDate(EvalUtil.modelToDate(tdm, target), dateType);
            } else {
                throw BuiltInForDate.newNonDateException(env, model, target);
            }
        }

        protected TemplateModel calculateResult(Date date, int dateType, Environment env) throws TemplateException {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    /**
     * Implements {@code ?iso(timeZone)}.
     */
    static class iso_BI extends AbstractISOBI {
        
        class Result implements TemplateMethodModelEx {
            private final Date date;
            private final int dateType;
            private final Environment env;
            
            Result(Date date, int dateType, Environment env) {
                this.date = date;
                this.dateType = dateType;
                this.env = env;
            }

            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                
                TemplateModel tzArgTM = (TemplateModel) args.get(0);
                TimeZone tzArg; 
                Object adaptedObj;
                if (tzArgTM instanceof AdapterTemplateModel
                        && (adaptedObj =
                                ((AdapterTemplateModel) tzArgTM)
                                .getAdaptedObject(TimeZone.class))
                            instanceof TimeZone) {
                    tzArg = (TimeZone) adaptedObj;                    
                } else if (tzArgTM instanceof TemplateScalarModel) {
                    String tzName = EvalUtil.modelToString((TemplateScalarModel) tzArgTM, null, null);
                    try {
                        tzArg = DateUtil.getTimeZone(tzName);
                    } catch (UnrecognizedTimeZoneException e) {
                        throw new _TemplateModelException(new Object[] {
                                "The time zone string specified for ?", key,
                                "(...) is not recognized as a valid time zone name: ",
                                new _DelayedJQuote(tzName) });
                    }
                } else {
                    throw MessageUtil.newMethodArgUnexpectedTypeException(
                            "?" + key, 0, "string or java.util.TimeZone", tzArgTM);
                }
                
                return new SimpleScalar(DateUtil.dateToISO8601String(
                        date,
                        dateType != TemplateDateModel.TIME,
                        dateType != TemplateDateModel.DATE,
                        shouldShowOffset(date, dateType, env),
                        accuracy,
                        tzArg, 
                        env.getISOBuiltInCalendarFactory()));
            }
            
        }

        iso_BI(Boolean showOffset, int accuracy) {
            super(showOffset, accuracy);
        }
        
        protected TemplateModel calculateResult(
                Date date, int dateType, Environment env)
        throws TemplateException {
            checkDateTypeNotUnknown(dateType);
            return new Result(date, dateType, env);
        }
        
    }

    /**
     * Implements {@code ?iso_utc} and {@code ?iso_local} variants, but not
     * {@code ?iso(timeZone)}.
     */
    static class iso_utc_or_local_BI extends AbstractISOBI {
        
        private final boolean useUTC;
        
        iso_utc_or_local_BI(Boolean showOffset, int accuracy, boolean useUTC) {
            super(showOffset, accuracy);
            this.useUTC = useUTC;
        }

        protected TemplateModel calculateResult(
                Date date, int dateType, Environment env)
        throws TemplateException {
            checkDateTypeNotUnknown(dateType);
            return new SimpleScalar(DateUtil.dateToISO8601String(
                    date,
                    dateType != TemplateDateModel.TIME,
                    dateType != TemplateDateModel.DATE,
                    shouldShowOffset(date, dateType, env),
                    accuracy,
                    useUTC
                            ? DateUtil.UTC
                            : env.shouldUseSQLDTTZ(date.getClass())
                                    ? env.getSQLDateAndTimeTimeZone()
                                    : env.getTimeZone(),
                    env.getISOBuiltInCalendarFactory()));
        }

    }
    
    // Can't be instantiated
    private BuiltInsForDates() { }

    static abstract class AbstractISOBI extends BuiltInForDate {
        protected final Boolean showOffset;
        protected final int accuracy;
    
        protected AbstractISOBI(Boolean showOffset, int accuracy) {
            this.showOffset = showOffset;
            this.accuracy = accuracy;
        }
        
        protected void checkDateTypeNotUnknown(int dateType)
        throws TemplateException {
            if (dateType == TemplateDateModel.UNKNOWN) {
                throw new _MiscTemplateException(new _ErrorDescriptionBuilder(new Object[] {
                            "The value of the following has unknown date type, but ?", key,
                            " needs a value where it's known if it's a date (no time part), time, or date-time value:"                        
                        }).blame(target).tip(MessageUtil.UNKNOWN_DATE_TYPE_ERROR_TIP));
            }
        }
    
        protected boolean shouldShowOffset(Date date, int dateType, Environment env) {
            if (dateType == TemplateDateModel.DATE) {
                return false;  // ISO 8061 doesn't allow zone for date-only values
            } else if (this.showOffset != null) {
                return this.showOffset.booleanValue();
            } else {
                // java.sql.Time values meant to carry calendar field values only, so we don't show offset for them.
                return !(date instanceof java.sql.Time
                        && _TemplateAPI.getTemplateLanguageVersionAsInt(this) >= _TemplateAPI.VERSION_INT_2_3_21);
            }
        }
        
    }
    
}
