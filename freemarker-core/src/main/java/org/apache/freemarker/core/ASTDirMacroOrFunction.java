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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.apache.freemarker.core.util._StringUtils;

/**
 * AST directive node: {@code #macro} or {@code #function}
 */
final class ASTDirMacroOrFunction extends ASTDirective implements TemplateModel {

    static final ASTDirMacroOrFunction PASS_MACRO = new ASTDirMacroOrFunction(
            false, ".pass",
            null, null,
            null, null,
            TemplateElements.EMPTY);
    
    static final int TYPE_MACRO = 0;
    static final int TYPE_FUNCTION = 1;

    static final String POSITIONAL_PARAMETER_OPTION_NAME = "positional";
    static final String NAMED_PARAMETER_OPTION_NAME = "named";

    private final String name;
    private final boolean function;
    /** See {@link #getParameterDefinitionByArgumentArrayIndex()}. */
    private final ParameterDefinition[] paramDefsByArgArrayIdx;
    private final ArgumentArrayLayout argsLayout;

    ASTDirMacroOrFunction(
            boolean function,
            String name,
            List<ParameterDefinition> predefPosParamDefs,
            ParameterDefinition posVarargsParamDef,
            List<ParameterDefinition> predefNamedParamDefs,
            ParameterDefinition namedVarargsParamDef,
            TemplateElements children) {
        this.function = function;
        this.name = name;

        int predefPosParamsCnt = predefPosParamDefs != null ? predefPosParamDefs.size() : 0;
        int predefNamedParamsCnt = predefNamedParamDefs != null ? predefNamedParamDefs.size() : 0;

        paramDefsByArgArrayIdx = new ParameterDefinition[
                predefPosParamsCnt + predefNamedParamsCnt
                + (posVarargsParamDef != null ? 1 : 0) + (namedVarargsParamDef != null ? 1 : 0)];

        int dstIdx = 0;
        if (predefPosParamDefs != null) {
            for (ParameterDefinition predefPosParam : predefPosParamDefs) {
                paramDefsByArgArrayIdx[dstIdx++] = predefPosParam;
            }
        }

        StringToIndexMap.Entry[] nameToIdxEntries;
        if (predefNamedParamsCnt != 0) {
            nameToIdxEntries = new StringToIndexMap.Entry[predefNamedParamsCnt];
            for (int i = 0; i < predefNamedParamsCnt; i++) {
                ParameterDefinition predefNamedParam = predefNamedParamDefs.get(i);
                paramDefsByArgArrayIdx[dstIdx] = predefNamedParam;
                nameToIdxEntries[i] = new StringToIndexMap.Entry(predefNamedParam.name, dstIdx);
                dstIdx++;
            }
        } else {
            nameToIdxEntries = null;
        }

        if (posVarargsParamDef != null) {
            paramDefsByArgArrayIdx[dstIdx++] = posVarargsParamDef;
        }
        if (namedVarargsParamDef != null) {
            paramDefsByArgArrayIdx[dstIdx++] = namedVarargsParamDef;
        }

        argsLayout = ArgumentArrayLayout.create(
                predefPosParamsCnt,
                posVarargsParamDef != null,
                nameToIdxEntries != null ? StringToIndexMap.of(nameToIdxEntries) : null,
                namedVarargsParamDef != null);

        setChildren(children);
    }

    public String getName() {
        return name;
    }

    @Override
    ASTElement[] accept(Environment env) {
        env.visitMacroOrFunctionDefinition(this);
        return null;
    }

    @Override
    String getASTNodeDescriptor() {
        return function ? "#function" : "#macro";
    }

    boolean isFunction() {
        return function;
    }

    ArgumentArrayLayout getArgumentArrayLayout() {
        return argsLayout;
    }

    /**
     * Returns the definition of the parameters by the index according the {@link ArgumentArrayLayout}.
     */
    ParameterDefinition[] getParameterDefinitionByArgumentArrayIndex() {
        return paramDefsByArgArrayIdx;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getASTNodeDescriptor());
        sb.append(' ');
        sb.append(_StringUtils.toFTLTopLevelTragetIdentifier(name));

        if (function) sb.append('(');

