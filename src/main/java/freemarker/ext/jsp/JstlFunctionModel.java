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

package freemarker.ext.jsp;

import java.lang.reflect.Method;
import java.util.List;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

/**
 * Wraps the static methods of a class to support JSTL functions.
 */
class JstlFunctionModel implements TemplateMethodModelEx {

    private static final Object [] EMPTY_ARGS = {};

    private final Class functionClass;
    private final Method functionMethod;
    private final Class [] paramTypes;
    private final BeansWrapper wrapper;

    public JstlFunctionModel(Class functionClass, Method functionMethod) {
        this.functionClass = functionClass;
        this.functionMethod = functionMethod;
        paramTypes = functionMethod.getParameterTypes();
        BeansWrapperBuilder builder = new BeansWrapperBuilder(new Version("2.3"));
        wrapper = builder.getResult();
    }

    public Object exec(List arguments) throws TemplateModelException {
        try {
            if (arguments == null) {
                return functionMethod.invoke(functionClass, (Object []) null);
            }
            else {
                return functionMethod.invoke(functionClass, unwrapArguments(arguments));
            }
        } catch (Exception e) {
            throw new TemplateModelException("JSTL function invocation failure.", e);
        }
    }

    private Object [] unwrapArguments(List arguments) throws TemplateModelException {
        if (arguments == null) {
            return null;
        }

        if (arguments.isEmpty()) {
            return EMPTY_ARGS;
        }

        if (paramTypes.length != arguments.size()) {
            throw new TemplateModelException("Wrong arguments size " + arguments.size() + " (Expected: " + paramTypes.length + ").");
        }

        Object [] args = new Object[arguments.size()];
        TemplateModel argModel = null;

        for (int i = 0; i < args.length; i++) {
            argModel = (TemplateModel) arguments.get(i);
            args[i] = wrapper.unwrap(argModel);
        }

        return args;
    }
}
