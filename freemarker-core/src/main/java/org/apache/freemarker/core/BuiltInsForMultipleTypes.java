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
import java.util.List;

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateCollectionModelEx;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateMethodModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateModelWithAPISupport;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleDate;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;
import org.apache.freemarker.core.valueformat.TemplateValueFormatException;

/**
 * A holder for builtins that didn't fit into any other category.
 */
class BuiltInsForMultipleTypes {

    static class cBI extends AbstractCBI {
        
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateNumberModel) {
                return formatNumber(env, model);
            } else if (model instanceof TemplateBooleanModel) {
                return new SimpleScalar(((TemplateBooleanModel) model).getAsBoolean()
                        ? MiscUtil.C_TRUE : MiscUtil.C_FALSE);
            } else {
                throw new UnexpectedTypeException(
                        target, model,
                        "number or boolean", new Class[] { TemplateNumberModel.class, TemplateBooleanModel.class },
                        env);
            }
        }

        @Override
        protected TemplateModel formatNumber(Environment env, TemplateModel model) throws TemplateModelException {
            Number num = _EvalUtil.modelToNumber((TemplateNumberModel) model, target);
            if (num instanceof Integer || num instanceof Long) {
                // Accelerate these fairly common cases
                return new SimpleScalar(num.toString());
            } else if (num instanceof Double) {
                double n = num.doubleValue();
                if (n == Double.POSITIVE_INFINITY) {
                    return new SimpleScalar("INF");
                }
                if (n == Double.NEGATIVE_INFINITY) {
                    return new SimpleScalar("-INF");
                }
                if (Double.isNaN(n)) {
                    return new SimpleScalar("NaN");
                }
                // Deliberately falls through
            } else if (num instanceof Float) {
                float n = num.floatValue();
                if (n == Float.POSITIVE_INFINITY) {
                    return new SimpleScalar("INF");
                }
                if (n == Float.NEGATIVE_INFINITY) {
                    return new SimpleScalar("-INF");
                }
                if (Float.isNaN(n)) {
                    return new SimpleScalar("NaN");
                }
                // Deliberately falls through
            }
        
            return new SimpleScalar(env.getCNumberFormat().format(num));
        }
        
    }

    static class dateBI extends ASTExpBuiltIn {
        private class DateParser
        implements
            TemplateDateModel,
            TemplateMethodModel,
            TemplateHashModel {
            private final String text;
            private final Environment env;
            private final TemplateDateFormat defaultFormat;
            private TemplateDateModel cachedValue;
            
            DateParser(String text, Environment env)
            throws TemplateException {
                this.text = text;
                this.env = env;
                defaultFormat = env.getTemplateDateFormat(dateType, Date.class, target, false);
            }
            
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 0, 1);
                return args.size() == 0 ? getAsDateModel() : get((String) args.get(0));
            }
            
            @Override
            public TemplateModel get(String pattern) throws TemplateModelException {
                TemplateDateFormat format;
                try {
                    format = env.getTemplateDateFormat(pattern, dateType, Date.class, target, dateBI.this, true);
                } catch (TemplateException e) {
                    // `e` should always be a TemplateModelException here, but to be sure: 
                    throw _CoreAPI.ensureIsTemplateModelException("Failed to get format", e); 
                }
                return toTemplateDateModel(parse(format));
            }

            private TemplateDateModel toTemplateDateModel(Object date) throws _TemplateModelException {
                if (date instanceof Date) {
                    return new SimpleDate((Date) date, dateType);
                } else {
                    TemplateDateModel tm = (TemplateDateModel) date;
                    if (tm.getDateType() != dateType) {
                        throw new _TemplateModelException("The result of the parsing was of the wrong date type.");
                    }
                    return tm;
                }
            }

            private TemplateDateModel getAsDateModel() throws TemplateModelException {
                if (cachedValue == null) {
                    cachedValue = toTemplateDateModel(parse(defaultFormat));
                }
                return cachedValue;
            }
            
            @Override
            public Date getAsDate() throws TemplateModelException {
                return getAsDateModel().getAsDate();
            }
    
            @Override
            public int getDateType() {
                return dateType;
            }
    
            @Override
            public boolean isEmpty() {
                return false;
            }
    
            private Object parse(TemplateDateFormat df)
            throws TemplateModelException {
                try {
                    return df.parse(text, dateType);
                } catch (TemplateValueFormatException e) {
                    throw new _TemplateModelException(e,
                            "The string doesn't match the expected date/time/date-time format. "
                            + "The string to parse was: ", new _DelayedJQuote(text), ". ",
                            "The expected format was: ", new _DelayedJQuote(df.getDescription()), ".",
                            e.getMessage() != null ? "\nThe nested reason given follows:\n" : "",
                            e.getMessage() != null ? e.getMessage() : "");
                }
            }
            
        }
        
        private final int dateType;
        
        dateBI(int dateType) {
            this.dateType = dateType;
        }
        
        @Override
        TemplateModel _eval(Environment env)
                throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateDateModel) {
                TemplateDateModel dmodel = (TemplateDateModel) model;
                int dtype = dmodel.getDateType();
                // Any date model can be coerced into its own type
                if (dateType == dtype) {
                    return model;
                }
                // unknown and dateTime can be coerced into any date type
                if (dtype == TemplateDateModel.UNKNOWN || dtype == TemplateDateModel.DATE_TIME) {
                    return new SimpleDate(dmodel.getAsDate(), dateType);
                }
                throw new _MiscTemplateException(this,
                            "Cannot convert ", TemplateDateModel.TYPE_NAMES.get(dtype),
                            " to ", TemplateDateModel.TYPE_NAMES.get(dateType));
            }
            // Otherwise, interpret as a string and attempt 
            // to parse it into a date.
            String s = target.evalAndCoerceToPlainText(env);
            return new DateParser(s, env);
        }

    }

    static class apiBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            if (!env.getAPIBuiltinEnabled()) {
                throw new _MiscTemplateException(this,
                        "Can't use ?api, because the \"", MutableProcessingConfiguration.API_BUILTIN_ENABLED_KEY,
                        "\" configuration setting is false. Think twice before you set it to true though. Especially, "
                        + "it shouldn't abused for modifying Map-s and Collection-s.");
            }
            final TemplateModel tm = target.eval(env);
            if (!(tm instanceof TemplateModelWithAPISupport)) {
                target.assertNonNull(tm, env);
                throw new APINotSupportedTemplateException(env, target, tm);
            }
            return ((TemplateModelWithAPISupport) tm).getAPI();
        }
    }

    static class has_apiBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            final TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return tm instanceof TemplateModelWithAPISupport ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }
    
    static class is_booleanBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateBooleanModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_collectionBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateCollectionModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_collection_exBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateCollectionModelEx) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_dateLikeBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateDateModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_dateOfTypeBI extends ASTExpBuiltIn {
        
        private final int dateType;
        
        is_dateOfTypeBI(int dateType) {
            this.dateType = dateType;
        }

        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateDateModel) && ((TemplateDateModel) tm).getDateType() == dateType
                ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_directiveBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            // WRONG: it also had to check ASTDirMacroOrFunction.isFunction()
            return (tm instanceof ASTDirMacroOrFunction || tm instanceof TemplateDirectiveModel) ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_enumerableBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel || tm instanceof TemplateCollectionModel)
                    ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_hash_exBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateHashModelEx) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_hashBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateHashModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_indexableBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_macroBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof Environment.TemplateLanguageDirective)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_markup_outputBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateMarkupOutputModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }
    
    static class is_methodBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateMethodModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_nodeBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateNodeModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_numberBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateNumberModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_sequenceBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return tm instanceof TemplateSequenceModel
                    ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_stringBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateScalarModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class namespaceBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            if (!(tm instanceof Environment.TemplateLanguageCallable)) {
                throw new UnexpectedTypeException(
                        target, tm,
                        "macro or function", new Class[] { Environment.TemplateLanguageCallable.class },
                        env);
            }
            return ((Environment.TemplateLanguageCallable) tm).getNamespace();
        }
    }

    static class sizeBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);

            final int size;
            if (model instanceof TemplateSequenceModel) {
                size = ((TemplateSequenceModel) model).size();
            } else if (model instanceof TemplateCollectionModelEx) {
                size = ((TemplateCollectionModelEx) model).size();
            } else if (model instanceof TemplateHashModelEx) {
                size = ((TemplateHashModelEx) model).size();
            } else {
                throw new UnexpectedTypeException(
                        target, model,
                        "extended-hash or sequence or extended collection",
                        new Class[] {
                                TemplateHashModelEx.class,
                                TemplateSequenceModel.class,
                                TemplateCollectionModelEx.class
                        },
                        env);
            }
            return new SimpleNumber(size);
        }
    }
    
    static class stringBI extends ASTExpBuiltIn {
        
        private class BooleanFormatter
        implements 
            TemplateScalarModel, 
            TemplateMethodModel {
            private final TemplateBooleanModel bool;
            private final Environment env;
            
            BooleanFormatter(TemplateBooleanModel bool, Environment env) {
                this.bool = bool;
                this.env = env;
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 2);
                return new SimpleScalar((String) args.get(bool.getAsBoolean() ? 0 : 1));
            }
    
            @Override
            public String getAsString() throws TemplateModelException {
                // Boolean should have come first... but that change would be non-BC. 
                if (bool instanceof TemplateScalarModel) {
                    return ((TemplateScalarModel) bool).getAsString();
                } else {
                    try {
                        return env.formatBoolean(bool.getAsBoolean(), true);
                    } catch (TemplateException e) {
                        throw new TemplateModelException(e);
                    }
                }
            }
        }
    
        private class DateFormatter
        implements
            TemplateScalarModel,
            TemplateHashModel,
            TemplateMethodModel {
            private final TemplateDateModel dateModel;
            private final Environment env;
            private final TemplateDateFormat defaultFormat;
            private String cachedValue;
    
            DateFormatter(TemplateDateModel dateModel, Environment env)
            throws TemplateException {
                this.dateModel = dateModel;
                this.env = env;
                
                final int dateType = dateModel.getDateType();
                defaultFormat = dateType == TemplateDateModel.UNKNOWN
                        ? null  // Lazy unknown type error in getAsString()
                        : env.getTemplateDateFormat(
                                dateType, _EvalUtil.modelToDate(dateModel, target).getClass(), target, true);
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return formatWith((String) args.get(0));
            }

            @Override
            public TemplateModel get(String key)
            throws TemplateModelException {
                return formatWith(key);
            }

            private TemplateModel formatWith(String key)
            throws TemplateModelException {
                try {
                    return new SimpleScalar(env.formatDateToPlainText(dateModel, key, target, stringBI.this, true));
                } catch (TemplateException e) {
                    // `e` should always be a TemplateModelException here, but to be sure: 
                    throw _CoreAPI.ensureIsTemplateModelException("Failed to format value", e); 
                }
            }
            
            @Override
            public String getAsString()
            throws TemplateModelException {
                if (cachedValue == null) {
                    if (defaultFormat == null) {
                        if (dateModel.getDateType() == TemplateDateModel.UNKNOWN) {
                            throw MessageUtil.newCantFormatUnknownTypeDateException(target, null);
                        } else {
                            throw new BugException();
                        }
                    }
                    try {
                        cachedValue = _EvalUtil.assertFormatResultNotNull(defaultFormat.formatToPlainText(dateModel));
                    } catch (TemplateValueFormatException e) {
                        try {
                            throw MessageUtil.newCantFormatDateException(defaultFormat, target, e, true);
                        } catch (TemplateException e2) {
                            // `e` should always be a TemplateModelException here, but to be sure: 
                            throw _CoreAPI.ensureIsTemplateModelException("Failed to format date/time/dateTime", e2);
                        }
                    }
                }
                return cachedValue;
            }
    
            @Override
            public boolean isEmpty() {
                return false;
            }
        }
        
        private class NumberFormatter
        implements
            TemplateScalarModel,
            TemplateHashModel,
            TemplateMethodModel {
            private final TemplateNumberModel numberModel;
            private final Number number;
            private final Environment env;
            private final TemplateNumberFormat defaultFormat;
            private String cachedValue;
    
            NumberFormatter(TemplateNumberModel numberModel, Environment env) throws TemplateException {
                this.env = env;
                
                // As we format lazily, we need a snapshot of the format inputs:
                this.numberModel = numberModel;
                number = _EvalUtil.modelToNumber(numberModel, target);  // for BackwardCompatibleTemplateNumberFormat-s
                try {
                    defaultFormat = env.getTemplateNumberFormat(stringBI.this, true);
                } catch (TemplateException e) {
                    // `e` should always be a TemplateModelException here, but to be sure: 
                    throw _CoreAPI.ensureIsTemplateModelException("Failed to get default number format", e); 
                }
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return get((String) args.get(0));
            }
    
            @Override
            public TemplateModel get(String key) throws TemplateModelException {
                TemplateNumberFormat format;
                try {
                    format = env.getTemplateNumberFormat(key, stringBI.this, true);
                } catch (TemplateException e) {
                    // `e` should always be a TemplateModelException here, but to be sure: 
                    throw _CoreAPI.ensureIsTemplateModelException("Failed to get number format", e); 
                }
                
                String result;
                try {
                    result = env.formatNumberToPlainText(numberModel, format, target, true);
                } catch (TemplateException e) {
                    // `e` should always be a TemplateModelException here, but to be sure: 
                    throw _CoreAPI.ensureIsTemplateModelException("Failed to format number", e); 
                }
                
                return new SimpleScalar(result);
            }
            
            @Override
            public String getAsString() throws TemplateModelException {
                if (cachedValue == null) {
                    try {
                        cachedValue = env.formatNumberToPlainText(numberModel, defaultFormat, target, true);
                    } catch (TemplateException e) {
                        // `e` should always be a TemplateModelException here, but to be sure: 
                        throw _CoreAPI.ensureIsTemplateModelException("Failed to format number", e); 
                    }
                }
                return cachedValue;
            }
    
            @Override
            public boolean isEmpty() {
                return false;
            }
        }
    
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateNumberModel) {
                TemplateNumberModel numberModel = (TemplateNumberModel) model;
                Number num = _EvalUtil.modelToNumber(numberModel, target);
                return new NumberFormatter(numberModel, env);
            } else if (model instanceof TemplateDateModel) {
                TemplateDateModel dm = (TemplateDateModel) model;
                return new DateFormatter(dm, env);
            } else if (model instanceof SimpleScalar) {
                return model;
            } else if (model instanceof TemplateBooleanModel) {
                return new BooleanFormatter((TemplateBooleanModel) model, env);
            } else if (model instanceof TemplateScalarModel) {
                return new SimpleScalar(((TemplateScalarModel) model).getAsString());
            } else {            
                throw new UnexpectedTypeException(
                        target, model,
                        "number, date, boolean or string",
                        new Class[] {
                            TemplateNumberModel.class, TemplateDateModel.class, TemplateBooleanModel.class,
                            TemplateScalarModel.class
                        },
                        env);
            }
        }
    }

    // Can't be instantiated
    private BuiltInsForMultipleTypes() { }

    static abstract class AbstractCBI extends ASTExpBuiltIn {
        
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateNumberModel) {
                return formatNumber(env, model);
            } else if (model instanceof TemplateBooleanModel) {
                return new SimpleScalar(((TemplateBooleanModel) model).getAsBoolean()
                        ? MiscUtil.C_TRUE : MiscUtil.C_FALSE);
            } else {
                throw new UnexpectedTypeException(
                        target, model,
                        "number or boolean", new Class[] { TemplateNumberModel.class, TemplateBooleanModel.class },
                        env);
            }
        }
    
        protected abstract TemplateModel formatNumber(Environment env, TemplateModel model) throws TemplateModelException;
        
    }

}
