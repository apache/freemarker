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
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.FTLUtil;

public class AssertDirective implements TemplateDirectiveModel {
    public static AssertDirective INSTANCE = new AssertDirective();

    private AssertDirective() { }
    
    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        TemplateModel test = args[0];
        if (test == null) {
            throw new MissingRequiredParameterException("test", env);
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
        return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
    }

    @Override
    public boolean isNestedContentSupported() {
        return false;
    }
}
