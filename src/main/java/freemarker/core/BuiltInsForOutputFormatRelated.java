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

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

class BuiltInsForOutputFormatRelated {

    static class no_escBI extends AbstractConverterBI {

        @Override
        protected TemplateModel calculateResult(String lho, MarkupOutputFormat outputFormat, Environment env)
                throws TemplateException {
            return outputFormat.fromMarkup(lho);
        }
        
    }

    static class escBI extends AbstractConverterBI {

        @Override
        protected TemplateModel calculateResult(String lho, MarkupOutputFormat outputFormat, Environment env)
                throws TemplateException {
            return outputFormat.fromPlainTextByEscaping(lho);
        }
        
    }
    
    static abstract class AbstractConverterBI extends MarkupOutputFormatBoundBuiltIn {

        @Override
        protected TemplateModel calculateResult(Environment env) throws TemplateException {
            TemplateModel lhoTM = target.eval(env);
            Object lhoMOOrStr = EvalUtil.coerceModelToStringOrMarkup(lhoTM, target, null, env);
            MarkupOutputFormat contextOF = outputFormat;
            if (lhoMOOrStr instanceof String) { // TemplateMarkupOutputModel
                return calculateResult((String) lhoMOOrStr, contextOF, env);
            } else {
                TemplateMarkupOutputModel lhoMO = (TemplateMarkupOutputModel) lhoMOOrStr;
                MarkupOutputFormat lhoOF = lhoMO.getOutputFormat();
                // ATTENTION: Keep this logic in sync. with ${...}'s logic!
                if (lhoOF == contextOF || contextOF.isOutputFormatMixingAllowed()) {
                    // bypass
                    return lhoMO;
                } else {
                    // ATTENTION: Keep this logic in sync. with ${...}'s logic!
                    String lhoPlainTtext = lhoOF.getSourcePlainText(lhoMO);
                    if (lhoPlainTtext == null) {
                        throw new _TemplateModelException(target,
                                "The left side operand of ?", key, " is in ", new _DelayedToString(lhoOF),
                                " format, which differs from the current output format, ",
                                new _DelayedToString(contextOF), ". Conversion wasn't possible.");
                    }
                    // Here we know that lho is escaped plain text. So we re-escape it to the current format and
                    // bypass it, just as if the two output formats were the same earlier.
                    return contextOF.fromPlainTextByEscaping(lhoPlainTtext);
                }
            }
        }
        
        protected abstract TemplateModel calculateResult(String lho, MarkupOutputFormat outputFormat, Environment env)
                throws TemplateException;
        
    }
    
}
