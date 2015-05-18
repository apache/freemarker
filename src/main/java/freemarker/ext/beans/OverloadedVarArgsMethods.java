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

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import freemarker.core.BugException;
import freemarker.template.ObjectWrapperAndUnwrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Stores the varargs methods for a {@link OverloadedMethods} object.
 */
class OverloadedVarArgsMethods extends OverloadedMethodsSubset
{

    OverloadedVarArgsMethods(boolean bugfixed) {
        super(bugfixed);
    }
    
    /**
     * Replaces the last parameter type with the array component type of it.
     */
    Class[] preprocessParameterTypes(CallableMemberDescriptor memberDesc) {
        final Class[] preprocessedParamTypes = (Class[]) memberDesc.getParamTypes().clone();
        int ln = preprocessedParamTypes.length;
        final Class varArgsCompType = preprocessedParamTypes[ln - 1].getComponentType();
        if (varArgsCompType == null) {
            throw new BugException("Only varargs methods should be handled here");
        }
        preprocessedParamTypes[ln - 1] = varArgsCompType;
        return preprocessedParamTypes;
    }

    void afterWideningUnwrappingHints(Class[] paramTypes, int[] paramNumericalTypes) {
        // Overview
        // --------
        //
        // So far, m(t1, t2...) was treated by the hint widening like m(t1, t2). So now we have to continue hint
        // widening like if we had further methods:
        // - m(t1, t2, t2), m(t1, t2, t2, t2), ...
        // - m(t1), because a varargs array can be 0 long
        //
        // But we can't do that for real, because we had to add infinite number of methods. Also, for efficiency we
        // don't want to create unwrappingHintsByParamCount entries at the indices which are still unused.
        // So we only update the already existing hints. Remember that we already have m(t1, t2) there.
        
        final int paramCount = paramTypes.length;
        final Class[][] unwrappingHintsByParamCount = getUnwrappingHintsByParamCount();
        
        // The case of e(t1, t2), e(t1, t2, t2), e(t1, t2, t2, t2), ..., where e is an *earlierly* added method.
        // When that was added, this method wasn't added yet, so it had no chance updating the hints of this method,
        // so we do that now:
        // FIXME: Only needed if m(t1, t2) was filled an empty slot, otherwise whatever was there was already
        // widened by the preceding hints, so this will be a no-op.
        for(int i = paramCount - 1; i >= 0; i--) {
            final Class[] previousHints = unwrappingHintsByParamCount[i];
            if(previousHints != null) {
                widenHintsToCommonSupertypes(
                        paramCount,
                        previousHints, getTypeFlags(i));
                break;  // we only do this for the first hit, as the methods before that has already widened it.
            }
        }
        // The case of e(t1), where e is an *earlier* added method.
        // When that was added, this method wasn't added yet, so it had no chance updating the hints of this method,
        // so we do that now:
        // FIXME: Same as above; it's often unnecessary.
        if(paramCount + 1 < unwrappingHintsByParamCount.length) {
            Class[] oneLongerHints = unwrappingHintsByParamCount[paramCount + 1];
            if(oneLongerHints != null) {
                widenHintsToCommonSupertypes(
                        paramCount,
                        oneLongerHints, getTypeFlags(paramCount + 1));
            }
        }
        
        // The case of m(t1, t2, t2), m(t1, t2, t2, t2), ..., where m is the currently added method.
        // Update the longer hints-arrays:  
        for(int i = paramCount + 1; i < unwrappingHintsByParamCount.length; i++) {
            widenHintsToCommonSupertypes(
                    i,
                    paramTypes, paramNumericalTypes);
        }
        // The case of m(t1) where m is the currently added method.
        // update the one-shorter hints-array:  
        if(paramCount > 0) {  // (should be always true, or else it wasn't a varags method)
            widenHintsToCommonSupertypes(
                    paramCount - 1,
                    paramTypes, paramNumericalTypes);
        }
        
    }
    
    private void widenHintsToCommonSupertypes(
            int paramCountOfWidened, Class[] wideningTypes, int[] wideningTypeFlags) {
        final Class[] typesToWiden = getUnwrappingHintsByParamCount()[paramCountOfWidened];
        if (typesToWiden == null) { 
            return;  // no such overload exists; nothing to widen
        }
        
        final int typesToWidenLen = typesToWiden.length;
        final int wideningTypesLen = wideningTypes.length;
        int min = Math.min(wideningTypesLen, typesToWidenLen);
        for(int i = 0; i < min; ++i) {
            typesToWiden[i] = getCommonSupertypeForUnwrappingHint(typesToWiden[i], wideningTypes[i]);
        }
        if(typesToWidenLen > wideningTypesLen) {
            Class varargsComponentType = wideningTypes[wideningTypesLen - 1];
            for(int i = wideningTypesLen; i < typesToWidenLen; ++i) {
                typesToWiden[i] = getCommonSupertypeForUnwrappingHint(typesToWiden[i], varargsComponentType);
            }
        }
        
        if (bugfixed) {
            mergeInTypesFlags(paramCountOfWidened, wideningTypeFlags);
        }
    }
    
