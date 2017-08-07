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

package org.apache.freemarker.servlet.jsp;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;

/**
 * Used when a custom JSP tag and an EL function uses the same name in a tag library, to invoke a single FTL value from
 * the two. As FTL as no separate namespace for "tags" and functions, both aspect has to be implemented by the same
 * value.
 */
class TemplateDirectiveModelAndTemplateFunctionModel
        implements TemplateDirectiveModel, TemplateFunctionModel {

    private final TemplateDirectiveModel templateDirectiveModel;
    private final TemplateFunctionModel templateFunctionModel;

    TemplateDirectiveModelAndTemplateFunctionModel( //
            TemplateDirectiveModel templateDirectiveModel, TemplateFunctionModel templateMethodModelEx) {
        this.templateDirectiveModel = templateDirectiveModel;
        this.templateFunctionModel = templateMethodModelEx;
    }

    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        templateDirectiveModel.execute(args, callPlace, out, env);
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return templateDirectiveModel.getDirectiveArgumentArrayLayout();
    }

    @Override
    public boolean isNestedContentSupported() {
        return templateDirectiveModel.isNestedContentSupported();
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return templateFunctionModel.getFunctionArgumentArrayLayout();
    }

    @Override
    public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
            throws TemplateException {
        return templateFunctionModel.execute(args, callPlace, env);
    }

}
