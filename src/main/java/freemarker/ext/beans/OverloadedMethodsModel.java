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

package freemarker.ext.beans;


import java.util.Collections;
import java.util.List;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * Wraps a set of same-name overloaded methods behind {@link freemarker.template.TemplateMethodModel} interface,
 * like if it was a single method, chooses among them behind the scenes on call-time based on the argument values.
 */
public class OverloadedMethodsModel
implements
	TemplateMethodModelEx,
	TemplateSequenceModel
{
    private final Object object;
    private final OverloadedMethods overloadedMethods;
    private final BeansWrapper wrapper;
    
    OverloadedMethodsModel(Object object, OverloadedMethods overloadedMethods, BeansWrapper wrapper)
    {
        this.object = object;
        this.overloadedMethods = overloadedMethods;
        this.wrapper = wrapper;
    }

    /**
     * Invokes the method, passing it the arguments from the list. The actual
     * method to call from several overloaded methods will be chosen based
     * on the classes of the arguments.
     * @throws TemplateModelException if the method cannot be chosen
     * unambiguously.
     */
    public Object exec(List arguments)
    throws
        TemplateModelException
    {
        MemberAndArguments maa = overloadedMethods.getMemberAndArguments(arguments, wrapper);
        try {
            return maa.invokeMethod(wrapper, object);
        }
        catch(Exception e)
        {
            if (e instanceof TemplateModelException) throw (TemplateModelException) e;
            
            throw _MethodUtil.newInvocationTemplateModelException(
                    object,
                    maa.getCallableMemberDescriptor(),
                    e);
        }
    }

    public TemplateModel get(int index) throws TemplateModelException
    {
        return (TemplateModel) exec(Collections.singletonList(
                new SimpleNumber(new Integer(index))));
    }

    public int size() throws TemplateModelException
    {
        throw new TemplateModelException("?size is unsupported for " + getClass().getName());
    }
}
