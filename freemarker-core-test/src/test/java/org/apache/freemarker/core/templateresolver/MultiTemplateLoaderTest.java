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
package org.apache.freemarker.core.templateresolver;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.MultiTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.junit.Test;

public class MultiTemplateLoaderTest {

    @Test
    public void testBasics() throws IOException {
        StringTemplateLoader stl1 = new StringTemplateLoader();
        stl1.putTemplate("1.f3ah", "1");
        stl1.putTemplate("both.f3ah", "both 1");

        StringTemplateLoader stl2 = new StringTemplateLoader();
        stl2.putTemplate("2.f3ah", "2");
        stl2.putTemplate("both.f3ah", "both 2");
        
        MultiTemplateLoader mtl = new MultiTemplateLoader(stl1, stl2);
        assertEquals("1", getTemplateContent(mtl, "1.f3ah"));
        assertEquals("2", getTemplateContent(mtl, "2.f3ah"));
        assertEquals("both 1", getTemplateContent(mtl, "both.f3ah"));
        assertNull(getTemplateContent(mtl, "neither.f3ah"));
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
        stl1.putTemplate("both.f3ah", "both 1");
        
        ByteArrayTemplateLoader stl2 = new ByteArrayTemplateLoader();
        stl2.putTemplate("both.f3ah", "both 2".getBytes(StandardCharsets.UTF_8));

        MultiTemplateLoader mtl = new MultiTemplateLoader(stl1, stl2);
        mtl.setSticky(sticky);
        
        assertEquals("both 1", getTemplateContent(mtl, "both.f3ah"));
        assertTrue(stl1.removeTemplate("both.f3ah"));
        assertEquals("both 2", getTemplateContent(mtl, "both.f3ah"));
        stl1.putTemplate("both.f3ah", "both 1");
        assertEquals(sticky ? "both 2" : "both 1", getTemplateContent(mtl, "both.f3ah"));
        assertTrue(stl2.removeTemplate("both.f3ah"));
        assertEquals("both 1", getTemplateContent(mtl, "both.f3ah"));
    }
    
    private String getTemplateContent(TemplateLoader tl, String name) throws IOException {
        TemplateLoaderSession ses = tl.createSession();
        try {
            TemplateLoadingResult res = tl.load(name, null, null, ses);
            if (res.getStatus() == TemplateLoadingResultStatus.NOT_FOUND) {
                return null;
            }
            return IOUtils.toString(
                    res.getReader() != null
                            ? res.getReader()
                            : new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8));
        } finally {
            if (ses != null) {
                ses.close();
            }
        }
    }
    
}
