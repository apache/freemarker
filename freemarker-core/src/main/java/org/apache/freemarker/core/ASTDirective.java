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

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.freemarker.core.util.StringToIndexMap;

/**
 * AST directive node superclass.
 * Concrete instances are normally created using {@link StaticallyLinkedNamespaceEntry#getDirectiveCallNodeFactory()}/
 */
// TODO [FM3] will be public
abstract class ASTDirective extends ASTElement {

    static final Set<String> BUILT_IN_DIRECTIVE_NAMES;
    static {
        TreeSet<String> names = new TreeSet<>();
        
        names.add("assign");
        names.add("attempt");
        names.add("autoEsc");
        names.add("break");
        names.add("case");
        names.add("compress");
        names.add("continue");
        names.add("default");
        names.add("else");
        names.add("elseIf");
        names.add("escape");
        names.add("fallback");
        names.add("flush");
        names.add("ftl");
        names.add("function");
        names.add("global");
        names.add("if");
        names.add("import");
        names.add("include");
        names.add("items");
        names.add("list");
        names.add("local");
        names.add("lt");
        names.add("macro");
        names.add("nested");
        names.add("noAutoEsc");
        names.add("noEscape");
        names.add("noParse");
        names.add("nt");
        names.add("outputFormat");
        names.add("recover");
        names.add("recurse");
        names.add("return");
        names.add("rt");
        names.add("sep");
        names.add("setting");
        names.add("stop");
        names.add("switch");
        names.add("t");
        names.add("visit");

        BUILT_IN_DIRECTIVE_NAMES = Collections.unmodifiableSet(names);

    }
    
    /**
     * Called by the parser to when it has parsed a parameter value expression for a positional parameter.
     * 
     * <p>
     * It's guaranteed that either {@link #setPositionalArgument(int, ASTExpression)} or
     * {@link #setNamedArgument(String, ASTExpression)} is called for each parameter in the source code exactly once, in
     * the order as the corresponding parameters occur in the source code. (While {@link DefaultTemplateLanguage}
     * guarantees that positional parameters are before the named parameters, other {@link TemplateLanguage}-s may don't
     * have such restriction.)
     * 
     * @param position
     *            The 0-based position of the parameter among the positional parameters.
     */
    public void setPositionalArgument(int position, ASTExpression valueExp)
            throws StaticLinkingCheckException {
        // TODO [FM3][FREEMARKER-99] Will be abstract
    }

    /**
     * Called by the parser when it has parsed a parameter expression.
     * 
     * <p>See guarantees regarding the call order and the number of calls in the description of
     * {@link #setPositionalArgument(int, ASTExpression)}.
     */
    public void setNamedArgument(String name, ASTExpression valueExp)
            throws StaticLinkingCheckException {
        // TODO [FM3][FREEMARKER-99] Will be abstract
    }
    
    /**
     * Called by the parser when it has already passed in all arguments (via
     * {@link #setPositionalArgument(int, ASTExpression)} and such). This allows the directive to check if all required
     * arguments were provided, and some more. It also sets the nested content parameter names (like {@code "i", "j"} in
     * {@code <#list m as i, j>}). (These two operations are packed into this method together as optimization, though
     * admittedly the gain is very small.)
     * 
     * @param nestedContentParamNames
     *            Will be {@code null} exactly if there are 0 nested content parameters.
     */
    public void checkArgumentsAndSetNestedContentParameters(StringToIndexMap nestedContentParamNames)
            throws StaticLinkingCheckException {
        // TODO [FM3][FREEMARKER-99] Will be abstract
    }

    /**
     * Tells if this directive can have nested content; the parser may need this information.
     */
    public boolean isNestedContentSupported() {
        // TODO [FM3][FREEMARKER-99] Will be abstract
        return true;
    }
    
    /**
     * @return {@code null} if there was no nested content parameter
     */
    public StringToIndexMap getNestedContentParamNames() {
        return null;
    }

}
