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
 * parameter, whose layout this class describes. Each parameter has a constant index in this array, which is the same
 * for all invocations of the same {@link TemplateCallableModel} object (regardless if there are omitted optional
 * parameters). Thus, the argument values can always be accessed at these constant indexes; no runtime name lookup is
 * needed inside the {@code execute} method of the {@link TemplateCallableModel} implementation. The
 * {@link ArgumentArrayLayout} object is usually stored a static final field of the {@link TemplateCallableModel}
 * implementation class.
 * <p>
 * The layout of the array is as follows:
 * <ol>
 * <li>
 *     {@link #getPredefinedPositionalArgumentCount()} elements for the predefined positional parameters. Index 0
 *     corresponds to the 1st positional parameter. For omitted parameters the corresponding array element is {@code
 *     null}.
 * <li>
 *     {@link #getPredefinedNamedArgumentsMap()}{@code .size()} elements for the predefined named arguments. These are at
 *     the indexes returned by {@link #getPredefinedNamedArgumentsMap()}{@code .get(String name)}. For omitted arguments
 *     the corresponding array element is {@code null}.
 * <li>
 *     If there's a positional varargs argument, then one element for the positional varargs parameter, at
 *     index {@link #getPositionalVarargsArgumentIndex()}.
 * <li>
 *     If there's a named varargs argument, then one element for the positional varargs parameter, at
 *     index {@link #getNamedVarargsArgumentIndex()}.
 * </ol>
 * <p>
 * The length of the array is {@link #getTotalLength()}}, or more, in which case the extra elements should be
 * ignored.
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

    /** Constant to be used when the {@link TemplateCallableModel} has 1 positional parameter, and no others. */
    public static final ArgumentArrayLayout SINGLE_POSITIONAL_PARAMETER = new ArgumentArrayLayout(
            1, false,
            null, false);

    /**
     * Creates a new instance, or returns some of the equivalent static constants (such as {@link #PARAMETERLESS} or
     * {@link #SINGLE_POSITIONAL_PARAMETER}).
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
                && !hasPositionalVarargsArgument && !hasNamedVarargsArgument) {
            if (predefinedPositionalArgumentCount == 0) {
                return PARAMETERLESS;
            }
            if (predefinedPositionalArgumentCount == 1) {
                return SINGLE_POSITIONAL_PARAMETER;
            }
        }
        return new ArgumentArrayLayout(
                predefinedPositionalArgumentCount, hasPositionalVarargsArgument,
                predefinedNamedArgumentsMap, hasNamedVarargsArgument);
    }

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
     * {@link #getPredefinedPositionalArgumentCount()}.
     *
     * @return -1 if there's no such named argument
     */
    public int getPositionalVarargsArgumentIndex() {
        return positionalVarargsArgumentIndex;
    }

    /**
     * Returns the index of the varargs argument into which named arguments that aren't predefined are collected, or -1
     * if there's no such varargs argument. The value of the named varargs argument is a {@link TemplateHashModelEx2}
     * with string keys that collects all the named arguments that aren't present in the {@link
     * #getPredefinedNamedArgumentsMap()}. The iteration order of this hash follows the order in which the arguments
     * were specified on the call site (in the template, typically).
     *
     * @return -1 if there's no such named argument
     */
    public int getNamedVarargsArgumentIndex() {
        return namedVarargsArgumentIndex;
    }

    /**
     * Returns the required (minimum) length of the {@code args} array that's passed to the {@code execute} method. As
     * there's an index reserved for each predefined parameters, this length always includes the space reserved for
     * optional parameters as well; it's not why it's said to be a minimum length. It's a minimum length because a
     * longer array might be reused for better performance (but {@code execute} should never read those excess
     * elements).
     */
    public int getTotalLength() {
        return arrayLength;
    }
}
