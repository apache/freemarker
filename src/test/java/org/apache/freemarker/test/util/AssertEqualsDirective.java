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

package org.apache.freemarker.test.util;

import java.io.IOException;
import java.util.Map;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.ast.Environment;
import org.apache.freemarker.core.ast._CoreAPI;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateDirectiveBody;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.util.StringUtil;

public class AssertEqualsDirective implements TemplateDirectiveModel {
    
    public static AssertEqualsDirective INSTANCE = new AssertEqualsDirective();

    private static final String ACTUAL_PARAM = "actual";
    private static final String EXPECTED_PARAM = "expected";

    private AssertEqualsDirective() { }
    
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        TemplateModel actual = null;
        TemplateModel expected = null;
        for (Object paramEnt  : params.entrySet()) {
            Map.Entry<String, TemplateModel> param = (Map.Entry) paramEnt;
            String paramName = param.getKey();
            if (paramName.equals(ACTUAL_PARAM)) {
                actual = param.getValue();
            } else if (paramName.equals(EXPECTED_PARAM)) {
                expected = param.getValue();
            } else {
                throw new UnsupportedParameterException(paramName, env);
            }
        }
        if (actual == null) {
            throw new MissingRequiredParameterException(ACTUAL_PARAM, env);
        }
        if (expected == null) {
            throw new MissingRequiredParameterException(EXPECTED_PARAM, env);
        }
        _CoreAPI.checkHasNoNestedContent(body);
        
        if (!env.applyEqualsOperatorLenient(actual, expected)) {
            throw new AssertationFailedInTemplateException("Assertion failed:\n"
                    + "Expected: " + tryUnwrapp(expected) + "\n"
                    + "Actual: " + tryUnwrapp(actual),
                    env);
        }
        
    }

    private String tryUnwrapp(TemplateModel value) throws TemplateModelException {
        if (value == null) return "null";
        // This is the same order as comparison goes:
        else if (value instanceof TemplateNumberModel) return ((TemplateNumberModel) value).getAsNumber().toString();
        else if (value instanceof TemplateDateModel) return ((TemplateDateModel) value).getAsDate().toString();
        else if (value instanceof TemplateScalarModel) return StringUtil.jQuote(((TemplateScalarModel) value).getAsString());
        else if (value instanceof TemplateBooleanModel) return String.valueOf(((TemplateBooleanModel) value).getAsBoolean());
        // This shouldn't be reached, as the comparison should have failed earlier:
        else return value.toString();
    }

}
