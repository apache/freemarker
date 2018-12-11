package org.apache.freemarker.core;

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

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.CommonSupplier;
import org.apache.freemarker.core.util.StringToIndexMap;

/**
 * Example of an AST directive that's not bound to some Configuration setting.
 */
//TODO [FM3][FREEMARKER-99] Delete this class when we are finished.
class ASTDirAdhocTest2 extends ASTDirective {
    private ASTExpression countExp;

    private ASTDirAdhocTest2() {
        //
    }

    final static CommonSupplier<ASTDirective> FACTORY = new CommonSupplier<ASTDirective>() {
        @Override
        public ASTDirective get() throws Exception {
            return new ASTDirAdhocTest2();
        }
    };
    
    static TemplateDirectiveModel VALUE = new TemplateDirectiveModel() {

        @Override
        public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
                throws TemplateException, IOException {
            throw new UnsupportedOperationException("execute not implemented");
        }

        @Override
        public boolean isNestedContentSupported() {
            return true;
        }

        @Override
        public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
            return ARGS_LAYOUT;
        }

    };

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
    
    @Override
    public void setPositionalArgument(int position, ASTExpression valueExp) throws StaticLinkingCheckException {
        if (position == 0) {
            countExp = valueExp;
        } else {
            // TODO: Standardize error message; see CallableUtils 
            throw new StaticLinkingCheckException(new _ErrorDescriptionBuilder(
                  "Too many positional arguments; can only have ",
                  ARGS_LAYOUT.getPredefinedPositionalArgumentCount(), "."));
        }
    }

    @Override
    public void setNamedArgument(String name, ASTExpression valueExp) throws StaticLinkingCheckException {
        // TODO: Standardize error message; see CallableUtils 
        throw new StaticLinkingCheckException(new _ErrorDescriptionBuilder(
                "Unsupported parameter name ", new _DelayedJQuote(name), ". This directive has no named parameters."));
    }

    @Override
    public void checkArgumentsAndSetNestedContentParameters(StringToIndexMap nestedContentParamNames)
            throws StaticLinkingCheckException {
        // TODO: Standardize error message; see CallableUtils 
        if (countExp == null) {
            throw new StaticLinkingCheckException(new _ErrorDescriptionBuilder(
                    "Missing mandatory ", new _DelayedOrdinal(1), " positional argument"));
        }

        // TODO: Standardize error message; see CallableUtils
        if (nestedContentParamNames != null) {
            throw new StaticLinkingCheckException("This directive doesn't support nested content parameters.");
        }
    }

    @Override
    public StringToIndexMap getNestedContentParamNames() {
        return null;
    }
    
    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        env.pushElement(this);
        try {
            Writer out = env.getOut();
            int count = CallableUtils.castArgumentValueToInt(countExp.eval(env), 0, false, 0, VALUE, false);
            for (int i = 0; i < count; i++) {
                out.write(".");
            }
            return null;
        } finally {
            env.popElement();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    String dump(boolean canonical) {
        return "<#adhocTest2 ...>";
    }

    @Override
    public String getLabelWithoutParameters() {
        return "#adhocTest2";
    }

    @Override
    int getParameterCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    Object getParameterValue(int idx) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isNestedContentSupported() {
        return false;
    }

}
