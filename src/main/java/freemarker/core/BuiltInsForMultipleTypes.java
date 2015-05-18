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

import java.text.NumberFormat;
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
import freemarker.template.TemplateModelWithAPISupport;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateTransformModel;
import freemarker.template._TemplateAPI;

/**
 * A holder for builtins that didn't fit into any other category.
 */
class BuiltInsForMultipleTypes {

    static class cBI extends AbstractCBI implements ICIChainMember {
        
        static class BIBeforeICE2d3d21 extends AbstractCBI {

            protected TemplateModel formatNumber(Environment env, TemplateModel model) throws TemplateModelException {
                Number num = EvalUtil.modelToNumber((TemplateNumberModel) model, target);
                if (num instanceof Integer || num instanceof Long) {
                    // Accelerate these fairly common cases
                    return new SimpleScalar(num.toString());
                } else {
                    return new SimpleScalar(env.getCNumberFormat().format(num));
                }
            }
            
        }
        
        private final BIBeforeICE2d3d21 prevICIObj = new BIBeforeICE2d3d21();

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

        protected TemplateModel formatNumber(Environment env, TemplateModel model) throws TemplateModelException {
            Number num = EvalUtil.modelToNumber((TemplateNumberModel) model, target);
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

        public int getMinimumICIVersion() {
            return _TemplateAPI.VERSION_INT_2_3_21;
        }
        
        public Object getPreviousICIChainMember() {
            return prevICIObj;
        }
        
    }

    static class dateBI extends BuiltIn {
        private class DateParser
        implements
            TemplateDateModel,
            TemplateMethodModel,
            TemplateHashModel
        {
            private final String text;
            private final Environment env;
            private final TemplateDateFormat defaultFormat;
            private Date cachedValue;
            
            DateParser(String text, Environment env)
            throws
                TemplateModelException
            {
                this.text = text;
                this.env = env;
                // Deliberately creating a snapshot here:
                this.defaultFormat = env.getTemplateDateFormat(dateType, Date.class, target);
            }
            
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return get((String) args.get(0));
            }
            
            public TemplateModel get(String pattern) throws TemplateModelException {
                return new SimpleDate(
                    parse(env.getTemplateDateFormat(dateType, Date.class, pattern, target)),
                    dateType);
            }
    
            public Date getAsDate() throws TemplateModelException {
                if(cachedValue == null) {
                    cachedValue = parse(defaultFormat);
                }
                return cachedValue;
            }
    
            public int getDateType() {
                return dateType;
            }
    
            public boolean isEmpty()
            {
                return false;
            }
    
            private Date parse(TemplateDateFormat df)
            throws
                TemplateModelException
            {
                try {
                    return df.parse(text);
                }
                catch(java.text.ParseException e) {
                    throw new _TemplateModelException(e, new Object[] {
                            "The string doesn't match the expected date/time/date-time format. "
                            + "The string to parse was: ", new _DelayedJQuote(text), ". ",
                            "The expected format was: ", new _DelayedJQuote(df.getDescription()), ".",
                            e.getMessage() != null ? "\nThe nested reason given follows:\n" : "",
                            e.getMessage() != null ? e.getMessage() : "" });
                }
            }
            
        }
        
        private final int dateType;
        
        dateBI(int dateType) {
            this.dateType = dateType;
        }
        
