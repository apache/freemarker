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
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
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
                    new StringReader("(" + s + ")"), target.beginLine, 
                    target.beginColumn, s.length() + 2);
            FMParserTokenManager token_source = new FMParserTokenManager(scs);
            token_source.incompatibleImprovements = env.getConfiguration().getIncompatibleImprovements().intValue();
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
            } catch (ParseException pe) {
                pe.setTemplateName(getTemplate().getName());
                throw newTemplateException("Failed to \"?eval\" string value:\n"
                        + pe.getMessage() + "\n\nThe failing expression:");
            }
            return exp.eval(env);
        }
    }

    static class numberBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env)  throws TemplateException
        {
            try {
                return new SimpleNumber(env.getArithmeticEngine().toNumber(s));
            }
            catch(NumberFormatException nfe) {
                throw newMalformedNumberException(s);
            }
        }
    }
    static class substringBI extends StringBuiltIn {
    	TemplateModel calculateResult(final String s, final Environment env) throws TemplateException {
    		return new TemplateMethodModelEx() {
    			public Object exec(java.util.List args) throws TemplateModelException {
    				int argCount = args.size(), left=0, right=0;
    				if (argCount != 1 && argCount != 2) {
    					throw newTemplateModelException(
    					        "Expecting 1 or 2 numerical arguments passed to the method coming from:");
    				}
   					try {
   						TemplateNumberModel tnm = (TemplateNumberModel) args.get(0);
   						left = tnm.getAsNumber().intValue();
   						if (argCount == 2) {
   							tnm = (TemplateNumberModel) args.get(1);
   							right = tnm.getAsNumber().intValue();
   						}
   					} catch (ClassCastException cce) {
   						throw newTemplateModelException("Expecting numerical argument passed to the method coming from:");
   					}
    				if (argCount == 1) {
    					return new SimpleScalar(s.substring(left));
    				} 
    				return new SimpleScalar(s.substring(left, right));
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
        
        private static final int MIN_ICE = Version.intValueFor(2, 3, 20); 
        private final BIBeforeICE2d3d20 prevICEObj = new BIBeforeICE2d3d20();
        
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.XHTMLEnc(s));
        }
        
        static class BIBeforeICE2d3d20 extends StringBuiltIn {
            TemplateModel calculateResult(String s, Environment env) {
                return new SimpleScalar(StringUtil.HTMLEnc(s));
            }
        }
    
        public int getMinimumICIVersion() {
            return MIN_ICE;
        }
    
        public Object getPreviousICIChainMember() {
            return prevICEObj;
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

    static class starts_withBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
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

    static class ends_withBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
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

    /** This isn't used on J2SE 1.4 and later. */
    static class replaceBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
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

    /** This isn't used on J2SE 1.4 and later. */
    static class splitBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
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

    static class left_padBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
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

    static class right_padBI extends StringBuiltIn {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
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

    static class containsBI extends BuiltIn {
        TemplateModel _eval(Environment env)
                throws TemplateException {
            return new BIMethod(target.evalAndCoerceToString(
                    env,
                    "For sequences/collections (lists and such) use \"?seq_contains\" instead."));
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
        TemplateModel _eval(Environment env)
                throws TemplateException {
            return new BIMethod(target.evalAndCoerceToString(
                    env,
                    "For sequences/collections (lists and such) use \"?seq_index_of\" instead."));
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
        TemplateModel _eval(Environment env)
                throws TemplateException {
            return new BIMethod(target.evalAndCoerceToString(
                    env,
                    "For sequences/collections (lists and such) use \"?seq_last_index_of\" instead."));
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
}
