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

package freemarker.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

/**
 * The local variables and such of an FTL macro or FTL function (or other future FTL callable) call.
 */
class CallableInvocationContext implements LocalContext {
    final UnboundCallable callableDefinition;
    final Environment.Namespace localVars; 
    final TemplateElement[] nestedContentBuffer;
    final Environment.Namespace nestedContentNamespace;
    final Template nestedContentTemplate;
    final List nestedContentParameterNames;
    final LocalContextStack prevLocalContextStack;
    final CallableInvocationContext prevMacroContext;
    
    CallableInvocationContext(UnboundCallable callableDefinition,
            Environment env, 
            TemplateElement[] nestedContentBuffer,
            List nestedContentParameterNames) {
        this.callableDefinition = callableDefinition;
        this.localVars = env.new Namespace();
        this.nestedContentBuffer = nestedContentBuffer;
        this.nestedContentNamespace = env.getCurrentNamespace();
        this.nestedContentTemplate = env.getCurrentTemplate();
        this.nestedContentParameterNames = nestedContentParameterNames;
        this.prevLocalContextStack = env.getLocalContextStack();
        this.prevMacroContext = env.getCurrentMacroContext();
    }
    
    Macro getCallableDefinition() {
        return callableDefinition;
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
            for (int i = 0; i < callableDefinition.getParamNames().length; ++i) {
                String argName = callableDefinition.getParamNames()[i];
                if (localVars.get(argName) == null) {
                    Expression valueExp = (Expression) callableDefinition.getParamDefaults().get(argName);
                    if (valueExp != null) {
                        try {
                            TemplateModel tm = valueExp.eval(env);
                            if (tm == null) {
                                if (!hasUnresolvedArg) {
                                    firstUnresolvedExpression = valueExp;
                                    hasUnresolvedArg = true;
                                }
                            } else {
                                localVars.put(argName, tm);
                                resolvedAnArg = true;
                            }
                        } catch (InvalidReferenceException e) {
                            if (!hasUnresolvedArg) {
                                hasUnresolvedArg = true;
                                firstReferenceException = e;
                            }
                        }
                    } else if (!env.isClassicCompatible()) {
                        boolean argWasSpecified = localVars.containsKey(argName);
                        throw new _MiscTemplateException(env,
                                new _ErrorDescriptionBuilder(new Object[] {
                                        "When calling macro ", new _DelayedJQuote(callableDefinition.getName()), 
                                        ", required parameter ", new _DelayedJQuote(argName),
                                        " (parameter #", Integer.valueOf(i + 1), ") was ", 
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
        } while (resolvedAnArg && hasUnresolvedArg);
        if (hasUnresolvedArg) {
            if (firstReferenceException != null) {
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
        for (TemplateModelIterator it = localVars.keys().iterator(); it.hasNext(); ) {
            result.add(it.next().toString());
        }
        return result;
    }
}