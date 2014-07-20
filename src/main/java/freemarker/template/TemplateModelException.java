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

import freemarker.core.Environment;
import freemarker.core._ErrorDescriptionBuilder;

/**
 * {@link TemplateModel} methods throw this exception if the requested data can't be retrieved.  
 */
public class TemplateModelException extends TemplateException {

    /**
     * Constructs a <tt>TemplateModelException</tt> with no
     * specified detail message.
     */
    public TemplateModelException() {
        this((String) null, null);
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
        this((String) null, cause);
    }

    /**
     * Constructs a <tt>TemplateModelException</tt> with the given underlying
     * Exception, but no detail message.
     *
     * @param cause the underlying {@link Exception} that caused this
     * exception to be raised
     */
    public TemplateModelException(Throwable cause) {
        this((String) null, cause);
    }

    
    /**
     * The same as {@link #TemplateModelException(String, Throwable)}; it's exists only for binary
     * backward-compatibility.
     */
    public TemplateModelException(String description, Exception cause) {
        super(description, cause, null);
    }

    /**
     * Constructs a TemplateModelException with both a description of the error
     * that occurred and the underlying Exception that caused this exception
     * to be raised.
     *
     * @param description the description of the error that occurred
     * @param cause the underlying {@link Exception} that caused this
     * exception to be raised
     */
    public TemplateModelException(String description, Throwable cause) {
        super(description, cause, null);
    }

    /**
     * Don't use this; this is to be used internally by FreeMarker.
     * @param preventAmbiguity its value is ignored; it's only to prevent constructor selection ambiguities for
     *     backward-compatibility
     */
    protected TemplateModelException(Throwable cause, Environment env, String description,
            boolean preventAmbiguity) {
        super(description, cause, env);
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
    }
    
}
