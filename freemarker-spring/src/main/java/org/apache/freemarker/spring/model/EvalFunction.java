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

package org.apache.freemarker.spring.model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.web.servlet.support.RequestContext;

/**
 * A <code>TemplateFunctionModel</code> providing functionality equivalent to the Spring Framework's
 * <code>&lt;spring:eval /&gt;</code> JSP Tag Library.
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;spring:message /&gt;</code> JSP Tag Library, this function
 * does not support <code>htmlEscape</code> parameter. It always returns the message not to escape HTML's
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */
public class EvalFunction extends AbstractSpringTemplateFunctionModel {

    public static final String NAME = "eval";

    private static final int EXPRESSION_PARAM_IDX = 0;

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(1, false, null, false);

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public EvalFunction(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public TemplateModel executeInternal(TemplateModel[] args, CallPlace callPlace, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException {
        final String expressionString = CallableUtils.getStringArgument(args, EXPRESSION_PARAM_IDX, this);
        final Expression expression = expressionParser.parseExpression(expressionString);

        // TODO: cache evaluationContext somewhere in request level....
        EvaluationContext evaluationContext = createEvaluationContext(env, requestContext);

        final Object result = expression.getValue(evaluationContext);
        return wrapObject(objectWrapperAndUnwrapper, result);
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    private EvaluationContext createEvaluationContext(final Environment env, final RequestContext requestContext) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        context.addPropertyAccessor(new EnvironmentVariablesPropertyAccessor(env));
        context.addPropertyAccessor(new MapAccessor());
        context.addPropertyAccessor(new EnvironmentAccessor());
        context.setBeanResolver(new BeanFactoryResolver(requestContext.getWebApplicationContext()));

        ConversionService conversionService = getConversionService();

        if (conversionService != null) {
            context.setTypeConverter(new StandardTypeConverter(conversionService));
        }

        return context;
    }

    private ConversionService getConversionService() {
        return (ConversionService) getRequest().getAttribute(ConversionService.class.getName());
    }

    private static class EnvironmentVariablesPropertyAccessor implements PropertyAccessor {

        private final Environment env;

        public EnvironmentVariablesPropertyAccessor(final Environment env) {
            this.env = env;
        }

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue)
                throws AccessException {
            // TODO Auto-generated method stub
        }
    }
}
