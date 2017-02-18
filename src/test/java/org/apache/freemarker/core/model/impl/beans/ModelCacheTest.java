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

package org.apache.freemarker.core.model.impl.beans;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.model.TemplateModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ModelCacheTest {
    
    @Test
    public void modelCacheOff() throws Exception {
        BeansWrapper bw = new BeansWrapperBuilder(Configuration.VERSION_3_0_0).build();
        assertFalse(bw.getUseModelCache());  // default is off
        
        String s = "foo";
        assertNotSame(bw.wrap(s), bw.wrap(s));
        
        C c = new C();
        assertNotSame(bw.wrap(c), bw.wrap(c));
    }
    
    @Test
    public void modelCacheOn() throws Exception {
        BeansWrapper bw = new BeansWrapper(Configuration.VERSION_3_0_0);
        bw.setUseModelCache(true);
        assertTrue(bw.getUseModelCache());
        
        String s = "foo";
        assertSame(bw.wrap(s), bw.wrap(s));
        
        C c = new C();
        TemplateModel wrappedC = bw.wrap(c);
        assertSame(wrappedC, bw.wrap(c));
        
        bw.clearClassIntrospecitonCache();
        assertNotSame(wrappedC, bw.wrap(c));
        assertSame(bw.wrap(c), bw.wrap(c));
    }

    static public class C { }
    
}
