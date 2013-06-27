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
