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

import java.util.Date;

import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateModel;

abstract class BuiltInForDate extends ASTExpBuiltIn {
    @Override
    TemplateModel _eval(Environment env)
            throws TemplateException {
        TemplateModel model = target.eval(env);
        if (model instanceof TemplateDateModel) {
            TemplateDateModel tdm = (TemplateDateModel) model;
            return calculateResult(_EvalUtils.modelToDate(tdm, target), tdm.getDateType(), env);
        } else {
            throw MessageUtils.newUnexpectedOperandTypeException(target, model, TemplateDateModel.class, env);
        }
    }

    /** Override this to implement the built-in. */
    protected abstract TemplateModel calculateResult(Date date, int dateType, Environment env) throws TemplateException;
    
}