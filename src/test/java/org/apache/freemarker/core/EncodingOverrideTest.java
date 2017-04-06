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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Test;

public class EncodingOverrideTest {

    @Test
    public void testMarchingCharset() throws Exception {
        Template t = createConfig(StandardCharsets.UTF_8).getTemplate("encodingOverride-UTF-8.ftl");
        assertEquals(StandardCharsets.UTF_8, t.getActualSourceEncoding());
        checkTemplateOutput(t);
    }

    @Test
    public void testDifferentCharset() throws Exception {
        Template t = createConfig(StandardCharsets.UTF_8).getTemplate("encodingOverride-ISO-8859-1.ftl");
        assertEquals(StandardCharsets.ISO_8859_1, t.getActualSourceEncoding());
        checkTemplateOutput(t);
    }

    private void checkTemplateOutput(Template t) throws TemplateException, IOException {
        StringWriter out = new StringWriter(); 
        t.process(Collections.emptyMap(), out);
        assertEquals("Béka", out.toString());
    }
    
    private Configuration createConfig(Charset charset) {
       Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
       cfg.setClassForTemplateLoading(EncodingOverrideTest.class, "");
       cfg.setSourceEncoding(charset);
       return cfg;
    }

}
