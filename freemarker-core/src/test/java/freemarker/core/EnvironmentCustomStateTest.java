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
package freemarker.core;

import static org.junit.Assert.*;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class EnvironmentCustomStateTest {
    
    private static final Object KEY_1 = new Object();
    private static final Object KEY_2 = new Object();

    @Test
    public void test() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_24);
        Template t = new Template(null, "", cfg);
        Environment env = t.createProcessingEnvironment(null, null);
        assertNull(env.getCustomState(KEY_1));
        assertNull(env.getCustomState(KEY_2));
        env.setCustomState(KEY_1, "a");
        env.setCustomState(KEY_2, "b");
        assertEquals("a", env.getCustomState(KEY_1));
        assertEquals("b", env.getCustomState(KEY_2));
        env.setCustomState(KEY_1, "c");
        env.setCustomState(KEY_2, null);
        assertEquals("c", env.getCustomState(KEY_1));
        assertNull(env.getCustomState(KEY_2));
    }
    
}
