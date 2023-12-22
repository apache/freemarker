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
import java.util.List;

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.OverloadedMethodsModel;
import freemarker.ext.beans.SimpleMethodModel;
import freemarker.ext.beans._BeansAPI;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateCollectionModelEx;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateModelWithAPISupport;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateTransformModel;
import freemarker.template._TemplateAPI;
import freemarker.template._VersionInts;
import freemarker.template.utility.NumberUtil;

/**
 * A holder for builtins that didn't fit into any other category.
 */
class BuiltInsForMultipleTypes {

    static class cBI extends AbstractCLikeBI {
        final protected String formatNull(Environment env) throws InvalidReferenceException {
            throw InvalidReferenceException.getInstance(target, env);
        }
    }

    static class cnBI extends AbstractCLikeBI {
        final protected String formatNull(Environment env) {
            return env.getCFormat().getNullString();
        }
    }

    private static abstract class AbstractCLikeBI extends BuiltIn {

        @Override
        final TemplateModel _eval(Environment env) throws TemplateException {
            final String result;
            final TemplateModel model = target.eval(env);
            if (model instanceof TemplateNumberModel) {
                TemplateNumberFormat cTemplateNumberFormat = env.getCTemplateNumberFormat();
                try {
                    result = cTemplateNumberFormat.formatToPlainText((TemplateNumberModel) model);
                } catch (TemplateValueFormatException e) {
                    throw _MessageUtil.newCantFormatNumberException(cTemplateNumberFormat, target, e, false);
                }
            } else if (model instanceof TemplateBooleanModel) {
                boolean b = ((TemplateBooleanModel) model).getAsBoolean();
                CFormat cFormat = env.getCFormat();
                result = b ? cFormat.getTrueString() : cFormat.getFalseString();
            } else if (model instanceof TemplateScalarModel) {
                String s = EvalUtil.modelToString((TemplateScalarModel) model, target, env);
                result = env.getCFormat().formatString(s, env);
            } else if (model == null) {
                result = formatNull(env);
            } else {
                throw new UnexpectedTypeException(
                        target, model,
                        "number, boolean, or string",
                        new Class[] { TemplateNumberModel.class, TemplateBooleanModel.class, TemplateScalarModel.class },
                        env);
            }
            return new SimpleScalar(result);
        }

        protected abstract String formatNull(Environment env) throws InvalidReferenceException;

    }

    static class dateBI extends BuiltIn {
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
                this.defaultFormat = env.getTemplateDateFormat(dateType, Date.class, target, false);
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
                // unknown and datetime can be coerced into any date type
                if (dtype == TemplateDateModel.UNKNOWN || dtype == TemplateDateModel.DATETIME) {
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

    static class apiBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            if (!env.isAPIBuiltinEnabled()) {
                throw new _MiscTemplateException(this,
                        "Can't use ?api, because the \"", Configurable.API_BUILTIN_ENABLED_KEY,
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

    static class has_apiBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            final TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return tm instanceof TemplateModelWithAPISupport ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }
    
    static class is_booleanBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateBooleanModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_collectionBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateCollectionModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_collection_exBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateCollectionModelEx) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_dateLikeBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateDateModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_dateOfTypeBI extends BuiltIn {
        
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

    static class is_directiveBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            // WRONG: it also had to check Macro.isFunction()
            return (tm instanceof TemplateTransformModel || tm instanceof Macro || tm instanceof TemplateDirectiveModel) ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_enumerableBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel || tm instanceof TemplateCollectionModel)
                    && (_TemplateAPI.getTemplateLanguageVersionAsInt(this) < _VersionInts.V_2_3_21
                        // These implement TemplateSequenceModel, yet they can't be #list-ed:
                        || !(tm instanceof SimpleMethodModel || tm instanceof OverloadedMethodsModel))
                    ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_hash_exBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateHashModelEx) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_hashBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateHashModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_indexableBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_macroBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            // WRONG: it also had to check Macro.isFunction()
            return (tm instanceof Macro)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_markup_outputBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateMarkupOutputModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }
    
