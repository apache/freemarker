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

import freemarker.template.*;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.StringUtil;
import freemarker.core.NodeBuiltins.*;
import freemarker.core.NumericalBuiltins.*;
import freemarker.core.SequenceBuiltins.*;
import freemarker.core.StringBuiltins.*;
import freemarker.core.DateBuiltins.*;

/**
 * The ? operator used to get the
 * functionality of built-in unary operators
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 */
abstract class BuiltIn extends Expression implements Cloneable {
    Expression target;
    String key;

    static final HashMap builtins = new HashMap();

    static {
        // These are the only ones we have now.
        // We throw a parse exception if it's not one of these.
        builtins.put("ancestors", new ancestorsBI());
        builtins.put("byte", new byteBI());
        builtins.put("c", new cBI());
        builtins.put("cap_first", new cap_firstBI());
        builtins.put("capitalize", new capitalizeBI());
        builtins.put("ceiling", new ceilingBI());
        builtins.put("children", new childrenBI());
        builtins.put("chop_linebreak", new chop_linebreakBI());
        builtins.put("contains", new containsBI());        
        builtins.put("date", new dateBI(TemplateDateModel.DATE));
        builtins.put("datetime", new dateBI(TemplateDateModel.DATETIME));
        builtins.put("default", new defaultBI());
        builtins.put("double", new doubleBI());
        builtins.put("ends_with", new ends_withBI());
        builtins.put("eval", new evalBI());
        builtins.put("exists", new existsBI());
        builtins.put("first", new firstBI());
        builtins.put("float", new floatBI());
        builtins.put("floor", new floorBI());
        builtins.put("chunk", new chunkBI());
        builtins.put("has_content", new has_contentBI());
        builtins.put("html", new htmlBI());
        builtins.put("if_exists", new if_existsBI());
        builtins.put("index_of", new index_ofBI());
        builtins.put("int", new intBI());
        builtins.put("interpret", new Interpret());
        builtins.put("is_boolean", new is_booleanBI());
        builtins.put("is_collection", new is_collectionBI());
        builtins.put("is_date", new is_dateBI());
        builtins.put("is_directive", new is_directiveBI());
        builtins.put("is_enumerable", new is_enumerableBI());
        builtins.put("is_hash_ex", new is_hash_exBI());
        builtins.put("is_hash", new is_hashBI());
        builtins.put("is_indexable", new is_indexableBI());
        builtins.put("is_macro", new is_macroBI());
        builtins.put("is_method", new is_methodBI());
        builtins.put("is_node", new is_nodeBI());
        builtins.put("is_number", new is_numberBI());
        builtins.put("is_sequence", new is_sequenceBI());
        builtins.put("is_string", new is_stringBI());
        builtins.put("is_transform", new is_transformBI());
        builtins.put("iso_utc", new iso_tz_BI("iso_utc",
                /* showOffset = */ true, DateUtil.ACCURACY_SECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_nz", new iso_tz_BI("iso_utc_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_SECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_ms", new iso_tz_BI("iso_utc_ms",
                /* showOffset = */ true, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_ms_nz", new iso_tz_BI("iso_utc_ms_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_m", new iso_tz_BI("iso_utc_m",
                /* showOffset = */ true, DateUtil.ACCURACY_MINUTES, /* useUTC = */ true));
        builtins.put("iso_utc_m_nz", new iso_tz_BI("iso_utc_m_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_MINUTES, /* useUTC = */ true));
        builtins.put("iso_utc_h", new iso_tz_BI("iso_utc_h",
                /* showOffset = */ true, DateUtil.ACCURACY_HOURS, /* useUTC = */ true));
        builtins.put("iso_utc_h_nz", new iso_tz_BI("iso_utc_h_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_HOURS, /* useUTC = */ true));
        builtins.put("iso_local", new iso_tz_BI("iso_local",
                /* showOffset = */ true, DateUtil.ACCURACY_SECONDS, /* useUTC = */ false));
        builtins.put("iso_local_nz", new iso_tz_BI("iso_local_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_SECONDS, /* useUTC = */ false));
        builtins.put("iso_local_ms", new iso_tz_BI("iso_local_ms",
                /* showOffset = */ true, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ false));
        builtins.put("iso_local_ms_nz", new iso_tz_BI("iso_local_ms_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ false));
        builtins.put("iso_local_m", new iso_tz_BI("iso_local_m",
                /* showOffset = */ true, DateUtil.ACCURACY_MINUTES, /* useUTC = */ false));
        builtins.put("iso_local_m_nz", new iso_tz_BI("iso_local_m_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_MINUTES, /* useUTC = */ false));
        builtins.put("iso_local_h", new iso_tz_BI("iso_local_h",
                /* showOffset = */ true, DateUtil.ACCURACY_HOURS, /* useUTC = */ false));
        builtins.put("iso_local_h_nz", new iso_tz_BI("iso_local_h_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_HOURS, /* useUTC = */ false));
        builtins.put("iso", new iso_BI("iso",
                /* showOffset = */ true, DateUtil.ACCURACY_SECONDS));
        builtins.put("iso_nz", new iso_BI("iso_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_SECONDS));
        builtins.put("iso_ms", new iso_BI("iso_ms",
                /* showOffset = */ true, DateUtil.ACCURACY_MILLISECONDS));
        builtins.put("iso_ms_nz", new iso_BI("iso_ms_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_MILLISECONDS));
        builtins.put("iso_m", new iso_BI("iso_m",
                /* showOffset = */ true, DateUtil.ACCURACY_MINUTES));
        builtins.put("iso_m_nz", new iso_BI("iso_m_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_MINUTES));
        builtins.put("iso_h", new iso_BI("iso_h",
                /* showOffset = */ true, DateUtil.ACCURACY_HOURS));
        builtins.put("iso_h_nz", new iso_BI("iso_h_nz",
                /* showOffset = */ false, DateUtil.ACCURACY_HOURS));
        builtins.put("j_string", new j_stringBI());
        builtins.put("js_string", new js_stringBI());
        builtins.put("keys", new keysBI());
        builtins.put("last_index_of", new last_index_ofBI());
        builtins.put("last", new lastBI());
        builtins.put("left_pad", new left_padBI());
        builtins.put("length", new lengthBI());
        builtins.put("long", new longBI());
        builtins.put("lower_case", new lower_caseBI());
        builtins.put("namespace", new namespaceBI());
        builtins.put("new", new NewBI());
        builtins.put("node_name", new node_nameBI());
        builtins.put("node_namespace", new node_namespaceBI());
        builtins.put("node_type", new node_typeBI());
        builtins.put("number", new numberBI());
        builtins.put("number_to_date", new number_to_dateBI(TemplateDateModel.DATE));
        builtins.put("number_to_time", new number_to_dateBI(TemplateDateModel.TIME));
        builtins.put("number_to_datetime", new number_to_dateBI(TemplateDateModel.DATETIME));
        builtins.put("parent", new parentBI());
        builtins.put("replace", new replaceBI());
        builtins.put("reverse", new reverseBI());
        builtins.put("right_pad", new right_padBI());
        builtins.put("root", new rootBI());
        builtins.put("round", new roundBI());
        builtins.put("rtf", new rtfBI());
        builtins.put("seq_contains", new seq_containsBI());
        builtins.put("seq_index_of", new seq_index_ofBI(1));
        builtins.put("seq_last_index_of", new seq_index_ofBI(-1));
        builtins.put("short", new shortBI());
        builtins.put("size", new sizeBI());
        builtins.put("sort_by", new sort_byBI());
        builtins.put("sort", new sortBI());
        builtins.put("split", new splitBI());
        builtins.put("starts_with", new starts_withBI());
        builtins.put("string", new stringBI());
        builtins.put("substring", new substringBI());
        builtins.put("time", new dateBI(TemplateDateModel.TIME));
        builtins.put("trim", new trimBI());
        builtins.put("uncap_first", new uncap_firstBI());
        builtins.put("upper_case", new upper_caseBI());
        builtins.put("url", new urlBI());
        builtins.put("values", new valuesBI());
        builtins.put("web_safe", builtins.get("html"));  // deprecated; use ?html instead
        builtins.put("word_list", new word_listBI());
        builtins.put("xhtml", new xhtmlBI());
        builtins.put("xml", new xmlBI());
        try {
            Class.forName("java.util.regex.Pattern");
            builtins.put("matches", instantiate("freemarker.core.RegexBuiltins$matchesBI"));
            builtins.put("groups", instantiate("freemarker.core.RegexBuiltins$groupsBI"));
            builtins.put("replace", instantiate("freemarker.core.RegexBuiltins$replace_reBI"));
            builtins.put("split", instantiate("freemarker.core.RegexBuiltins$split_reBI"));
        } catch (Exception e) {}
    }

    private static Object instantiate(String className) throws Exception
    {
        return ClassUtil.forName(className).newInstance();
    }
    
    static BuiltIn newBuiltIn(Expression target, String key, Token tok, String templateName) throws ParseException {
        BuiltIn bi = (BuiltIn) builtins.get(key);
        if (bi == null) {
            String locationInfo = "Error on line " + tok.beginLine + ", column " + tok.beginColumn + ", in template " + templateName + "\n";
            StringBuilder buf = new StringBuilder("Found " + key + ", expecting one of: ");
            for (Iterator it= builtins.keySet().iterator(); it.hasNext();) {
                if (it.hasNext()) {
                    buf.append(" ");
                } else {
                    buf.append( " or ");
                }
                buf.append(it.next());
                if (it.hasNext()) {
                    buf.append(", ");
                }
            }
            throw new ParseException(locationInfo + buf, target);
        }
        try {
            bi = (BuiltIn) bi.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        bi.target = target;
        bi.key = key;
        return bi;
    }

    public String getCanonicalForm() {
        return target.getCanonicalForm() + "?" + key;
    }

    boolean isLiteral() {
        return false; // be on the safe side.
    }

    Expression _deepClone(String name, Expression subst) {
    	try {
	    	BuiltIn clone = (BuiltIn)clone();
	    	clone.target = target.deepClone(name, subst);
	    	return clone;
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }


    static class lengthBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
        throws TemplateException
        {
            return new SimpleNumber(target.getStringValue(env).length());
        }
    }

    static class dateBI extends BuiltIn {
        private final int dateType;
        
        dateBI(int dateType) {
            this.dateType = dateType;
        }
        
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.getAsTemplateModel(env);
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
                throw new TemplateException(
                    "Cannot convert " + TemplateDateModel.TYPE_NAMES.get(dtype)
                    + " into " + TemplateDateModel.TYPE_NAMES.get(dateType), env);
            }
            // Otherwise, interpret as a string and attempt 
            // to parse it into a date.
            String s = target.getStringValue(env);
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

            public Object exec(List arguments)
                throws TemplateModelException {
                if (arguments.size() != 1) {
                    throw new TemplateModelException(
                            "string?" + key + "(...) requires exactly 1 argument.");
                }
                return get((String) arguments.get(0));
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
                    String mess = "Error: " + getStartLocation()
                                 + "\nThe string doesn't match the expected date/time format. "
                                 + "The string to parse was: " + StringUtil.jQuote(text)
                                 + (pattern != null
                                         ? ". The expected format was: "
                                           + StringUtil.jQuote(pattern) + "."
                                         : "");
                    throw new TemplateModelException(mess, e);
                }
            }
        }
    }

    static class stringBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateNumberModel) {
                return new NumberFormatter(EvaluationRules.getNumber((TemplateNumberModel)model, target, env), env);
            }
            if (model instanceof TemplateDateModel) {
                TemplateDateModel dm = (TemplateDateModel)model;
                int dateType = dm.getDateType();
                return new DateFormatter(EvaluationRules.getDate(dm, target, env), dateType, env);
            }
            if (model instanceof SimpleScalar) {
                return model;
            }
            if (model instanceof TemplateBooleanModel) {
                return new BooleanFormatter((TemplateBooleanModel) model, env);
            }
            if (model instanceof TemplateScalarModel) {
                return new SimpleScalar(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "number, date, or string");
        }

        private static class NumberFormatter
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
            
            public Object exec(List arguments)
                throws TemplateModelException {
                if (arguments.size() != 1) {
                    throw new TemplateModelException(
                            "number?string(...) requires exactly 1 argument.");
                }
                return get((String) arguments.get(0));
            }

            public boolean isEmpty()
            {
                return false;
            }
        }
        
        private static class DateFormatter
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
                    throw new TemplateModelException("Can't convert the date to string, because it is not known which parts of the date variable are in use. Use ?date, ?time or ?datetime built-in, or ?string.<format> or ?string(format) built-in with this date.");
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
            
            public Object exec(List arguments)
                throws TemplateModelException {
                if (arguments.size() != 1) {
                    throw new TemplateModelException(
                            "date?string(...) requires exactly 1 argument.");
                }
                return get((String) arguments.get(0));
            }

            public boolean isEmpty()
            {
                return false;
            }
        }

        private static class BooleanFormatter
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
                if (bool instanceof TemplateScalarModel) {
                    return ((TemplateScalarModel) bool).getAsString();
                } else {
                    return env.getBooleanFormat(bool.getAsBoolean());
                }
            }

            public Object exec(List arguments)
                    throws TemplateModelException {
                if (arguments.size() != 2) {
                    throw new TemplateModelException(
                            "boolean?string(...) requires exactly "
                            + "2 arguments.");
                }
                return new SimpleScalar(
                    (String) arguments.get(bool.getAsBoolean() ? 0 : 1));
            }
        }
    }

    static class trimBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(s.trim());
        }
    }

    static class htmlBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.HTMLEnc(s));
        }
    }

    static class xmlBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.XMLEnc(s));
        }
    }

    static class xhtmlBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.XHTMLEnc(s));
        }
    }

    static class rtfBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.RTFEnc(s));
        }
    }

    static class urlBI extends StringBuiltIn {
        
        TemplateModel calculateResult(String s, Environment env) {
            return new urlBIResult(s, env);
        }
        
        static class urlBIResult implements
                TemplateScalarModel, TemplateMethodModel {
            
            private final String target;
            private final Environment env;
            private String cachedResult;

            private urlBIResult(String target, Environment env) {
                this.target = target;
                this.env = env;
            }
            
            public String getAsString() throws TemplateModelException {
                if (cachedResult == null) {
                    String cs = env.getEffectiveURLEscapingCharset();
                    if (cs == null) {
                        throw new TemplateModelException(
                                "To do URL encoding, the framework that encloses "
                                + "FreeMarker must specify the output encoding "
                                + "or the URL encoding charset, so ask the "
                                + "programmers to fix it. Or, as a last chance, "
                                + "you can set the url_encoding_charset setting in "
                                + "the template, e.g. "
                                + "<#setting url_escaping_charset='ISO-8859-1'>, or "
                                + "give the charset explicitly to the buit-in, e.g. "
                                + "foo?url('ISO-8859-1').");
                    }
                    try {
                        cachedResult = StringUtil.URLEnc(target, cs);
                    } catch (UnsupportedEncodingException e) {
                        throw new TemplateModelException(
                                "Failed to execute URL encoding.", e);
                    }
                }
                return cachedResult;
            }

            public Object exec(List args) throws TemplateModelException {
                if (args.size() != 1) {
                    throw new TemplateModelException("The \"url\" built-in "
                            + "needs exactly 1 parameter, the charset.");
                }
                try {
                    return new SimpleScalar(
                            StringUtil.URLEnc(target, (String) args.get(0)));
                } catch (UnsupportedEncodingException e) {
                    throw new TemplateModelException(
                            "Failed to execute URL encoding.", e);
                }
            }
            
        }
    }
    
    static class keysBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateHashModelEx) {
                TemplateCollectionModel keys = ((TemplateHashModelEx) model).keys();
                assertNonNull(keys, this, env);
                if (!(keys instanceof TemplateSequenceModel))
                    keys = new CollectionAndSequence(keys);
                return keys;
            }
            throw invalidTypeException(model, target, env, "extended hash");
        }
    }

    static class valuesBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateHashModelEx) {
                TemplateCollectionModel values = ((TemplateHashModelEx) model).values();
                assertNonNull(values, this, env);
                if (!(values instanceof TemplateSequenceModel))
                    values = new CollectionAndSequence(values);
                return values;
            }
            throw invalidTypeException(model, target, env, "extended hash");
        }
    }

    static class sizeBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateSequenceModel) {
                int size = ((TemplateSequenceModel) model).size();
                return new SimpleNumber(size);
            }
            if (model instanceof TemplateHashModelEx) {
                int size = ((TemplateHashModelEx) model).size();
                return new SimpleNumber(size);
            }
            throw invalidTypeException(model, target, env, "extended-hash or sequence");
        }
    }

    static class existsBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException
        {
            try {
                TemplateModel model = target.getAsTemplateModel(env);
                return model==null ? TemplateBooleanModel.FALSE : TemplateBooleanModel.TRUE;
            } catch (InvalidReferenceException ire) {
                if (target instanceof ParentheticalExpression) {
                    return TemplateBooleanModel.FALSE;
                }
                throw ire;
            }
        }

        boolean isTrue(Environment env) throws TemplateException {
            return _getAsTemplateModel(env) == TemplateBooleanModel.TRUE;
        }
    }

    static class has_contentBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException
        {
            try {
                TemplateModel model = target.getAsTemplateModel(env);
                return Expression.isEmpty(model) ?
                    TemplateBooleanModel.FALSE : TemplateBooleanModel.TRUE;
            } catch (InvalidReferenceException ire) {
                if (target instanceof ParentheticalExpression) {
                    return TemplateBooleanModel.FALSE;
                }
                throw ire;
            }
        }

        boolean isTrue(Environment env) throws TemplateException {
            return _getAsTemplateModel(env) == TemplateBooleanModel.TRUE;
        }
    }

    static class if_existsBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException
        {
            try {
                TemplateModel model = target.getAsTemplateModel(env);
                return model == null ? TemplateModel.NOTHING : model;
            } catch (InvalidReferenceException ire) {
                if (target instanceof ParentheticalExpression) {
                    return TemplateModel.NOTHING;
                }
                throw ire;
            }
        }
    }

    static class is_stringBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateScalarModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_numberBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateNumberModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_nodeBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateNodeModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_booleanBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateBooleanModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_dateBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateDateModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_methodBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateMethodModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_macroBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof Macro)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_transformBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateTransformModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_hashBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateHashModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_hash_exBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateHashModelEx) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_sequenceBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateSequenceModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_collectionBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateCollectionModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_indexableBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateSequenceModel) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_enumerableBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateSequenceModel || tm instanceof TemplateCollectionModel)  ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class is_directiveBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            assertNonNull(tm, target, env);
            return (tm instanceof TemplateTransformModel || tm instanceof Macro || tm instanceof TemplateDirectiveModel) ?
                TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class namespaceBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel tm = target.getAsTemplateModel(env);
            if (!(tm instanceof Macro)) {
                invalidTypeException(tm, target, env, "macro");
            }
            return env.getMacroNamespace((Macro) tm);
        }
    }

    static class defaultBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(final Environment env)
                throws TemplateException
        {
            try {
                TemplateModel model = target.getAsTemplateModel(env);
                return
                    model == null
                    ? FIRST_NON_NULL_METHOD
                    : new ConstantMethod(model);
            } catch (InvalidReferenceException ire) {
                if (target instanceof ParentheticalExpression) {
                    return FIRST_NON_NULL_METHOD;
                }
                throw ire;
            }
        }

        private static class ConstantMethod implements TemplateMethodModelEx
        {
            private final TemplateModel constant;

            ConstantMethod(TemplateModel constant) {
                this.constant = constant;
            }

            public Object exec(List args) {
                return constant;
            }
        }

        /**
         * A method that goes through the arguments one by one and returns
         * the first one that is non-null. If all args are null, returns null.
         */
        private static final TemplateMethodModelEx FIRST_NON_NULL_METHOD =
            new TemplateMethodModelEx() {
                public Object exec(List args) throws TemplateModelException {
                    if(args.isEmpty()) {
                        throw new TemplateModelException(
                            "?default(arg) expects at least one argument.");
                    }
                    TemplateModel result = null;
                    for (int i = 0; i< args.size(); i++ ) {
                        result = (TemplateModel) args.get(i);
                        if (result != null) {
                            break;
                        }
                    }
                    return result;
                }
            };
    }

    static class containsBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new BIMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }

        private static class BIMethod implements TemplateMethodModelEx {
            private String s;

            private BIMethod(String s) {
                this.s = s;
            }

            public Object exec(List args) throws TemplateModelException {
                Object obj;
                String sub;

                int ln  = args.size();
                if (ln != 1) {
                    throw new TemplateModelException(
                            "?contains(...) expects one argument.");
                }

                obj = args.get(0);
                if (!(obj instanceof TemplateScalarModel)) {
                    throw new TemplateModelException(
                            "?contains(...) expects a string as "
                            + "its first argument.");
                }
                sub = ((TemplateScalarModel) obj).getAsString();

                return
                    (s.indexOf(sub) != -1) ?
                    TemplateBooleanModel.TRUE :
                    TemplateBooleanModel.FALSE;
            }
        }
    }
    
    static class index_ofBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new BIMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }
        
        private static class BIMethod implements TemplateMethodModelEx {
            private String s;
            
            private BIMethod(String s) {
                this.s = s;
            }
            
            public Object exec(List args) throws TemplateModelException {
                Object obj;
                String sub;
                int fidx;

                int ln  = args.size();
                if (ln == 0) {
                    throw new TemplateModelException(
                            "?index_of(...) expects at least one argument.");
                }
                if (ln > 2) {
                    throw new TemplateModelException(
                            "?index_of(...) expects at most two arguments.");
                }

                obj = args.get(0);       
                if (!(obj instanceof TemplateScalarModel)) {
                    throw new TemplateModelException(
                            "?index_of(...) expects a string as "
                            + "its first argument.");
                }
                sub = ((TemplateScalarModel) obj).getAsString();
                
                if (ln > 1) {
                    obj = args.get(1);
                    if (!(obj instanceof TemplateNumberModel)) {
                        throw new TemplateModelException(
                                "?index_of(...) expects a number as "
                                + "its second argument.");
                    }
                    fidx = ((TemplateNumberModel) obj).getAsNumber().intValue();
                } else {
                    fidx = 0;
                }

                return new SimpleNumber(s.indexOf(sub, fidx));
            }
        } 
    }

    static class last_index_ofBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new BIMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }

        private static class BIMethod implements TemplateMethodModelEx {
            private String s;

            private BIMethod(String s) {
                this.s = s;
            }

            public Object exec(List args) throws TemplateModelException {
                Object obj;
                String sub;

                int ln  = args.size();
                if (ln == 0) {
                    throw new TemplateModelException(
                            "?last_index_of(...) expects at least one argument.");
                }
                if (ln > 2) {
                    throw new TemplateModelException(
                            "?last_index_of(...) expects at most two arguments.");
                }

                obj = args.get(0);
                if (!(obj instanceof TemplateScalarModel)) {
                    throw new TemplateModelException(
                            "?last_index_of(...) expects a string as "
                            + "its first argument.");
                }
                sub = ((TemplateScalarModel) obj).getAsString();

                if (ln > 1) {
                    obj = args.get(1);
                    if (!(obj instanceof TemplateNumberModel)) {
                        throw new TemplateModelException(
                                "?last_index_of(...) expects a number as "
                                + "its second argument.");
                    }
                    int fidx = ((TemplateNumberModel) obj).getAsNumber().intValue();
                    return new SimpleNumber(s.lastIndexOf(sub, fidx));
                } else {
                    return new SimpleNumber(s.lastIndexOf(sub));
                }
            }
        }
    }
    
    static class starts_withBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new BIMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }

        private static class BIMethod implements TemplateMethodModelEx {
            private String s;

            private BIMethod(String s) {
                this.s = s;
            }

            public Object exec(List args) throws TemplateModelException {
                String sub;

                if (args.size() != 1) {
                    throw new TemplateModelException(
                            "?starts_with(...) expects exactly 1 argument.");
                }

                Object obj = args.get(0);
                if (!(obj instanceof TemplateScalarModel)) {
                    throw new TemplateModelException(
                            "?starts_with(...) expects a string argument");
                }
                sub = ((TemplateScalarModel) obj).getAsString();

                return s.startsWith(sub) ?
                        TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    }

    static class ends_withBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new BIMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }

        private static class BIMethod implements TemplateMethodModelEx {
            private String s;

            private BIMethod(String s) {
                this.s = s;
            }

            public Object exec(List args) throws TemplateModelException {
                String sub;

                if (args.size() != 1) {
                    throw new TemplateModelException(
                            "?ends_with(...) expects exactly 1 argument.");
                }

                Object obj = args.get(0);
                if (!(obj instanceof TemplateScalarModel)) {
                    throw new TemplateModelException(
                            "?ends_with(...) expects a string argument");
                }
                sub = ((TemplateScalarModel) obj).getAsString();

                return s.endsWith(sub) ?
                        TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    }
    
    static class replaceBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new BIMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }

        private static class BIMethod implements TemplateMethodModel {
            private String s;

            private BIMethod(String s) {
                this.s = s;
            }

            public Object exec(List args) throws TemplateModelException {
                int numArgs = args.size();
                if (numArgs < 2 || numArgs > 3) {
                    throw new TemplateModelException(
                            "?replace(...) needs 2 or 3 arguments.");
                }
                String first = (String) args.get(0);
                String second = (String) args.get(1);
                String flags = numArgs >2 ? (String) args.get(2) : "";
                boolean caseInsensitive = flags.indexOf('i') >=0;
                boolean firstOnly = flags.indexOf('f') >=0;
                if (flags.indexOf('r') >=0) {
                    throw new TemplateModelException("The regular expression classes are not available.");
                }
                return new SimpleScalar(StringUtil.replace(
                        s, first, second, caseInsensitive, firstOnly));
            }
        }
    }

    static class splitBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new BIMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }

        private static class BIMethod implements TemplateMethodModel {
            private String s;

            private BIMethod(String s) {
                this.s = s;
            }

            public Object exec(List args) throws TemplateModelException {
                int numArgs = args.size();
                if (numArgs != 1 && numArgs !=2) {
                    throw new TemplateModelException(
                            "?split(...) expects 1 or 2 arguments.");
                }
                String splitString = (String) args.get(0);
                String flags = numArgs ==2 ? (String) args.get(1) : "";
                boolean caseInsensitive = flags.indexOf('i') >=0;
                if (flags.indexOf('r') >=0) {
                    throw new TemplateModelException("regular expression classes not available");
                }
                return new StringArraySequence(StringUtil.split(
                        s, splitString, caseInsensitive));
            }
        }
    }

    static class left_padBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new BIMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }

        private static class BIMethod implements TemplateMethodModelEx {
            private String s;

            private BIMethod(String s) {
                this.s = s;
            }

            public Object exec(List args) throws TemplateModelException {
                Object obj;

                int ln  = args.size();
                if (ln == 0) {
                    throw new TemplateModelException(
                            "?left_pad(...) expects at least 1 argument.");
                }
                if (ln > 2) {
                    throw new TemplateModelException(
                            "?left_pad(...) expects at most 2 arguments.");
                }

                obj = args.get(0);
                if (!(obj instanceof TemplateNumberModel)) {
                    throw new TemplateModelException(
                            "?left_pad(...) expects a number as "
                            + "its 1st argument.");
                }
                int width = ((TemplateNumberModel) obj).getAsNumber().intValue();

                if (ln > 1) {
                    obj = args.get(1);
                    if (!(obj instanceof TemplateScalarModel)) {
                        throw new TemplateModelException(
                                "?left_pad(...) expects a string as "
                                + "its 2nd argument.");
                    }
                    String filling = ((TemplateScalarModel) obj).getAsString();
                    try {
                        return new SimpleScalar(StringUtil.leftPad(s, width, filling));
                    } catch (IllegalArgumentException e) {
                        if (filling.length() == 0) {
                            throw new TemplateModelException(
                                    "The 2nd argument of ?left_pad(...) "
                                    + "can't be a 0 length string.");
                        } else {
                            throw new TemplateModelException(
                                    "Error while executing the ?left_pad(...) "
                                    + "built-in.", e);
                        }
                    }
                } else {
                    return new SimpleScalar(StringUtil.leftPad(s, width));
                }
            }
        }
    }

    static class right_padBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new BIMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }

        private static class BIMethod implements TemplateMethodModelEx {
            private String s;

            private BIMethod(String s) {
                this.s = s;
            }

            public Object exec(List args) throws TemplateModelException {
                Object obj;

                int ln  = args.size();
                if (ln == 0) {
                    throw new TemplateModelException(
                            "?right_pad(...) expects at least 1 argument.");
                }
                if (ln > 2) {
                    throw new TemplateModelException(
                            "?right_pad(...) expects at most 2 arguments.");
                }

                obj = args.get(0);
                if (!(obj instanceof TemplateNumberModel)) {
                    throw new TemplateModelException(
                            "?right_pad(...) expects a number as "
                            + "its 1st argument.");
                }
                int width = ((TemplateNumberModel) obj).getAsNumber().intValue();

                if (ln > 1) {
                    obj = args.get(1);
                    if (!(obj instanceof TemplateScalarModel)) {
                        throw new TemplateModelException(
                                "?right_pad(...) expects a string as "
                                + "its 2nd argument.");
                    }
                    String filling = ((TemplateScalarModel) obj).getAsString();
                    try {
                        return new SimpleScalar(StringUtil.rightPad(s, width, filling));
                    } catch (IllegalArgumentException e) {
                        if (filling.length() == 0) {
                            throw new TemplateModelException(
                                    "The 2nd argument of ?right_pad(...) "
                                    + "can't be a 0 length string.");
                        } else {
                            throw new TemplateModelException(
                                    "Error while executing the ?right_pad(...) "
                                    + "built-in.", e);
                        }
                    }
                } else {
                    return new SimpleScalar(StringUtil.rightPad(s, width));
                }
            }
        }
    }
    
}
