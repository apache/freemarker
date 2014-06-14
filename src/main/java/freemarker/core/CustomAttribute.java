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

import freemarker.template.Template;

/**
 * A class that allows one to associate custom data with a configuration, 
 * a template, or environment. It works pretty much like {@link ThreadLocal}, a
 * class that allows one to associate custom data with a thread.
 * @author Attila Szegedi
 */
public class CustomAttribute
{
    /**
     * Constant used in the constructor specifying that this attribute is
     * scoped by the environment.
     */
    public static final int SCOPE_ENVIRONMENT = 0;
        
    /**
     * Constant used in the constructor specifying that this attribute is
     * scoped by the template.
     */
    public static final int SCOPE_TEMPLATE = 1;
        
    /**
     * Constant used in the constructor specifying that this attribute is
     * scoped by the configuration.
     */
    public static final int SCOPE_CONFIGURATION = 2;

    // We use an internal key instead of 'this' so that malicious subclasses 
    // overriding equals() and hashCode() can't gain access to other attribute
    // values. That's also the reason why get() and set() are marked final.
    private final Object key = new Object();
    private final int scope;
    
    /**
     * Creates a new custom attribute with the specified scope
     * @param scope one of <tt>SCOPE_</tt> constants. 
     */
    public CustomAttribute(int scope) {
        if(scope != SCOPE_ENVIRONMENT && 
           scope != SCOPE_TEMPLATE && 
           scope != SCOPE_CONFIGURATION) {
                throw new IllegalArgumentException();
            }
        this.scope = scope;
    }
    
    /**
     * This method is invoked when {@link #get()} is invoked without 
     * {@link #set(Object)} being invoked before it to define the value in the 
     * current scope. Override it to create the attribute value on-demand.  
     * @return the initial value for the custom attribute. By default returns null.
     */
    protected Object create() {
        return null;
    }
    
    /**
     * @return the value of the attribute in the context of the current environment.
     * @throws IllegalStateException if there is no current environment (and
     * hence also no current template and configuration), therefore the
     * attribute's current scope object can't be resolved.
     */
    public final Object get() {
        return getScopeConfigurable().getCustomAttribute(key, this);
    }
    
    /**
     * @return the value of a template-scope attribute in the context of a 
     * given template.
     * @throws UnsupportedOperationException if this custom attribute is not a
     * template-scope attribute
     * @throws NullPointerException if t is null
     */
    public final Object get(Template t) {
        if(scope != SCOPE_TEMPLATE) {
            throw new UnsupportedOperationException("This is not a template-scope attribute");
        }
        return ((Configurable)t).getCustomAttribute(key, this);
    }
    
    /**
     * Sets the value of the attribute in the context of the current environment.
     * @param value the new value of the attribute
     * @throws IllegalStateException if there is no current environment (and
     * hence also no current template and configuration), therefore the
     * attribute's current scope object can't be resolved.
     */
    public final void set(Object value) {
        getScopeConfigurable().setCustomAttribute(key, value);
    }

    /**
     * Sets the value of a template-scope attribute in the context of the given
     * template.
     * @param value the new value of the attribute
     * @param t the template 
     * @throws UnsupportedOperationException if this custom attribute is not a
     * template-scope attribute
     * @throws NullPointerException if t is null
     */
    public final void set(Object value, Template t) {
        if(scope != SCOPE_TEMPLATE) {
            throw new UnsupportedOperationException("This is not a template-scope attribute");
        }
        ((Configurable)t).setCustomAttribute(key, value);
    }

    private Configurable getScopeConfigurable() {
        Configurable c = Environment.getCurrentEnvironment();
        if(c == null) {
            throw new IllegalStateException("No current environment");
        }
        switch(scope) {
            case SCOPE_ENVIRONMENT: {
                return c;
            }
            case SCOPE_TEMPLATE: {
                return c.getParent();
            }
            case SCOPE_CONFIGURATION: {
                return c.getParent().getParent();
            }
            default: {
                throw new Error();
            }
        }
    }
}
