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

import static org.apache.freemarker.core.util.CallableUtils.*;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.util.StringToIndexMap;

public class AllFeaturesFunction extends TestTemplateCallableModel implements TemplateFunctionModel {

    public static final AllFeaturesFunction INSTANCE = new AllFeaturesFunction();

    private static final int P1_ARG_IDX = 0;
    private static final int P2_ARG_IDX = 1;
    private static final int N1_ARG_IDX = 2;
    private static final int N2_ARG_IDX = 3;

    private static final String N1_ARG_NAME = "n1";
    private static final String N2_ARG_NAME = "n2";

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
            2,
            true,
            StringToIndexMap.of(
                    N1_ARG_NAME, N1_ARG_IDX,
                    N2_ARG_NAME, N2_ARG_IDX),
            true
    );

    private static final int P_VARARGS_ARG_IDX = ARGS_LAYOUT.getPositionalVarargsArgumentIndex();
    private static final int N_VARARGS_ARG_IDX = ARGS_LAYOUT.getNamedVarargsArgumentIndex();

    private AllFeaturesFunction() {
        //
    }

    @Override
    public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env) throws TemplateException {
        return execute(
                getOptionalNumberArgument(args, P1_ARG_IDX, this),
                getOptionalNumberArgument(args, P2_ARG_IDX, this),
                (TemplateSequenceModel) args[P_VARARGS_ARG_IDX],
                getOptionalNumberArgument(args, N1_ARG_IDX, this),
                getOptionalNumberArgument(args, N2_ARG_IDX, this),
                (TemplateHashModelEx) args[N_VARARGS_ARG_IDX]);
    }

    private TemplateModel execute(Number p1, Number p2, TemplateSequenceModel pOthers,
            Number n1, Number n2, TemplateHashModelEx nOthers) throws TemplateException {
        StringBuilder sb = new StringBuilder();

        sb.append("fa(");
        printParam("p1", p1, sb, true);
        printParam("p2", p2, sb);
        printParam("pVarargs", pOthers, sb);
        printParam(N1_ARG_NAME, n1, sb);
        printParam(N2_ARG_NAME, n2, sb);
        printParam("nVarargs", nOthers, sb);
        sb.append(")");

        return new SimpleString(sb.toString());
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

}
