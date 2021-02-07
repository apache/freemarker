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

package org.apache.freemarker.test.templateutil;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.apache.freemarker.core.util._StringUtils;

public class AssertEqualsDirective implements TemplateDirectiveModel {
    
    public static AssertEqualsDirective INSTANCE = new AssertEqualsDirective();

    private static final int ACTUAL_ARG_IDX = 0;
    private static final int EXPECTED_ARG_IDX = 1;

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
            0,
            false,
            StringToIndexMap.of(
                    "actual", ACTUAL_ARG_IDX,
                    "expected", EXPECTED_ARG_IDX),
            false);

    private AssertEqualsDirective() { }
    
    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        TemplateModel actual = CallableUtils.getArgument(args, ACTUAL_ARG_IDX, null, this);
        TemplateModel expected = CallableUtils.getArgument(args, EXPECTED_ARG_IDX, null, this);
        if (!env.applyEqualsOperatorLenient(actual, expected)) {
            throw new AssertionFailedInTemplateException("Assertion failed:\n"
                    + "Expected: " + tryUnwrap(expected) + "\n"
                    + "Actual: " + tryUnwrap(actual),
                    env);
        }
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    public boolean isNestedContentSupported() {
        return false;
    }

    private String tryUnwrap(TemplateModel value) throws TemplateException {
        if (value == null) return "null";
            // This is the same order as comparison goes:
        else if (value instanceof TemplateNumberModel) return ((TemplateNumberModel) value).getAsNumber().toString();
        else if (value instanceof TemplateDateModel) return ((TemplateDateModel) value).getAsDate().toString();
        else if (value instanceof TemplateStringModel) return _StringUtils.jQuote(((TemplateStringModel) value).getAsString());
        else if (value instanceof TemplateBooleanModel) return String.valueOf(((TemplateBooleanModel) value).getAsBoolean());
            // This shouldn't be reached, as the comparison should have failed earlier:
        else return value.toString();
    }

}
