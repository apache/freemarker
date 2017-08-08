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

package org.apache.freemarker.core.model;

import org.apache.freemarker.core.util.StringToIndexMap;

/**
 * {@link TemplateCallableModel} subinterfaces define a method called {@code execute}, which has an argument array
 * parameter, whose layout this class describes. The layout specifies the (minimum) array length, what's the index
 * of which parameters, and if there are varargs parameters, in which case they must not be left {@code null}.
 * (Note that a {@link TemplateCallableModel} may have {@code null} layout; see the documentation of {@code execute}
 * for more.)
 * <p>
 * Each parameter has a constant index in this array, which is the same for all invocations of the same
 * {@link TemplateCallableModel} object (regardless if there are omitted optional parameters). Thus, the argument
 * values can always be accessed at these constant indexes; no runtime name lookup is needed inside the {@code
 * execute} method of the {@link TemplateCallableModel} implementation. The {@link ArgumentArrayLayout} object is
 * usually stored in a static final field of the {@link TemplateCallableModel} implementation class. Said constant
 * indexes are alsi usually defined in the {@link TemplateCallableModel} implementation as static final constants
 * (then feed into the {@link ArgumentArrayLayout}). Some {@link TemplateCallableModel} implementations, such those
 * stand for macros and functions defined in the template, decide the layout on runtime instead. Note the less, once
 * the {@link TemplateCallableModel} was crated, the layout is fixed.
 * <p>
 * The layout of the array is as follows:
 * <ol>
 * <li>
 *     {@link #getPredefinedPositionalArgumentCount()} elements for the predefined positional parameters. Index 0
 *     corresponds to the 1st positional parameter, index 1 for the second, etc. For omitted parameters (optional
 *     parameters usually) the corresponding array element is {@code null}.
 * <li>
 *     {@link #getPredefinedNamedArgumentsMap()}{@code .size()} elements for the predefined named arguments. These are
 *     at the indexes returned by {@link #getPredefinedNamedArgumentsMap()}{@code .get(String name)}. Yet again, for
 *     omitted arguments the corresponding array element is {@code null}. Within this index range reserved for the
 *     named arguments, the {@link TemplateCallableModel} object is free to chose what index belongs to which name (as
 *     far as two names don't share the same index).
 * <li>
 *     If there's a positional varargs argument, then 1 element for the positional varargs parameter (whose value
 *     will be a {@link TemplateSequenceModel}), at index {@link #getPositionalVarargsArgumentIndex()}. This must not
 *     be left {@code null} in the argument array. In case there are 0 positional varargs, the caller must set it to
 *     an empty {@link TemplateSequenceModel} (like {@link TemplateSequenceModel#EMPTY_SEQUENCE}).
 * <li>
 *     If there's a named varargs argument, then 1 element for the positional varargs parameter (whose value will be
 *     a {@link TemplateHashModelEx2}), at index {@link #getNamedVarargsArgumentIndex()}. This must not be left
 *     {@code null} in the argument array. In case there are 0 named varargs, the caller must set it to an empty
 *     {@link TemplateHashModelEx2} (like {@link TemplateHashModel#EMPTY_HASH}).
 * </ol>
 * <p>
 * The length of the argument array (allocated by the caller of {@code execute}) is {@link #getTotalLength()}}, or
 * more (in case you have a longer array for reuse), in which case the extra elements should be ignored by the callee.
 * <p>
 * Instances of this class are immutable, thread-safe objects.
 */
public final class ArgumentArrayLayout {
    private final int predefinedPositionalArgumentCount;
    private final StringToIndexMap predefinedNamedArgumentsMap;

    private final int positionalVarargsArgumentIndex;
    private final int namedVarargsArgumentIndex;
    private final int arrayLength;

    /** Constant to be used when the {@link TemplateCallableModel} has no parameters. */
    public static final ArgumentArrayLayout PARAMETERLESS = new ArgumentArrayLayout(
            0, false,
            null, false);

    /**
     * Constant to be used when the {@link TemplateCallableModel} has 1 positional parameter, and no others.
     * (The argument array index of the single positional parameter will be 0.)
     */
    public static final ArgumentArrayLayout SINGLE_POSITIONAL_PARAMETER = new ArgumentArrayLayout(
            1, false,
            null, false);

