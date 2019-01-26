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

import static org.apache.freemarker.core.util.CallableUtils.*;

import java.util.Date;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelWithAPISupport;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.SimpleDate;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util.CallableUtils;
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
                return new SimpleString(((TemplateBooleanModel) model).getAsBoolean()
                        ? TemplateBooleanFormat.C_TRUE : TemplateBooleanFormat.C_FALSE);
            } else {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        target, model,
                        "number or boolean", new Class[] { TemplateNumberModel.class, TemplateBooleanModel.class },
                        null, env);
            }
        }

        @Override
        protected TemplateModel formatNumber(Environment env, TemplateModel model) throws TemplateException {
            Number num = _EvalUtils.modelToNumber((TemplateNumberModel) model, target);
            if (num instanceof Integer || num instanceof Long) {
                // Accelerate these fairly common cases
                return new SimpleString(num.toString());
            } else if (num instanceof Double) {
                double n = num.doubleValue();
                if (n == Double.POSITIVE_INFINITY) {
                    return new SimpleString("INF");
                }
                if (n == Double.NEGATIVE_INFINITY) {
                    return new SimpleString("-INF");
                }
                if (Double.isNaN(n)) {
                    return new SimpleString("NaN");
                }
                // Deliberately falls through
            } else if (num instanceof Float) {
                float n = num.floatValue();
                if (n == Float.POSITIVE_INFINITY) {
                    return new SimpleString("INF");
                }
                if (n == Float.NEGATIVE_INFINITY) {
                    return new SimpleString("-INF");
                }
                if (Float.isNaN(n)) {
                    return new SimpleString("NaN");
                }
                // Deliberately falls through
            }
        
            return new SimpleString(env.getCNumberFormat().format(num));
        }
        
    }

    static class dateBI extends ASTExpBuiltIn {
        private class DateParser extends BuiltInCallableImpl
                implements TemplateDateModel, TemplateFunctionModel, TemplateHashModel {
            private final String text;
            private final Environment env;
            private final TemplateDateFormat defaultFormat;
            private TemplateDateModel cachedValue;
            
            DateParser(String text, Environment env) throws TemplateException {
                this.text = text;
                this.env = env;
                defaultFormat = env.getTemplateDateFormat(dateType, Date.class, target);
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String pattern = getOptionalStringArgument(args, 0, this);
                return pattern == null ? getAsDateModel() : get(pattern);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }

            @Override
            public TemplateModel get(String pattern) throws TemplateException {
                TemplateDateFormat format = env.getTemplateDateFormat(
                        pattern, dateType, Date.class, target, dateBI.this);
                return toTemplateDateModel(parse(format));
            }

            private TemplateDateModel toTemplateDateModel(Object date) throws TemplateException {
                if (date instanceof Date) {
                    return new SimpleDate((Date) date, dateType);
                } else {
                    TemplateDateModel tm = (TemplateDateModel) date;
                    if (tm.getDateType() != dateType) {
                        throw new TemplateException("The result of the parsing was of the wrong date type.");
                    }
                    return tm;
                }
            }

            private TemplateDateModel getAsDateModel() throws TemplateException {
                if (cachedValue == null) {
                    cachedValue = toTemplateDateModel(parse(defaultFormat));
                }
                return cachedValue;
            }
            
            @Override
            public Date getAsDate() throws TemplateException {
                return getAsDateModel().getAsDate();
            }
    
            @Override
            public int getDateType() {
                return dateType;
            }
    
            private Object parse(TemplateDateFormat df)
            throws TemplateException {
                try {
                    return df.parse(text, dateType);
                } catch (TemplateValueFormatException e) {
                    throw new TemplateException(e,
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
                throw new TemplateException(this,
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
                throw new TemplateException(this,
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
            return (tm instanceof TemplateBooleanModel) ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_iterableBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateIterableModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
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

    static class is_dateLikeBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateDateModel) ?
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

    static class is_markup_outputBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateMarkupOutputModel) ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }
    
    static class is_functionBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateFunctionModel) ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_nodeBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateNodeModel) ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_numberBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateNumberModel) ?
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
            return (tm instanceof TemplateStringModel) ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class namespaceBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            if (!(tm instanceof Environment.TemplateLanguageCallable)) {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        target, tm,
                        Environment.TemplateLanguageCallable.class,
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
            if (model instanceof TemplateCollectionModel) {
                size = ((TemplateCollectionModel) model).getCollectionSize();
            } else if (model instanceof TemplateHashModelEx) {
                size = ((TemplateHashModelEx) model).getHashSize();
            } else {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        target, model,
                        "collection (like a sequence) or extended-hash",
                        new Class[] { TemplateCollectionModel.class, TemplateHashModelEx.class },
                        null,
                        env);
            }
            return new SimpleNumber(size);
        }
    }
    
    static class stringBI extends ASTExpBuiltIn {
        
        private class BooleanFormatter extends BuiltInCallableImpl
                implements TemplateStringModel, TemplateFunctionModel {
            private final TemplateBooleanModel bool;
            private final Environment env;
            
            BooleanFormatter(TemplateBooleanModel bool, Environment env) {
                this.bool = bool;
                this.env = env;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                int argIdx = bool.getAsBoolean() ? 0 : 1;
                TemplateModel result = args[argIdx];
                if (!(result instanceof TemplateStringModel)) {
                    // Cause usual type exception
                    CallableUtils.castArgumentValueToString(
                            result, argIdx, false, null, this, true);
                }
                return result;
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }

            @Override
            public String getAsString() throws TemplateException {
                // TODO [FM3] Boolean should have come first... but that change would be non-BC.
                if (bool instanceof TemplateStringModel) {
                    return ((TemplateStringModel) bool).getAsString();
                } else {
                    return env.formatBoolean(bool.getAsBoolean());
                }
            }
        }
    
        private class DateFormatter extends BuiltInCallableImpl
                implements TemplateStringModel, TemplateHashModel, TemplateFunctionModel {
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
                                dateType, _EvalUtils.modelToDate(dateModel, target).getClass(), target);
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                return formatWith(CallableUtils.getStringArgument(args, 0, this));
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }

            @Override
            public TemplateModel get(String key) throws TemplateException {
                return formatWith(key);
            }

            private TemplateModel formatWith(String key) throws TemplateException {
                return new SimpleString(env.formatDateToPlainText(dateModel, key, target, stringBI.this));
            }
            
            @Override
            public String getAsString()
            throws TemplateException {
                if (cachedValue == null) {
                    if (defaultFormat == null) {
                        if (dateModel.getDateType() == TemplateDateModel.UNKNOWN) {
                            throw MessageUtils.newCantFormatUnknownTypeDateException(target, null);
                        } else {
                            throw new BugException();
                        }
                    }
                    try {
                        cachedValue = _EvalUtils.assertFormatResultNotNull(defaultFormat.formatToPlainText(dateModel));
                    } catch (TemplateValueFormatException e) {
                        throw MessageUtils.newCantFormatDateException(defaultFormat, target, e);
                    }
                }
                return cachedValue;
            }
        }
        
        private class NumberFormatter extends BuiltInCallableImpl
                implements TemplateStringModel, TemplateHashModel, TemplateFunctionModel {
            private final TemplateNumberModel numberModel;
            private final Environment env;
            private final TemplateNumberFormat defaultFormat;
            private String cachedValue;
    
            NumberFormatter(TemplateNumberModel numberModel, Environment env) throws TemplateException {
                this.env = env;
                
                // As we format lazily, we need a snapshot of the format inputs:
                this.numberModel = numberModel;
                defaultFormat = env.getTemplateNumberFormat(stringBI.this);
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                return get(CallableUtils.getStringArgument(args, 0, this));
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }

            @Override
            public TemplateModel get(String key) throws TemplateException {
                TemplateNumberFormat format = env.getTemplateNumberFormat(key, stringBI.this);

                String result = env.formatNumberToPlainText(numberModel, format, target);

                return new SimpleString(result);
            }
            
            @Override
            public String getAsString() throws TemplateException {
                if (cachedValue == null) {
                    cachedValue = env.formatNumberToPlainText(numberModel, defaultFormat, target);
                }
                return cachedValue;
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
            } else if (model instanceof SimpleString) {
                return model;
            } else if (model instanceof TemplateBooleanModel) {
                return new BooleanFormatter((TemplateBooleanModel) model, env);
            } else if (model instanceof TemplateStringModel) {
                return new SimpleString(((TemplateStringModel) model).getAsString());
            } else {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        target, model,
                        "number, date, boolean or string",
                        new Class[] {
                            TemplateNumberModel.class, TemplateDateModel.class, TemplateBooleanModel.class,
                            TemplateStringModel.class
                        },
                        null, env);
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
                return new SimpleString(((TemplateBooleanModel) model).getAsBoolean()
                        ? TemplateBooleanFormat.C_TRUE : TemplateBooleanFormat.C_FALSE);
            } else {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        target, model,
                        "number or boolean", new Class[] { TemplateNumberModel.class, TemplateBooleanModel.class },
                        null, env);
            }
        }
    
        protected abstract TemplateModel formatNumber(Environment env, TemplateModel model) throws TemplateException;
        
    }

}
