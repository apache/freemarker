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

package freemarker.core;

import freemarker.template.TemplateException;

/**
 * A subclass of TemplateException that says there
 * is no value associated with a given expression.
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 */
public class InvalidReferenceException extends TemplateException {

    static final InvalidReferenceException FAST_INSTANCE = new InvalidReferenceException(
            "Invalid reference. Details are unavilable, as this should have been handled by an FTL construct. "
            + "If it wasn't, that's problably a bug in FreeMarker.",
            null);
    
    private static final String[] TIP = new String[] {
        "If the failing expression is known to be legally null/missing, either specify a "
        + "default value with myOptionalVar!myDefault, or use ",
        "<#if myOptionalVar??>", "when-present", "<#else>", "when-missing", "</#if>",
        ". (These only cover the last step of the expression; to cover the whole expression, "
        + "use parenthessis: (myOptionVar.foo)!myDefault, (myOptionVar.foo)??"
    };
    
    public InvalidReferenceException(Environment env) {
        super("Invalid reference", env);
    }

    public InvalidReferenceException(String description, Environment env) {
        super(description, env);
    }

    InvalidReferenceException(_ErrorDescriptionBuilder description, Environment env) {
        super(null, env, description, true);
    }

    /**
     * Use this whenever possible, as it returns {@link #FAST_INSTANCE} instead of creatin a new instance when
     * appropriate.
     */
    static InvalidReferenceException getInstance(Expression blame, Environment env) {
        if (env != null && env.getFastInvalidReferenceExceptions()) {
            return FAST_INSTANCE;
        } else {
            if (blame != null) {
                return new InvalidReferenceException(
                        new _ErrorDescriptionBuilder("The following has evaluated to null or missing:")
                                .blame(blame)
                                .tip(InvalidReferenceException.TIP),
                            env);
            } else {
                return new InvalidReferenceException(env);
            }
        }
    }
    
}
