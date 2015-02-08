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

import java.io.IOException;

import freemarker.core.Environment.Namespace;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * A macro or function (or other future callable entity) associated to a namespace and a template.
 * 
 * <p>
 * With an analogy, a {@link UnboundCallable} is like a non-static {@link java.lang.reflect.Method} in Java; it
 * describes everything about the method, but it isn't bound to any object on which the method could be called.
 * Continuing this analogy, a {@link BoundCallable} is like a {@link java.lang.reflect.Method} paired with the object
 * whose method it is (the {@code this} object), and is thus callable in itself. In the case of FTL macros and FTL
 * functions, instead of a single {@code this} object, we have two such objects: a namespace and a template. (One may
 * wonder why the namespace is not enough, given that a namespace already specifies a template (
 * {@link Namespace#getTemplate()} ). It's because a namespace can contain macros from included templates, and so the
 * template that the callable belongs to isn't always the same as {@link Namespace#getTemplate()}, which just gives the
 * "root" template of the namespace. Furthermore, several namespaces my include exactly the same template, so we can't
 * get away with a template instead of a namespace either. Also note that knowing which template we are in is needed for
 * example to resolve relative references to other templates.)
 * 
 * <p>
 * Historical note: Prior to 2.4, the two concepts ({@link UnboundCallable} and {@link BoundCallable}) were these same,
 * represented by {@link Macro}, which still exists due to backward compatibility constraints. This class extends
 * {@link Macro} only for the sake of legacy applications which expect macro and function FTL variables to be
 * {@link Macro}-s. Especially this class should not extend {@link TemplateElement} (which it does, because
 * {@link Macro} is a subclass of that), but it had to, for backward compatibility. It just delegates {@link Macro}
 * methods to the embedded {@link UnboundCallable}.
 * 
 * @see UnboundCallable
 * 
 * @since 2.4.0
 */
public final class BoundCallable extends Macro {
    
    private final UnboundCallable unboundCallable;
    private final Template template;
    private final Namespace namespace;
    
    BoundCallable(UnboundCallable callableDefinition, Template template, Namespace namespace) {
        this.unboundCallable = callableDefinition;
        this.template = template;
        this.namespace = namespace;
    }

    UnboundCallable getUnboundCallable() {
        return unboundCallable;
    }
    
    Template getTemplate() {
        return template;
    }
    
    Namespace getNamespace() {
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

    /** For backward compatibility only; delegates to the {@link UnboundCallable}'s identical method. */
    @Override
    public String getCatchAll() {
        return unboundCallable.getCatchAll();
    }

    /** For backward compatibility only; delegates to the {@link UnboundCallable}'s identical method. */
    @Override
    public String[] getArgumentNames() {
        return unboundCallable.getArgumentNames();
    }

    /** For backward compatibility only; delegates to the {@link UnboundCallable}'s identical method. */
    @Override
    public String getName() {
        return unboundCallable.getName();
    }

    /** For backward compatibility only; delegates to the {@link UnboundCallable}'s identical method. */
    @Override
    public boolean isFunction() {
        return unboundCallable.isFunction();
    }

    /** For backward compatibility only; delegates to the {@link UnboundCallable}'s identical method. */
    @Override
    void accept(Environment env) throws TemplateException, IOException {
        unboundCallable.accept(env);
    }

    /** For backward compatibility only; delegates to the {@link UnboundCallable}'s identical method. */
    @Override
    protected String dump(boolean canonical) {
        return unboundCallable.dump(canonical);
    }

    /** For backward compatibility only; delegates to the {@link UnboundCallable}'s identical method. */
    @Override
    String getNodeTypeSymbol() {
        return unboundCallable.getNodeTypeSymbol();
    }

    /** For backward compatibility only; delegates to the {@link UnboundCallable}'s identical method. */
    @Override
    int getParameterCount() {
        return unboundCallable.getParameterCount();
    }

    /** For backward compatibility only; delegates to the {@link UnboundCallable}'s identical method. */
    @Override
    Object getParameterValue(int idx) {
        return unboundCallable.getParameterValue(idx);
    }

    /** For backward compatibility only; delegates to the {@link UnboundCallable}'s identical method. */
    @Override
    ParameterRole getParameterRole(int idx) {
        return unboundCallable.getParameterRole(idx);
    }
    
}
