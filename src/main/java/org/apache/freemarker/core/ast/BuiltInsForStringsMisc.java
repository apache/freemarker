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

package org.apache.freemarker.core.ast;

import java.io.StringReader;

import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.ast.FMParser;
import org.apache.freemarker.core.ast.FMParserConstants;
import org.apache.freemarker.core.ast.FMParserTokenManager;
import org.apache.freemarker.core.ast.SimpleCharStream;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.SimpleNumber;

class BuiltInsForStringsMisc {

    static class booleanBI extends BuiltInForString {
        @Override
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
                        "Can't convert this string to boolean: ", new _DelayedJQuote(s));
            }
            return b ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class evalBI extends OutputFormatBoundBuiltIn {
        
        @Override
        protected TemplateModel calculateResult(Environment env) throws TemplateException {
            return calculateResult(BuiltInForString.getTargetString(target, env), env);
        }
        
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            Template parentTemplate = getTemplate();
            
            Expression exp = null;
            try {
                try {
                    ParserConfiguration pCfg = parentTemplate.getParserConfiguration();
                    
                    SimpleCharStream simpleCharStream = new SimpleCharStream(
                            new StringReader("(" + s + ")"),
                            RUNTIME_EVAL_LINE_DISPLACEMENT, 1,
                            s.length() + 2);
                    simpleCharStream.setTabSize(pCfg.getTabSize());
                    FMParserTokenManager tkMan = new FMParserTokenManager(
                            simpleCharStream);
                    tkMan.SwitchTo(FMParserConstants.FM_EXPRESSION);

                    // pCfg.outputFormat is exceptional: it's inherited from the lexical context
                    if (pCfg.getOutputFormat() != outputFormat) {
                        pCfg = new _ParserConfigurationWithInheritedFormat(
                                pCfg, outputFormat, Integer.valueOf(autoEscapingPolicy));
                    }
                    
                    FMParser parser = new FMParser(
                            parentTemplate, false, tkMan, pCfg, TemplateSpecifiedEncodingHandler.DEFAULT);
                    
                    exp = parser.Expression();
                } catch (TokenMgrError e) {
                    throw e.toParseException(parentTemplate);
                }
            } catch (ParseException e) {
                throw new _MiscTemplateException(this, env,
                        "Failed to \"?", key, "\" string with this error:\n\n",
                        MessageUtil.EMBEDDED_MESSAGE_BEGIN,
                        new _DelayedGetMessage(e),
                        MessageUtil.EMBEDDED_MESSAGE_END,
                        "\n\nThe failing expression:");
            }
            try {
                return exp.eval(env);
            } catch (TemplateException e) {
                throw new _MiscTemplateException(this, env,
                        "Failed to \"?", key, "\" string with this error:\n\n",
                        MessageUtil.EMBEDDED_MESSAGE_BEGIN,
                        new _DelayedGetMessageWithoutStackTop(e),
                        MessageUtil.EMBEDDED_MESSAGE_END,
                        "\n\nThe failing expression:");
            }
        }
        
    }

    static class numberBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env)  throws TemplateException {
            try {
                return new SimpleNumber(env.getArithmeticEngine().toNumber(s));
            } catch (NumberFormatException nfe) {
                throw NonNumericalException.newMalformedNumberException(this, s, env);
            }
        }
    }

    // Can't be instantiated
    private BuiltInsForStringsMisc() { }
    
}
