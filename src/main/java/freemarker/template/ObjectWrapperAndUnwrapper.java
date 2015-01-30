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

package freemarker.template;

import freemarker.ext.util.WrapperTemplateModel;

/**
 * <b>Experimental - subject to change:</b> Adds functionality to {@link ObjectWrapper} that creates a plain Java object
 * from a {@link TemplateModel}. This is usually implemented by {@link ObjectWrapper}-s and reverses
 * {@link ObjectWrapper#wrap(Object)}. However, an implementation of this interface should make a reasonable effort to
 * "unwrap" {@link TemplateModel}-s that wasn't the result of object wrapping (such as those created directly in FTL),
 * or which was created by another {@link ObjectWrapper}. The author of an {@link ObjectWrapperAndUnwrapper} should be
 * aware of the {@link TemplateModelAdapter} and {@link WrapperTemplateModel} interfaces, which should be used for
 * unwrapping if the {@link TemplateModel} implements them.
 * 
 * <p>
 * <b>Experimental status warning:</b> This interface is subject to change on non-backward compatible ways, hence, it
 * shouldn't be implemented outside FreeMarker yet.
 * 
 * @since 2.3.22
 */
public interface ObjectWrapperAndUnwrapper extends ObjectWrapper {

    /**
     * Indicates that while the unwrapping is <em>maybe</em> possible, the result surely can't be the instance of the
     * desired class, nor it can be {@code null}.
     * 
     * @see #tryUnwrapTo(TemplateModel, Class)
     * 
     * @since 2.3.22
     */
    Object CANT_UNWRAP_TO_TARGET_CLASS = new Object();

    /**
     * Unwraps a {@link TemplateModel} to a plain Java object.
     * 
     * @return The plain Java object. Can be {@code null}, if {@code null} is the appropriate Java value to represent
     *         the template model. {@code null} must not be used to indicate an unwrapping failure. It must NOT be
     *         {@link #CANT_UNWRAP_TO_TARGET_CLASS}.
     * 
     * @throws TemplateModelException
     *             If the unwrapping fails from any reason.
     * 
     * @see #tryUnwrapTo(TemplateModel, Class)
     * 
     * @since 2.3.22
     */
    Object unwrap(TemplateModel tm) throws TemplateModelException;

    /**
     * Attempts to unwrap a {@link TemplateModel} to a plain Java object that's the instance of the given class (or is
     * {@code null}).
     * 
     * @param targetClass
     *            The class that the return value must be an instance of (except when the return value is {@code null}).
     *            Can't be {@code null}; if the caller doesn't care, it should either use {#unwrap(TemplateModel)}, or
     *            {@code Object.class} as the parameter value.
     *
     * @return The unwrapped value that's either an instance of {@code targetClass}, or is {@code null} (if {@code null}
     *         is the appropriate Java value to represent the template model), or is
     *         {@link #CANT_UNWRAP_TO_TARGET_CLASS} if the unwrapping can't satisfy the {@code targetClass} (nor the
     *         result can be {@code null}). However, {@link #CANT_UNWRAP_TO_TARGET_CLASS} must not be returned if the
     *         {@code targetClass} parameter was {@code Object.class}.
     * 
     * @throws TemplateModelException
     *             If the unwrapping fails for a reason than doesn't fit the meaning of the
     *             {@link #CANT_UNWRAP_TO_TARGET_CLASS} return value.
     * 
     * @see #unwrap(TemplateModel)
     * 
     * @since 2.3.22
     */
    Object tryUnwrapTo(TemplateModel tm, Class targetClass) throws TemplateModelException;

}
