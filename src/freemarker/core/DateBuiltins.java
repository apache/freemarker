/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
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
import freemarker.template.utility.StringUtil;
import freemarker.template.utility.UnrecognizedTimeZoneException;

/**
 * A holder for built-ins that operate exclusively on {@link TemplateDateModel}-a.
 */
abstract class DateBuiltins {
    abstract static class DateBuiltin extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateDateModel) {
                TemplateDateModel tdm = (TemplateDateModel) model;
                return calculateResult(EvaluationRules.getDate(tdm, target, env), tdm.getDateType(), env);
            } else {
                if(model == null) {
                    throw new InvalidReferenceException(target + " is undefined.", env);
                }
                throw new NonDateException(
                        target + " should be a date, time, or date+time, but it's a(n) "
                        + model.getClass().getName(), env);                
            }
        }

        /** Override this to implement the built-in. */
        protected abstract TemplateModel calculateResult(
                Date date, int dateType, Environment env)
        throws TemplateException;
        
    }

    static abstract class AbstractISOBI extends DateBuiltin {
        protected final String biName;
        protected final boolean showOffset;
        protected final int accuracy;

        protected AbstractISOBI(String biName,
                boolean showOffset, int accuracy) {
            this.biName = biName;
            this.showOffset = showOffset;
            this.accuracy = accuracy;
        }
        
        protected void checkDateTypeNotUnknown(int dateType, Environment env)
        throws TemplateException {
            if (dateType == TemplateDateModel.UNKNOWN) {
                throw new TemplateException(
                        "Unknown date type: ?" + biName + " needs a date value "
                        + "where it's known if it's a date-only, time-only, or "
                        + "date+time value. Use ?time, ?date or ?datetime "
                        + "before ? " + biName + " to estabilish that.",
                        env);
            }
        }
    }
    
    /**
     * Implements {@code ?iso_utc} and {@code ?iso_local} variants, but not
     * {@code ?iso(timeZone)}.
     */
    static class iso_tz_BI extends AbstractISOBI {
        
        private final boolean useUTC;
        
        iso_tz_BI(String biName,
                boolean showOffset, int accuracy, boolean useUTC) {
            super(biName, showOffset, accuracy);
            this.useUTC = useUTC;
        }

        protected TemplateModel calculateResult(
                Date date, int dateType, Environment env)
        throws TemplateException {
            checkDateTypeNotUnknown(dateType, env);
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
        
        iso_BI(String biName,
                boolean showOffset, int accuracy) {
            super(biName, showOffset, accuracy);
        }

        protected TemplateModel calculateResult(
                Date date, int dateType, Environment env)
        throws TemplateException {
            checkDateTypeNotUnknown(dateType, env);
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
                if (args.size() != 1) {
                    throw new TemplateModelException(
                        "?" + biName + "(...) expects exactly 1 argument, but had "
                        + args.size() + ".");
                }
                
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
                    String tzName = ((TemplateScalarModel) tzArgTM).getAsString();
                    try {
                        tzArg = DateUtil.getTimeZone(tzName);
                    } catch (UnrecognizedTimeZoneException e) {
                        throw new TemplateModelException(
                                "The time zone string specified for ?" + biName +
                                "(...) is not recognized as a valid time zone name: " +
                                StringUtil.jQuote(tzName));
                    }
 
                } else {
                    throw new TemplateModelException(
                            "The argument to ?" + biName +
                            "(...) must be a String or a " +
                            "java.util.TimeZone but it was a " +
                            (tzArgTM != null ? tzArgTM.getClass().getName() : "null") +
                            ".");
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
