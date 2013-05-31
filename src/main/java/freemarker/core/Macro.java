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
import java.util.*;
import freemarker.template.*;
import freemarker.template.utility.StringUtil;

/**
 * An element representing a macro declaration.
 */
public final class Macro extends TemplateElement implements TemplateModel {
    private final String name;
    private final String[] argumentNames;
    private Map args;
    private String catchAll;
    boolean isFunction;
    static final Macro DO_NOTHING_MACRO = new Macro(".pass", 
            Collections.EMPTY_LIST, 
            freemarker.template.utility.Collections12.EMPTY_MAP,
            TextBlock.EMPTY_BLOCK);
    
    Macro(String name, List argumentNames, Map args, 
            TemplateElement nestedBlock) 
    {
        this.name = name;
        this.argumentNames = (String[])argumentNames.toArray(
                new String[argumentNames.size()]);
        this.args = args;
        this.nestedBlock = nestedBlock;
    }

    public String getCatchAll() {
        return catchAll;
    }
    
    public void setCatchAll(String value) {
        catchAll = value;
    }

    public String[] getArgumentNames() {
        return (String[])argumentNames.clone();
    }

    String[] getArgumentNamesInternal() {
        return argumentNames;
    }

    boolean hasArgNamed(String name) {
        return args.containsKey(name);
    }

    public String getName() {
        return name;
    }

    void accept(Environment env) {
        env.visitMacroDef(this);
    }

    public String getCanonicalForm() {
        String directiveName = isFunction ? "function" : "macro";
        StringBuffer buf = new StringBuffer("<#");
        buf.append(directiveName);
        buf.append(' ');
        buf.append(name);
        buf.append('(');
        int size = argumentNames.length;
        for (int i = 0; i<size; i++) {
            buf.append(argumentNames[i]);
            if (i != (size-1)) {
                buf.append(",");
            }
        }
        buf.append(")>");
        if (nestedBlock != null) {
            buf.append(nestedBlock.getCanonicalForm());
        }
        buf.append("</#");
        buf.append(directiveName);
        buf.append('>');
        return buf.toString();
    }

    public String getDescription() {
        return (isFunction() ? "function " : "macro ") + name;
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
                for(int i = 0; i < argumentNames.length; ++i) {
                    String argName = argumentNames[i];
                    if(localVars.get(argName) == null) {
                        Expression valueExp = (Expression) args.get(argName);
                        if (valueExp != null) {
                            try {
                                TemplateModel tm = valueExp.getAsTemplateModel(env);
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
                        else {
                            throw new TemplateException(
                                    "When calling macro " + StringUtil.jQuote(name) 
                                    + ", required parameter " + StringUtil.jQuote(argName)
                                    + " (parameter #" + (i + 1) + ") was "
                                    + (localVars.containsKey(argName)
                                            ? "specified, but had null/missing value.\n"
                                              + "(Hint: If the parameter value expression is known to be legally "
                                              + "null/missing, you may want to specify a default value with the \"!\" "
                                              + "operator, like paramValueExpression!defaultValueExpression.)"
                                            : "not specified.\n"
                                    		  + "(Hint: If the omission was deliberate, you may consider making "
                                              + "the parameter optional in the macro by specifying a default value for "
                                              + "it, like "
                                              + StringUtil.encloseAsTag(
                                                      getTemplate(), "#macro myMacro paramName=defaultExpr")
                                              + ".)"),
                                    env);
                        }
                    }
                }
            }
            while(resolvedAnArg && hasUnresolvedArg);
            if(hasUnresolvedArg) {
                if(firstReferenceException != null) {
                    throw firstReferenceException;
                }
                else {
                    firstUnresolvedExpression.invalidReferenceException(env);
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
}
