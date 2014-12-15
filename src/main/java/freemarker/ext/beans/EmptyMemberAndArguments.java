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

import freemarker.core._DelayedOrdinal;

/**
 * Describes a failed member lookup. Instances of this must not be cached as instances may store the actual argument
 * values.
 */
final class EmptyMemberAndArguments extends MaybeEmptyMemberAndArguments {
    
    static final EmptyMemberAndArguments WRONG_NUMBER_OF_ARGUMENTS
            = new EmptyMemberAndArguments(
                    "No compatible overloaded variation was found; wrong number of arguments.", true, null);
    
    private final Object errorDescription;
    private final boolean numberOfArgumentsWrong;
    private final Object[] unwrappedArguments;
    
    private EmptyMemberAndArguments(
            Object errorDescription, boolean numberOfArgumentsWrong, Object[] unwrappedArguments) {
        this.errorDescription = errorDescription;
        this.numberOfArgumentsWrong = numberOfArgumentsWrong;
        this.unwrappedArguments = unwrappedArguments;
    }

    static EmptyMemberAndArguments noCompatibleOverload(int unwrappableIndex) {
        return new EmptyMemberAndArguments(
                new Object[] { "No compatible overloaded variation was found; can't convert (unwrap) the ",
                new _DelayedOrdinal(new Integer(unwrappableIndex)), " argument to the desired Java type." },
                false,
                null);
    }
    
    static EmptyMemberAndArguments noCompatibleOverload(Object[] unwrappedArgs) {
        return new EmptyMemberAndArguments(
                "No compatible overloaded variation was found; declared parameter types and argument value types mismatch.",
                false,
                unwrappedArgs);
    }

    static EmptyMemberAndArguments ambiguous(Object[] unwrappedArgs) {
        return new EmptyMemberAndArguments(
                "Multiple compatible overloaded variations were found with the same priority.",
                false,
                unwrappedArgs);
    }

    static MaybeEmptyMemberAndArguments from(
            EmptyCallableMemberDescriptor emtpyMemberDesc, Object[] unwrappedArgs) {
        if (emtpyMemberDesc == EmptyCallableMemberDescriptor.NO_SUCH_METHOD) {
            return noCompatibleOverload(unwrappedArgs);
        } else if (emtpyMemberDesc == EmptyCallableMemberDescriptor.AMBIGUOUS_METHOD) {
            return ambiguous(unwrappedArgs);
        } else {
            throw new IllegalArgumentException("Unrecognized constant: " + emtpyMemberDesc);
        }
    }

    Object getErrorDescription() {
        return errorDescription;
    }

    /**
     * @return {@code null} if the error has occurred earlier than the full argument list was unwrapped.
     */
    Object[] getUnwrappedArguments() {
        return unwrappedArguments;
    }

    public boolean isNumberOfArgumentsWrong() {
        return numberOfArgumentsWrong;
    }
    
}
