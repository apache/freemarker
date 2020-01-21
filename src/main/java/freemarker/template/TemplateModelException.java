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

import freemarker.core.Environment;
import freemarker.core._ErrorDescriptionBuilder;

/**
 * {@link ObjectWrapper}-s may throw this when wrapping/unwrapping fails, or {@link TemplateModel} methods throw this
 * if the requested data can't be retrieved.
 */
public class TemplateModelException extends TemplateException {
    
    private final boolean replaceWithCause;

    /**
     * Constructs a <tt>TemplateModelException</tt> with no
     * specified detail message.
     */
    public TemplateModelException() {
        this(null, null);
    }

    /**
     * Constructs a <tt>TemplateModelException</tt> with the
     * specified detail message.
     *
     * @param description the detail message.
     */
    public TemplateModelException(String description) {
        this(description, null);
    }

    /**
     * The same as {@link #TemplateModelException(Throwable)}; it's exists only for binary
     * backward-compatibility.
     */
    public TemplateModelException(Exception cause) {
        this(null, cause);
    }

    /**
     * Constructs a <tt>TemplateModelException</tt> with the given underlying
     * Exception, but no detail message.
     *
     * @param cause the underlying {@link Exception} that caused this
     * exception to be raised
     */
    public TemplateModelException(Throwable cause) {
        this(null, cause);
    }
    
    /**
     * The same as {@link #TemplateModelException(String, Throwable)}; it's exists only for binary
     * backward-compatibility.
     */
    public TemplateModelException(String description, Exception cause) {
        this(description, (Throwable) cause);
    }

    /**
     * Same as {@link #TemplateModelException(String, boolean, Throwable)} with {@code false} {@code replaceWithCause}
     * argument.
     */
    public TemplateModelException(String description, Throwable cause) {
        this(description, false, cause);
    }

    /**
     * Constructs a TemplateModelException with both a description of the error
     * that occurred and the underlying Exception that caused this exception
     * to be raised.
     *
     * @param description the description of the error that occurred
     * @param replaceWithCause See {@link #getReplaceWithCause()}; usually {@code false}, unless you are forced to wrap
     *     {@link TemplateException} into a {@link TemplateModelException} merely due to API constraints.
     * @param cause the underlying {@link Exception} that caused this
     * exception to be raised
     * 
     * @since 2.3.28
     */
    public TemplateModelException(String description, boolean replaceWithCause, Throwable cause) {
        super(description, cause, null);
        this.replaceWithCause = replaceWithCause;
    }
    
    /**
     * Don't use this; this is to be used internally by FreeMarker.
     * @param preventAmbiguity its value is ignored; it's only to prevent constructor selection ambiguities for
     *     backward-compatibility
     */
    protected TemplateModelException(Throwable cause, Environment env, String description,
            boolean preventAmbiguity) {
        super(description, cause, env);
        this.replaceWithCause = false;
    }
    
    /**
     * Don't use this; this is to be used internally by FreeMarker.
     * @param preventAmbiguity its value is ignored; it's only to prevent constructor selection ambiguities for
     *     backward-compatibility
     */
    protected TemplateModelException(
            Throwable cause, Environment env, _ErrorDescriptionBuilder descriptionBuilder,
            boolean preventAmbiguity) {
        super(cause, env, null, descriptionBuilder);
        this.replaceWithCause = false;
    }
    
    /**
     * Indicates that the cause exception should be thrown instead of this exception; it was only wrapped into this
     * exception due to API constraints. Such unwanted wrapping typically occurs when you are only allowed to throw
     * {@link TemplateModelException}, but the exception to propagate is a more generic {@link TemplateException}.
     * The error handler mechanism of FreeMarker will replace the exception with its {@link #getCause()} when it has
     * bubbled up to a place where that constraint doesn't apply anymore. 
     * 
     * @since 2.3.28
     */
    public boolean getReplaceWithCause() {
        return replaceWithCause;
    }
    
}
