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

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.Template;

@SuppressWarnings("boxing")
public class CustomAttributeInUnboundTemplatesTest {

    @Test
    public void inFtlHeaderTest() throws IOException {
        Template t = new Template(null, "<#ftl attributes={'a': 1?int}>", new Configuration(Configuration.VERSION_2_3_23));
        t.setCustomAttribute("b", 2);
        assertEquals(1, t.getCustomAttribute("a"));
        assertEquals(2, t.getCustomAttribute("b"));
        assertEquals(ImmutableMap.of("a", 1), t.getUnboundTemplate().getCustomAttributes());
    }
    
    @Test
    public void inTemplateConfigurationTest() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        
        TemplateConfiguration tc = new TemplateConfiguration();
        tc.setCustomAttribute("a", 1);
        tc.setParentConfiguration(cfg);
        
        Template t = new Template(null, null, new StringReader(""), cfg, tc, null);
        t.setCustomAttribute("b", 2);
        assertNull(t.getCustomAttribute("a"));
        tc.apply(t);
        assertEquals(1, t.getCustomAttribute("a"));
        assertEquals(2, t.getCustomAttribute("b"));
        assertNull(t.getUnboundTemplate().getCustomAttributes());
    }

}
