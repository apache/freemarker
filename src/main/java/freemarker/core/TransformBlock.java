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
            throw new UnexpectedTypeException(
                    transformExpression, tm,
                    "transform", new Class[] { TemplateTransformModel.class }, env);
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

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}
