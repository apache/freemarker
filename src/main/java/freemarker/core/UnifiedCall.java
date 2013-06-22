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

/**
 * An element for the unified macro/transform syntax. 
 */
final class UnifiedCall extends TemplateElement {

    private Expression nameExp;
    private Map namedArgs;
    private List positionalArgs, bodyParameterNames;
    boolean legacySyntax;
    private transient volatile SoftReference/*List<Map.Entry<String,Expression>>*/ sortedNamedArgsCache;

    UnifiedCall(Expression nameExp,
         Map namedArgs,
         TemplateElement nestedBlock,
         List bodyParameterNames) 
    {
        this.nameExp = nameExp;
        this.namedArgs = namedArgs;
        this.nestedBlock = nestedBlock;
        this.bodyParameterNames = bodyParameterNames;
    }

    UnifiedCall(Expression nameExp,
         List positionalArgs,
         TemplateElement nestedBlock,
         List bodyParameterNames) 
    {
        this.nameExp = nameExp;
        this.positionalArgs = positionalArgs;
        if (nestedBlock == TextBlock.EMPTY_BLOCK) {
            nestedBlock = null;
        }
        this.nestedBlock = nestedBlock;
        this.bodyParameterNames = bodyParameterNames;
    }

    void accept(Environment env) throws TemplateException, IOException {
        TemplateModel tm = nameExp.eval(env);
        if (tm == Macro.DO_NOTHING_MACRO) return; // shortcut here.
        if (tm instanceof Macro) {
            Macro macro = (Macro) tm;
            if (macro.isFunction && !legacySyntax) {
                throw new _MiscTemplateException(env, new Object[] {
                        "Routine ", new _DelayedJQuote(macro.getName()), " is a function, not a directive. "
                        + "Functions can only be called from expressions, like in ${f()}, ${x + f()} or ",
                        "<@someDirective someParam=f() />", "." });
            }    
            env.visit(macro, namedArgs, positionalArgs, bodyParameterNames,
                    nestedBlock);
        }
        else {
            boolean isDirectiveModel = tm instanceof TemplateDirectiveModel; 
            if (isDirectiveModel || tm instanceof TemplateTransformModel) {
                Map args;
                if (namedArgs != null && !namedArgs.isEmpty()) {
                    args = new HashMap();
                    for (Iterator it = namedArgs.entrySet().iterator(); it.hasNext();) {
                        Map.Entry entry = (Map.Entry) it.next();
                        String key = (String) entry.getKey();
                        Expression valueExp = (Expression) entry.getValue();
                        TemplateModel value = valueExp.eval(env);
                        args.put(key, value);
                    }
                } else {
                    args = EmptyMap.instance;
                }
                if(isDirectiveModel) {
                    env.visit(nestedBlock, (TemplateDirectiveModel) tm, args, 
                            bodyParameterNames);
                }
                else { 
                    env.visitAndTransform(nestedBlock, (TemplateTransformModel) tm, args);
                }
            }
            else if (tm == null) {
                throw InvalidReferenceException.getInstance(nameExp, env);
            } else {
                throw new UnexpectedTypeException(nameExp, tm, "user-defined directive (macro, etc.)", env);
            }
        }
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append('@');
        MessageUtil.appendExpressionAsUntearable(sb, nameExp);
        boolean nameIsInParen = sb.charAt(sb.length() - 1) == ')';
        if (positionalArgs != null) {
            for (int i=0; i < positionalArgs.size(); i++) {
                Expression argExp = (Expression) positionalArgs.get(i);
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
                sb.append(entry.getKey());
                sb.append('=');
                MessageUtil.appendExpressionAsUntearable(sb, argExp);
            }
        }
        if (canonical) {
            if (nestedBlock == null) {
                sb.append("/>");
            } 
            else {
                sb.append('>');
                sb.append(nestedBlock.getCanonicalForm());
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

    String getNodeTypeSymbol() {
        return "@";
    }

    int getParameterCount() {
        return 1/*nameExp*/
                + (positionalArgs != null ? positionalArgs.size() : 0)
                + (namedArgs != null ? namedArgs.size() * 2 : 0)
                + (bodyParameterNames != null ? bodyParameterNames.size() : 0);
    }

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
    
/*
    //REVISIT
    boolean heedsOpeningWhitespace() {
        return nestedBlock == null;
    }

    //REVISIT
    boolean heedsTrailingWhitespace() {
        return nestedBlock == null;
    }*/
}
