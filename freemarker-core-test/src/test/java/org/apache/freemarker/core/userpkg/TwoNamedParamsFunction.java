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
import org.apache.freemarker.core.util.StringToIndexMap;

public class TwoNamedParamsFunction extends TestTemplateCallableModel implements TemplateFunctionModel {

    public static final TwoNamedParamsFunction INSTANCE = new TwoNamedParamsFunction();

    private static final int N1_ARG_IDX = 0;
    private static final int N2_ARG_IDX = 1;

    private static final String N1_ARG_NAME = "n1";
    private static final String N2_ARG_NAME = "n2";

    private static final StringToIndexMap ARG_NAME_TO_IDX = StringToIndexMap.of(
            N1_ARG_NAME, N1_ARG_IDX,
            N2_ARG_NAME, N2_ARG_IDX);

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
            0, false,
            ARG_NAME_TO_IDX, false);

    private TwoNamedParamsFunction() {
        //
    }

    @Override
    public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
            throws TemplateException {
        StringBuilder sb = new StringBuilder();

        sb.append("fn(");
        printParam(N1_ARG_NAME, args[N1_ARG_IDX], sb, true);
        printParam(N2_ARG_NAME, args[N2_ARG_IDX], sb);
        sb.append(")");

        return new SimpleString(sb.toString());
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

}
