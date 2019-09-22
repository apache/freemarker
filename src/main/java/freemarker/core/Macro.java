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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * An element representing a macro or function declaration.
 * 
 * @deprecated Subject to be changed or renamed any time; no "stable" replacement exists yet.
 */
@Deprecated
public final class Macro extends TemplateElement implements TemplateModel {

    static final Macro DO_NOTHING_MACRO = new Macro(".pass", 
            Collections.EMPTY_MAP,
            null, false,
            TemplateElements.EMPTY);
    
    final static int TYPE_MACRO = 0;
    final static int TYPE_FUNCTION = 1;
    
    private final String name;
    private final String[] paramNames;
    private final Map<String, Expression> paramNamesWithDefault;
    private final SpreadArgs spreadArgs;
    private final String catchAllParamName;
    private final boolean function;
    private final Object namespaceLookupKey;

    /**
     * @param paramNamesWithDefault Maps the parameter names to its default value expression, or to {@code null} if
     *      there's no default value. As parameter order is significant; use {@link LinkedHashMap} or similar.
     *      This doesn't include the catch-all parameter (as that can be specified by name on the caller side).
     */
    Macro(String name,
            Map<String, Expression> paramNamesWithDefault,
            String catchAllParamName, boolean function,
            TemplateElements children) {
        // Attention! Keep this constructor in sync with the other constructor!
        this.name = name;
        this.paramNamesWithDefault = paramNamesWithDefault;
        this.paramNames = paramNamesWithDefault.keySet().toArray(new String[0]);
        this.catchAllParamName = catchAllParamName;
        this.spreadArgs = null;
        this.function = function;
        this.setChildren(children);
        this.namespaceLookupKey = this;
        // Attention! Keep this constructor in sync with the other constructor!
    }

    /**
     * Copy-constructor with replacing {@link #spreadArgs} (with the quirk that the parent of the
     * child elements will stay the copied macro).
     *
     * @param spreadArgs Usually {@code null}; used by {@link BuiltInsForCallables.spread_argsBI} to
     *      set arbitrary default value to parameters. Note that the defaults aren't
     *      {@link Expression}-s, but {@link TemplateModel}-s.
     */
    Macro(Macro that, SpreadArgs spreadArgs) {
        // Attention! Keep this constructor in sync with the other constructor!
        this.name = that.name;
        this.paramNamesWithDefault = that.paramNamesWithDefault;
        this.paramNames = that.paramNames;
        this.catchAllParamName = that.catchAllParamName;
        this.spreadArgs = spreadArgs; // Using the argument value here
        this.function = that.function;
        this.namespaceLookupKey = that.namespaceLookupKey;
        super.copyFieldsFrom(that);
        // Attention! Keep this constructor in sync with the other constructor!
    }

    public String getCatchAll() {
        return catchAllParamName;
    }
    
    public String[] getArgumentNames() {
        return paramNames.clone();
    }

    String[] getArgumentNamesInternal() {
        return paramNames;
    }

    boolean hasArgNamed(String name) {
        return paramNamesWithDefault.containsKey(name);
    }
    
    public String getName() {
        return name;
    }

    public SpreadArgs getSpreadArgs() {
        return spreadArgs;
    }

    public Object getNamespaceLookupKey() {
        return namespaceLookupKey;
    }

