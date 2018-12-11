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
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.CommonSupplier;
import org.apache.freemarker.core.util.StringToIndexMap;

/**
 * Example of an AST directive that's bound to some Configuration setting (the htmlOutputFormat in this case).
 */
//TODO [FM3][FREEMARKER-99] Delete this class when we are finished.
class ASTDirAdhocTest1 extends ASTDirective {
    private final MarkupOutputFormat<?> htmlOutputFormat;
    private ASTExpression p1;
    private ASTExpression p2;
    private ASTExpression n1;
    private ASTExpression n2;
    private StringToIndexMap nestedContentParamNames;

    private ASTDirAdhocTest1(MarkupOutputFormat<?> htmlOutputFormat) {
        this.htmlOutputFormat = htmlOutputFormat;
    }

    static class Factory implements CommonSupplier<ASTDirective> {
        private final MarkupOutputFormat<?> htmlOutputFormat;

        Factory(MarkupOutputFormat<?> htmlOutputFormat) {
            this.htmlOutputFormat = htmlOutputFormat;
        }

        @Override
        public ASTDirective get() {
            return new ASTDirAdhocTest1(htmlOutputFormat);
        }
    }
    
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

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(2, false,
            StringToIndexMap.of("n1", 2, "n2", 3), false);
    
    @Override
    public void setPositionalArgument(int position, ASTExpression valueExp) throws StaticLinkingCheckException {
        if (position == 0) {
            p1 = valueExp;
        } else if (position == 1) {
            p2 = valueExp;
        } else {
            // TODO: Standardize error message; see CallableUtils 
            throw new StaticLinkingCheckException(new _ErrorDescriptionBuilder(
                  "Too many positional arguments; can only have ",
                  ARGS_LAYOUT.getPredefinedPositionalArgumentCount(), "."));
        }
    }

    @Override
    public void setNamedArgument(String name, ASTExpression valueExp) throws StaticLinkingCheckException {
        if (name.equals("n1")) {
            n1 = valueExp;
        } else if (name.equals("n2")) {
            n2 = valueExp;
        } else {
            // TODO: Standardize error message; see CallableUtils 
            throw new StaticLinkingCheckException(new _ErrorDescriptionBuilder(
                    "Unsupported parameter name ", new _DelayedJQuote(name), ". The supported parameters are: ",
                    new _DelayedJQuotedListing(ARGS_LAYOUT.getPredefinedNamedArgumentsMap().getKeys())));
        }
    }

    @Override
    public void checkArgumentsAndSetNestedContentParameters(StringToIndexMap nestedContentParamNames)
            throws StaticLinkingCheckException {
        // TODO: Standardize error message; see CallableUtils 
        if (p1 == null) {
            throw new StaticLinkingCheckException(new _ErrorDescriptionBuilder(
                    "Missing mandatory ", new _DelayedOrdinal(1), " positional argument"));
        }
        // TODO: Standardize error message; see CallableUtils 
        if (n1 == null) {
            throw new StaticLinkingCheckException(new _ErrorDescriptionBuilder(
                    "Missing mandatory named argument, ", new _DelayedJQuote("n1")));
        }
        
        // TODO: Make this check easier? 
        if (nestedContentParamNames == null || nestedContentParamNames.size() != 1) {
            throw new StaticLinkingCheckException(
                    MessageUtils.newBadNumberOfNestedContentParameterPassedMessage(nestedContentParamNames, 1));
        }
        this.nestedContentParamNames = nestedContentParamNames;
    }

    @Override
    public StringToIndexMap getNestedContentParamNames() {
        return nestedContentParamNames;
    }
    
    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        env.pushElement(this);
        try {
            Writer out = env.getOut();
            out.write("#foo(");
            // TODO Bind to cfg.getOutputFormat("HTML")
            
            int p1Int = CallableUtils.castArgumentValueToInt(p1.eval(env), 0, false, 0, VALUE, false);
            
            env.interpolate(p1, htmlOutputFormat, AutoEscapingPolicy.ENABLE_IF_DEFAULT, env);
            if (p2 != null) {
                out.write(", ");
                env.interpolate(p2, htmlOutputFormat, AutoEscapingPolicy.ENABLE_IF_DEFAULT, env);
            }
            out.write(", n1=");
            env.interpolate(n1, htmlOutputFormat, AutoEscapingPolicy.ENABLE_IF_DEFAULT, env);
            if (n2 != null) {
                out.write(", n2=");
                env.interpolate(n2, htmlOutputFormat, AutoEscapingPolicy.ENABLE_IF_DEFAULT, env);
            }
            out.write(") {\n");
            
            for (int i = 0; i < p1Int; i++) {
                env.executeNestedContent(this, nestedContentParamNames, new TemplateModel[] { new SimpleNumber(i) });
            }
            
            out.write("}-foo\n");
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
        return "<#adhocTest1 ...>";
    }

    @Override
    public String getLabelWithoutParameters() {
        return "#adhocTest1";
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
    
    

}