        TemplateModel _eval(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateDateModel) {
                TemplateDateModel dmodel = (TemplateDateModel)model;
                int dtype = dmodel.getDateType();
                // Any date model can be coerced into its own type
                if(dateType == dtype) {
                    return model;
                }
                // unknown and datetime can be coerced into any date type
                if(dtype == TemplateDateModel.UNKNOWN || dtype == TemplateDateModel.DATETIME) {
                    return new SimpleDate(dmodel.getAsDate(), dateType);
                }
                throw new _MiscTemplateException(this, new Object[] {
                            "Cannot convert ", TemplateDateModel.TYPE_NAMES.get(dtype),
                            " to ", TemplateDateModel.TYPE_NAMES.get(dateType) });
            }
            // Otherwise, interpret as a string and attempt 
            // to parse it into a date.
            String s = target.evalAndCoerceToString(env);
            return new DateParser(s, env);
        }

    }

    static class apiBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            if (!env.isAPIBuiltinEnabled()) {
                throw new _MiscTemplateException(this, new Object[] {
                        "Can't use ?api, because the \"", Configurable.API_BUILTIN_ENABLED_KEY,
                        "\" configuration setting is false. Think twice before you set it to true though. Especially, "
                        + "it shouldn't abussed for modifying Map-s and Collection-s." });
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
        TemplateModel _eval(Environment env) throws TemplateException {
            final TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return tm instanceof TemplateModelWithAPISupport ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }
    
    static class is_booleanBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateBooleanModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_collectionBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateCollectionModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_collection_exBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateCollectionModelEx) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_dateLikeBI extends BuiltIn {
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

        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateDateModel) && ((TemplateDateModel) tm).getDateType() == dateType
                ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_directiveBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            // WRONG: it also had to check Macro.isFunction()
            return (tm instanceof TemplateTransformModel || tm instanceof Macro || tm instanceof TemplateDirectiveModel) ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_enumerableBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel || tm instanceof TemplateCollectionModel)
                    && (_TemplateAPI.getTemplateLanguageVersionAsInt(this) < _TemplateAPI.VERSION_INT_2_3_21
                        // These implement TemplateSequenceModel, yet they can't be #list-ed:
                        || !(tm instanceof SimpleMethodModel || tm instanceof OverloadedMethodsModel))
                    ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_hash_exBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateHashModelEx) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_hashBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateHashModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_indexableBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_macroBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            // WRONG: it also had to check Macro.isFunction()
            return (tm instanceof Macro)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_methodBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateMethodModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_nodeBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateNodeModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_numberBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateNumberModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_sequenceBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_stringBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateScalarModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_transformBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateTransformModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class namespaceBI extends BuiltIn {
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
    
    static class stringBI extends BuiltIn {
        
        private class BooleanFormatter
        implements 
            TemplateScalarModel, 
            TemplateMethodModel 
        {
            private final TemplateBooleanModel bool;
            private final Environment env;
            
            BooleanFormatter(TemplateBooleanModel bool, Environment env) {
                this.bool = bool;
                this.env = env;
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 2);
                return new SimpleScalar((String) args.get(bool.getAsBoolean() ? 0 : 1));
            }
    
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
            TemplateMethodModel
        {
            private final TemplateDateModel dateModel;
            private final Environment env;
            private final TemplateDateFormat defaultFormat;
            private String cachedValue;
    
            DateFormatter(TemplateDateModel dateModel, Environment env)
            throws
                TemplateModelException
            {
                this.dateModel = dateModel;
                this.env = env;
                
                final int dateType = dateModel.getDateType();
                this.defaultFormat = dateType == TemplateDateModel.UNKNOWN
                        ? null  // Lazy unknown type error in getAsString()
                        : env.getTemplateDateFormat(
                                dateType, EvalUtil.modelToDate(dateModel, target).getClass(), target);
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return get((String) args.get(0));
            }

            public TemplateModel get(String key)
            throws
                TemplateModelException
            {
                return new SimpleScalar(env.formatDate(dateModel, key, target));
            }
            
            public String getAsString()
            throws
                TemplateModelException
            {
                if(cachedValue == null) {
                    try {
                        if (defaultFormat == null) {
                            if (dateModel.getDateType() == TemplateDateModel.UNKNOWN) {
                                throw MessageUtil.newCantFormatUnknownTypeDateException(target, null);
                            } else {
                                throw new BugException();
                            }
                        }
                        cachedValue = defaultFormat.format(dateModel);
                    } catch (UnformattableDateException e) {
                        throw MessageUtil.newCantFormatDateException(target, e);
                    }
                }
                return cachedValue;
            }
    
            public boolean isEmpty()
            {
                return false;
            }
        }
        
        private class NumberFormatter
        implements
            TemplateScalarModel,
            TemplateHashModel,
            TemplateMethodModel
        {
            private final Number number;
            private final Environment env;
            private final NumberFormat defaultFormat;
            private String cachedValue;
    
            NumberFormatter(Number number, Environment env)
            {
                this.number = number;
                this.env = env;
                defaultFormat = env.getNumberFormatObject(env.getNumberFormat());
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return get((String) args.get(0));
            }
    
            public TemplateModel get(String key)
            {
                return new SimpleScalar(env.getNumberFormatObject(key).format(number));
            }
            
            public String getAsString()
            {
                if(cachedValue == null) {
                    cachedValue = defaultFormat.format(number);
                }
                return cachedValue;
            }
    
            public boolean isEmpty()
            {
                return false;
            }
        }
    
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateNumberModel) {
                return new NumberFormatter(EvalUtil.modelToNumber((TemplateNumberModel)model, target), env);
            } else if (model instanceof TemplateDateModel) {
                TemplateDateModel dm = (TemplateDateModel)model;
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

    static abstract class AbstractCBI extends BuiltIn {
        
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
