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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import freemarker.template.Version;

final class ClassIntrospectorBuilder implements Cloneable {
    
    private final boolean bugfixed;

    private static final Map/*<PropertyAssignments, Reference<ClassIntrospector>>*/ INSTANCE_CACHE = new HashMap();
    private static final ReferenceQueue INSTANCE_CACHE_REF_QUEUE = new ReferenceQueue(); 
    
    // Properties and their *defaults*:
    private int exposureLevel = BeansWrapper.EXPOSE_SAFE;
    private boolean exposeFields;
    private MethodAppearanceFineTuner methodAppearanceFineTuner;
    private MethodSorter methodSorter;
    // Attention:
    // - This is also used as a cache key, so non-normalized field values should be avoided.
    // - If some field has a default value, it must be set until the end of the constructor. No field that has a
    //   default can be left unset (like null).
    // - If you add a new field, review all methods in this class, also the ClassIntrospector constructor
    
    ClassIntrospectorBuilder(ClassIntrospector ci) {
        bugfixed = ci.bugfixed;
        exposureLevel = ci.exposureLevel;
        exposeFields = ci.exposeFields;
        methodAppearanceFineTuner = ci.methodAppearanceFineTuner;
        methodSorter = ci.methodSorter; 
    }
    
    ClassIntrospectorBuilder(Version incompatibleImprovements) {
        // Warning: incompatibleImprovements must not affect this object at versions increments where there's no
        // change in the BeansWrapper.normalizeIncompatibleImprovements results. That is, this class may don't react
        // to some version changes that affects BeansWrapper, but not the other way around. 
        bugfixed = BeansWrapper.is2321Bugfixed(incompatibleImprovements);
    }
    
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone ClassIntrospectorBuilder", e);
        }
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (bugfixed ? 1231 : 1237);
        result = prime * result + (exposeFields ? 1231 : 1237);
        result = prime * result + exposureLevel;
        result = prime * result + System.identityHashCode(methodAppearanceFineTuner);
        result = prime * result + System.identityHashCode(methodSorter);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ClassIntrospectorBuilder other = (ClassIntrospectorBuilder) obj;
        
        if (bugfixed != other.bugfixed) return false;
        if (exposeFields != other.exposeFields) return false;
        if (exposureLevel != other.exposureLevel) return false;
        if (methodAppearanceFineTuner != other.methodAppearanceFineTuner) return false;
        if (methodSorter != other.methodSorter) return false;
        
        return true;
    }
    
    public int getExposureLevel() {
        return exposureLevel;
    }

    /** See {@link BeansWrapper#setExposureLevel(int)}. */
    public void setExposureLevel(int exposureLevel) {
        if (exposureLevel < BeansWrapper.EXPOSE_ALL || exposureLevel > BeansWrapper.EXPOSE_NOTHING) {
            throw new IllegalArgumentException("Illegal exposure level: " + exposureLevel);
        }
        
        this.exposureLevel = exposureLevel;
    }

    public boolean getExposeFields() {
        return exposeFields;
    }

    /** See {@link BeansWrapper#setExposeFields(boolean)}. */
    public void setExposeFields(boolean exposeFields) {
        this.exposeFields = exposeFields;
    }

    public MethodAppearanceFineTuner getMethodAppearanceFineTuner() {
        return methodAppearanceFineTuner;
    }

    public void setMethodAppearanceFineTuner(MethodAppearanceFineTuner methodAppearanceFineTuner) {
        this.methodAppearanceFineTuner = methodAppearanceFineTuner;
    }

    public MethodSorter getMethodSorter() {
        return methodSorter;
    }

    public void setMethodSorter(MethodSorter methodSorter) {
        this.methodSorter = methodSorter;
    }

    private static void removeClearedReferencesFromInstanceCache() {
        Reference clearedRef;
        while ((clearedRef = INSTANCE_CACHE_REF_QUEUE.poll()) != null) {
            synchronized (INSTANCE_CACHE) {
                findClearedRef: for (Iterator it = INSTANCE_CACHE.values().iterator(); it.hasNext(); ) {
                    if (it.next() == clearedRef) {
                        it.remove();
                        break findClearedRef;
                    }
                }
            }
        }
    }

    /** For unit testing only */
    static void clearInstanceCache() {
        synchronized (INSTANCE_CACHE) {
            INSTANCE_CACHE.clear();
        }
    }
    
    /** For unit testing only */
    static Map getInstanceCache() {
        return INSTANCE_CACHE;
    }

    /**
     * Returns an instance that is possibly shared (singleton). Note that this comes with its own "shared lock",
     * since everyone who uses this object will have to lock with that common object.
     */
    ClassIntrospector build() {
        if ((methodAppearanceFineTuner == null || methodAppearanceFineTuner instanceof SingletonCustomizer)
                && (methodSorter == null || methodSorter instanceof SingletonCustomizer)) {
            // Instance can be cached.
            ClassIntrospector instance;
            synchronized (INSTANCE_CACHE) {
                Reference instanceRef = (Reference) INSTANCE_CACHE.get(this);
                instance = instanceRef != null ? (ClassIntrospector) instanceRef.get() : null;
                if (instance == null) {
                    ClassIntrospectorBuilder thisClone = (ClassIntrospectorBuilder) clone();  // prevent any aliasing issues
                    instance = new ClassIntrospector(thisClone, new Object(), true, true);
                    INSTANCE_CACHE.put(thisClone, new WeakReference(instance, INSTANCE_CACHE_REF_QUEUE));
                }
            }
            
            removeClearedReferencesFromInstanceCache();
            
            return instance;
        } else {
            // If methodAppearanceFineTuner or methodSorter is specified and isn't marked as a singleton, the
            // ClassIntrospector can't be shared/cached as those objects could contain a back-reference to the
            // BeansWrapper.
            return new ClassIntrospector(this, new Object(), true, false);
        }
    }

    public boolean isBugfixed() {
        return bugfixed;
    }
    
}