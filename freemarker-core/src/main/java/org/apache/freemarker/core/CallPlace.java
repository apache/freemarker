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

package org.apache.freemarker.core;

import java.io.IOException;
import java.io.Writer;
import java.util.IdentityHashMap;

import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CommonSupplier;

/**
 * The place (in a template, usually) from where a directive (like a macro) or function was called;
 * <b>Do not implement this interface yourself</b>, as new methods may be added any time! Only FreeMarker itself
 * should provide implementations. In case you have to call something from outside a template, use
 * {@link NonTemplateCallPlace#INSTANCE}.
 */
public interface CallPlace {

    // -------------------------------------------------------------------------------------------------------------
    // Nested content:

    /**
     * Tells if there's a non-zero-length nested content. This is {@code false} for {@code <@foo />} or
     * {@code <@foo></@foo>} or for calls inside expressions (i.e., for function calls).
     */
    boolean hasNestedContent();

    /**
     * The number of nested content parameters in this call (like 2 in {@code <@foo xs; k, v>...</@>}). If you want the
     * caller to specify a fixed number of nested content parameters, then this is not interesting for you, and just
     * pass an array of that length to {@link #executeNestedContent(TemplateModel[], Writer, Environment)}. If, however,
     * you want to allow the directive/function call to declare fewer parameters, then this is how you know how many
     * parameters you should calculate and pass to {@link #executeNestedContent(TemplateModel[], Writer, Environment)}.
     */
    int getNestedContentParameterCount();

    /**
     * Executes the nested content; it there's none, it just does nothing.
     *
     * @param nestedContentArgs
     *         The nested content parameter values to pass to the nested content (as in <code>&lt;@foo bar; i, jgt;${i},
     *         ${j}&lt;/@foo&gt;</code> there are 2 such parameters, whose value you set here), or {@code null} if
     *         there's none.
     *         This array must be {@link #getNestedContentParameterCount()} long, or else FreeMarker will throw an
     *         {@link TemplateException} with a descriptive error message that tells to user that they need to declare
     *         that many nested content parameters as the length of this array. If you want to allow the  caller to not
     *         declare some of the nested content parameters, then you have to make this array shorter according to
     *         {@link #getNestedContentParameterCount()}.
     * @param out
     *         The {@link Writer} to print. 
     */
    void executeNestedContent(TemplateModel[] nestedContentArgs, Writer out, Environment env)
            throws TemplateException, IOException;

    // -------------------------------------------------------------------------------------------------------------
    // Source code info:

    /**
     * The template that contains this call; {@code null} if the call is not from a template (but directly from
     * user Java code, for example).
     */
    Template getTemplate();

    /**
     * The 1-based column number of the first character of the directive call in the template source code, or -1 if it's
     * not known.
     */
    int getBeginColumn();

    /**
     * The 1-based line number of the first character of the directive call in the template source code, or -1 if it's
     * not known.
     */
    int getBeginLine();

    /**
     * The 1-based column number of the last character of the directive call in the template source code, or -1 if it's
     * not known. If the directive has an end-tag ({@code </@...>}), then it points to the last character of that.
     */
    int getEndColumn();

    /**
     * The 1-based line number of the last character of the directive call in the template source code, or -1 if it's
     * not known. If the directive has an end-tag ({@code </@...>}), then it points to the last character of that.
     */
    int getEndLine();

    // -------------------------------------------------------------------------------------------------------------
    // Caching:

    /**
     * Returns the custom data, or if that's {@code null}, then it creates and stores it in an atomic operation then
     * returns it. This method is thread-safe, however, it doesn't ensure thread safe (like synchronized) access to the
     * custom data itself. Be sure that the custom data only depends on things that get their final value during
     * template parsing, not on runtime settings.
     * <p>
     * This method will block other calls while the {@code supplier} is executing, thus, the object will be
     * <em>usually</em> created only once, even if multiple threads request the value when it's still {@code null}. It
     * doesn't stand though when {@code providerIdentity} mismatches occur (see later). Furthermore, then it's also
     * possible that multiple objects created by the same {@link CommonSupplier} will be in use on the same time,
     * because of directive executions already running in parallel, and because of memory synchronization delays
     * (hardware dependent) between the threads.
     *
     * @param providerIdentity
     *         This is usually the class of the {@link TemplateDirectiveModel} that creates (and uses) the custom data,
     *         or if you are using your own class for the custom data object (as opposed to a class from some more
     *         generic API), then that class. This is needed as the same call place might calls different directives
     *         depending on runtime conditions, and so it must be ensured that these directives won't accidentally read
     *         each other's custom data, ending up with class cast exceptions or worse. In the current implementation,
     *         if there's a {@code providerIdentity} mismatch (means, the {@code providerIdentity} object used when the
     *         custom data was last set isn't the exactly same object as the one provided with the parameter now), the
     *         previous custom data will be just ignored as if it was {@code null}. So if multiple directives that use
     *         the custom data feature use the same call place, the caching of the custom data can be inefficient, as
     *         they will keep overwriting each other's custom data. (In a more generic implementation the {@code
     *         providerIdentity} would be a key in a {@link IdentityHashMap}, but then this feature would be slower,
     *         while {@code providerIdentity} mismatches aren't occurring in most applications.)
     * @param supplier
     *         Called when the custom data wasn't yet set, to invoke its initial value. If this parameter is {@code
     *         null} and the custom data wasn't set yet, then {@code null} will be returned. The returned value of
     *         {@link CommonSupplier#get()} can be any kind of object, but can't be {@code null}.
     *
     * @return The current custom data object, or possibly {@code null} if there was no {@link CommonSupplier} provided.
     *
     * @throws CallPlaceCustomDataInitializationException
     *         If the {@link CommonSupplier} had to be invoked but failed.
     * @throws UnsupportedOperationException
     *         If this call place doesn't support storing custom date; see {@link #isCustomDataSupported()}.
     */
    Object getOrCreateCustomData(Object providerIdentity, CommonSupplier<?> supplier)
            throws CallPlaceCustomDataInitializationException;

    /**
     * Tells if this call place supports storing custom data. As of this writing, only top-level (i.e., outside
     * expression) directive calls do.
     */
    boolean isCustomDataSupported();

    /**
     * Tells if the output of the nested content can be safely cached, as it only depends on the template content (not
     * on variable values and such) and has no side-effects (other than writing to the output). Examples of cases that
     * give {@code false}: {@code <@foo>Name: } <tt>${name}</tt>{@code</@foo>}, {@code <@foo>Name: <#if
     * condition>bar</#if></@foo>}. Examples of cases that give {@code true}: {@code <@foo>Name: Joe</@foo>}, {@code
     * <@foo />}. Note that we get {@code true} for no nested content, because that's equivalent to 0-length nested
     * content.
     * <p>
     * This method returns a pessimistic result. For example, if it sees a custom directive call, it can't know what it
     * does, so it will assume that it's not cacheable.
     */
    boolean isNestedOutputCacheable();

    // -------------------------------------------------------------------------------------------------------------
    // Overloaded method selection:

    /**
     * The index of the first item in the argument array passed to {@code execute} that has this information.
     * Used solely for speed optimization (to minimize the number of
     * {@link #getTargetJavaParameterType(int)} calls).
     *
     * @return -1 if no parameter has type hint
     */
    int getFirstTargetJavaParameterTypeIndex();

    /**
     * The type of the parameter in the target Java method; used for overloaded Java method selection. This optional
     * information is specified by the template author in the source code (the syntax is not yet decided when I write
     * this).
     *
     * @param argIndex
     *         The index of the argument in the argument array
     *
     * @return The desired Java type or {@code null} if this information wasn't specified in the template.
     *
     * @throws IndexOutOfBoundsException
     *         Might be thrown if {@code argIndex} is an invalid index according the number of arguments on the call
     *         site. Some implementations may just return {@code null} in that case though.
     */
    Class<?> getTargetJavaParameterType(int argIndex);

}