        boolean firstParam = true;
        // Positional params:
        int predefPosArgCnt = argsLayout.getPredefinedPositionalArgumentCount();
        for (int idx = 0; idx < predefPosArgCnt; idx++) {
            if (!firstParam) {
                sb.append(", ");
            } else {
                if (!function) sb.append(" ");
                firstParam = false;
            }

            ParameterDefinition paramDef = paramDefsByArgArrayIdx[idx];

            sb.append(_StringUtils.toFTLTopLevelIdentifierReference(paramDef.name));
            if (!function) {
                sb.append("{").append(POSITIONAL_PARAMETER_OPTION_NAME).append("}");
            }

            if (paramDef.defaultExpression != null) {
                sb.append('=');
                sb.append(paramDef.defaultExpression.getCanonicalForm());
            }
        }
        int posVarargsArgIdx = argsLayout.getPositionalVarargsArgumentIndex();
        if (posVarargsArgIdx != -1) {
            if (!firstParam) {
                sb.append(", ");
            } else {
                if (!function) sb.append(" ");
                firstParam = false;
            }

            sb.append(_StringUtils.toFTLTopLevelIdentifierReference(paramDefsByArgArrayIdx[posVarargsArgIdx].name));
            if (!function) {
                sb.append("{").append(POSITIONAL_PARAMETER_OPTION_NAME).append("}");
            }
            sb.append("...");
        }
        // Named params:
        int predefNamedArgCnt = argsLayout.getPredefinedNamedArgumentsMap().size();
        for (int idx = predefPosArgCnt; idx < predefPosArgCnt + predefNamedArgCnt; idx++) {
            if (function) {
                if (!firstParam) {
                    sb.append(", ");
                } else {
                    firstParam = false;
                }
            } else {
                sb.append(" ");
                firstParam = false;
            }

            ParameterDefinition paramDef = paramDefsByArgArrayIdx[idx];

            sb.append(_StringUtils.toFTLTopLevelIdentifierReference(paramDef.name));
            if (function) {
                sb.append("{").append(NAMED_PARAMETER_OPTION_NAME).append("}");
            }

            if (paramDef.defaultExpression != null) {
                sb.append('=');
                sb.append(paramDef.defaultExpression.getCanonicalForm());
            }
        }
        int namedVarargsArgIdx = argsLayout.getNamedVarargsArgumentIndex();
        if (namedVarargsArgIdx != -1) {
            if (function) {
                if (!firstParam) {
                    sb.append(", ");
                } else {
                    firstParam = false;
                }
            } else {
                sb.append(" ");
                firstParam = false;
            }

            sb.append(_StringUtils.toFTLTopLevelIdentifierReference(paramDefsByArgArrayIdx[namedVarargsArgIdx].name));
            if (function) {
                sb.append("{").append(NAMED_PARAMETER_OPTION_NAME).append("}");
            }
            sb.append("...");
        }

        if (function) sb.append(')');

        if (canonical) {
            sb.append('>');
            sb.append(getChildrenCanonicalForm());
            sb.append("</").append(getASTNodeDescriptor()).append('>');
        }
        return sb.toString();
    }

    class Context implements LocalContext {
        final Environment.Namespace localVars;
        final Environment.TemplateLanguageCallable callable;
        final CallPlace callPlace;
        final Environment.Namespace nestedContentNamespace;
        final LocalContextStack prevLocalContextStack;
        final Context prevMacroContext;

        /**
         * @param callPlace Not {@code null}
         * @param callable Not {@code null}
         * @param env Not {@code null}
         */
        Context(Environment.TemplateLanguageCallable callable, CallPlace callPlace, Environment env) {
            localVars = env.new Namespace(); // TODO [FM3] Pass in expected Map size
            this.callable = callable;
            this.callPlace = callPlace;
            nestedContentNamespace = env.getCurrentNamespace();
            prevLocalContextStack = env.getLocalContextStack();
            prevMacroContext = env.getCurrentMacroContext();
        }
                
        ASTDirMacroOrFunction getMacro() {
            return ASTDirMacroOrFunction.this;
        }

        /**
         * @return the local variable of the given name
         * or null if it doesn't exist.
         */ 
        @Override
        public TemplateModel getLocalVariable(String name) throws TemplateException {
             return localVars.get(name);
        }

        Environment.Namespace getLocals() {
            return localVars;
        }
        
        /**
         * Set a local variable in this macro 
         */
        void setLocalVar(String name, TemplateModel var) {
            localVars.put(name, var);
        }

        @Override
        public Collection<String> getLocalVariableNames() throws TemplateException {
            HashSet<String> result = new HashSet<>();
            for (TemplateModelIterator it = localVars.keys().iterator(); it.hasNext(); ) {
                result.add(it.next().toString());
            }
            return result;
        }
    }

    @Override
    int getParameterCount() {
        return 1/*name*/ + paramDefsByArgArrayIdx.length + 1/*type*/;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx == 0) {
            return name;
        } else if (idx - 1 < paramDefsByArgArrayIdx.length) {
            // TODO [FM3] This is not traversable with AST node API-s down to the default expression. Also, it's not
            // extractable if the parameter is positional, named, and if it's varargs. But, as it's likely that the AST
            // API will change, fixing this was postponed.
            return paramDefsByArgArrayIdx[idx - 1];
        } else if (idx == getParameterCount() - 1) {
            return function ? TYPE_FUNCTION : TYPE_MACRO;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx == 0) {
            return ParameterRole.ASSIGNMENT_TARGET;
        } else if (idx - 1 < paramDefsByArgArrayIdx.length) {
            return ParameterRole.PARAMETER_DEFINITION;
        } else if (idx == getParameterCount() - 1) {
            return ParameterRole.AST_NODE_SUBTYPE;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        // Because of recursive calls
        return true;
    }

    /**
     * A parameter definition from the #macro/#function start tag.
     */
    static class ParameterDefinition {
        private final String name;
        private final ASTExpression defaultExpression;

        ParameterDefinition(String name, ASTExpression defaultExpression) {
            this.name = name;
            this.defaultExpression = defaultExpression;
        }

        String getName() {
            return name;
        }

        ASTExpression getDefaultExpression() {
            return defaultExpression;
        }

        @Override
        public String toString() {
            return "ParameterDefinition(" +
                    "name=" + _StringUtils.jQuote(name)
                    + (defaultExpression != null ? ", default=" + defaultExpression.getCanonicalForm() : "")
                    + ')';
        }
    }
    
}
