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
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * {@link TemplateException}-s that don't fit into any category that warrant its own class. In fact, this was added
 * because the API of {@link TemplateException} is too simple for the purposes of the core, but it can't be
 * extended without breaking backward compatibility and exposing internals.  
 */
public class _MiscTemplateException extends TemplateException {

    // Note: On Java 5 we will use `String descPart1, Object... furtherDescParts` instead of `Object[] descriptionParts`
    //       and `String description`. That's why these are at the end of the parameter list.
    
    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public _MiscTemplateException(String description) {
        super(description, null);
    }

    public _MiscTemplateException(Environment env, String description) {
        super(description, env);
    }
    
    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:

    public _MiscTemplateException(Throwable cause, String description) {
        this(cause, null, description);
    }

    public _MiscTemplateException(Throwable cause, Environment env) {
        this(cause, env, (String) null);
    }

    public _MiscTemplateException(Throwable cause) {
        this(cause, null, (String) null);
    }
    
    public _MiscTemplateException(Throwable cause, Environment env, String description) {
        super(description, cause, env);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public _MiscTemplateException(_ErrorDescriptionBuilder description) {
        this(null, description);
    }

    public _MiscTemplateException(Environment env, _ErrorDescriptionBuilder description) {
        this(null, env, description);
    }

    public _MiscTemplateException(Throwable cause, Environment env, _ErrorDescriptionBuilder description) {
        super(cause, env, null, description);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public _MiscTemplateException(Object[] descriptionParts) {
        this((Environment) null, descriptionParts);
    }

    public _MiscTemplateException(Environment env, Object[] descriptionParts) {
        this((Throwable) null, env, descriptionParts);
    }

    public _MiscTemplateException(Throwable cause, Object[] descriptionParts) {
        this(cause, null, descriptionParts);
    }

    public _MiscTemplateException(Throwable cause, Environment env, Object[] descriptionParts) {
        super(cause, env, null, new _ErrorDescriptionBuilder(descriptionParts));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public _MiscTemplateException(Expression blamed, Object[] descriptionParts) {
        this(blamed, null, descriptionParts);
    }

    public _MiscTemplateException(Expression blamed, Environment env, Object[] descriptionParts) {
        this(blamed, null, env, descriptionParts);
    }

    public _MiscTemplateException(Expression blamed, Throwable cause, Environment env, Object[] descriptionParts) {
        super(cause, env, blamed, new _ErrorDescriptionBuilder(descriptionParts).blame(blamed));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public _MiscTemplateException(Expression blamed, String description) {
        this(blamed, null, description);
    }

    public _MiscTemplateException(Expression blamed, Environment env, String description) {
        this(blamed, null, env, description);
    }

    public _MiscTemplateException(Expression blamed, Throwable cause, Environment env, String description) {
        super(cause, env, blamed, new _ErrorDescriptionBuilder(description).blame(blamed));
    }
    
}