    MaybeEmptyMemberAndArguments getMemberAndArguments(List tmArgs, BeansWrapper unwrapper) 
    throws TemplateModelException {
        if(tmArgs == null) {
            // null is treated as empty args
            tmArgs = Collections.EMPTY_LIST;
        }
        final int argsLen = tmArgs.size();
        final Class[][] unwrappingHintsByParamCount = getUnwrappingHintsByParamCount();
        final Object[] pojoArgs = new Object[argsLen];
        int[] typesFlags = null;
        // Going down starting from methods with args.length + 1 parameters, because we must try to match against a case
        // where all specified args are fixargs, and we have 0 varargs.
        outer: for(int paramCount = Math.min(argsLen + 1, unwrappingHintsByParamCount.length - 1); paramCount >= 0; --paramCount) {
            Class[] unwarappingHints = unwrappingHintsByParamCount[paramCount];
            if(unwarappingHints == null) {
                if (paramCount == 0) {
                    return EmptyMemberAndArguments.WRONG_NUMBER_OF_ARGUMENTS;
                }
                continue;
            }
            
            typesFlags = getTypeFlags(paramCount);
            if (typesFlags == ALL_ZEROS_ARRAY) {
                typesFlags = null;
            }
            
            // Try to unwrap the arguments
            Iterator it = tmArgs.iterator();
            for(int i = 0; i < argsLen; ++i) {
                int paramIdx = i < paramCount ? i : paramCount - 1;
                Object pojo = unwrapper.tryUnwrapTo(
                        (TemplateModel)it.next(),
                        unwarappingHints[paramIdx],
                        typesFlags != null ? typesFlags[paramIdx] : 0);
                if(pojo == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                    continue outer;
                }
                pojoArgs[i] = pojo;
            }
            break outer;
        }
        
        MaybeEmptyCallableMemberDescriptor maybeEmtpyMemberDesc = getMemberDescriptorForArgs(pojoArgs, true);
        if(maybeEmtpyMemberDesc instanceof CallableMemberDescriptor) {
            CallableMemberDescriptor memberDesc = (CallableMemberDescriptor) maybeEmtpyMemberDesc;
            Object[] pojoArgsWithArray;
            Object argsOrErrorIdx = replaceVarargsSectionWithArray(pojoArgs, tmArgs, memberDesc, unwrapper);
            if(argsOrErrorIdx instanceof Object[]) {
                pojoArgsWithArray = (Object[]) argsOrErrorIdx;
            } else {
                return EmptyMemberAndArguments.noCompatibleOverload(((Integer) argsOrErrorIdx).intValue());
            }
            if (bugfixed) {
                if (typesFlags != null) {
                    // Note that overloaded method selection has already accounted for overflow errors when the method
                    // was selected. So this forced conversion shouldn't cause such corruption. Except, conversion from
                    // BigDecimal is allowed to overflow for backward-compatibility.
                    forceNumberArgumentsToParameterTypes(pojoArgsWithArray, memberDesc.getParamTypes(), typesFlags);
                }
            } else {
                BeansWrapper.coerceBigDecimals(memberDesc.getParamTypes(), pojoArgsWithArray);
            }
            return new MemberAndArguments(memberDesc, pojoArgsWithArray);
        } else {
            return EmptyMemberAndArguments.from((EmptyCallableMemberDescriptor) maybeEmtpyMemberDesc, pojoArgs);
        }
    }
    
    /**
     * Converts a flat argument list to one where the last argument is an array that collects the varargs, also
     * re-unwraps the varargs to the component type. Note that this couldn't be done until we had the concrete
     * member selected.
     * 
     * @return An {@code Object[]} if everything went well, or an {@code Integer} the
     *    order (1-based index) of the argument that couldn't be unwrapped. 
     */
    private Object replaceVarargsSectionWithArray(
            Object[] args, List modelArgs, CallableMemberDescriptor memberDesc, BeansWrapper unwrapper) 
    throws TemplateModelException {
        final Class[] paramTypes = memberDesc.getParamTypes();
        final int paramCount = paramTypes.length;
        final Class varArgsCompType = paramTypes[paramCount - 1].getComponentType(); 
        final int totalArgCount = args.length;
        final int fixArgCount = paramCount - 1;
        if (args.length != paramCount) {
            Object[] packedArgs = new Object[paramCount];
            System.arraycopy(args, 0, packedArgs, 0, fixArgCount);
            Object varargs = Array.newInstance(varArgsCompType, totalArgCount - fixArgCount);
            for (int i = fixArgCount; i < totalArgCount; ++i) {
                Object val = unwrapper.tryUnwrapTo((TemplateModel)modelArgs.get(i), varArgsCompType);
                if (val == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                    return new Integer(i + 1);
                }
                Array.set(varargs, i - fixArgCount, val);
            }
            packedArgs[fixArgCount] = varargs;
            return packedArgs;
        } else {
            Object val = unwrapper.tryUnwrapTo((TemplateModel)modelArgs.get(fixArgCount), varArgsCompType);
            if (val == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                return new Integer(fixArgCount + 1);
            }
            Object array = Array.newInstance(varArgsCompType, 1);
            Array.set(array, 0, val);
            args[fixArgCount] = array;
            return args;
        }
    }
    
}