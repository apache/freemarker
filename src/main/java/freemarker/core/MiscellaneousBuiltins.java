package freemarker.core;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans._BeansAPI;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateTransformModel;

/**
 * A holder for builtins that didn't fit into any other category.
 */
class MiscellaneousBuiltins {

    // Can't be instantiated
    private MiscellaneousBuiltins() { }
    
    static class sizeBI extends BuiltIn {
        TemplateModel _eval(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateSequenceModel) {
                int size = ((TemplateSequenceModel) model).size();
                return new SimpleNumber(size);
            }
            if (model instanceof TemplateHashModelEx) {
                int size = ((TemplateHashModelEx) model).size();
                return new SimpleNumber(size);
            }
            throw new UnexpectedTypeException(target, model, "extended-hash or sequence", env);
        }
    }

    static class dateBI extends BuiltIn {
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
                            " into ", TemplateDateModel.TYPE_NAMES.get(dateType) });
            }
            // Otherwise, interpret as a string and attempt 
            // to parse it into a date.
            String s = target.evalAndCoerceToString(env);
            return new DateParser(s, env);
        }
        
        private class DateParser
        implements
            TemplateDateModel,
            TemplateMethodModel,
            TemplateHashModel
        {
            private final String text;
            private final Environment env;
            private final DateFormat defaultFormat;
            private Date cachedValue;
            
            DateParser(String text, Environment env)
            throws
                TemplateModelException
            {
                this.text = text;
                this.env = env;
                this.defaultFormat = env.getDateFormatObject(dateType);
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
    
            public TemplateModel get(String pattern) throws TemplateModelException {
                return new SimpleDate(
                    parse(env.getDateFormatObject(dateType, pattern)),
                    dateType);
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return get((String) args.get(0));
            }
    
            public boolean isEmpty()
            {
                return false;
            }
    
            private Date parse(DateFormat df)
            throws
                TemplateModelException
            {
                try {
                    return df.parse(text);
                }
                catch(java.text.ParseException e) {
                    String pattern = null;
                    if (df instanceof SimpleDateFormat) {
                        pattern = ((SimpleDateFormat) df).toPattern();
                    }
                    throw new _TemplateModelException(new Object[] {
                            "The string doesn't match the expected date/time format. The string to parse was: ",
                            new _DelayedJQuote(text), ". ",
                            (pattern != null ? "The expected format was: " : ""),
                            (pattern != null ? (Object) new _DelayedJQuote(pattern) : (Object) ""),
                            (pattern != null ? ". " : "") });
                }
            }
        }
    }

    static class stringBI extends BuiltIn {
        
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateNumberModel) {
                return new NumberFormatter(EvalUtil.modelToNumber((TemplateNumberModel)model, target), env);
            } else if (model instanceof TemplateDateModel) {
                TemplateDateModel dm = (TemplateDateModel)model;
                int dateType = dm.getDateType();
                return new DateFormatter(EvalUtil.modelToDate(dm, target), dateType, env);
            } else if (model instanceof SimpleScalar) {
                return model;
            } else if (model instanceof TemplateBooleanModel) {
                return new BooleanFormatter((TemplateBooleanModel) model, env);
            } else if (model instanceof TemplateScalarModel) {
                return new SimpleScalar(((TemplateScalarModel) model).getAsString());
            } else if (env.isClassicCompatible() && model instanceof BeanModel) {
                return new SimpleScalar(_BeansAPI.getAsClassicCompatibleString((BeanModel) model));
            } else {            
                throw new UnexpectedTypeException(target, model, "number, date, or string", env);
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
    
            public String getAsString()
            {
                if(cachedValue == null) {
                    cachedValue = defaultFormat.format(number);
                }
                return cachedValue;
            }
    
            public TemplateModel get(String key)
            {
                return new SimpleScalar(env.getNumberFormatObject(key).format(number));
            }
            
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return get((String) args.get(0));
            }
    
            public boolean isEmpty()
            {
                return false;
            }
        }
        
        private class DateFormatter
        implements
            TemplateScalarModel,
            TemplateHashModel,
            TemplateMethodModel
        {
            private final Date date;
            private final int dateType;
            private final Environment env;
            private final DateFormat defaultFormat;
            private String cachedValue;
    
            DateFormatter(Date date, int dateType, Environment env)
            throws
                TemplateModelException
            {
                this.date = date;
                this.dateType = dateType;
                this.env = env;
                defaultFormat = env.getDateFormatObject(dateType);
            }
    
            public String getAsString()
            throws
                TemplateModelException
            {
                if(dateType == TemplateDateModel.UNKNOWN) {
                    throw new _TemplateModelException(new _ErrorDescriptionBuilder(
                            "Can't convert the date to string, because it isn't known if it's a "
                            + "date-only, time-only, or date-time value.")
                            .tip(MessageUtil.UNKNOWN_DATE_TO_STRING_TIPS));
                }
                if(cachedValue == null) {
                    cachedValue = defaultFormat.format(date);
                }
                return cachedValue;
            }
    
            public TemplateModel get(String key)
            throws
                TemplateModelException
            {
                return new SimpleScalar(env.getDateFormatObject(dateType, key).format(date));
            }
            
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return get((String) args.get(0));
            }
    
            public boolean isEmpty()
            {
                return false;
            }
        }
    
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
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 2);
                return new SimpleScalar((String) args.get(bool.getAsBoolean() ? 0 : 1));
            }
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

    static class is_numberBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateNumberModel)  ?
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

    static class is_booleanBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateBooleanModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_dateBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateDateModel)  ?
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

    static class is_macroBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            // WRONG: it also had to check Macro.isFunction()
            return (tm instanceof Macro)  ?
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

    static class is_hashBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateHashModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_hash_exBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateHashModelEx) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_sequenceBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_collectionBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateCollectionModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_indexableBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_enumerableBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            target.assertNonNull(tm, env);
            return (tm instanceof TemplateSequenceModel || tm instanceof TemplateCollectionModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
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

    static class namespaceBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel tm = target.eval(env);
            if (!(tm instanceof Macro)) {
                throw new UnexpectedTypeException(target, tm, "macro or function", env);
            } else {
                return env.getMacroNamespace((Macro) tm);
            }
        }
    }

    static class cBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateNumberModel) {
                Number num = EvalUtil.modelToNumber((TemplateNumberModel) model, target);
                if (num instanceof Integer || num instanceof Long) {
                    // Accelerate these fairly common cases
                    return new SimpleScalar(num.toString());
                } else {
                    return new SimpleScalar(env.getCNumberFormat().format(num));
                }
            } else if (model instanceof TemplateBooleanModel) {
                return new SimpleScalar(((TemplateBooleanModel) model).getAsBoolean()
                        ? MiscUtil.C_TRUE : MiscUtil.C_FALSE);
            } else {
                throw new UnexpectedTypeException(target, model, "number or boolean", env);
            }
        }
    }

}
