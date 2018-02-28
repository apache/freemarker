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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.DefaultMapAdapter;
import freemarker.template.DefaultNonListCollectionAdapter;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2.KeyValuePair;
import freemarker.template.TemplateHashModelEx2.KeyValuePairIterator;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.test.TemplateTest;

public class TemplateModelUtilTest extends TemplateTest {

    @Test
    public void testGetKeyValuePairIterator() throws Exception {
        Map<Object, Object> map = new LinkedHashMap<Object, Object>();
        TemplateHashModelEx thme = new TemplateHashModelExOnly(map);
        
        assertetKeyValuePairIteratorResult("", thme);
        
        map.put("k1", 11);
        assertetKeyValuePairIteratorResult("str(k1): num(11)", thme);
        
        map.put("k2", "v2");
        assertetKeyValuePairIteratorResult("str(k1): num(11), str(k2): str(v2)", thme);

        map.put("k2", null);
        assertetKeyValuePairIteratorResult("str(k1): num(11), str(k2): null", thme);
        
        map.put(3, 33);
        try {
            assertetKeyValuePairIteratorResult("fails anyway...", thme);
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("keys must be"), containsString("string"), containsString("number")));
        }
        map.remove(3);
        
        map.put(null, 44);
        try {
            assertetKeyValuePairIteratorResult("fails anyway...", thme);
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("keys must be"), containsString("string"), containsString("Null")));
        }
    }

    @Test
    public void testGetKeyValuePairIteratorWithEx2() throws Exception {
        Map<Object, Object> map = new LinkedHashMap<Object, Object>();
        TemplateHashModelEx thme = DefaultMapAdapter.adapt(
                map, (ObjectWrapperWithAPISupport) getConfiguration().getObjectWrapper());
        
        assertetKeyValuePairIteratorResult("", thme);
        
        map.put("k1", 11);
        map.put("k2", "v2");
        map.put("k2", null);
        map.put(3, 33);
        map.put(null, 44);
        assertetKeyValuePairIteratorResult("str(k1): num(11), str(k2): null, num(3): num(33), null: num(44)", thme);
    }
    
    private void assertetKeyValuePairIteratorResult(String expected, TemplateHashModelEx thme)
            throws TemplateModelException {
         StringBuilder sb = new StringBuilder();
         KeyValuePairIterator kvpi = TemplateModelUtils.getKeyValuePairIterator(thme);
         while (kvpi.hasNext()) {
             KeyValuePair kvp = kvpi.next();
             if (sb.length() != 0) {
                 sb.append(", ");
             }
             sb.append(toAssertionString(kvp.getKey())).append(": ").append(toAssertionString(kvp.getValue()));
         }
    }
    
    private String toAssertionString(TemplateModel model) throws TemplateModelException {
        if (model instanceof TemplateNumberModel) {
            return "num(" + ((TemplateNumberModel) model).getAsNumber() + ")";
        } else if (model instanceof TemplateScalarModel) {
            return "str(" + ((TemplateScalarModel) model).getAsString() + ")";
        } else if (model == null) {
            return "null";
        }
        
        throw new IllegalArgumentException("Type unsupported by test: " + model.getClass().getName());
    }

    private static class TemplateHashModelExOnly implements TemplateHashModelEx {
        
        private final Map<?, ?> map;
        private final ObjectWrapperWithAPISupport objectWrapper;
        
        public TemplateHashModelExOnly(Map<?, ?> map) {
            this.map = map;
            objectWrapper = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_27).build();
        }

        public TemplateModel get(String key) throws TemplateModelException {
            return objectWrapper.wrap(map.get(key));
        }

        public boolean isEmpty() throws TemplateModelException {
            return map.isEmpty();
        }

        public int size() throws TemplateModelException {
            return 2;
        }

        public TemplateCollectionModel keys() throws TemplateModelException {
            return DefaultNonListCollectionAdapter.adapt(map.keySet(), objectWrapper);
        }

        public TemplateCollectionModel values() throws TemplateModelException {
            return DefaultNonListCollectionAdapter.adapt(map.values(), objectWrapper);
        } 
        
    }
    
}
