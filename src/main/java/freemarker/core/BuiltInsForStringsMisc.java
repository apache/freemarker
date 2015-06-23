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

import freemarker.template.Configuration;
import freemarker.template.SimpleNumber;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

class BuiltInsForStringsMisc {

    static class booleanBI extends BuiltInForString {
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

    static class evalBI extends BuiltInForString {
        TemplateModel calculateResult(String s, Environment env) throws TemplateException 
        {
            SimpleCharStream scs = new SimpleCharStream(
                    new StringReader("(" + s + ")"), RUNTIME_EVAL_LINE_DISPLACEMENT, 1, s.length() + 2);
            FMParserTokenManager token_source = new FMParserTokenManager(scs);
            final Configuration cfg = env.getConfiguration();
            token_source.incompatibleImprovements = cfg.getIncompatibleImprovements().intValue();
            token_source.SwitchTo(FMParserConstants.FM_EXPRESSION);
            int namingConvention = cfg.getNamingConvention();
            token_source.initialNamingConvention = namingConvention;
            token_source.namingConvention = namingConvention;
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

    static class numberBI extends BuiltInForString {
        TemplateModel calculateResult(String s, Environment env)  throws TemplateException
        {
            try {
                return new SimpleNumber(env.getArithmeticEngine().toNumber(s));
            } catch(NumberFormatException nfe) {
                throw NonNumericalException.newMalformedNumberException(this, s, env);
            }
        }
    }

    // Can't be instantiated
    private BuiltInsForStringsMisc() { }
    
}
