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
package org.apache.freemarker.core.templatesuite.models;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.model.ObjectWrappingException;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.SimpleHash;
import org.apache.freemarker.core.model.impl.SimpleSequence;

/**
 * Forces using "simple" models for {@link Map}-s, {@link Collection}-s and arrays. This is mostly useful for template
 * test cases that wish to test with these models, but otherwise need to able to wrap beans and such. 
 */
public class SimpleMapAndCollectionObjectWrapper extends DefaultObjectWrapper {

    public SimpleMapAndCollectionObjectWrapper(Version incompatibleImprovements) {
        super(new DefaultObjectWrapper.Builder(incompatibleImprovements));
    }

    @Override
    public TemplateModel wrap(Object obj) throws ObjectWrappingException {
        if (obj == null) {
            return super.wrap(null);
        }        
        if (obj.getClass().isArray()) {
            obj = Arrays.asList((Object[]) obj);
        }
        if (obj instanceof Collection) {
            return new SimpleSequence((Collection<?>) obj, this);
        }
        if (obj instanceof Map) {
            return new SimpleHash((Map<?, ?>) obj, this);
        }
        
        return super.wrap(obj);
    }

}
