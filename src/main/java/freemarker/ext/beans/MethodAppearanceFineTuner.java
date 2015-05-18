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

package freemarker.ext.beans;

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;

import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecision;
import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecisionInput;

/**
 * Used for customizing how the methods are visible from templates, via
 * {@link BeansWrapper#setMethodAppearanceFineTuner(MethodAppearanceFineTuner)}.
 * The object that implements this should also implement {@link SingletonCustomizer} whenever possible.
 * 
 * @since 2.3.21
 */
public interface MethodAppearanceFineTuner {

    /**
     * <b>Experimental method; subject to change!</b>
     * Implement this to tweak certain aspects of how methods appear in the
     * data-model. {@link BeansWrapper} will pass in all Java methods here that
     * it intends to expose in the data-model as methods (so you can do
     * <tt>obj.foo()</tt> in the template).
     * With this method it you can do the following tweaks:
     * <ul>
     *   <li>Hide a method that would be otherwise shown by calling
     *     {@link MethodAppearanceDecision#setExposeMethodAs(String)}
     *     with <tt>null</tt> parameter. Note that you can't un-hide methods
     *     that are not public or are considered to by unsafe
     *     (like {@link Object#wait()}) because
     *     {@link #process} is not called for those.</li>
     *   <li>Show the method with a different name in the data-model than its
     *     real name by calling
     *     {@link MethodAppearanceDecision#setExposeMethodAs(String)}
     *     with non-<tt>null</tt> parameter.
     *   <li>Create a fake JavaBean property for this method by calling
     *     {@link MethodAppearanceDecision#setExposeAsProperty(PropertyDescriptor)}.
     *     For example, if you have <tt>int size()</tt> in a class, but you
     *     want it to be accessed from the templates as <tt>obj.size</tt>,
     *     rather than as <tt>obj.size()</tt>, you can do that with this.
     *     The default is {@code null}, which means that no fake property is
     *     created for the method. You need not and shouldn't set this
     *     to non-<tt>null</tt> for the getter methods of real JavaBean
     *     properties, as those are automatically shown as properties anyway.
     *     The property name in the {@link PropertyDescriptor} can be anything,
     *     but the method (or methods) in it must belong to the class that
     *     is given as the <tt>clazz</tt> parameter or it must be inherited from
     *     that class, or else whatever errors can occur later.
     *     {@link IndexedPropertyDescriptor}-s are supported.
     *     If a real JavaBean property of the same name exists, it won't be
     *     replaced by the fake one. Also if a fake property of the same name
     *     was assigned earlier, it won't be replaced.
     *   <li>Prevent the method to hide a JavaBean property (fake or real) of
     *     the same name by calling
     *     {@link MethodAppearanceDecision#setMethodShadowsProperty(boolean)}
     *     with <tt>false</tt>. The default is <tt>true</tt>, so if you have
     *     both a property and a method called "foo", then in the template
     *     <tt>myObject.foo</tt> will return the method itself instead
     *     of the property value, which is often undesirable.
     * </ul>
     * 
     * <p>Note that you can expose a Java method both as a method and as a
     * JavaBean property on the same time, however you have to chose different
     * names for them to prevent shadowing. 
     * 
     * @param in Describes the method about which the decision will have to be made.
     *  
     * @param out Stores how the method will be exposed in the
     *   data-model after {@link #process} returns.
     *   This is initialized so that it reflects the default
     *   behavior of {@link BeansWrapper}, so you don't have to do anything with this
     *   when you don't want to change the default behavior.
     */
    void process(MethodAppearanceDecisionInput in, MethodAppearanceDecision out);    
    
}
