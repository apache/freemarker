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

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * A class that allows one to associate custom data with a {@link Configuration}, a {@link Template}, or
 * {@link Environment}.
 * 
 * <p>This API has similar approach to that of {@link ThreadLocal} (which allows one to associate
 * custom data with a thread). With an example:</p>
 * 
 * <pre>
 * // The object identity itself will serve as the attribute identifier; there's no attribute name String:
 * public static final CustomAttribute MY_ATTR = new CustomAttribute(CustomAttribute.SCOPE_CONFIGURATION);
 * ...
 *     // Set the attribute in this particular Configuration object:
 *     MY_ATTR.set(myAttrValue, cfg);
 *     ...
 *     // Read the attribute from this particular Configuration object:
 *     myAttrValue = MY_ATTR.get(cfg);
 * </pre>
 */
public class CustomAttribute {
    
    /**
     * Constant used in the constructor specifying that this attribute is {@link Environment}-scoped.
     */
    public static final int SCOPE_ENVIRONMENT = 0;
        
    /**
     * Constant used in the constructor specifying that this attribute is {@link Template}-scoped.
     */
    public static final int SCOPE_TEMPLATE = 1;
        
    /**
     * Constant used in the constructor specifying that this attribute is {@link Configuration}-scoped.
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
     * Gets the attribute from the appropriate scope that's accessible through the specified {@link Environment}. If
     * the attribute has {@link #SCOPE_ENVIRONMENT} scope, it will be get from the given {@link Environment} directly.
     * If the attribute has {@link #SCOPE_TEMPLATE} scope, it will be get from the parent of the given
     * {@link Environment} (that is, in {@link Environment#getParent()}) directly). If the attribute has
     * {@link #SCOPE_CONFIGURATION} scope, it will be get from {@link Environment#getConfiguration()}.
     * 
     * @throws NullPointerException
     *             If {@code env} is null
     * 
     * @return The new value of the attribute (possibly {@code null}), or {@code null} if the attribute doesn't exist.
     * 
     * @since 2.3.22
     */
    public final Object get(Environment env) {
        return getScopeConfigurable(env).getCustomAttribute(key, this);
    }

    /**
     * Same as {@link #get(Environment)}, but uses {@link Environment#getCurrentEnvironment()} to fill the 2nd argument.
     * 
     * @throws IllegalStateException
     *             If there is no current {@link Environment}, which is usually the case when the current thread isn't
     *             processing a template.
     */
    public final Object get() {
        return getScopeConfigurable(getRequiredCurrentEnvironment()).getCustomAttribute(key, this);
    }
    
    /**
     * Gets the value of a {@link Template}-scope attribute from the given {@link Template}.
     * 
     * @throws UnsupportedOperationException
     *             If this custom attribute has different scope than {@link #SCOPE_TEMPLATE}.
     * @throws NullPointerException
     *             If {@code template} is null
     */
    public final Object get(Template template) {
        if(scope != SCOPE_TEMPLATE) {
            throw new UnsupportedOperationException("This is not a template-scope attribute");
        }
        return ((Configurable)template).getCustomAttribute(key, this);
    }
    
    /**
     * Gets the value of a {@link Configuration}-scope attribute from the given {@link Configuration}.
     * 
     * @throws UnsupportedOperationException
     *             If this custom attribute has different scope than {@link #SCOPE_CONFIGURATION}.
     * @throws NullPointerException
     *             If {@code cfg} is null
     * 
     * @since 2.3.22
     */
    public final Object get(Configuration cfg) {
        if(scope != SCOPE_CONFIGURATION) {
            throw new UnsupportedOperationException("This is not a template-scope attribute");
        }
        return ((Configurable) cfg).getCustomAttribute(key, this);
    }
    
    
    /**
     * Sets the attribute inside the appropriate scope that's accessible through the specified {@link Environment}. If
     * the attribute has {@link #SCOPE_ENVIRONMENT} scope, it will be set in the given {@link Environment} directly. If
     * the attribute has {@link #SCOPE_TEMPLATE} scope, it will be set in the parent of the given {@link Environment}
     * (that is, in {@link Environment#getParent()}) directly). If the attribute has {@link #SCOPE_CONFIGURATION} scope,
     * it will be set in {@link Environment#getConfiguration()}.
     * 
     * @param value
     *            The new value of the attribute. Can be {@code null}.
     * 
     * @throws NullPointerException
     *             If {@code env} is null
     * 
     * @since 2.3.22
     */
    public final void set(Object value, Environment env) {
        getScopeConfigurable(env).setCustomAttribute(key, value);
    }

    /**
     * Same as {@link #set(Object, Environment)}, but uses {@link Environment#getCurrentEnvironment()} to fill the 2nd
     * argument.
     * 
     * @throws IllegalStateException
     *             If there is no current {@link Environment}, which is usually the case when the current thread isn't
     *             processing a template.
     */
    public final void set(Object value) {
        getScopeConfigurable(getRequiredCurrentEnvironment()).setCustomAttribute(key, value);
    }

    /**
     * Sets the value of a {@link Template}-scope attribute in the given {@link Template}.
     * 
     * @param value
     *            The new value of the attribute. Can be {@code null}.
     * 
     * @throws UnsupportedOperationException
     *             If this custom attribute has different scope than {@link #SCOPE_TEMPLATE}.
     * @throws NullPointerException
     *             If {@code template} is null
     */
    public final void set(Object value, Template template) {
        if(scope != SCOPE_TEMPLATE) {
            throw new UnsupportedOperationException("This is not a template-scope attribute");
        }
        ((Configurable) template).setCustomAttribute(key, value);
    }

    /**
     * Sets the value of a {@link Configuration}-scope attribute in the given {@link Configuration}.
     * 
     * @param value
     *            The new value of the attribute. Can be {@code null}.
     * 
     * @throws UnsupportedOperationException
     *             If this custom attribute has different scope than {@link #SCOPE_CONFIGURATION}.
     * @throws NullPointerException
     *             If {@code cfg} is null
     * 
     * @since 2.3.22
     */
    public final void set(Object value, Configuration cfg) {
        if(scope != SCOPE_CONFIGURATION) {
            throw new UnsupportedOperationException("This is not a configuration-scope attribute");
        }
        ((Configurable) cfg).setCustomAttribute(key, value);
    }
    
    private Environment getRequiredCurrentEnvironment() {
        Environment c = Environment.getCurrentEnvironment();
        if(c == null) {
            throw new IllegalStateException("No current environment");
        }
        return c;
    }

    private Configurable getScopeConfigurable(Environment env) throws Error {
        switch (scope) {
        case SCOPE_ENVIRONMENT:
            return env;
        case SCOPE_TEMPLATE:
            return env.getParent();
        case SCOPE_CONFIGURATION:
            return env.getParent().getParent();
        default:
            throw new BugException();
        }
    }
    
}
