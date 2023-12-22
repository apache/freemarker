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
package org.apache.freemarker.core.model;

import java.io.Writer;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.impl.JavaMethodModel;
import org.apache.freemarker.core.util.CallableUtils;

/**
 * A {@link TemplateCallableModel}, which returns its result as a {@link TemplateModel} at the end of its execution.
 * This is in contrast with {@link TemplateDirectiveModel}, which writes its result progressively to the output, if it
 * has output at all. Also, {@link TemplateFunctionModel}-s can only be called where an expression is expected. (If
 * some future template languages allows calling functions outside expression context, on the top-level, then
 * that's a shorthand to doing that in with interpolation, like {@code ${f()}}.)
 * <p>
 * Example usage in templates: {@code < a href="${my.toProductURL(product.id)}">},
 * {@code <#list my.groupByFirstLetter(products, property="name") as productGroup>}
 * <p>
 * You can find utilities for implementing {@link TemplateFunctionModel}-s in {@link CallableUtils}.
 */
public interface TemplateFunctionModel extends TemplateCallableModel {

    /**
     * Invokes the function.
     *
     * @param args
     *         See the similar parameter of {@link TemplateDirectiveModel#execute(TemplateModel[], CallPlace, Writer,
     *         Environment)}
     * @param callPlace
     *         See the similar parameter of {@link TemplateDirectiveModel#execute(TemplateModel[], CallPlace, Writer,
     *         Environment)}
     * @param env
     *         See the similar parameter of {@link TemplateDirectiveModel#execute(TemplateModel[], CallPlace, Writer,
     *         Environment)}
     *
     * @return The return value of the function.
     */
    TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env) throws TemplateException;

    /**
     * Returns the argument array layout to use when calling the {@code {@link #execute(TemplateModel[], CallPlace,
     * Environment)}} method, or rarely {@code null}. If it's {@code null} then there can only be positional
     * arguments, any number of them (though of course the {@code execute} method implementation itself may restrict
     * the acceptable argument count), and the argument array will be simply as long as the number of arguments
     * specified at the call place. This layoutless mode is for example used by {@link JavaMethodModel}-s.
     */
    ArgumentArrayLayout getFunctionArgumentArrayLayout();

}
