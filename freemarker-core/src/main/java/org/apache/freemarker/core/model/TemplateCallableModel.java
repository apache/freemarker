/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.model;

import java.util.Collection;

import org.apache.freemarker.core.util.StringToIndexMap;

/**
 * Super interface of {@link TemplateFunctionModel} and {@link TemplateDirectiveModel2}.
 */
public interface TemplateCallableModel extends TemplateModel {

    // -------------------------------------------------------------------------------------------------------------
    // Arguments:

    /**
     * The highest allowed number of positional arguments, not counting the positional varargs argument. The actual
     * positional argument count can be less than this if there are optional positional argument. When calling the
     * {@code execute} method, this many items will be reserved for the positional arguments in the argument array (not
     * counting the item for the positional varargs argument, if there's one). Positional arguments
     * above this count will go to the varargs argument (if there's one, otherwise it's an error).
     */
    int getPredefinedPositionalArgumentCount();

    /**
     * Tells if there's no position varargs argument. If there is, then it must be in the argument array at the
     * index equals to {@link #getPredefinedPositionalArgumentCount()}. The positional varargs argument is a
     * {@link TemplateSequenceModel} that collects all positional arguments whose index would be greater than
     * or equal to {@link #getPredefinedPositionalArgumentCount()}.
     */
    boolean hasPositionalVarargsArgument();

    /**
     * For the given argument name (that corresponds to a parameter that meant to be passed by name, not by position)
     * return its intended index in the {@code args} array argument of the {@code execute} method, or -1 if there's
     * no such parameter. Consider using a static final {@link StringToIndexMap} field to implement this.
     *
     * @return -1 if there's no such named argument
     */
    int getNamedArgumentIndex(String name);

    /**
     * Returns the index of the named varargs argument in the argument array, or -1 if there's no named varargs
     * argument. The named varargs argument is a {@link TemplateHashModelEx2} with string keys that collects all
     * the named arguments for which {@link #getNamedArgumentIndex(String)} returns -1. The iteration order of this
     * hash follows the order in which the arguments were specified in the calling template.
     *
     * @return -1 if there's no named varargs argument
     */
    int getNamedVarargsArgumentIndex();

    /**
     * The required (minimum) length of the {@code args} array passed to the {@code execute} method. This length always
     * includes the space reserved for optional arguments; it's not why it's said to be a minimum length. It's a minimum
     * length because a longer array might be reused for better performance (but {@code execute} should never read
     * those excess elements).
     * The return value should be equal to the sum of these (but we don't want to calculate it on-the-fly,
     * for speed), or else {@link IndexOutOfBoundsException}-s might will occur:
     * <ul>
     *     <li>{@link #getPredefinedPositionalArgumentCount()} (note that predefined optional arguments are counted in)
     *     <li>If {@link #hasPositionalVarargsArgument()} is {@code true}, then 1, else 0.
     *     <li>Size of {@link #getPredefinedNamedArgumentNames()} (again, predefined optional arguments are counted in)
     *     <li>If {@link #getNamedVarargsArgumentIndex()} is not -1, then 1, else 0. (Also, obviously, if
     *     {@link #getNamedVarargsArgumentIndex()} is not -1, then it's one less than the return value of this method.)
     * </ul>
     */
    int getTotalArgumentCount();

    /**
     * The valid names for arguments that are passed by name (not by position), in the order as they should be displayed
     * in error messages, or {@code null} if there's none. If you have implemented
     * {@link #getNamedArgumentIndex(String)} with a {@link StringToIndexMap}, you should return
     * {@link StringToIndexMap#getKeys()} here.
     */
    Collection<String> getPredefinedNamedArgumentNames();

}
