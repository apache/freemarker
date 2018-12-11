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

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateModel;

/**
 * A holder for builtins that deal with null left-hand values.
 */
class BuiltInsForExistenceHandling {

    // Can't be instantiated
    private BuiltInsForExistenceHandling() { }

    private static abstract class ExistenceBuiltIn extends ASTExpBuiltIn {
    
        protected TemplateModel evalMaybeNonexistentTarget(Environment env) throws TemplateException {
            TemplateModel tm;
            if (target instanceof ASTExpParenthesis) {
                boolean lastFIRE = env.setFastInvalidReferenceExceptions(true);
                try {
                    tm = target.eval(env);
                } catch (InvalidReferenceException ire) {
                    tm = null;
                } finally {
                    env.setFastInvalidReferenceExceptions(lastFIRE);
                }
            } else {
                tm = target.eval(env);
            }
            return tm;
        }
        
    }
    
    static class has_contentBI extends BuiltInsForExistenceHandling.ExistenceBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            return ASTExpression.isEmpty(evalMaybeNonexistentTarget(env))
                    ? TemplateBooleanModel.FALSE
                    : TemplateBooleanModel.TRUE;
        }
    
        @Override
        boolean evalToBoolean(Environment env) throws TemplateException {
            return _eval(env) == TemplateBooleanModel.TRUE;
        }
    }
    
}
