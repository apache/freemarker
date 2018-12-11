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

import java.io.IOException;
import java.util.List;

import org.apache.freemarker.core.model.TemplateModel;

/**
 * AST directive node: {@code #nested}.
 */
final class ASTDirNested extends ASTDirective {
    
    
    private List<ASTExpression> nestedContentParameters;
    
    
    ASTDirNested(List nestedContentParameters) {
        this.nestedContentParameters = nestedContentParameters;
    }
    
    List getNestedContentParameters() {
        return nestedContentParameters;
    }

    @Override
    ASTElement[] execute(Environment env) throws IOException, TemplateException {
        CallPlace macroCallPlace = env.getCurrentMacroContext().callPlace;

        // When nestedContParamCnt < nestedContentParameters.getCollectionSize(), then we just skip calculating the
        // extra parameters, and CallPlace.executeNestedContent will be successful. Note sure if this lenient
        // behavior is a good idea, but for now it's inherited from FM2, so TODO [FM3].
        // When nestedContParamCnt > nestedContentParameters.getCollectionSize(), then later
        // CallPlace.executeNestedContent will throw exception, but we let that happen so that the error message
        // generation remains centralized. (In FM2 not even this was an error.)
        TemplateModel[] nestedContParamValues;
        if (nestedContentParameters != null) {
            nestedContParamValues = new TemplateModel[
                    Math.min(macroCallPlace.getNestedContentParameterCount(), nestedContentParameters.size())];
            for (int i = 0; i < nestedContParamValues.length; i++) {
                nestedContParamValues[i] = nestedContentParameters.get(i).eval(env);
            }
        } else {
            nestedContParamValues = null;
        }

        env.executeNestedContentOfMacro(nestedContParamValues);

        return null;
    }

    @Override
    String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getLabelWithoutParameters());
        if (nestedContentParameters != null) {
            for (int i = 0; i < nestedContentParameters.size(); i++) {
                sb.append(' ');
                sb.append(nestedContentParameters.get(i).getCanonicalForm());
            }
        }
        if (canonical) sb.append('>');
        return sb.toString();
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return "#nested";
    }
    
    @Override
    int getParameterCount() {
        return nestedContentParameters != null ? nestedContentParameters.size() : 0;
    }

    @Override
    Object getParameterValue(int idx) {
        checkIndex(idx);
        return nestedContentParameters.get(idx);
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        checkIndex(idx);
        return ParameterRole.PASSED_VALUE;
    }

    private void checkIndex(int idx) {
        if (nestedContentParameters == null || idx >= nestedContentParameters.size()) {
            throw new IndexOutOfBoundsException();
        }
    }
    
    /*
    boolean heedsOpeningWhitespace() {
        return true;
    }

    boolean heedsTrailingWhitespace() {
        return true;
    }
    */
    
    @Override
    boolean isShownInStackTrace() {
        return true;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
}