    /**
     * Constant to be used when the {@link TemplateCallableModel} has 2 positional parameter, and no others.
     * (The argument array index of the positional parameters will be 0 and 1.)
     */
    public static final ArgumentArrayLayout TWO_POSITIONAL_PARAMETERS = new ArgumentArrayLayout(
            2, false,
            null, false);

    /**
     * Constant to be used when the {@link TemplateCallableModel} has 3 positional parameter, and no others.
     * (The argument array index of the positional parameters will be 0, 1, and 2.)
     */
    public static final ArgumentArrayLayout THREE_POSITIONAL_PARAMETERS = new ArgumentArrayLayout(
            3, false,
            null, false);

    /**
     * Constant to be used when the {@link TemplateCallableModel} has 1 positional varargs parameter, and no others.
     * (The argument array index of the positional varargs parameter will be 0.)
     *  */
    public static final ArgumentArrayLayout POSITIONAL_VARARGS_PARAMETER_ONLY = new ArgumentArrayLayout(
            0, true,
            null, false);

    /**
     * Creates a new instance, or returns some of the equivalent static constants (for example {@link #PARAMETERLESS}).
     *
     * @param predefinedPositionalArgumentCount
     *         The highest allowed number of positional arguments, not counting the positional varargs argument. The
     *         actual positional argument count can be less than this if there are optional positional argument. When
     *         calling the {@code execute} method of the {@link TemplateCallableModel}, this many items will be reserved
     *         for the positional arguments in the argument array (not counting the item for the positional varargs
     *         argument, if there's one). Positional arguments above this count will go to the varargs argument (if
     *         there's one, otherwise it's an error).
     * @param hasPositionalVarargsArgument
     *         Specifies if there's a varargs argument into which positional arguments that aren't predefined are
     *         collected
     * @param predefinedNamedArgumentsMap
     *         The valid names for named arguments (not counting named varargs arguments), and their indexes in the
     *         argument array passed to the {@code execute} method of the {@link TemplateCallableModel}. Can be {@code
     *         null}, which is equivalent to {@link StringToIndexMap#EMPTY}. Indexes must fall into the range starting
     *         with {@code predefinedPositionalArgumentCount} (inclusive), and ending with {@code
     *         predefinedPositionalArgumentCount}{@code + predefinedNamedArgumentsMap.size()} (exclusive). If not, an
     *         {@link IllegalArgumentException} will be thrown. (As {@link ArgumentArrayLayout}-s are normally created
     *         during class initialization, such an exception will later cause {@link NoClassDefFoundError} "Could not
     *         initialize" exceptions every time you try to access the class, which are not very informative. Only for
     *         the first access of the class will you get an {@link ExceptionInInitializerError} with the {@link
     *         IllegalArgumentException} as its cause exception, which contains the error details.)
     * @param hasNamedVarargsArgument
     *         Specifies if there's a varargs argument into which named arguments that aren't predefined are collected
     *
     * @throws IllegalArgumentException
     *         If the {@code predefinedNamedArgumentsMap} contains indexes that are out of range. See the documentation
     *         of that parameter for more.
     */
    public static ArgumentArrayLayout create(
            int predefinedPositionalArgumentCount, boolean hasPositionalVarargsArgument,
            StringToIndexMap predefinedNamedArgumentsMap, boolean hasNamedVarargsArgument) {
        if ((predefinedNamedArgumentsMap == null || predefinedNamedArgumentsMap == StringToIndexMap.EMPTY)
                && !hasNamedVarargsArgument) {
            if (predefinedPositionalArgumentCount == 0) {
                return hasPositionalVarargsArgument ? POSITIONAL_VARARGS_PARAMETER_ONLY : PARAMETERLESS;
            }
            if (predefinedPositionalArgumentCount == 1 && !hasPositionalVarargsArgument) {
                return SINGLE_POSITIONAL_PARAMETER;
            }
        }
        return new ArgumentArrayLayout(
                predefinedPositionalArgumentCount, hasPositionalVarargsArgument,
                predefinedNamedArgumentsMap, hasNamedVarargsArgument);
    }

