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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecision;
import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecisionInput;

class GetlessMethodsAsPropertyGettersRule implements MethodAppearanceFineTuner, SingletonCustomizer {
    
    static final GetlessMethodsAsPropertyGettersRule INSTANCE = new GetlessMethodsAsPropertyGettersRule();
    
    // Can't be constructed from outside
    private GetlessMethodsAsPropertyGettersRule() { }

    public void process(
            MethodAppearanceDecisionInput in, MethodAppearanceDecision out) {
        legacyProcess(in.getContainingClass(), in.getMethod(), out);
    }

    /** This only exists as the tests need to call this through the deprecated method too. */
    public void legacyProcess(
            Class clazz, Method m, MethodAppearanceDecision decision) {
        if (m.getDeclaringClass() != Object.class
                && m.getReturnType() != void.class
                && m.getParameterTypes().length == 0) {
            String mName = m.getName();
            if (!looksLikePropertyReadMethod(mName)) {
                decision.setExposeMethodAs(null);
                try {
                    decision.setExposeAsProperty(new PropertyDescriptor(
                            mName, clazz, mName, null));
                } catch (IntrospectionException e) {  // Won't happen...
                    throw new RuntimeException(e); 
                }
            }
        }
    }
    
    private static boolean looksLikePropertyReadMethod(String name) {
        final int verbEnd;
        if (name.startsWith("get")) verbEnd = 3;
        else if (name.startsWith("is")) verbEnd = 2;
        else return false;
        
        return name.length() == verbEnd || Character.isUpperCase(name.charAt(verbEnd));
    }

}