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
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateTransformModel;

/**
 * A template element that contains a nested block
 * that is transformed according to an instance of T
 * TemplateTransformModel
 */
final class TransformBlock extends TemplateElement {

    private Expression transformExpression;
    Map namedArgs;
    private transient volatile SoftReference/*List<Map.Entry<String,Expression>>*/ sortedNamedArgsCache;

    /**
     * Creates new TransformBlock, with a given transformation
     */
    TransformBlock(Expression transformExpression, 
                   Map namedArgs,
                   TemplateElement nestedBlock) {
        this.transformExpression = transformExpression;
        this.namedArgs = namedArgs;
        this.nestedBlock = nestedBlock;
    }

    void accept(Environment env) 
    throws TemplateException, IOException
    {
        TemplateTransformModel ttm = env.getTransform(transformExpression);
        if (ttm != null) {
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
            env.visitAndTransform(nestedBlock, ttm, args);
        }
        else {
            TemplateModel tm = transformExpression.eval(env);
            throw new UnexpectedTypeException(transformExpression, tm, "transform", env);
        }
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        sb.append(' ');
        sb.append(transformExpression);
        if (namedArgs != null) {
            for (Iterator it = getSortedNamedArgs().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                sb.append(' ');
                sb.append(entry.getKey());
                sb.append('=');
                MessageUtil.appendExpressionAsUntearable(sb, (Expression) entry.getValue());
            }
        }
        if (canonical) {
            sb.append(">");
            if (nestedBlock != null) {
                sb.append(nestedBlock.getCanonicalForm());
            }
            sb.append("</").append(getNodeTypeSymbol()).append('>');
        }
        return sb.toString();
    }
    
    String getNodeTypeSymbol() {
        return "#transform";
    }
    
    int getParameterCount() {
        return 1/*nameExp*/ + (namedArgs != null ? namedArgs.size() * 2 : 0);
    }

    Object getParameterValue(int idx) {
        if (idx == 0) {
            return transformExpression;
        } else if (namedArgs != null && idx - 1 < namedArgs.size() * 2) {
            Map.Entry namedArg = (Map.Entry) getSortedNamedArgs().get((idx - 1) / 2);
            return (idx - 1) % 2 == 0 ? namedArg.getKey() : namedArg.getValue();
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        if (idx == 0) {
            return ParameterRole.CALLEE;
        } else if (idx - 1 < namedArgs.size() * 2) {
                return (idx - 1) % 2 == 0 ? ParameterRole.ARGUMENT_NAME : ParameterRole.ARGUMENT_VALUE;
        } else {
            throw new IndexOutOfBoundsException();
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
    
}
