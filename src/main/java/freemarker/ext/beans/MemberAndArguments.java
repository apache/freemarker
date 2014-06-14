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

import java.lang.reflect.InvocationTargetException;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 */
class MemberAndArguments extends MaybeEmptyMemberAndArguments {
    
    private final CallableMemberDescriptor callableMemberDesc;
    private final Object[] args;
    
    /**
     * @param args The already unwrapped arguments
     */
    MemberAndArguments(CallableMemberDescriptor memberDesc, Object[] args) {
        this.callableMemberDesc = memberDesc;
        this.args = args;
    }
    
    /**
     * The already unwrapped arguments.
     */
    Object[] getArgs() {
        return args;
    }
    
    TemplateModel invokeMethod(BeansWrapper bw, Object obj)
            throws TemplateModelException, InvocationTargetException, IllegalAccessException {
        return callableMemberDesc.invokeMethod(bw, obj, args);
    }

    Object invokeConstructor(BeansWrapper bw)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
            TemplateModelException {
        return callableMemberDesc.invokeConstructor(bw, args);
    }
    
    CallableMemberDescriptor getCallableMemberDescriptor() {
        return callableMemberDesc;
    }
    
}
