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
import java.util.Iterator;
import java.util.List;

import freemarker.template.ObjectWrapperAndUnwrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Stores the non-varargs methods for a {@link OverloadedMethods} object.
 */
class OverloadedFixArgsMethods extends OverloadedMethodsSubset {
    
    OverloadedFixArgsMethods(boolean bugfixed) {
        super(bugfixed);
    }

    Class[] preprocessParameterTypes(CallableMemberDescriptor memberDesc) {
        return memberDesc.getParamTypes();
    }
    
    void afterWideningUnwrappingHints(Class[] paramTypes, int[] paramNumericalTypes) {
        // Do nothing
    }

    MaybeEmptyMemberAndArguments getMemberAndArguments(List tmArgs, BeansWrapper unwrapper) 
    throws TemplateModelException {
        if(tmArgs == null) {
            // null is treated as empty args
            tmArgs = Collections.EMPTY_LIST;
        }
        final int argCount = tmArgs.size();
        final Class[][] unwrappingHintsByParamCount = getUnwrappingHintsByParamCount();
        if(unwrappingHintsByParamCount.length <= argCount) {
            return EmptyMemberAndArguments.WRONG_NUMBER_OF_ARGUMENTS;
        }
        Class[] unwarppingHints = unwrappingHintsByParamCount[argCount];
        if(unwarppingHints == null) {
            return EmptyMemberAndArguments.WRONG_NUMBER_OF_ARGUMENTS;
        }
        
        Object[] pojoArgs = new Object[argCount];
        
        int[] typeFlags = getTypeFlags(argCount);
        if (typeFlags == ALL_ZEROS_ARRAY) {
            typeFlags = null;
        }

        Iterator it = tmArgs.iterator();
        for(int i = 0; i < argCount; ++i) {
            Object pojo = unwrapper.tryUnwrapTo(
                    (TemplateModel) it.next(),
                    unwarppingHints[i],
                    typeFlags != null ? typeFlags[i] : 0);
            if(pojo == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                return EmptyMemberAndArguments.noCompatibleOverload(i + 1);
            }
            pojoArgs[i] = pojo;
        }
        
        MaybeEmptyCallableMemberDescriptor maybeEmtpyMemberDesc = getMemberDescriptorForArgs(pojoArgs, false);
        if(maybeEmtpyMemberDesc instanceof CallableMemberDescriptor) {
            CallableMemberDescriptor memberDesc = (CallableMemberDescriptor) maybeEmtpyMemberDesc;
            if (bugfixed) {
                if (typeFlags != null) {
                    // Note that overloaded method selection has already accounted for overflow errors when the method
                    // was selected. So this forced conversion shouldn't cause such corruption. Except, conversion from
                    // BigDecimal is allowed to overflow for backward-compatibility.
                    forceNumberArgumentsToParameterTypes(pojoArgs, memberDesc.getParamTypes(), typeFlags);
                }
            } else {
                BeansWrapper.coerceBigDecimals(memberDesc.getParamTypes(), pojoArgs);
            }
            return new MemberAndArguments(memberDesc, pojoArgs);
        } else {
            return EmptyMemberAndArguments.from((EmptyCallableMemberDescriptor) maybeEmtpyMemberDesc, pojoArgs);
        }
    }
    
}