    @Override
    TemplateElement[] accept(Environment env) {
        env.visitMacroDef(this);
        return null;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        if (spreadArgs != null) {
            // As such a node won't be part of a template, this is probably never needed.
            sb.append('?')
                    .append(getTemplate().getActualNamingConvention() == Configuration.CAMEL_CASE_NAMING_CONVENTION
                            ? BuiltIn.BI_NAME_CAMEL_CASE_SPREAD_ARGS
                            : BuiltIn.BI_NAME_SNAKE_CASE_SPREAD_ARGS)
                    .append("(...)");
        }
        sb.append(' ');
        sb.append(_CoreStringUtils.toFTLTopLevelTragetIdentifier(name));
        if (function) sb.append('(');
        int argCnt = paramNames.length;
        for (int i = 0; i < argCnt; i++) {
            if (function) {
                if (i != 0) {
                    sb.append(", ");
                }
            } else {
                sb.append(' ');
            }

            String paramName = paramNames[i];
            sb.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(paramName));

            Expression paramDefaultExp = (Expression) paramNamesWithDefault.get(paramName);
            if (paramDefaultExp != null) {
                sb.append('=');
                if (function) {
                    sb.append(paramDefaultExp.getCanonicalForm());
                } else {
                    _MessageUtil.appendExpressionAsUntearable(sb, paramDefaultExp);
                }
            }
        }
        if (catchAllParamName != null) {
            if (function) {
                if (argCnt != 0) {
                    sb.append(", ");
                }
            } else {
                sb.append(' ');
            }
            sb.append(catchAllParamName);
            sb.append("...");
        }
        if (function) sb.append(')');
        if (canonical) {
            sb.append('>');
            sb.append(getChildrenCanonicalForm());
            sb.append("</").append(getNodeTypeSymbol()).append('>');
        }
        return sb.toString();
    }
    
    @Override
    String getNodeTypeSymbol() {
        return function ? "#function" : "#macro";
    }
    
    public boolean isFunction() {
        return function;
    }

    class Context implements LocalContext {
        final Environment.Namespace localVars; 
        final TemplateObject callPlace;
        final Environment.Namespace nestedContentNamespace;
        final List<String> nestedContentParameterNames;
        final LocalContextStack prevLocalContextStack;
        final Context prevMacroContext;
        
        Context(Environment env, 
                TemplateObject callPlace,
                List<String> nestedContentParameterNames) {
            this.localVars = env.new Namespace(); 
            this.callPlace = callPlace;
            this.nestedContentNamespace = env.getCurrentNamespace();
            this.nestedContentParameterNames = nestedContentParameterNames;
            this.prevLocalContextStack = env.getLocalContextStack();
            this.prevMacroContext = env.getCurrentMacroContext();
        }
                
        
        Macro getMacro() {
            return Macro.this;
        }

        // Set default parameters, check if all the required parameters are defined.
        void checkParamsSetAndApplyDefaults(Environment env) throws TemplateException {
            boolean resolvedAnArg, hasUnresolvedArg;
            Expression firstUnresolvedExpression;
            InvalidReferenceException firstReferenceException;
            do {
                firstUnresolvedExpression = null;
                firstReferenceException = null;
                resolvedAnArg = hasUnresolvedArg = false;
                for (int i = 0; i < paramNames.length; ++i) {
                    String argName = paramNames[i];
                    if (localVars.get(argName) == null) {
                        Expression defaultValueExp = paramNamesWithDefault.get(argName);
                        if (defaultValueExp != null) {
                            try {
                                TemplateModel tm = defaultValueExp.eval(env);
                                if (tm == null) {
                                    if (!hasUnresolvedArg) {
                                        firstUnresolvedExpression = defaultValueExp;
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
                                    new _ErrorDescriptionBuilder(
                                            "When calling ", (isFunction() ? "function" : "macro"), " ",
                                            new _DelayedJQuote(name), 
                                            ", required parameter ", new _DelayedJQuote(argName),
                                            " (parameter #", Integer.valueOf(i + 1), ") was ", 
                                            (argWasSpecified
                                                    ? "specified, but had null/missing value."
                                                    : "not specified.") 
                                    ).tip(argWasSpecified
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
                result.add(((TemplateScalarModel) it.next()).getAsString());
            }
            return result;
        }
    }

    @Override
    int getParameterCount() {
        return 1/*name*/ + paramNames.length * 2/*name=default*/ + 1/*catchAll*/ + 1/*type*/;
    }

    @Override
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
                    return paramNamesWithDefault.get(paramName);
                }
            } else if (idx == argDescsEnd) {
                return catchAllParamName;
            } else if (idx == argDescsEnd + 1) {
                return Integer.valueOf(function ? TYPE_FUNCTION : TYPE_MACRO);
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    @Override
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

    @Override
    boolean isNestedBlockRepeater() {
        // Because of recursive calls
        return true;
    }

    static final class SpreadArgs {
        private final TemplateHashModelEx byName;
        private final TemplateSequenceModel byPosition;

        SpreadArgs(TemplateHashModelEx byName) {
            this.byName = byName;
            this.byPosition = null;
        }

        SpreadArgs(TemplateSequenceModel byPosition) {
            this.byName = null;
            this.byPosition = byPosition;
        }

        public TemplateHashModelEx getByName() {
            return byName;
        }

        public TemplateSequenceModel getByPosition() {
            return byPosition;
        }
    }
    
}
