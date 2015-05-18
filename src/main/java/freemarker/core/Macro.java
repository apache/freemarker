/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

/**
 * An element representing a macro declaration.
 * 
 * @deprecated Subject to be changed or renamed any time; no "stable" replacement exists yet.
 */
public final class Macro extends TemplateElement implements TemplateModel {

    static final Macro DO_NOTHING_MACRO = new Macro(".pass", 
            Collections.EMPTY_LIST, 
            Collections.EMPTY_MAP,
            null, false,
            TextBlock.EMPTY_BLOCK);
    
    final static int TYPE_MACRO = 0;
    final static int TYPE_FUNCTION = 1;
    
    private final String name;
    private final String[] paramNames;
    private final Map paramDefaults;
    private final String catchAllParamName;
    private final boolean function;

    Macro(String name, List argumentNames, Map args, 
            String catchAllParamName, boolean function,
            TemplateElement nestedBlock) 
    {
        this.name = name;
        this.paramNames = (String[])argumentNames.toArray(
                new String[argumentNames.size()]);
        this.paramDefaults = args;
        
        this.function = function;
        this.catchAllParamName = catchAllParamName; 
        
        this.nestedBlock = nestedBlock;
    }

    public String getCatchAll() {
        return catchAllParamName;
    }
    
    public String[] getArgumentNames() {
        return (String[])paramNames.clone();
    }

    String[] getArgumentNamesInternal() {
        return paramNames;
    }

    boolean hasArgNamed(String name) {
        return paramDefaults.containsKey(name);
    }
    
    public String getName() {
        return name;
    }