    /**
     * Creates a new instance. Note that the index layout rules are not internal implementation details; they can't be
     * changed without breaking backward compatibility. Also some internal parts, such as macro definitions, depend on
     * those rules.
     */
    private ArgumentArrayLayout(int predefinedPositionalArgumentCount, boolean hasPositionalVarargsArgument,
            StringToIndexMap predefinedNamedArgumentsMap, boolean hasNamedVarargsArgument) {
        if (predefinedNamedArgumentsMap == null) {
            predefinedNamedArgumentsMap = StringToIndexMap.EMPTY;
        }

        this.predefinedPositionalArgumentCount = predefinedPositionalArgumentCount;
        this.predefinedNamedArgumentsMap = predefinedNamedArgumentsMap;

        int arrayLength = predefinedPositionalArgumentCount + predefinedNamedArgumentsMap.size();
        if (hasPositionalVarargsArgument) {
            positionalVarargsArgumentIndex = arrayLength;
            arrayLength++;
        } else {
            positionalVarargsArgumentIndex = -1;
        }
        if (hasNamedVarargsArgument) {
            namedVarargsArgumentIndex = arrayLength;
            arrayLength++;
        } else {
            namedVarargsArgumentIndex = -1;
        }
        this.arrayLength = arrayLength;

        predefinedNamedArgumentsMap.checkIndexRange(predefinedPositionalArgumentCount);
    }

    /**
     * See the related parameter of {@link ArgumentArrayLayout#create(int, boolean, StringToIndexMap, boolean)}.
     */
    public int getPredefinedPositionalArgumentCount() {
        return predefinedPositionalArgumentCount;
    }

    /**
     * See the related parameter of {@link ArgumentArrayLayout#create(int, boolean, StringToIndexMap, boolean)}.
     */
    public StringToIndexMap getPredefinedNamedArgumentsMap() {
        return predefinedNamedArgumentsMap;
    }

    /**
     * Returns the index of the varargs argument into which positional arguments that aren't predefined are collected,
     * or -1 if there's no such varargs argument. The value of the positional varargs argument is a {@link
     * TemplateSequenceModel} that collects all positional arguments whose index would be greater than or equal to
     * {@link #getPredefinedPositionalArgumentCount()}. The value of this argument can't be {@code null}.
     *
     * @return -1 if there's no positional varargs argument
     */
    public int getPositionalVarargsArgumentIndex() {
        return positionalVarargsArgumentIndex;
    }

    /**
     * Returns the index of the varargs argument into which named arguments that aren't predefined (via {@link
     * #getPredefinedNamedArgumentsMap()}) are collected, or -1 if there's no such varargs argument. The value of the
     * named varargs argument is a {@link TemplateHashModelEx2} with string keys that collects all the named arguments
     * that aren't present in the {@link #getPredefinedNamedArgumentsMap()}. The iteration order of this hash
     * corresponds to the order in which the arguments were specified on the call site (in a template, typically).
     * The value of this argument can't be {@code null}.
     *
     * @return -1 if there's no named varargs argument
     */
    public int getNamedVarargsArgumentIndex() {
        return namedVarargsArgumentIndex;
    }

    /**
     * Returns the required (minimum) length of the {@code args} array that's passed to the {@code execute} method of
     * the {@link TemplateCallableModel} subinterface. As there's an index reserved for each predefined parameters (and
     * a varargs parameter always counts as 1 parameter), this length always includes the space reserved for optional
     * parameters as well; it's not why it's said to be a minimum length. It's a minimum length because a longer array
     * might be reused for better performance (but {@code execute} should never read those excess elements).
     */
    public int getTotalLength() {
        return arrayLength;
    }

    /**
     * Tells if there can be any positional parameters (predefined or varargs).
     */
    public boolean isPositionalParametersSupported() {
        return getPredefinedPositionalArgumentCount() != 0 || getPositionalVarargsArgumentIndex() != -1;
    }

    /**
     * Tells if there can be any named parameters (predefined or varargs).
     */
    public boolean isNamedParametersSupported() {
        return getPredefinedNamedArgumentsMap().size() != 0 || getNamedVarargsArgumentIndex() != -1;
    }

}
