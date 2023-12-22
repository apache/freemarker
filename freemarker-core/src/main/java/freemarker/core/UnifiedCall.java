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

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import freemarker.template.EmptyMap;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateTransformModel;
import freemarker.template.utility.ObjectFactory;
import freemarker.template.utility.StringUtil;

/**
 * An element for calling a macro/directive/transform.
 */
final class UnifiedCall extends TemplateElement implements DirectiveCallPlace {

    private Expression nameExp;
    private Map<String, ? extends Expression> namedArgs;
    private List<? extends Expression> positionalArgs;
    private List<String> bodyParameterNames;
    boolean legacySyntax;
    private transient volatile SoftReference/*List<Map.Entry<String,Expression>>*/ sortedNamedArgsCache;
    private CustomDataHolder customDataHolder;

    UnifiedCall(Expression nameExp,
         Map<String, ? extends Expression> namedArgs,
         TemplateElements children,
         List<String> bodyParameterNames) {
        this.nameExp = nameExp;
        this.namedArgs = namedArgs;
        setChildren(children);
        this.bodyParameterNames = bodyParameterNames;
    }

    UnifiedCall(Expression nameExp,
         List<? extends Expression> positionalArgs,
         TemplateElements children,
         List<String> bodyParameterNames) {
        this.nameExp = nameExp;
        this.positionalArgs = positionalArgs;
        setChildren(children);
        this.bodyParameterNames = bodyParameterNames;
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        TemplateModel tm = nameExp.eval(env);
        if (tm == Macro.DO_NOTHING_MACRO) return null; // shortcut here.
        if (tm instanceof Macro) {
            Macro macro = (Macro) tm;
            if (macro.isFunction() && !legacySyntax) {
                throw new _MiscTemplateException(env,
                        "Routine ", new _DelayedJQuote(macro.getName()), " is a function, not a directive. "
                        + "Functions can only be called from expressions, like in ${f()}, ${x + f()} or ",
                        "<@someDirective someParam=f() />", ".");
            }    
            env.invokeMacro(macro, namedArgs, positionalArgs, bodyParameterNames, this);
        } else {
            boolean isDirectiveModel = tm instanceof TemplateDirectiveModel; 
            if (isDirectiveModel || tm instanceof TemplateTransformModel) {
                Map args;
                if (namedArgs != null && !namedArgs.isEmpty()) {
                    args = new HashMap();
                    for (Iterator it = namedArgs.entrySet().iterator(); it.hasNext(); ) {
                        Map.Entry entry = (Map.Entry) it.next();
                        String key = (String) entry.getKey();
                        Expression valueExp = (Expression) entry.getValue();
                        TemplateModel value = valueExp.eval(env);
                        args.put(key, value);
                    }
                } else {
                    args = EmptyMap.instance;
                }
                if (isDirectiveModel) {
                    env.visit(getChildBuffer(), (TemplateDirectiveModel) tm, args, bodyParameterNames);
                } else { 
                    env.visitAndTransform(getChildBuffer(), (TemplateTransformModel) tm, args);
                }
            } else if (tm == null) {
                throw InvalidReferenceException.getInstance(nameExp, env);
            } else {
                throw new NonUserDefinedDirectiveLikeException(nameExp, tm, env);
            }
        }
        return null;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append('@');
        _MessageUtil.appendExpressionAsUntearable(sb, nameExp);
        boolean nameIsInParen = sb.charAt(sb.length() - 1) == ')';
        if (positionalArgs != null) {
            for (int i = 0; i < positionalArgs.size(); i++) {
                Expression argExp = positionalArgs.get(i);
                if (i != 0) {
                    sb.append(',');
                }
                sb.append(' ');
                sb.append(argExp.getCanonicalForm());
            }
        } else {
            List entries = getSortedNamedArgs();
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry entry = (Map.Entry) entries.get(i);
                Expression argExp = (Expression) entry.getValue();
                sb.append(' ');
                sb.append(_CoreStringUtils.toFTLTopLevelIdentifierReference((String) entry.getKey()));
                sb.append('=');
                _MessageUtil.appendExpressionAsUntearable(sb, argExp);
            }
        }
        if (bodyParameterNames != null && !bodyParameterNames.isEmpty()) {
            sb.append("; ");
            for (int i = 0; i < bodyParameterNames.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(bodyParameterNames.get(i)));
            }
        }
        if (canonical) {
            if (getChildCount() == 0) {
                sb.append("/>");
            } else {
                sb.append('>');
                sb.append(getChildrenCanonicalForm());
                sb.append("</@");
                if (!nameIsInParen
                        && (nameExp instanceof Identifier
                            || (nameExp instanceof Dot && ((Dot) nameExp).onlyHasIdentifiers()))) {
                    sb.append(nameExp.getCanonicalForm());
                }
                sb.append('>');
            }
        }
        return sb.toString();
    }

    @Override
    String getNodeTypeSymbol() {
        return "@";
    }

    @Override
    int getParameterCount() {
        return 1/*nameExp*/
                + (positionalArgs != null ? positionalArgs.size() : 0)
                + (namedArgs != null ? namedArgs.size() * 2 : 0)
                + (bodyParameterNames != null ? bodyParameterNames.size() : 0);
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx == 0) {
            return nameExp;
        } else {
            int base = 1;
            final int positionalArgsSize = positionalArgs != null ? positionalArgs.size() : 0;  
            if (idx - base < positionalArgsSize) {
                return positionalArgs.get(idx - base);
            } else {
                base += positionalArgsSize;
                final int namedArgsSize = namedArgs != null ? namedArgs.size() : 0;
                if (idx - base < namedArgsSize * 2) {
                    Map.Entry namedArg = (Map.Entry) getSortedNamedArgs().get((idx - base) / 2);
                    return (idx - base) % 2 == 0 ? namedArg.getKey() : namedArg.getValue();
                } else {
                    base += namedArgsSize * 2;
                    final int bodyParameterNamesSize = bodyParameterNames != null ? bodyParameterNames.size() : 0;
                    if (idx - base < bodyParameterNamesSize) {
                        return bodyParameterNames.get(idx - base);
                    } else {
                        throw new IndexOutOfBoundsException();
                    }
                }
            }
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx == 0) {
            return ParameterRole.CALLEE;
        } else {
            int base = 1;
            final int positionalArgsSize = positionalArgs != null ? positionalArgs.size() : 0;  
            if (idx - base < positionalArgsSize) {
                return ParameterRole.ARGUMENT_VALUE;
            } else {
                base += positionalArgsSize;
                final int namedArgsSize = namedArgs != null ? namedArgs.size() : 0;
                if (idx - base < namedArgsSize * 2) {
                    return (idx - base) % 2 == 0 ? ParameterRole.ARGUMENT_NAME : ParameterRole.ARGUMENT_VALUE;
                } else {
                    base += namedArgsSize * 2;
                    final int bodyParameterNamesSize = bodyParameterNames != null ? bodyParameterNames.size() : 0;
                    if (idx - base < bodyParameterNamesSize) {
                        return ParameterRole.TARGET_LOOP_VARIABLE;
                    } else {
                        throw new IndexOutOfBoundsException();
                    }
                }
            }
        }
    }
    
    /**
     * Returns the named args by source-code order; it's not meant to be used during template execution, too slow for
     * that!
     */
    private List/*<Map.Entry<String, Expression>>*/ getSortedNamedArgs() {
        Reference ref = sortedNamedArgsCache;
        if (ref != null) {
            List res = (List) ref.get();
            if (res != null) return res;
        }
        
        List res = MiscUtil.sortMapOfExpressions(namedArgs);
        sortedNamedArgsCache = new SoftReference(res);
        return res;
    }

    @Override
    @SuppressFBWarnings(value={ "IS2_INCONSISTENT_SYNC", "DC_DOUBLECHECK" }, justification="Performance tricks")
    public Object getOrCreateCustomData(Object providerIdentity, ObjectFactory objectFactory)
            throws CallPlaceCustomDataInitializationException {
        // We are using double-checked locking, utilizing Java memory model "final" trick.
        // Note that this.customDataHolder is NOT volatile.
        
        CustomDataHolder customDataHolder = this.customDataHolder;  // Findbugs false alarm
        if (customDataHolder == null) {  // Findbugs false alarm
            synchronized (this) {
                customDataHolder = this.customDataHolder;
                if (customDataHolder == null || customDataHolder.providerIdentity != providerIdentity) {
                    customDataHolder = createNewCustomData(providerIdentity, objectFactory);
                    this.customDataHolder = customDataHolder; 
                }
            }
        }
        
        if (customDataHolder.providerIdentity != providerIdentity) {
            synchronized (this) {
                customDataHolder = this.customDataHolder;
                if (customDataHolder == null || customDataHolder.providerIdentity != providerIdentity) {
                    customDataHolder = createNewCustomData(providerIdentity, objectFactory);
                    this.customDataHolder = customDataHolder;
                }
            }
        }
        
        return customDataHolder.customData;
    }

    private CustomDataHolder createNewCustomData(Object provierIdentity, ObjectFactory objectFactory)
            throws CallPlaceCustomDataInitializationException {
        CustomDataHolder customDataHolder;
        Object customData;
        try {
            customData = objectFactory.createObject();
        } catch (Exception e) {
            throw new CallPlaceCustomDataInitializationException(
                    "Failed to initialize custom data for provider identity "
                    + StringUtil.tryToString(provierIdentity) + " via factory "
                    + StringUtil.tryToString(objectFactory), e);
        }
        if (customData == null) {
            throw new NullPointerException("ObjectFactory.createObject() has returned null");
        }
        customDataHolder = new CustomDataHolder(provierIdentity, customData);
        return customDataHolder;
    }

    @Override
    public boolean isNestedOutputCacheable() {
        return isChildrenOutputCacheable();
    }
    
/*
    //REVISIT
    boolean heedsOpeningWhitespace() {
        return nestedBlock == null;
    }

    //REVISIT
    boolean heedsTrailingWhitespace() {
        return nestedBlock == null;
    }*/
    
    /**
     * Used for implementing double check locking in implementing the
     * {@link DirectiveCallPlace#getOrCreateCustomData(Object, ObjectFactory)}.
     */
    private static class CustomDataHolder {
        
        private final Object providerIdentity;
        private final Object customData;
        public CustomDataHolder(Object providerIdentity, Object customData) {
            this.providerIdentity = providerIdentity;
            this.customData = customData;
        }
        
    }
    
    @Override
    boolean isNestedBlockRepeater() {
        return true;
    }

    @Override
    boolean isShownInStackTrace() {
        return true;
    }
    
}