    void accept(Environment env) {
        env.visitMacroDef(this);
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        sb.append(' ');
        sb.append(_CoreStringUtils.toFTLTopLevelTragetIdentifier(name));
        sb.append(function ? '(' : ' ');
        int argCnt = paramNames.length;
        for (int i = 0; i < argCnt; i++) {
            if (i != 0) {
                if (function) {
                    sb.append(", ");
                } else {
                    sb.append(' ');
                }
            }
            String argName = paramNames[i];
            sb.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(argName));
            if (paramDefaults != null && paramDefaults.get(argName) != null) {
                sb.append('=');
                Expression defaultExpr = (Expression) paramDefaults.get(argName);
                if (function) {
                    sb.append(defaultExpr.getCanonicalForm());
                } else {
                    MessageUtil.appendExpressionAsUntearable(sb, defaultExpr);
                }
            }
        }
        if (catchAllParamName != null) {
            if (argCnt != 0) sb.append(", ");
            sb.append(catchAllParamName);
            sb.append("...");
        }
        if (function) sb.append(')');
        if (canonical) {
            sb.append('>');
            if (nestedBlock != null) {
                sb.append(nestedBlock.getCanonicalForm());
            }
            sb.append("</").append(getNodeTypeSymbol()).append('>');
        }
        return sb.toString();
    }
    
    String getNodeTypeSymbol() {
        return function ? "#function" : "#macro";
    }
    
    boolean isShownInStackTrace() {
        return false;
    }
    
    public boolean isFunction() {
        return function;
    }

    class Context implements LocalContext {
        final Environment.Namespace localVars; 
        final TemplateElement nestedContent;
        final Environment.Namespace nestedContentNamespace;
        final List nestedContentParameterNames;
        final ArrayList prevLocalContextStack;
        final Context prevMacroContext;
        
        Context(Environment env, 
                TemplateElement nestedContent,
                List nestedContentParameterNames) 
        {
            this.localVars = env.new Namespace();
            this.nestedContent = nestedContent;
            this.nestedContentNamespace = env.getCurrentNamespace();
            this.nestedContentParameterNames = nestedContentParameterNames;
            this.prevLocalContextStack = env.getLocalContextStack();
            this.prevMacroContext = env.getCurrentMacroContext();
        }
                
        
        Macro getMacro() {
            return Macro.this;
        }

        void runMacro(Environment env) throws TemplateException, IOException {
            sanityCheck(env);
            // Set default values for unspecified parameters
            if (nestedBlock != null) {
                env.visit(nestedBlock);
            }
        }

        // Set default parameters, check if all the required parameters are defined.
        void sanityCheck(Environment env) throws TemplateException {
            boolean resolvedAnArg, hasUnresolvedArg;
            Expression firstUnresolvedExpression;
            InvalidReferenceException firstReferenceException;
            do {
                firstUnresolvedExpression = null;
                firstReferenceException = null;
                resolvedAnArg = hasUnresolvedArg = false;
                for(int i = 0; i < paramNames.length; ++i) {
                    String argName = paramNames[i];
                    if(localVars.get(argName) == null) {
                        Expression valueExp = (Expression) paramDefaults.get(argName);
                        if (valueExp != null) {
                            try {
                                TemplateModel tm = valueExp.eval(env);
                                if(tm == null) {
                                    if(!hasUnresolvedArg) {
                                        firstUnresolvedExpression = valueExp;
                                        hasUnresolvedArg = true;
                                    }
                                }
                                else {
                                    localVars.put(argName, tm);
                                    resolvedAnArg = true;
                                }
                            }
                            catch(InvalidReferenceException e) {
                                if(!hasUnresolvedArg) {
                                    hasUnresolvedArg = true;
                                    firstReferenceException = e;
                                }
                            }
                        }
                        else if (!env.isClassicCompatible()) {
                            boolean argWasSpecified = localVars.containsKey(argName);
                            throw new _MiscTemplateException(env,
                                    new _ErrorDescriptionBuilder(new Object[] {
                                            "When calling macro ", new _DelayedJQuote(name), 
                                            ", required parameter ", new _DelayedJQuote(argName),
                                            " (parameter #", new Integer(i + 1), ") was ", 
                                            (argWasSpecified
                                                    ? "specified, but had null/missing value."
                                                    : "not specified.") 
                                    }).tip(argWasSpecified
                                            ? new Object[] {
                                                    "If the parameter value expression on the caller side is known to "
                                                    + "be legally null/missing, you may want to specify a default "
                                                    + "value for it with the \"!\" operator, like "
                                                    + "paramValue!defaultValue." }
                                            : new Object[] { 
                                                    "If the omission was deliberate, you may consider making the "
                                                    + "parameter optional in the macro by specifying a default value "
                                                    + "for it, like ", "<#macro macroName paramName=defaultExpr>", ")" }
                                            ));
                        }
                    }
                }
            }
            while(resolvedAnArg && hasUnresolvedArg);
            if(hasUnresolvedArg) {
                if(firstReferenceException != null) {
                    throw firstReferenceException;
                } else if (!env.isClassicCompatible()) {
                    throw InvalidReferenceException.getInstance(firstUnresolvedExpression, env);
                }
            }
        }

        /**
         * @return the local variable of the given name
         * or null if it doesn't exist.
         */ 
        public TemplateModel getLocalVariable(String name) throws TemplateModelException {
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

        public Collection getLocalVariableNames() throws TemplateModelException {
            HashSet result = new HashSet();
            for (TemplateModelIterator it = localVars.keys().iterator(); it.hasNext();) {
                result.add(it.next().toString());
            }
            return result;
        }
    }

    int getParameterCount() {
        return 1/*name*/ + paramNames.length * 2/*name=default*/ + 1/*catchAll*/ + 1/*type*/;
    }

    Object getParameterValue(int idx) {
        if (idx == 0) {
            return name;
        } else {
            final int argDescsEnd = paramNames.length * 2 + 1;
            if (idx < argDescsEnd) {
                String paramName = paramNames[(idx - 1) / 2];
                if (idx % 2 != 0) {
                    return paramName;
                } else {
                    return paramDefaults.get(paramName);
                }
            } else if (idx == argDescsEnd) {
                return catchAllParamName;
            } else if (idx == argDescsEnd + 1) {
                return new Integer(function ? TYPE_FUNCTION : TYPE_MACRO);
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    ParameterRole getParameterRole(int idx) {
        if (idx == 0) {
            return ParameterRole.ASSIGNMENT_TARGET;
        } else {
            final int argDescsEnd = paramNames.length * 2 + 1;
            if (idx < argDescsEnd) {
                if (idx % 2 != 0) {
                    return ParameterRole.PARAMETER_NAME;
                } else {
                    return ParameterRole.PARAMETER_DEFAULT;
                }
            } else if (idx == argDescsEnd) {
                return ParameterRole.CATCH_ALL_PARAMETER_NAME;
            } else if (idx == argDescsEnd + 1) {
                return ParameterRole.AST_NODE_SUBTYPE;
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    boolean isNestedBlockRepeater() {
        // Because of recursive calls
        return true;
    }
    
}
