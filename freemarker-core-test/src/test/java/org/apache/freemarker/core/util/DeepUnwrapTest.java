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

package org.apache.freemarker.core.util;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.junit.Test;

public class DeepUnwrapTest {

    @SuppressWarnings("rawtypes")
    @Test
    public void testHashEx2Unwrapping() throws Exception {
        Map<Object, Object> map = new LinkedHashMap<Object, Object>();
        map.put("k1", "v1");
        map.put("k2", null);
        map.put(3, "v3");
        map.put(null, "v4");
        
        DefaultObjectWrapper dow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        TemplateModel model = dow.wrap(map);
        
        assertSame(map, DeepUnwrap.unwrap(model));
        
        Object unwrapped = DeepUnwrap.unwrap(new PurelyTemplateHashModelEx((TemplateHashModelEx) model));
        assertNotSame(map, unwrapped);
        assertEquals(map, unwrapped);
        // Order is kept:
        assertArrayEquals(new Object[] { "k1", "k2", 3, null }, ((Map) unwrapped).keySet().toArray());
    }
    
    public static class PurelyTemplateHashModelEx implements TemplateHashModelEx {
        private final TemplateHashModelEx delegate;

        public PurelyTemplateHashModelEx(TemplateHashModelEx delegate) {
            this.delegate = delegate;
        }

        @Override
        public TemplateModel get(String key) throws TemplateException {
            return delegate.get(key);
        }

        @Override
        public int getHashSize() throws TemplateException {
            return delegate.getHashSize();
        }

        @Override
        public boolean isEmptyHash() throws TemplateException {
            return delegate.isEmptyHash();
        }

        @Override
        public TemplateCollectionModel keys() throws TemplateException {
            return delegate.keys();
        }

        @Override
        public TemplateCollectionModel values() throws TemplateException {
            return delegate.values();
        }

        @Override
        public KeyValuePairIterator keyValuePairIterator() throws TemplateException {
            return delegate.keyValuePairIterator();
        }
    }
    
}
