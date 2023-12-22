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

package freemarker.template.utility;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class DeepUnwrapTest {

    @SuppressWarnings("rawtypes")
    @Test
    public void testHashEx2Unwrapping() throws Exception {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("k1", "v1");
        map.put("k2", null);
        map.put(3, "v3");
        map.put(null, "v4");
        
        DefaultObjectWrapper dow = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_27).build();
        TemplateModel model = dow.wrap(map);
        
        assertSame(map, DeepUnwrap.unwrap(model));
        
        Object unwrapped = DeepUnwrap.unwrap(new PurelyTemplateHashModelEx2((TemplateHashModelEx2) model));
        assertNotSame(map, unwrapped);
        assertEquals(map, unwrapped);
        // Order is kept:
        assertArrayEquals(new Object[] { "k1", "k2", 3, null }, ((Map) unwrapped).keySet().toArray());
    }
    
    public static class PurelyTemplateHashModelEx2 implements TemplateHashModelEx2 {
        private final TemplateHashModelEx2 delegate;

        public PurelyTemplateHashModelEx2(TemplateHashModelEx2 delegate) {
            this.delegate = delegate;
        }

        public TemplateModel get(String key) throws TemplateModelException {
            return delegate.get(key);
        }

        public int size() throws TemplateModelException {
            return delegate.size();
        }

        public KeyValuePairIterator keyValuePairIterator() throws TemplateModelException {
            return delegate.keyValuePairIterator();
        }

        public TemplateCollectionModel keys() throws TemplateModelException {
            return delegate.keys();
        }

        public boolean isEmpty() throws TemplateModelException {
            return delegate.isEmpty();
        }

        public TemplateCollectionModel values() throws TemplateModelException {
            return delegate.values();
        }
    }
    
}
