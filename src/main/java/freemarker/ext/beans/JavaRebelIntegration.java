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

import java.lang.ref.WeakReference;

import org.zeroturnaround.javarebel.ClassEventListener;
import org.zeroturnaround.javarebel.ReloaderFactory;

/**
 */
class JavaRebelIntegration
{
    static void testAvailability() {
        ReloaderFactory.getInstance();
    }
    
    /**
     * Adds a JavaRebel class reloading listener for a that will invalidate 
     * cached information for that class in the specified BeansWrapper when the
     * class is reloaded. The beans wrapper is weakly referenced and the 
     * listener is unregistered if the wrapper is garbage collected.
     * @param w the beans wrapper to register.
     */
    static void register(ClassIntrospector w) {
        ReloaderFactory.getInstance().addClassReloadListener(
                new ClassIntrospectorCacheInvalidator(w));
    }
    
    private static class ClassIntrospectorCacheInvalidator 
    implements ClassEventListener
    {
        private final WeakReference ref;
        
        ClassIntrospectorCacheInvalidator(ClassIntrospector w) {
            ref = new WeakReference(w);
        }
        
        public void onClassEvent(int eventType, Class klass) {
            ClassIntrospector ci = (ClassIntrospector)ref.get();
            if(ci == null) {
                ReloaderFactory.getInstance().removeClassReloadListener(this);
            }
            else if(eventType == ClassEventListener.EVENT_RELOADED) {
                ci.remove(klass);
            }
        }
    }
}
