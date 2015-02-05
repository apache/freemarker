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

import freemarker.core.Environment.Namespace;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

/**
 * A macro or function (or other future callable entity) associated to a namespace and a template. Starting from
 * FreeMarker 2.4, this is used in place of {@link Macro}. With an analogy, a {@link Macro} is like a non-static
 * {@link java.lang.reflect.Method} in Java; it describes everything about the method, but it isn't bound to any object
 * on which the method could be called. Continuing this analogy, a {@link BoundCallable} is like a
 * {@link java.lang.reflect.Method} plus the object whose method it is (the {@code this} object), packed together, and
 * thus being callable in itself. In the case of FTL macros, instead of a single {@code this} object, we have two such
 * objects: a namespace and a template. One may wonder why the namespace is not enough, given that a namespace already
 * specifies a template ({@link Namespace#getTemplate()} ). It's because a namespace can contain macros from included
 * templates, and so the template isn't always the same as {@link Namespace#getTemplate()} (which is only the "root"
 * template of the namespace). (Knowing which template we are in is needed for example to resolve relative references to
 * other templates.) Furthermore, several namespaces my include exactly the same template, so we can't get away with a
 * template instead of a namespace either.
 * 
 * @since 2.4.0
 */
public final class BoundCallable implements TemplateModel {
    
    private final Macro unboundCallable;
    private final Template template;
    private final Namespace namespace;
    
    public BoundCallable(Macro macro, Template template, Namespace namespace) {
        this.unboundCallable = macro;
        this.template = template;
        this.namespace = namespace;
    }

    public Macro getUnboundCallable() {
        return unboundCallable;
    }
    
    public Template getTemplate() {
        return template;
    }

    
    public Namespace getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return "BoundCallable("
                + "unboundCallable=" + unboundCallable.getName()
                + ", template=" + template.getName()
                + ", namespace=" + namespace.getTemplate().getName()
                + ")";
    }
    
}
