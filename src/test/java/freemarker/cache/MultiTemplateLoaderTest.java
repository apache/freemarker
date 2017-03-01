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
package freemarker.cache;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class MultiTemplateLoaderTest {

    @Test
    public void testBasics() throws IOException {
        StringTemplateLoader stl1 = new StringTemplateLoader();
        stl1.putTemplate("1.ftl", "1");
        stl1.putTemplate("both.ftl", "both 1");

        StringTemplateLoader stl2 = new StringTemplateLoader();
        stl2.putTemplate("2.ftl", "2");
        stl2.putTemplate("both.ftl", "both 2");
        
        MultiTemplateLoader mtl = new MultiTemplateLoader(new TemplateLoader[] { stl1, stl2 });
        assertEquals("1", getTemplateContent(mtl, "1.ftl"));
        assertEquals("2", getTemplateContent(mtl, "2.ftl"));
        assertEquals("both 1", getTemplateContent(mtl, "both.ftl"));
        assertNull(getTemplateContent(mtl, "neither.ftl"));
    }

    @Test
    public void testSticky() throws IOException {
        testStickiness(true);
    }

    @Test
    public void testNonSticky() throws IOException {
        testStickiness(false);
    }
    
    private void testStickiness(boolean sticky) throws IOException {
        StringTemplateLoader stl1 = new StringTemplateLoader();
        stl1.putTemplate("both.ftl", "both 1");
        
        StringTemplateLoader stl2 = new StringTemplateLoader();
        stl2.putTemplate("both.ftl", "both 2");

        MultiTemplateLoader mtl = new MultiTemplateLoader(new TemplateLoader[] { stl1, stl2 });
        mtl.setSticky(sticky);
        
        assertEquals("both 1", getTemplateContent(mtl, "both.ftl"));
        assertTrue(stl1.removeTemplate("both.ftl"));
        assertEquals("both 2", getTemplateContent(mtl, "both.ftl"));
        stl1.putTemplate("both.ftl", "both 1");
        assertEquals(sticky ? "both 2" : "both 1", getTemplateContent(mtl, "both.ftl"));
        assertTrue(stl2.removeTemplate("both.ftl"));
        assertEquals("both 1", getTemplateContent(mtl, "both.ftl"));
    }
    
    private String getTemplateContent(TemplateLoader tl, String name) throws IOException {
        Object tSrc = tl.findTemplateSource(name);
        if (tSrc == null) {
            return null;
        }
        
        return IOUtils.toString(tl.getReader(tSrc, "UTF-8"));
    }
    
}
