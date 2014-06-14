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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;

public class _TemplateModelException extends TemplateModelException {

    // Note: On Java 5 we will use `String descPart1, Object... furtherDescParts` instead of `Object[] descriptionParts`
    //       and `String description`. That's why these are at the end of the parameter list.
    
    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public _TemplateModelException(String description) {
        super(description);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:

    public _TemplateModelException(Throwable cause, String description) {
        this(cause, null, description);
    }

    public _TemplateModelException(Environment env, String description) {
        this((Throwable) null, env, description);
    }
    
    public _TemplateModelException(Throwable cause, Environment env) {
        this(cause, env, (String) null);
    }

    public _TemplateModelException(Throwable cause) {
        this(cause, null, (String) null);
    }
    
    public _TemplateModelException(Throwable cause, Environment env, String description) {
        super(cause, env, description, true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public _TemplateModelException(_ErrorDescriptionBuilder description) {
        this(null, description);
    }

    public _TemplateModelException(Environment env, _ErrorDescriptionBuilder description) {
        this(null, env, description);
    }

    public _TemplateModelException(Throwable cause, Environment env, _ErrorDescriptionBuilder description) {
        super(cause, env, description, true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public _TemplateModelException(Object[] descriptionParts) {
        this((Environment) null, descriptionParts);
    }

    public _TemplateModelException(Environment env, Object[] descriptionParts) {
        this((Throwable) null, env, descriptionParts);
    }

    public _TemplateModelException(Throwable cause, Object[] descriptionParts) {
        this(cause, null, descriptionParts);
    }

    public _TemplateModelException(Throwable cause, Environment env, Object[] descriptionParts) {
        super(cause, env, new _ErrorDescriptionBuilder(descriptionParts), true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public _TemplateModelException(Expression blamed, Object[] descriptionParts) {
        this(blamed, null, descriptionParts);
    }

    public _TemplateModelException(Expression blamed, Environment env, Object[] descriptionParts) {
        this(blamed, null, env, descriptionParts);
    }

    public _TemplateModelException(Expression blamed, Throwable cause, Environment env, Object[] descriptionParts) {
        super(cause, env, new _ErrorDescriptionBuilder(descriptionParts).blame(blamed), true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public _TemplateModelException(Expression blamed, String description) {
        this(blamed, null, description);
    }

    public _TemplateModelException(Expression blamed, Environment env, String description) {
        this(blamed, null, env, description);
    }

    public _TemplateModelException(Expression blamed, Throwable cause, Environment env, String description) {
        super(cause, env, new _ErrorDescriptionBuilder(description).blame(blamed), true);
    }

    static Object[] modelHasStoredNullDescription(Class expected, TemplateModel model) {
        return new Object[] {
                "The FreeMarker value exists, but has nothing inside it; the TemplateModel object (class: ",
                model.getClass().getName(), ") has returned a null instead of a ",
                ClassUtil.getShortClassName(expected), ". ",
                "This is possibly a bug in the non-FreeMarker code that builds the data-model." };
    }
    
}
