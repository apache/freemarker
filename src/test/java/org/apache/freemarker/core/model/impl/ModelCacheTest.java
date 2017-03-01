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

import static org.junit.Assert.*;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.model.TemplateModel;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ModelCacheTest {
    
    @Test
    public void modelCacheOff() throws Exception {
        DefaultObjectWrapper ow = new DefaultObjectWrapperBuilder(Configuration.VERSION_3_0_0).build();
        assertFalse(ow.getUseModelCache());  // default is off
        
        String s = "foo";
        assertNotSame(ow.wrap(s), ow.wrap(s));
        
        C c = new C();
        assertNotSame(ow.wrap(c), ow.wrap(c));
    }
    
    @Test
    @Ignore // ModelCache is current removed in FM3
    public void modelCacheOn() throws Exception {
        DefaultObjectWrapper ow = new DefaultObjectWrapper(Configuration.VERSION_3_0_0);
        ow.setUseModelCache(true);
        assertTrue(ow.getUseModelCache());

        TestBean obj = new TestBean();
        assertSame(ow.wrap(obj), ow.wrap(obj));
        
        C c = new C();
        TemplateModel wrappedC = ow.wrap(c);
        assertSame(wrappedC, ow.wrap(c));
        
        ow.clearClassIntrospecitonCache();
        assertNotSame(wrappedC, ow.wrap(c));
        assertSame(ow.wrap(c), ow.wrap(c));
    }

    static public class C { }

    public static class TestBean {
        //
    }
    
}
