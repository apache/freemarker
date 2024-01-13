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
 * Adds an extra getter method to {@link TemplateHashModel} that can return different result than {@link #get(String)},
 * knowing that the result of it will be called as a method. At least as of 2.3.33, this is only utilized by the
 * template language for 0-argument non-void method calls directly after the dot operator and the key (like
 * {@code obj.m()}), or for the equivalent with square brackets ({@code obj["m"]()}). For example, if in the
 * template you have {@code someRecord.someComponent()}, and there {@code someRecord} was wrapped by the
 * {@link ObjectWrapper} into a {@link TemplateHashModel} that also implements this interface, then the dot operator
 * will call {@link #getBeforeMethodCall(String) getBeforeMethodCall("someComponent")}, rather than
 * {@link #get(String) get("someComponent")}. This is needed to implement subtle features like
 * {@link BeansWrapper.MethodAppearanceDecision#setMethodInsteadOfPropertyValueBeforeCall(boolean)},
 * which is needed to implement {@link ZeroArgumentNonVoidMethodPolicy#BOTH_PROPERTY_AND_METHOD}.
 *
 * <p>While technically we could do the same for method calls with more the 0 arguments, as of 2.3.33 at least, we
 * don't want to generalize this to that case. The FreeMarker 2.x template language doesn't have separated namespace for
 * methods, so this is already a hack as is, but we had to address the issue with Java records (see that at
 * {@link BeansWrapper.MethodAppearanceDecision#setMethodInsteadOfPropertyValueBeforeCall(boolean)}).
 *
 * <p>Objects wrapped with {@link BeansWrapper}, and hence with {@link DefaultObjectWrapper}, will implement this
 * interface, when they are "generic" objects (that is, when they are not classes with special wrapper, like
 * {@link Map}-s, {@link Collection}-s, {@link Number}-s, etc.).
 *
 * @since 2.3.33
 */
public interface MethodCallAwareTemplateHashModel extends TemplateHashModel {

    /**
     * This is called instead of {@link #get(String)}, if we know that the return value should be callable like a
     * method. The advantage of this is that we can coerce the value to a method when desirable, and otherwise can give
     * a more specific error message in the resulting exception than the standard {@link NonMethodException} would.
     *
     * @param key
     *      Same as for {@link #get(String)}
     *
     * @return
     *      Same as for just like {@link #get(String)}, except it should return a
     *      {@link TemplateMethodModelEx}, or a {@link TemplateMethodModel}, or in very rare case a {@link Macro}
     *      that was created with the {@code function} directive. Or, {@code null} in the same case as
     *      {@link #get(String)}. The method should never return something that's not callable in the template language
     *      as a method or function.
     *
     * @throws ShouldNotBeGetAsMethodException
     *      If the value for the given key exists, but it shouldn't be coerced something callable as a method. This will
     *      be converted to {@link NonMethodException} by the engine, but in this exception you can optionally give a
     *      more specific explanation, and that will be added to the resulting {@link NonMethodException} as a hint to
     *      the user.
     */
    TemplateModel getBeforeMethodCall(String key)
            throws TemplateModelException, ShouldNotBeGetAsMethodException;

    /**
     * Thrown by {@link #getBeforeMethodCall(String)}; see there.
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