    static class is_methodBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateMethodModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_nodeBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateNodeModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_numberBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateNumberModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_sequenceBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel
                        && (
                            !(tm instanceof OverloadedMethodsModel || tm instanceof SimpleMethodModel)
                            || !env.isIcI2324OrLater())
                        )
                        ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_stringBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateScalarModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_transformBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateTransformModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class namespaceBI extends BuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            if (!(tm instanceof Macro)) {
                throw new UnexpectedTypeException(
                        target, tm,
                        "macro or function", new Class[] { Macro.class },
                        env);
            } else {
                return env.getMacroNamespace((Macro) tm);
            }
        }
    }

    static class sizeBI extends BuiltIn {

        @Override
        protected void setTarget(Expression target) {
            super.setTarget(target);
            target.enableLazilyGeneratedResult();
        }

        private int countingLimit;

        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);

            final int size;
            if (countingLimit == 1 && model instanceof TemplateCollectionModelEx) {
                size = ((TemplateCollectionModelEx) model).isEmpty() ? 0 : 1;
            } else if (model instanceof TemplateSequenceModel) {
                size = ((TemplateSequenceModel) model).size();
            } else if (model instanceof TemplateCollectionModelEx) {
                size = ((TemplateCollectionModelEx) model).size();
            } else if (model instanceof TemplateHashModelEx) {
                size = ((TemplateHashModelEx) model).size();
            } else if (model instanceof LazilyGeneratedCollectionModel
                    && ((LazilyGeneratedCollectionModel) model).isSequence()) {
                // While this is a TemplateCollectionModel, and thus ?size will be O(N), and N might be infinite,
                // it's for the result of ?filter(predicate) or such. Those "officially" return a sequence. Returning a
                // TemplateCollectionModel (a LazilyGeneratedCollectionModel to be exact) is a (mostly) transparent
                // optimization to avoid creating the result sequence in memory, which would be unnecessary work for
                // ?size. Creating that result sequence would be O(N) too, so the O(N) time complexity should be
                // expected by the template author, and we just made that calculation less wasteful here.
                TemplateModelIterator iterator = ((LazilyGeneratedCollectionModel) model).iterator();
                int counter = 0;
                countElements: while (iterator.hasNext()) {
                    counter++;
                    if (counter == countingLimit) {
                        break countElements;
                    }
                    iterator.next();
                }
                size = counter;
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

        /**
         * Enables an optimization trick when the result of the built-in will be compared with a number literal.
         * For example, in the case of {@code things?size != 0} we only need to check of the target is non-empty, which
         * is often more efficient than telling exactly how many elements it has.
         */
        void setCountingLimit(int cmpOperator, NumberLiteral rightOperand) {
            int cmpInt;
            try {
                cmpInt = NumberUtil.toIntExact(rightOperand.getAsNumber());
            } catch (ArithmeticException e) {
                // As we can't know what the ArithmeticEngine will be on runtime, we won't risk this for non-integers.
                return;
            }

            switch (cmpOperator) {
                case EvalUtil.CMP_OP_EQUALS: countingLimit = cmpInt + 1; break;
                case EvalUtil.CMP_OP_NOT_EQUALS: countingLimit = cmpInt + 1; break;
                case EvalUtil.CMP_OP_LESS_THAN: countingLimit = cmpInt; break;
                case EvalUtil.CMP_OP_GREATER_THAN: countingLimit = cmpInt + 1; break;
                case EvalUtil.CMP_OP_LESS_THAN_EQUALS: countingLimit = cmpInt + 1; break;
                case EvalUtil.CMP_OP_GREATER_THAN_EQUALS: countingLimit = cmpInt; break;
                default: throw new BugException("Unsupported comparator operator code: " + cmpOperator);
            }
        }
    }
    
    static class stringBI extends BuiltIn {
        
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
                this.defaultFormat = dateType == TemplateDateModel.UNKNOWN
                        ? null  // Lazy unknown type error in getAsString()
                        : env.getTemplateDateFormat(
                                dateType, EvalUtil.modelToDate(dateModel, target).getClass(), target, true);
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
                            throw _MessageUtil.newCantFormatUnknownTypeDateException(target, null);
                        } else {
                            throw new BugException();
                        }
                    }
                    try {
                        cachedValue = EvalUtil.assertFormatResultNotNull(defaultFormat.formatToPlainText(dateModel));
                    } catch (TemplateValueFormatException e) {
                        try {
                            throw _MessageUtil.newCantFormatDateException(defaultFormat, target, e, true);
                        } catch (TemplateException e2) {
                            // `e` should always be a TemplateModelException here, but to be sure: 
                            throw _CoreAPI.ensureIsTemplateModelException("Failed to format date/time/datetime", e2); 
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
                number = EvalUtil.modelToNumber(numberModel, target);  // for BackwardCompatibleTemplateNumberFormat-s
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
                    if (format instanceof BackwardCompatibleTemplateNumberFormat) {
                        result = env.formatNumberToPlainText(number, (BackwardCompatibleTemplateNumberFormat) format, target);
                    } else {
                        result = env.formatNumberToPlainText(numberModel, format, target, true);
                    }
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
                        if (defaultFormat instanceof BackwardCompatibleTemplateNumberFormat) {
                            cachedValue = env.formatNumberToPlainText(
                                    number, (BackwardCompatibleTemplateNumberFormat) defaultFormat, target);
                        } else {
                            cachedValue = env.formatNumberToPlainText(numberModel, defaultFormat, target, true);
                        }
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
                return new NumberFormatter((TemplateNumberModel) model, env);
            } else if (model instanceof TemplateDateModel) {
                TemplateDateModel dm = (TemplateDateModel) model;
                return new DateFormatter(dm, env);
            } else if (model instanceof SimpleScalar) {
                return model;
            } else if (model instanceof TemplateBooleanModel) {
                return new BooleanFormatter((TemplateBooleanModel) model, env);
            } else if (model instanceof TemplateScalarModel) {
                return new SimpleScalar(((TemplateScalarModel) model).getAsString());
            } else if (env.isClassicCompatible() && model instanceof BeanModel) {
                return new SimpleScalar(_BeansAPI.getAsClassicCompatibleString((BeanModel) model));
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

}
