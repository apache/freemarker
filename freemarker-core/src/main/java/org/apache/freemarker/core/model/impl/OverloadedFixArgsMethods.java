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
package org.apache.freemarker.core.model.impl;

import org.apache.freemarker.core.model.Constants;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;

/**
 * Stores the non-varargs methods for a {@link OverloadedMethods} object.
 */
class OverloadedFixArgsMethods extends OverloadedMethodsSubset {
    
    OverloadedFixArgsMethods() {
        super();
    }

    @Override
    Class<?>[] preprocessParameterTypes(CallableMemberDescriptor memberDesc) {
        return memberDesc.getParamTypes();
    }
    
    @Override
    void afterWideningUnwrappingHints(Class<?>[] paramTypes, int[] paramNumericalTypes) {
        // Do nothing
    }

    @Override
    MaybeEmptyMemberAndArguments getMemberAndArguments(TemplateModel[] tmArgs, DefaultObjectWrapper unwrapper)
    throws TemplateModelException {
        if (tmArgs == null) {
            // null is treated as empty args
            tmArgs = Constants.EMPTY_TEMPLATE_MODEL_ARRAY;
        }
        final int argCount = tmArgs.length;
        final Class<?>[][] unwrappingHintsByParamCount = getUnwrappingHintsByParamCount();
        if (unwrappingHintsByParamCount.length <= argCount) {
            return EmptyMemberAndArguments.WRONG_NUMBER_OF_ARGUMENTS;
        }
        Class<?>[] unwarppingHints = unwrappingHintsByParamCount[argCount];
        if (unwarppingHints == null) {
            return EmptyMemberAndArguments.WRONG_NUMBER_OF_ARGUMENTS;
        }
        
        Object[] pojoArgs = new Object[argCount];
        
        int[] typeFlags = getTypeFlags(argCount);
        if (typeFlags == ALL_ZEROS_ARRAY) {
            typeFlags = null;
        }

        for (int i = 0; i < argCount; i++) {
            Object pojo = unwrapper.tryUnwrapTo(
                    tmArgs[i],
                    unwarppingHints[i],
                    typeFlags != null ? typeFlags[i] : 0);
            if (pojo == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                return EmptyMemberAndArguments.noCompatibleOverload(i + 1);
            }
            pojoArgs[i] = pojo;
        }
        
        MaybeEmptyCallableMemberDescriptor maybeEmtpyMemberDesc = getMemberDescriptorForArgs(pojoArgs, false);
        if (maybeEmtpyMemberDesc instanceof CallableMemberDescriptor) {
            CallableMemberDescriptor memberDesc = (CallableMemberDescriptor) maybeEmtpyMemberDesc;
            if (typeFlags != null) {
                // Note that overloaded method selection has already accounted for overflow errors when the method
                // was selected. So this forced conversion shouldn't cause such corruption. Except, conversion from
                // BigDecimal is allowed to overflow for backward-compatibility.
                forceNumberArgumentsToParameterTypes(pojoArgs, memberDesc.getParamTypes(), typeFlags);
            }
            return new MemberAndArguments(memberDesc, pojoArgs);
        } else {
            return EmptyMemberAndArguments.from((EmptyCallableMemberDescriptor) maybeEmtpyMemberDesc, pojoArgs);
        }
    }
    
}
