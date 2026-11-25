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

package freemarker.template;

import java.util.Collection;
import java.util.Map;

import freemarker.core.Macro;
import freemarker.core.NonMethodException;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.ZeroArgumentNonVoidMethodPolicy;
import freemarker.template.utility.NullArgumentException;

/**
 * Adds extra getter methods to {@link TemplateHashModel} that can return different results than {@link #get(String)},
 * knowing that the result will be called as a method. The template language uses these for method calls that are
 * <em>directly</em> after the dot operator and key (like {@code obj.m()}), or the equivalent with square brackets
 * ({@code obj["m"]()}).
 *
 * <p>Background knowledge needed to understand this: In the FreeMarker template language, methods/functions are
 * first class values (just like strings, numbers,etc.). Also, unlike in Java, there's no separate namespace for
 * methods, and for the other field-like members. When you have {@code obj.m()} in a template, first, the dot operator
 * gets the value for the key "m", and after that, and independently of that, the method call operator tries to call
 * that value (and if it's not a method or function, that will fail). The dot operator, before 2.3.33, was never aware
 * of what the value it gets will be used for (like will it be called, will it be printed, etc.). Now it can be.
 *
 * For example, if in the template you have {@code someRecord.someComponent()}, and there {@code someRecord} was wrapped
 * by the {@link ObjectWrapper} into a {@link TemplateHashModel} that also implements this interface, then the dot
 * operator will call {@link #getBeforeMethodCall(String, int) getBeforeMethodCall("someComponent", 0)}, rather than
 * {@link #get(String) get("someComponent")}. This is needed to implement subtle features like
 * {@link BeansWrapper.MethodAppearanceDecision#setMethodInsteadOfPropertyValueBeforeCall(boolean)},
 * which is needed to implement
 * {@link ZeroArgumentNonVoidMethodPolicy#BOTH_METHOD_AND_PROPERTY_UNLESS_BEAN_PROPERTY_READ_METHOD}.
 *
 * <p>The argument-count overload defaults to the historical behavior for zero-argument calls and to {@link #get(String)}
 * for calls with arguments, so existing implementations remain compatible. Implementations that need argument-aware
 * overload resolution can override it.
 *
 * <p>Objects wrapped with {@link BeansWrapper}, and hence with {@link DefaultObjectWrapper}, will implement this
 * interface when they are "generic" objects (that is, when they are not classes with special wrapper, like
 * {@link Map}-s, {@link Collection}-s, {@link Number}-s, etc.).
 *
 * @since 2.3.33
 */
public interface MethodCallAwareTemplateHashModel extends TemplateHashModel {

    /**
     * This is called instead of {@link #get(String)} if we know that the return value should be callable like a method.
     * The advantage of this is that we can coerce the value to a method when desirable, and otherwise can give
     * a more specific error message in the resulting exception than the standard {@link NonMethodException} would.
     *
     * @param key
     *      Same as for {@link #get(String)}
     *
     * @return
     *      Same as for {@link #get(String)}, except it should return a {@link TemplateMethodModel}
     *      (a {@link TemplateMethodModelEx} if possible), or in very rare case a {@link Macro} that was created with
     *      the {@code function} directive. Or, {@code null} in the same case as
     *      {@link #get(String)}. The method should never return something that's not callable in the template language
     *      as a method or function; the return type is {@link TemplateModel} because that's the most specific common
     *      super-interface of {@link TemplateMethodModel}, and {@link Macro}.
     *
     * @throws ShouldNotBeGetAsMethodException
     *      If the value for the given key exists, but shouldn't be coerced to something callable as a method. This will
     *      be converted to {@link NonMethodException} by the engine, but in this exception you can optionally give a
     *      more specific explanation, and that will be added to the resulting {@link NonMethodException} as a hint to
     *      the user.
     */
    TemplateModel getBeforeMethodCall(String key)
            throws TemplateModelException, ShouldNotBeGetAsMethodException;

    /**
     * Called when the key is immediately followed by a method call. Implementations that can resolve overloads based
     * on the number of arguments may override this; existing implementations retain the historical zero-argument
     * behavior and use {@link #get(String)} for calls with arguments.
     *
     * @since 2.3.36
     */
    default TemplateModel getBeforeMethodCall(String key, int argumentCount)
            throws TemplateModelException, ShouldNotBeGetAsMethodException {
        return argumentCount == 0 ? getBeforeMethodCall(key) : get(key);
    }

    /**
     * Thrown by {@link #getBeforeMethodCall(String)}; see there.
     *
     * @since 2.3.33
     */
    final class ShouldNotBeGetAsMethodException extends Exception {
        private final TemplateModel actualValue;
        private final String hint;

        /**
         * Same as {@link ShouldNotBeGetAsMethodException(TemplateModel, String, Throwable)}, with {@code null}
         * cause exception argument.
         */
        public ShouldNotBeGetAsMethodException(TemplateModel actualValue, String hint) {
            this(actualValue, hint, null);
        }

        /**
         * @param actualValue
         *      The actual value we got instead of a method; can't be {@code null}!
         * @param hint
         *      Hint for the user, that's added to the error message; {@code null} if you just want the plain
         *      {@link NonMethodException} error message.
         * @param cause
         *      Can be {@code null}.
         */
        public ShouldNotBeGetAsMethodException(TemplateModel actualValue, String hint, Throwable cause) {
            super(null, cause, true, false);
            NullArgumentException.check(actualValue);
            this.actualValue = actualValue;
            this.hint = hint;
        }

        /**
         * The actual value we got instead of a method; not {@code null}.
         */
        public TemplateModel getActualValue() {
            return actualValue;
        }

        /**
         * Additional hint for the user; maybe {@code null}.
         */
        public String getHint() {
            return hint;
        }
    }
}
