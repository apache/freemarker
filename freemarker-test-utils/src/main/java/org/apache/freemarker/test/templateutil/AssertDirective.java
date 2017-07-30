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

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.NestedContentNotSupportedException;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.CallPlace;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.FTLUtil;
import org.apache.freemarker.core.util.StringToIndexMap;

public class AssertDirective implements TemplateDirectiveModel {
    public static AssertDirective INSTANCE = new AssertDirective();

    private static final String TEST_ARG_NAME = "test";
    private static final int TEST_ARG_IDX = 0;
    private static final StringToIndexMap ARG_NAMES_TO_IDX = StringToIndexMap.of(TEST_ARG_NAME, TEST_ARG_IDX);
    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
            0, false,
            ARG_NAMES_TO_IDX, false);

    private AssertDirective() { }
    
    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        NestedContentNotSupportedException.check(callPlace);

        TemplateModel test = args[TEST_ARG_IDX];
        if (test == null) {
            throw new MissingRequiredParameterException(TEST_ARG_NAME, env);
        }
        if (!(test instanceof TemplateBooleanModel)) {
            throw new AssertationFailedInTemplateException("Assertion failed:\n"
                    + "The value had to be boolean, but it was of type" + FTLUtil.getTypeDescription(test),
                    env);
        }
        if (!((TemplateBooleanModel) test).getAsBoolean()) {
            throw new AssertationFailedInTemplateException("Assertion failed:\n"
                    + "the value was false.",
                    env);
        }
    }

    @Override
    public ArgumentArrayLayout getArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    public boolean isNestedContentSupported() {
        return false;
    }
}
