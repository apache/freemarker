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
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.UnrecognizedTimeZoneException;

/**
 * A holder for built-ins that operate exclusively on date left-hand values.
 */
class DateBuiltins {
    
    // Can't be instantiated
    private DateBuiltins() { }
    
    private abstract static class DateBuiltin extends BuiltIn {
        TemplateModel _eval(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateDateModel) {
                TemplateDateModel tdm = (TemplateDateModel) model;
                return calculateResult(EvalUtil.modelToDate(tdm, target), tdm.getDateType(), env);
            } else {
                if(model == null) {
                    throw InvalidReferenceException.getInstance(target, env);
                } else {
                    throw new NonDateException(target, model, "date", env);
                }
            }
        }

        /** Override this to implement the built-in. */
        protected abstract TemplateModel calculateResult(
                Date date, int dateType, Environment env)
        throws TemplateException;
        
    }

    static abstract class AbstractISOBI extends DateBuiltin {
        protected final boolean showOffset;
        protected final int accuracy;

        protected AbstractISOBI(boolean showOffset, int accuracy) {
            this.showOffset = showOffset;
            this.accuracy = accuracy;
        }
        
        protected void checkDateTypeNotUnknown(int dateType)
        throws TemplateException {
            if (dateType == TemplateDateModel.UNKNOWN) {
                throw new _MiscTemplateException(new _ErrorDescriptionBuilder(new Object[] {
                            "The value of the following has unknown date type, but ?", key,
                            " needs a date value where it's known if it's a date-only, time-only, or date+time value:"                        
                        }).blame(target).tips(MessageUtil.UNKNOWN_DATE_TYPE_ERROR_TIPS));
            }
        }
    }
    
    /**
     * Implements {@code ?iso_utc} and {@code ?iso_local} variants, but not
     * {@code ?iso(timeZone)}.
     */
    static class iso_tz_BI extends AbstractISOBI {
        
        private final boolean useUTC;
        
        iso_tz_BI(boolean showOffset, int accuracy, boolean useUTC) {
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
                    showOffset && dateType != TemplateDateModel.DATE,
                    accuracy,
                    useUTC ? DateUtil.UTC : env.getTimeZone(),
                    env.getISOBuiltInCalendar()));
        }

    }

    /**
     * Implements {@code ?iso(timeZone)}.
     */
    static class iso_BI extends AbstractISOBI {
        
        iso_BI(boolean showOffset, int accuracy) {
            super(showOffset, accuracy);
        }

        protected TemplateModel calculateResult(
                Date date, int dateType, Environment env)
        throws TemplateException {
            checkDateTypeNotUnknown(dateType);
            return new Result(date, dateType, env);
        }
        
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
                        showOffset && dateType != TemplateDateModel.DATE,
                        accuracy,
                        tzArg, 
                        env.getISOBuiltInCalendar()));
            }
            
        }
        
    }
    
}
