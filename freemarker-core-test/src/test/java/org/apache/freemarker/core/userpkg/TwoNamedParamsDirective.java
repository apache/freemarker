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

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.NestedContentNotSupportedException;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.CallPlace;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.StringToIndexMap;

public class TwoNamedParamsDirective extends TestTemplateDirectiveModel {

    public static final TwoNamedParamsDirective INSTANCE = new TwoNamedParamsDirective();

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

    private TwoNamedParamsDirective() {
        //
    }

    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        NestedContentNotSupportedException.check(callPlace);
        out.write("#n(");
        printParam(N1_ARG_NAME, args[N1_ARG_IDX], out, true);
        printParam(N2_ARG_NAME, args[N2_ARG_IDX], out);
        out.write(")");
    }

    @Override
    public ArgumentArrayLayout getArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }
}
