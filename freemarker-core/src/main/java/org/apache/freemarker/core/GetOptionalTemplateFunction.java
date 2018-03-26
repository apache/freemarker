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

package org.apache.freemarker.core;

import static org.apache.freemarker.core.util.CallableUtils.*;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.templateresolver.MalformedTemplateNameException;

/**
 * Implements {@code .getOptionalTemplate(name, options)}.
 */
class GetOptionalTemplateFunction implements TemplateFunctionModel {

    static final GetOptionalTemplateFunction INSTANCE = new GetOptionalTemplateFunction();
    
    private static final String RESULT_INCLUDE = "include";
    private static final String RESULT_IMPORT = "import";
    private static final String RESULT_EXISTS = "exists";

    private GetOptionalTemplateFunction() {
        // No op.
    }

    @Override
    public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env) throws TemplateException {
        final String absTemplateName;
        try {
            absTemplateName = env.toFullTemplateName(
                    env.getCurrentTemplate().getLookupName(),
                    getStringArgument(args, 0, this));
        } catch (MalformedTemplateNameException e) {
            throw new TemplateException("Failed to convert template path to full path; see cause exception.", e);
        }

        final Template template;
        try {
            template = env.getTemplateForInclusion(absTemplateName, true);
        } catch (IOException e) {
            throw new TemplateException(
                    e, "Error when trying to include template ", new _DelayedJQuote(absTemplateName),
                    "; see cause exception");
        }
        
        NativeHashEx result = new NativeHashEx();
        result.put(RESULT_EXISTS, template != null ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE);
        // If the template is missing, result.include and such will be missing too, so that a default can be
        // conveniently provided like in <@optTemp.include!myDefaultMacro />.
        if (template != null) {
            result.put(RESULT_INCLUDE, new TemplateDirectiveModel() {
                @Override
                public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
                        throws TemplateException, IOException {
                    env.include(template);
                }

                @Override
                public boolean isNestedContentSupported() {
                    return false;
                }

                @Override
                public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
                    return ArgumentArrayLayout.PARAMETERLESS;
                }
            });
            result.put(RESULT_IMPORT, new TemplateFunctionModel() {
                @Override
                public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                        throws TemplateException {
                    try {
                        return env.importLib(template, null);
                    } catch (IOException e) {
                        throw new TemplateException(e, "Failed to import loaded template; see cause exception");
                    } catch (TemplateException e) {
                        throw new TemplateException(e, "Failed to import loaded template; see cause exception");
                    }
                }

                @Override
                public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                    return ArgumentArrayLayout.PARAMETERLESS;
                }
            });
        }
        return result;
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
    }

}
