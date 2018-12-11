/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.model.impl;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Version;

/**
 * A hack to keep some unit tests producing exactly the same results on Java 8 as on Java 5-7, by hiding members
 * added after Java 7.
 */
public class Java7MembersOnlyDefaultObjectWrapper extends DefaultObjectWrapper {
    
    private static final Set<String> POST_JAVA_7_MAP_METHODS = newHashSet(
            "compute", "computeIfAbsent", "computeIfPresent",
            "forEach", "getOrDefault", "merge", "putIfAbsent", "replace", "replaceAll");

    private static final Set<String> POST_JAVA_7_ITERABLE_METHODS = newHashSet("forEach");
    private static final Set<String> POST_JAVA_7_COLLECTION_METHODS = newHashSet("parallelStream", "removeIf", "stream");
    private static final Set<String> POST_JAVA_7_LIST_METHODS = newHashSet("sort", "spliterator");
    
    static final MethodAppearanceFineTuner POST_JAVA_7_FILTER = new MethodAppearanceFineTuner() {

        @Override
        public void process(DecisionInput in, Decision out) {
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

        private void hideMember(Decision out) {
            out.setExposeMethodAs(null);
            out.setExposeAsProperty(null);
        }
        
    };
    
    public Java7MembersOnlyDefaultObjectWrapper(Version version) {
        super(new DefaultObjectWrapper.Builder(version).methodAppearanceFineTuner(POST_JAVA_7_FILTER), true);
    }

    private static <T> Set<T> newHashSet(T... items) {
        HashSet<T> r = new HashSet<>();
        for (T item : items) {
            r.add(item);
        }
        return r;
    }

    public Java7MembersOnlyDefaultObjectWrapper() {
        this(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    }
    
}
