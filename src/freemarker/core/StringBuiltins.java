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
import freemarker.template.utility.StringUtil;

import java.io.StringReader;
import java.util.StringTokenizer;


/**
 * A holder for builtins that operate exclusively on strings.
 */

abstract class StringBuiltins {
    abstract static class StringBuiltIn extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
        throws TemplateException
        {
            return calculateResult(target.getStringValue(env), env);
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
            token_source.SwitchTo(FMParserConstants.FM_EXPRESSION);
            FMParser parser = new FMParser(token_source);
            parser.template = getTemplate();
            Expression exp = null;
            try {
                exp = parser.Expression();
            } catch (ParseException pe) {
                pe.setTemplateName(getTemplate().getName());
                throw new TemplateException(pe, env);
            }
            return exp.getAsTemplateModel(env);
        }
    }

    static class numberBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env)  throws TemplateException
        {
            try {
                return new SimpleNumber(env.getArithmeticEngine().toNumber(s));
            }
            catch(NumberFormatException nfe) {
                String mess = "Error: " + getStartLocation()
                             + "\nExpecting a number here, found: " + s;
                throw new NonNumericalException(mess, env);
            }
        }
    }
    static class substringBI extends StringBuiltIn {
    	TemplateModel calculateResult(final String s, final Environment env) throws TemplateException {
    		return new TemplateMethodModelEx() {
    			public Object exec(java.util.List args) throws TemplateModelException {
    				int argCount = args.size(), left=0, right=0;
    				if (argCount != 1 && argCount != 2) {
    					throw new TemplateModelException("Error: +getStartLocation() + \nExpecting 1 or 2 numerical arguments here");
    				}
   					try {
   						TemplateNumberModel tnm = (TemplateNumberModel) args.get(0);
   						left = tnm.getAsNumber().intValue();
   						if (argCount == 2) {
   							tnm = (TemplateNumberModel) args.get(1);
   							right = tnm.getAsNumber().intValue();
   						}
   					} catch (ClassCastException cce) {
   						String mess = "Error: " + getStartLocation() + "\nExpecting numerical argument here";
   						throw new TemplateModelException(mess);
   					}
    				if (argCount == 1) {
    					return new SimpleScalar(s.substring(left));
    				} 
    				return new SimpleScalar(s.substring(left, right));
    			}
    		};
    	}
    }
}
