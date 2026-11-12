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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    Map<String, Expression> namedArgs;
    private transient volatile SoftReference<List<Map.Entry<String,Expression>>> sortedNamedArgsCache;

    /**
     * Creates new TransformBlock, with a given transformation
     */
    TransformBlock(Expression transformExpression, 
                   Map<String, Expression> namedArgs,
                   TemplateElements children) {
        this.transformExpression = transformExpression;
        this.namedArgs = namedArgs;
        setChildren(children);
    }

    @Override
    TemplateElement[] accept(Environment env)
    throws TemplateException, IOException {
        TemplateTransformModel ttm = env.getTransform(transformExpression);
        if (ttm != null) {
            Map<String, TemplateModel> args;
            if (namedArgs != null && !namedArgs.isEmpty()) {
                args = new HashMap<>();
                for (Map.Entry<String, Expression> entry : namedArgs.entrySet()) {
                    String key = entry.getKey();
                    Expression valueExp = entry.getValue();
                    TemplateModel value = valueExp.eval(env);
                    args.put(key, value);
                }
            } else {
                args = Collections.emptyMap();
            }
            env.visitAndTransform(getChildBuffer(), ttm, args);
        } else {
            TemplateModel tm = transformExpression.eval(env);
            throw new UnexpectedTypeException(
                    transformExpression, tm,
                    "transform", new Class[] { TemplateTransformModel.class }, env);
        }
        return null;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        sb.append(' ');
        sb.append(transformExpression);
        if (namedArgs != null) {
            for (Map.Entry<String, Expression> entry : getSortedNamedArgs()) {
                sb.append(' ');
                sb.append(entry.getKey());
                sb.append('=');
                _MessageUtil.appendExpressionAsUntearable(sb, entry.getValue());
            }
        }
        if (canonical) {
            sb.append(">");
            sb.append(getChildrenCanonicalForm());
            sb.append("</").append(getNodeTypeSymbol()).append('>');
        }
        return sb.toString();
    }
    
    @Override
    String getNodeTypeSymbol() {
        return "#transform";
    }
    
    @Override
    int getParameterCount() {
        return 1/*nameExp*/ + (namedArgs != null ? namedArgs.size() * 2 : 0);
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx == 0) {
            return transformExpression;
        } else if (namedArgs != null && idx - 1 < namedArgs.size() * 2) {
            Map.Entry<String, Expression> namedArg = getSortedNamedArgs().get((idx - 1) / 2);
            return (idx - 1) % 2 == 0 ? namedArg.getKey() : namedArg.getValue();
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
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
    private List<Map.Entry<String, Expression>> getSortedNamedArgs() {
        Reference<List<Map.Entry<String,Expression>>> ref = sortedNamedArgsCache;
        if (ref != null) {
            List<Map.Entry<String, Expression>>  res = ref.get();
            if (res != null) return res;
        }
        
        List<Map.Entry<String, Expression>>  res = MiscUtil.sortMapOfExpressions(namedArgs);
        sortedNamedArgsCache = new SoftReference<>(res);
        return res;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }

    @Override
    boolean isShownInStackTrace() {
        return true;
    }
    
}
