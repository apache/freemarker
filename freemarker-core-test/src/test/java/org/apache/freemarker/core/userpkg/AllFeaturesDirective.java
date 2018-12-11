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

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.util.StringToIndexMap;

public class AllFeaturesDirective extends TestTemplateCallableModel implements TemplateDirectiveModel {

    public static final AllFeaturesDirective INSTANCE = new AllFeaturesDirective();

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

    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        execute(getOptionalNumberArgument(args, P1_ARG_IDX, this),
                getOptionalNumberArgument(args, P2_ARG_IDX, this),
                (TemplateSequenceModel) args[P_VARARGS_ARG_IDX],
                getOptionalNumberArgument(args, N1_ARG_IDX, this),
                getOptionalNumberArgument(args, N2_ARG_IDX, this),
                (TemplateHashModelEx) args[N_VARARGS_ARG_IDX],
                out, env, callPlace);
    }

    private void execute(Number p1, Number p2, TemplateSequenceModel pOthers,
            Number n1, Number n2, TemplateHashModelEx nOthers,
            Writer out, Environment env, CallPlace callPlace) throws IOException, TemplateException {
        out.write("#a(");
        printParam("p1", p1, out, true);
        printParam("p2", p2, out);
        printParam("pVarargs", pOthers, out);
        printParam(N1_ARG_NAME, n1, out);
        printParam(N2_ARG_NAME, n2, out);
        printParam("nVarargs", nOthers, out);
        int nestedContParamCnt = callPlace.getNestedContentParameterCount();
        if (nestedContParamCnt != 0) {
            out.write("; " + nestedContParamCnt);
        }
        out.write(")");

        if (callPlace.hasNestedContent()) {
            out.write(" {");
            if (p1 != null) {
                int intP1 = p1.intValue();
                for (int i = 0; i < intP1; i++) {
                    // We dynamically set as many nested content parameters as many the caller has declared; this is
                    // unusual, and is for testing purposes only.
                    TemplateModel[] nestedContParamValues = new TemplateModel[nestedContParamCnt];
                    for (int paramIdx = 0; paramIdx < nestedContParamValues.length; paramIdx++) {
                        nestedContParamValues[paramIdx] = new SimpleNumber((i + 1) * (paramIdx + 1));
                    }
                    callPlace.executeNestedContent(nestedContParamValues, out, env);
                }
            }
            out.write("}");
        }
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    public boolean isNestedContentSupported() {
        return true;
    }

}
