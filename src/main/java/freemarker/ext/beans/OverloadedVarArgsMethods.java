/*
 * Copyright (c) 2003-2007 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */
package freemarker.ext.beans;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Stores the varargs methods for a {@link OverloadedMethods} object.
 * @author Attila Szegedi
 */
class OverloadedVarArgsMethods extends OverloadedMethodsSubset
{
    private static final Map canoncialArgPackers = new HashMap();
    private final Map argPackers = new HashMap();

    OverloadedVarArgsMethods(boolean bugfixed) {
        super(bugfixed);
    }
    
    // TODO: Do we really need this class?
    private static class ArgumentPacker {
        private final int paramCount;
        private final Class varArgsCompType;
        
        ArgumentPacker(int paramCount, Class varArgsCompType) {
            this.paramCount = paramCount;
            this.varArgsCompType = varArgsCompType; 
        }
        
        Object[] packArgs(Object[] args, List modelArgs, BeansWrapper unwrapper) 
        throws TemplateModelException {
            final int totalArgCount = args.length;
            final int fixArgCount = paramCount - 1;
            if(args.length != paramCount) {
                Object[] packedArgs = new Object[paramCount];
                System.arraycopy(args, 0, packedArgs, 0, fixArgCount);
                Object varargs = Array.newInstance(varArgsCompType, totalArgCount - fixArgCount);
                for(int i = fixArgCount; i < totalArgCount; ++i) {
                    Object val = unwrapper.tryUnwrap((TemplateModel)modelArgs.get(i), varArgsCompType);
                    if(val == BeansWrapper.CAN_NOT_UNWRAP) {
                        return null;
                    }
                    Array.set(varargs, i - fixArgCount, val);
                }
                packedArgs[fixArgCount] = varargs;
                return packedArgs;
            }
            else {
                Object val = unwrapper.tryUnwrap((TemplateModel)modelArgs.get(fixArgCount), varArgsCompType);
                if(val == BeansWrapper.CAN_NOT_UNWRAP) {
                    return null;
                }
                Object array = Array.newInstance(varArgsCompType, 1);
                Array.set(array, 0, val);
                args[fixArgCount] = array;
                return args;
            }
        }
        
        public boolean equals(Object obj) {
            if(obj instanceof ArgumentPacker) {
                ArgumentPacker p = (ArgumentPacker)obj;
                return paramCount == p.paramCount && varArgsCompType == p.varArgsCompType;
            }
            return false;
        }
        
        public int hashCode() {
            return paramCount ^ varArgsCompType.hashCode();
        }
    }

    Class[] preprocessParameterTypes(CallableMemberDescriptor memberDesc) {
        final Class[] preprocessedParamTypes = (Class[]) memberDesc.paramTypes.clone();
        int ln = preprocessedParamTypes.length;
        final Class varArgsCompType = preprocessedParamTypes[ln - 1].getComponentType();
        preprocessedParamTypes[ln - 1] = varArgsCompType;
        
        ArgumentPacker argPacker = new ArgumentPacker(ln, varArgsCompType);
        synchronized(canoncialArgPackers) {
            ArgumentPacker canonical = (ArgumentPacker) canoncialArgPackers.get(argPacker);
            if(canonical == null) {
                canoncialArgPackers.put(argPacker, argPacker);
            }
            else {
                argPacker = canonical;
            }
        argPackers.put(memberDesc.member, argPacker);
        }
        
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
        
        // The case of e(t1, t2), e(t1, t2, t2), e(t1, t2, t2, t2), ..., where e is an *earlier* added method.
        // When that was added, this method wasn't added yet, so it had no chance updating the hints of this method,
        // so we do that now:
        // FIXME: Only needed if m(t1, t2) was filled an empty slot, otherwise whatever was there was already
        // widened by the preceding hints, so this will be a no-op.
        for(int i = paramCount - 1; i >= 0; i--) {
            final Class[] previousHints = unwrappingHintsByParamCount[i];
            if(previousHints != null) {
                widenHintsToCommonSupertypes(
                        paramCount,
                        previousHints, getPossibleNumericalTypes(i));
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
                        oneLongerHints, getPossibleNumericalTypes(paramCount + 1));
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
            int paramCountOfWidened, Class[] wideningTypes, int[] wideningNumTypes) {
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
            mergeInNumericalTypes(paramCountOfWidened, wideningNumTypes);
        }
    }
    
    MaybeEmptyMemberAndArguments getMemberAndArguments(List tmArgs, BeansWrapper unwrapper) 
    throws TemplateModelException {
        if(tmArgs == null) {
            // null is treated as empty args
            tmArgs = Collections.EMPTY_LIST;
        }
        int argsLen = tmArgs.size();
        Class[][] unwrappingHintsByParamCount = getUnwrappingHintsByParamCount();
        Object[] pojoArgs = new Object[argsLen];
        int[] possibleNumericalTypes = null;
        // Starting from args.length + 1 as we must try to match against a case
        // where all specified args are fixargs, and the vararg portion 
        // contains zero args
        outer: for(int paramCount = Math.min(argsLen + 1, unwrappingHintsByParamCount.length - 1); paramCount >= 0; --paramCount) {
            Class[] unwarappingHints = unwrappingHintsByParamCount[paramCount];
            if(unwarappingHints == null) {
                if (paramCount == 0) {
                    return EmptyMemberAndArguments.NO_SUCH_METHOD;
                }
                continue;
            }
            
            possibleNumericalTypes = getPossibleNumericalTypes(paramCount);
            if (possibleNumericalTypes == ALL_ZEROS_ARRAY) {
                possibleNumericalTypes = null;
            }
            
            // Try to unwrap the arguments
            Iterator it = tmArgs.iterator();
            for(int i = 0; i < argsLen; ++i) {
                int paramIdx = i < paramCount ? i : paramCount - 1;
                Object pojo = unwrapper.tryUnwrap(
                        (TemplateModel)it.next(),
                        unwarappingHints[paramIdx],
                        possibleNumericalTypes != null ? possibleNumericalTypes[paramIdx] : 0);
                if(pojo == BeansWrapper.CAN_NOT_UNWRAP) {
                    continue outer;
                }
                pojoArgs[i] = pojo;
            }
            break;
        }
        
        MaybeEmptyCallableMemberDescriptor maybeEmtpyMemberDesc = getMemberDescriptorForArgs(pojoArgs, true);
        if(maybeEmtpyMemberDesc instanceof CallableMemberDescriptor) {
            CallableMemberDescriptor memberDesc = (CallableMemberDescriptor) maybeEmtpyMemberDesc;
            pojoArgs = ((ArgumentPacker) argPackers.get(memberDesc.member)).packArgs(pojoArgs, tmArgs, unwrapper);
            if(pojoArgs == null) {
                return EmptyMemberAndArguments.NO_SUCH_METHOD;
            }
            if (bugfixed) {
                if (possibleNumericalTypes != null) {
                    // Note that overloaded method selection has already accounted for overflow errors when the method
                    // was selected. So this forced conversion shouldn't cause such corruption. Except, conversion from
                    // BigDecimal is allowed to overflow for backward-compatibility.
                    forceNumberArgumentsToParameterTypes(pojoArgs, memberDesc.paramTypes, possibleNumericalTypes);
                }
            } else {
                BeansWrapper.coerceBigDecimals(memberDesc.paramTypes, pojoArgs);
            }
            return new MemberAndArguments(memberDesc.member, pojoArgs);
        } else {
            return EmptyMemberAndArguments.from((EmptyCallableMemberDescriptor) maybeEmtpyMemberDesc); // either NOT_FOUND or AMBIGUOUS
        }
    }
}