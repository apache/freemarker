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
