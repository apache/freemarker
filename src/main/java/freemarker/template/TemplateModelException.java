/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
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
        super(cause, env, descriptionBuilder, true);
    }
    
}
