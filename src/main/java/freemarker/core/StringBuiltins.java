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

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.StringTokenizer;

import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.StringUtil;


/**
 * A holder for builtins that operate exclusively on (coerced) string left-hand value.
 */
class StringBuiltins {
    
    // Can't be instantiated
    private StringBuiltins() { }
    
    // Also used by RegexpBuiltins
    abstract static class StringBuiltIn extends BuiltIn {
        TemplateModel _eval(Environment env)
        throws TemplateException
        {
            return calculateResult(target.evalAndCoerceToString(env), env);
        }
        abstract TemplateModel calculateResult(String s, Environment env) throws TemplateException;
    }
    
    static class capitalizeBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.capitalize(s));
        }
    }

    static class chop_linebreakBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.chomp(s));
        }
    }

    static class j_stringBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.javaStringEnc(s));
        }
    }

    static class js_stringBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.javaScriptStringEnc(s));
        }
    }

    static class json_stringBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.jsonStringEnc(s));
        }
    }

    static class cap_firstBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            int i = 0;
            int ln = s.length();
            while (i < ln  &&  Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i < ln) {
                StringBuffer b = new StringBuffer(s);
                b.setCharAt(i, Character.toUpperCase(s.charAt(i)));
                s = b.toString();
            }
            return new SimpleScalar(s);
        }
    }

    static class uncap_firstBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            int i = 0;
            int ln = s.length();
            while (i < ln  &&  Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i < ln) {
                StringBuffer b = new StringBuffer(s);
                b.setCharAt(i, Character.toLowerCase(s.charAt(i)));
                s = b.toString();
            }
            return new SimpleScalar(s);
        }
    }

    static class upper_caseBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env)
        {
            return new SimpleScalar(s.toUpperCase(env.getLocale()));
        }
    }

    static class lower_caseBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env)
        {
            return new SimpleScalar(s.toLowerCase(env.getLocale()));
        }
    }

    static class word_listBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            SimpleSequence result = new SimpleSequence();
            StringTokenizer st = new StringTokenizer(s);
            while (st.hasMoreTokens()) {
               result.add(st.nextToken());
            }
            return result;
        }
    }

    static class evalBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) throws TemplateException 
        {
            SimpleCharStream scs = new SimpleCharStream(
                    new StringReader("(" + s + ")"), RUNTIME_EVAL_LINE_DISPLACEMENT, 1, s.length() + 2);
            FMParserTokenManager token_source = new FMParserTokenManager(scs);
            token_source.incompatibleImprovements = _TemplateAPI.getTemplateLanguageVersionAsInt(this);
            token_source.SwitchTo(FMParserConstants.FM_EXPRESSION);
            FMParser parser = new FMParser(token_source);
            parser.setTemplate(getTemplate());
            Expression exp = null;
            try {
                try {
                    exp = parser.Expression();
                } catch (TokenMgrError e) {
                    throw e.toParseException(getTemplate());
                }
            } catch (ParseException e) {
                throw new _MiscTemplateException(this, env, new Object[] {
                        "Failed to \"?", key, "\" string with this error:\n\n",
                        MessageUtil.EMBEDDED_MESSAGE_BEGIN,
                        new _DelayedGetMessage(e),
                        MessageUtil.EMBEDDED_MESSAGE_END,
                        "\n\nThe failing expression:" });
            }
            try {
                return exp.eval(env);
            } catch (TemplateException e) {
                throw new _MiscTemplateException(this, env, new Object[] {
                        "Failed to \"?", key, "\" string with this error:\n\n",
                        MessageUtil.EMBEDDED_MESSAGE_BEGIN,
                        new _DelayedGetMessageWithoutStackTop(e),
                        MessageUtil.EMBEDDED_MESSAGE_END,
                        "\n\nThe failing expression:" });
            }
        }
    }

    static class numberBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env)  throws TemplateException
        {
            try {
                return new SimpleNumber(env.getArithmeticEngine().toNumber(s));
            } catch(NumberFormatException nfe) {
                throw NonNumericalException.newMalformedNumberException(this, s, env);
            }
        }
    }

    static class booleanBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env)  throws TemplateException {
            final boolean b;
            if (s.equals("true")) {
                b = true;
            } else if (s.equals("false")) {
                b = false;
            } else if (s.equals(env.getTrueStringValue())) {
                b = true;
            } else if (s.equals(env.getFalseStringValue())) {
                b = false;
            } else {
                throw new _MiscTemplateException(this, env,
                        new Object[] { "Can't convert this string to boolean: ", new _DelayedJQuote(s) });
            }
            return b ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }
    
    static class substringBI extends StringBuiltIn {
        
        TemplateModel calculateResult(final String s, final Environment env) throws TemplateException {
            return new TemplateMethodModelEx() {
                
                public Object exec(java.util.List args) throws TemplateModelException {
                    int argCount = args.size();
                    checkMethodArgCount(argCount, 1, 2);

                    int beginIdx = getNumberMethodArg(args, 0).intValue();

                    final int len = s.length();

                    if (beginIdx < 0) {
                        throw newIndexLessThan0Exception(0, beginIdx);
                    } else if (beginIdx > len) {
                        throw newIndexGreaterThanLengthException(0, beginIdx, len);
                    }

                    if (argCount > 1) {
                        int endIdx = getNumberMethodArg(args, 1).intValue();
                        if (endIdx < 0) {
                            throw newIndexLessThan0Exception(1, endIdx);
                        } else if (endIdx > len) {
                            throw newIndexGreaterThanLengthException(1, endIdx, len);
                        }
                        if (beginIdx > endIdx) {
                            throw MessageUtil.newMethodArgsInvalidValueException(
                                    "?" + key, new Object[] {
                                            "The begin index argument, ", new Integer(beginIdx),
                                            ", shouldn't be greater than the end index argument, ",
                                            new Integer(endIdx), "." });
                        }
                        return new SimpleScalar(s.substring(beginIdx, endIdx));
                    } else {
                        return new SimpleScalar(s.substring(beginIdx));
                    }
                }

                private TemplateModelException newIndexGreaterThanLengthException(
                        int argIdx, int idx, final int len) throws TemplateModelException {
                    return MessageUtil.newMethodArgInvalidValueException(
                            "?" + key, argIdx, new Object[] {
                                    "The index mustn't be greater than the length of the string, ",
                                    new Integer(len),
                                    ", but it was ", new Integer(idx), "." });
                }

                private TemplateModelException newIndexLessThan0Exception(
                        int argIdx, int idx) throws TemplateModelException {
                    return MessageUtil.newMethodArgInvalidValueException(
                            "?" + key, argIdx, new Object[] {
                                    "The index must be at least 0, but was ",
                                    new Integer(idx), "." });
                }
                
            };
        }
    }

    static class lengthBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new SimpleNumber(s.length());
        }
        
    }

    static class trimBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(s.trim());
        }
    }

    static class htmlBI extends StringBuiltIn implements ICIChainMember {
        
        private final BIBeforeICI2d3d20 prevICIObj = new BIBeforeICI2d3d20();
        
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.XHTMLEnc(s));
        }
        
        static class BIBeforeICI2d3d20 extends StringBuiltIn {
            TemplateModel calculateResult(String s, Environment env) {
                return new SimpleScalar(StringUtil.HTMLEnc(s));
            }
        }
    
        public int getMinimumICIVersion() {
            return _TemplateAPI.VERSION_INT_2_3_20;
        }
    
        public Object getPreviousICIChainMember() {
            return prevICIObj;
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

    static abstract class AbstractUrlBIResult implements
    TemplateScalarModel, TemplateMethodModel {
        
        protected final BuiltIn parent;
        protected final String targetAsString;
        private final Environment env;
        private String cachedResult;
        
        protected AbstractUrlBIResult(BuiltIn parent, String target, Environment env) {
            this.parent = parent;
            this.targetAsString = target;
            this.env = env;
        }
        
        public String getAsString() throws TemplateModelException {
            if (cachedResult == null) {
                String cs = env.getEffectiveURLEscapingCharset();
                if (cs == null) {
                    throw new _TemplateModelException(
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
                    cachedResult = encodeWithCharset(cs);
                } catch (UnsupportedEncodingException e) {
                    throw new _TemplateModelException(e, "Failed to execute URL encoding.");
                }
            }
            return cachedResult;
        }

        protected abstract String encodeWithCharset(String cs) throws UnsupportedEncodingException;
        
        public Object exec(List args) throws TemplateModelException {
            parent.checkMethodArgCount(args.size(), 1);
            try {
                return new SimpleScalar(encodeWithCharset((String) args.get(0)));
            } catch (UnsupportedEncodingException e) {
                throw new _TemplateModelException(e, "Failed to execute URL encoding.");
            }
        }
        
    }
    
    static class urlBI extends StringBuiltIn {
        
        TemplateModel calculateResult(String s, Environment env) {
            return new UrlBIResult(this, s, env);
        }
        
        static class UrlBIResult extends AbstractUrlBIResult {

            protected UrlBIResult(BuiltIn parent, String target, Environment env) {
                super(parent, target, env);
            }

            protected String encodeWithCharset(String cs) throws UnsupportedEncodingException {
                return StringUtil.URLEnc(targetAsString, cs);
            }
            
        }
        
    }

    static class urlPathBI extends StringBuiltIn {

        TemplateModel calculateResult(String s, Environment env) {
            return new UrlPathBIResult(this, s, env);
        }
        
        static class UrlPathBIResult extends AbstractUrlBIResult {

            protected UrlPathBIResult(BuiltIn parent, String target, Environment env) {
                super(parent, target, env);
            }

            protected String encodeWithCharset(String cs) throws UnsupportedEncodingException {
                return StringUtil.URLPathEnc(targetAsString, cs);
            }
            
        }
        
    }
    
    static class starts_withBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    
        private class BIMethod implements TemplateMethodModelEx {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return s.startsWith(getStringMethodArg(args, 0)) ?
                        TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    }

    static class ends_withBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    
        private class BIMethod implements TemplateMethodModelEx {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return s.endsWith(getStringMethodArg(args, 0)) ?
                        TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    }

    /** This isn't used on J2SE 1.4 and later. Remove it in 2.4. */
    static class replaceBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    
        private class BIMethod implements TemplateMethodModel {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 2, 3);
                String first = (String) args.get(0);
                String second = (String) args.get(1);
                boolean caseInsensitive;
                boolean firstOnly;
                if (argCnt > 2) {
                    String flags = (String) args.get(2);
                    caseInsensitive = flags.indexOf('i') >= 0;
                    firstOnly = flags.indexOf('f') >= 0;
                    if (flags.indexOf('r') >=0) {
                        throw new _TemplateModelException(
                                "The regular expression classes are not available.");
                    }
                } else {
                    caseInsensitive = false;
                    firstOnly = false;
                }
                return new SimpleScalar(StringUtil.replace(
                        s, first, second, caseInsensitive, firstOnly));
            }
        }
    }

    /** This isn't used on J2SE 1.4 and later. Remove it in 2.4. */
    static class splitBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    
        private class BIMethod implements TemplateMethodModel {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                String splitString = (String) args.get(0);
                String flags = argCnt == 2 ? (String) args.get(1) : "";
                boolean caseInsensitive = flags.indexOf('i') >=0;
                if (flags.indexOf('r') >=0) {
                    throw new _TemplateModelException("Regular expression classes not available");
                }
                return new StringArraySequence(StringUtil.split(
                        s, splitString, caseInsensitive));
            }
        }
    }

    static class padBI extends StringBuiltIn {
        
        private final boolean leftPadder;
    
        public padBI(boolean leftPadder) {
            this.leftPadder = leftPadder;
        }

        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    
        private class BIMethod implements TemplateMethodModelEx {
            
            private final String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            public Object exec(List args) throws TemplateModelException {
                int argCnt  = args.size();
                checkMethodArgCount(argCnt, 1, 2);
    
                int width = getNumberMethodArg(args, 0).intValue();
    
                if (argCnt > 1) {
                    String filling = getStringMethodArg(args, 1);
                    try {
                        return new SimpleScalar(
                                leftPadder
                                        ? StringUtil.leftPad(s, width, filling)
                                        : StringUtil.rightPad(s, width, filling));
                    } catch (IllegalArgumentException e) {
                        if (filling.length() == 0) {
                            throw new _TemplateModelException(new Object[] {
                                    "?", key, "(...) argument #2 can't be a 0-length string." });
                        } else {
                            throw new _TemplateModelException(e, new Object[] {
                                    "?", key, "(...) failed: ", e });
                        }
                    }
                } else {
                    return new SimpleScalar(leftPadder ? StringUtil.leftPad(s, width) : StringUtil.rightPad(s, width));
                }
            }
        }
    }

    static class containsBI extends BuiltIn {
        
        TemplateModel _eval(Environment env) throws TemplateException {
            return new BIMethod(target.evalAndCoerceToString(env,
                    "For sequences/collections (lists and such) use \"?seq_contains\" instead."));
        }
    
        private class BIMethod implements TemplateMethodModelEx {
            
            private final String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return s.indexOf(getStringMethodArg(args, 0)) != -1
                        ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    }

    static class index_ofBI extends BuiltIn {
        
        private final boolean findLast;
        
        public index_ofBI(boolean findLast) {
            this.findLast = findLast;
        }

        TemplateModel _eval(Environment env) throws TemplateException {
            return new BIMethod(target.evalAndCoerceToString(env,
                    "For sequences/collections (lists and such) use \"?seq_index_of\" instead."));
        }
        
        private class BIMethod implements TemplateMethodModelEx {
            
            private final String s;
            
            private BIMethod(String s) {
                this.s = s;
            }
            
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                String subStr = getStringMethodArg(args, 0);
                if (argCnt > 1) {
                    int startIdx = getNumberMethodArg(args, 1).intValue();
                    return new SimpleNumber(findLast ? s.lastIndexOf(subStr, startIdx) : s.indexOf(subStr, startIdx));
                } else {
                    return new SimpleNumber(findLast ? s.lastIndexOf(subStr) : s.indexOf(subStr));
                }
            }
        } 
    }

}
