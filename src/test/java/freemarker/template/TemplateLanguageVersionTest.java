/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.template;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
public class TemplateLanguageVersionTest {

    @Test
    public void testDefaultVersion() throws IOException {
        testDefaultWithVersion(Configuration.VERSION_2_3_0, Configuration.VERSION_2_3_0);
        testDefaultWithVersion(new Version(2, 3, 18), Configuration.VERSION_2_3_0);
        testDefaultWithVersion(Configuration.VERSION_2_3_19, Configuration.VERSION_2_3_19);
        testDefaultWithVersion(Configuration.VERSION_2_3_20, Configuration.VERSION_2_3_20);
        testDefaultWithVersion(Configuration.VERSION_2_3_21, Configuration.VERSION_2_3_21);
        try {
            testDefaultWithVersion(new Version(2, 3, 24), Configuration.VERSION_2_3_21);
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
