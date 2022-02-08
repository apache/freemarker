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

package freemarker.ext.beans;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import freemarker.ext.util.ModelCache;
import freemarker.ext.util.ModelFactory;
import freemarker.template.TemplateModel;

public class BeansModelCache extends ModelCache {
    private final Map<Class<?>, ModelFactory> classToFactory = new ConcurrentHashMap<>();
    private final Set<String> mappedClassNames = new HashSet<>();

    private final BeansWrapper wrapper;
    
    BeansModelCache(BeansWrapper wrapper) {
        this.wrapper = wrapper;
    }
    
    @Override
    protected boolean isCacheable(Object object) {
        return object.getClass() != Boolean.class; 
    }
    
    @Override
    @SuppressFBWarnings(value="JLM_JSR166_UTILCONCURRENT_MONITORENTER", justification="Locks for factory creation only")
    protected TemplateModel create(Object object) {
        Class clazz = object.getClass();
        
        ModelFactory factory = classToFactory.get(clazz);
        
        if (factory == null) {
            // Synchronized so that we won't unnecessarily create the same factory for multiple times in parallel.
            synchronized (classToFactory) {
                factory = classToFactory.get(clazz);
                if (factory == null) {
                    String className = clazz.getName();
                    // clear mappings when class reloading is detected
                    if (!mappedClassNames.add(className)) {
                        classToFactory.clear();
                        mappedClassNames.clear();
                        mappedClassNames.add(className);
                    }
                    factory = wrapper.getModelFactory(clazz);
                    classToFactory.put(clazz, factory);
                }
            }
        }
        
        return factory.create(object, wrapper);
    }
}
