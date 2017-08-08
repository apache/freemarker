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

package org.apache.freemarker.core;

import java.util.Date;
import java.util.TimeZone;

import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.impl.SimpleDate;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.util.UnrecognizedTimeZoneException;
import org.apache.freemarker.core.util._DateUtils;

/**
 * A holder for built-ins that operate exclusively on date left-hand values.
 */
class BuiltInsForDates {
    
    static class dateType_if_unknownBI extends ASTExpBuiltIn {
        
        private final int dateType;

        dateType_if_unknownBI(int dateType) {
            this.dateType = dateType;
        }

        @Override
        TemplateModel _eval(Environment env)
                throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateDateModel) {
                TemplateDateModel tdm = (TemplateDateModel) model;
                int tdmDateType = tdm.getDateType();
                if (tdmDateType != TemplateDateModel.UNKNOWN) {
                    return tdm;
                }
                return new SimpleDate(_EvalUtils.modelToDate(tdm, target), dateType);
            } else {
                throw BuiltInForDate.newNonDateException(env, model, target);
            }
        }

    }
    
    /**
     * Implements {@code ?iso(timeZone)}.
     */
    static class iso_BI extends AbstractISOBI {
        
        class Result implements TemplateFunctionModel {
            private final Date date;
            private final int dateType;
            private final Environment env;
            
            Result(Date date, int dateType, Environment env) {
                this.date = date;
                this.dateType = dateType;
                this.env = env;
            }


            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                TemplateModel tzArgTM = args[0];
                TimeZone tzArg;
                Object adaptedObj;
                if (tzArgTM instanceof AdapterTemplateModel
                        && (adaptedObj =
                                ((AdapterTemplateModel) tzArgTM)
                                .getAdaptedObject(TimeZone.class))
                            instanceof TimeZone) {
                    tzArg = (TimeZone) adaptedObj;
                } else if (tzArgTM instanceof TemplateScalarModel) {
                    String tzName = _EvalUtils.modelToString((TemplateScalarModel) tzArgTM, null, null);
                    try {
                        tzArg = _DateUtils.getTimeZone(tzName);
                    } catch (UnrecognizedTimeZoneException e) {
                        throw new _TemplateModelException(
                                "The time zone string specified for ?", key,
                                "(...) is not recognized as a valid time zone name: ",
                                new _DelayedJQuote(tzName));
                    }
                } else {
                    throw MessageUtils.newMethodArgUnexpectedTypeException(
                            "?" + key, 0, "string or java.util.TimeZone", tzArgTM);
                }

                return new SimpleScalar(_DateUtils.dateToISO8601String(
                        date,
                        dateType != TemplateDateModel.TIME,
                        dateType != TemplateDateModel.DATE,
                        shouldShowOffset(date, dateType, env),
                        accuracy,
                        tzArg,
                        env.getISOBuiltInCalendarFactory()));
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }

        }

        iso_BI(Boolean showOffset, int accuracy) {
            super(showOffset, accuracy);
        }
        
        @Override
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

        @Override
        protected TemplateModel calculateResult(
                Date date, int dateType, Environment env)
        throws TemplateException {
            checkDateTypeNotUnknown(dateType);
            return new SimpleScalar(_DateUtils.dateToISO8601String(
                    date,
                    dateType != TemplateDateModel.TIME,
                    dateType != TemplateDateModel.DATE,
                    shouldShowOffset(date, dateType, env),
                    accuracy,
                    useUTC
                            ? _DateUtils.UTC
                            : env.shouldUseSQLDTTZ(date.getClass())
                                    ? env.getSQLDateAndTimeTimeZone()
                                    : env.getTimeZone(),
                    env.getISOBuiltInCalendarFactory()));
        }

    }
    
    // Can't be instantiated
    private BuiltInsForDates() { }

    static abstract class AbstractISOBI extends BuiltInForDate {
        final Boolean showOffset;
        final int accuracy;
    
        AbstractISOBI(Boolean showOffset, int accuracy) {
            this.showOffset = showOffset;
            this.accuracy = accuracy;
        }
        
        void checkDateTypeNotUnknown(int dateType)
        throws TemplateException {
            if (dateType == TemplateDateModel.UNKNOWN) {
                throw new _MiscTemplateException(new _ErrorDescriptionBuilder(
                            "The value of the following has unknown date type, but ?", key,
                            " needs a value where it's known if it's a date (no time part), time, or date-time value:"                        
                        ).blame(target).tip(MessageUtils.UNKNOWN_DATE_TYPE_ERROR_TIP));
            }
        }
    
        boolean shouldShowOffset(Date date, int dateType, Environment env) {
            if (dateType == TemplateDateModel.DATE) {
                return false;  // ISO 8061 doesn't allow zone for date-only values
            } else if (showOffset != null) {
                return showOffset;
            } else {
                // java.sql.Time values meant to carry calendar field values only, so we don't show offset for them.
                return !(date instanceof java.sql.Time);
            }
        }
        
    }
    
}
