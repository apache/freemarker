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

package org.apache.freemarker.core.userpkg;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.SimpleString;

public class NamedVarargsOnlyFunction extends TestTemplateCallableModel implements TemplateFunctionModel {

    public static final NamedVarargsOnlyFunction INSTANCE = new NamedVarargsOnlyFunction();

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
            0, false,
            null, true);

    private static final int NAMED_VARARGS_ARG_IDX = ARGS_LAYOUT.getNamedVarargsArgumentIndex();

    private NamedVarargsOnlyFunction() {
        //
    }

    @Override
    public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
            throws TemplateException {
        StringBuilder sb = new StringBuilder();

        sb.append("fnvo(");
        printParam("nVarargs", args[NAMED_VARARGS_ARG_IDX], sb, true);
        sb.append(")");

        return new SimpleString(sb.toString());
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

}
