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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.template.Configuration;
import freemarker.template.Version;

/**
 * A hack to keep some unit tests producing exactly the same results on Java 8 as on Java 5-7, by hiding members
 * added after Java 7.
 */
public class Java7MembersOnlyBeansWrapper extends BeansWrapper {
    
    private static final Set<String> POST_JAVA_7_MAP_METHODS = newHashSet(
            "compute", "computeIfAbsent", "computeIfPresent",
            "forEach", "getOrDefault", "merge", "putIfAbsent", "replace", "replaceAll");

    private static final Set<String> POST_JAVA_7_ITERABLE_METHODS = newHashSet("forEach");
    private static final Set<String> POST_JAVA_7_COLLECTION_METHODS = newHashSet("parallelStream", "removeIf", "stream");
    private static final Set<String> POST_JAVA_7_LIST_METHODS = newHashSet("sort", "spliterator");
    
    MethodAppearanceFineTuner POST_JAVA_7_FILTER = new MethodAppearanceFineTuner() {

        public void process(MethodAppearanceDecisionInput in, MethodAppearanceDecision out) {
            Method m = in.getMethod();
            Class declCl = m.getDeclaringClass();
            if (Map.class.isAssignableFrom(declCl)) {
                if (POST_JAVA_7_MAP_METHODS.contains(m.getName())) {
                    hideMember(out);
                    return;
                }
            }
            if (Iterable.class.isAssignableFrom(declCl)) {
                if (POST_JAVA_7_ITERABLE_METHODS.contains(m.getName())) {
                    hideMember(out);
                    return;
                }
            }
            if (Collection.class.isAssignableFrom(declCl)) {
                if (POST_JAVA_7_COLLECTION_METHODS.contains(m.getName())) {
                    hideMember(out);
                    return;
                }
            }
            if (List.class.isAssignableFrom(declCl)) {
                if (POST_JAVA_7_LIST_METHODS.contains(m.getName())) {
                    hideMember(out);
                    return;
                }
            }
        }

        private void hideMember(MethodAppearanceDecision out) {
            out.setExposeMethodAs(null);
            out.setExposeAsProperty(null);
        }
        
    };
    
    public Java7MembersOnlyBeansWrapper(Version version) {
        super(version);
        setMethodAppearanceFineTuner(POST_JAVA_7_FILTER);
    }

    private static <T> Set<T> newHashSet(T... items) {
        HashSet<T> r = new HashSet<T>();
        for (T item : items) {
            r.add(item);
        }
        return r;
    }

    public Java7MembersOnlyBeansWrapper() {
        this(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    }
    
}
