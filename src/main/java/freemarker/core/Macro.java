/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
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
 */
public final class Macro extends TemplateElement implements TemplateModel {
    
    final int TYPE_MACRO = 0;
    final int TYPE_FUNCTION = 1;
    
    private final String name;
    private final String[] paramNames;
    private Map paramDefaults;
    private String catchAllParamName;
    boolean isFunction;
    static final Macro DO_NOTHING_MACRO = new Macro(".pass", 
            Collections.EMPTY_LIST, 
            freemarker.template.utility.Collections12.EMPTY_MAP,
            TextBlock.EMPTY_BLOCK);
    
    Macro(String name, List argumentNames, Map args, 
            TemplateElement nestedBlock) 
    {
        this.name = name;
        this.paramNames = (String[])argumentNames.toArray(
                new String[argumentNames.size()]);
        this.paramDefaults = args;
        this.nestedBlock = nestedBlock;
    }

    public String getCatchAll() {
        return catchAllParamName;
    }
    
    public void setCatchAll(String value) {
        catchAllParamName = value;
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
        sb.append(name);
        sb.append(isFunction ? '(' : ' ');
        int argCnt = paramNames.length;
        for (int i = 0; i < argCnt; i++) {
            if (i != 0) {
                if (isFunction) {
                    sb.append(", ");
                } else {
                    sb.append(' ');
                }
            }
            String argName = paramNames[i];
            sb.append(argName);
            if (paramDefaults != null && paramDefaults.get(argName) != null) {
                sb.append('=');
                Expression defaultExpr = (Expression) paramDefaults.get(argName);
                if (isFunction) {
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
        if (isFunction) sb.append(')');
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
        return isFunction ? "#function" : "#macro";
    }
    
    boolean isShownInStackTrace() {
        return false;
    }
    
    public boolean isFunction() {
        return isFunction;
    }

    class Context implements LocalContext {
        Environment.Namespace localVars; 
        TemplateElement body;
        Environment.Namespace bodyNamespace;
        List bodyParameterNames;
        Context prevMacroContext;
        ArrayList prevLocalContextStack;
        
        Context(Environment env, 
                TemplateElement body,
                List bodyParameterNames) 
        {
            this.localVars = env.new Namespace();
            this.prevMacroContext = env.getCurrentMacroContext();
            this.bodyNamespace = env.getCurrentNamespace();
            this.prevLocalContextStack = env.getLocalContextStack();
            this.body = body;
            this.bodyParameterNames = bodyParameterNames;
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
                return new Integer(isFunction ? TYPE_FUNCTION : TYPE_MACRO);
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
    
}
