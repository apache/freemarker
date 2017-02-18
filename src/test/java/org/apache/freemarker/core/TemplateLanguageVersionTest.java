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

package org.apache.freemarker.core;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.test.util.TestUtil;
import org.junit.Test;
public class TemplateLanguageVersionTest {

    @Test
    public void testDefaultVersion() throws IOException {
        testDefaultWithVersion(Configuration.VERSION_3_0_0, Configuration.VERSION_3_0_0);
        try {
            testDefaultWithVersion(TestUtil.getClosestFutureVersion(), Configuration.VERSION_3_0_0);
            fail("Maybe you need to update this test for the new FreeMarker version");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("version"));
        }
    }

    private void testDefaultWithVersion(Version cfgV, Version ftlV) throws IOException {
        Configuration cfg = createConfiguration(cfgV);
        Template t = new Template("adhoc", "foo", cfg);
        assertEquals(ftlV, t.getTemplateLanguageVersion());
        t = cfg.getTemplate("test.ftl");
        assertEquals(ftlV, t.getTemplateLanguageVersion());
    }
    
    private Configuration createConfiguration(Version version) {
        Configuration cfg = new Configuration(version);
        StringTemplateLoader stl = new StringTemplateLoader();
        stl.putTemplate("test.ftl", "foo");
        cfg.setTemplateLoader(stl);
        return cfg;
    }
    
}
