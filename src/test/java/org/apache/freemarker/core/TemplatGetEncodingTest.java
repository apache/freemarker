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
import java.nio.charset.StandardCharsets;

import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.StrongCacheStorage;
import org.junit.Test;

public class TemplatGetEncodingTest {

    @Test
    public void test() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        {
            cfg.setDefaultEncoding("ISO-8859-2");
            ByteArrayTemplateLoader tl = new ByteArrayTemplateLoader();
            tl.putTemplate("t", "test".getBytes(StandardCharsets.UTF_8));
            tl.putTemplate("tnp", "<#test>".getBytes(StandardCharsets.UTF_8));
            cfg.setTemplateLoader(tl);
            cfg.setCacheStorage(new StrongCacheStorage());
        }

        {
            Template tDefEnc = cfg.getTemplate("t");
            assertEquals("ISO-8859-2", tDefEnc.getEncoding());
            assertSame(tDefEnc, cfg.getTemplate("t"));

            Template tDefEnc2 = cfg.getTemplate("t", (String) null);
            assertEquals("ISO-8859-2", tDefEnc2.getEncoding());
            assertSame(tDefEnc, tDefEnc2);
            
            Template tUTF8 = cfg.getTemplate("t", "UTF-8");
            assertEquals("UTF-8", tUTF8.getEncoding());
            assertSame(tUTF8, cfg.getTemplate("t", "UTF-8"));
            assertNotSame(tDefEnc, tUTF8);
        }

        {
            Template tDefEnc = cfg.getTemplate("tnp", null, null, false);
            assertEquals("ISO-8859-2", tDefEnc.getEncoding());
            assertSame(tDefEnc, cfg.getTemplate("tnp", null, null, false));

            Template tUTF8 = cfg.getTemplate("tnp", null, "UTF-8", false);
            assertEquals("UTF-8", tUTF8.getEncoding());
            assertSame(tUTF8, cfg.getTemplate("tnp", null, "UTF-8", false));
            assertNotSame(tDefEnc, tUTF8);
        }
        
        {
            Template nonStoredT = new Template(null, "test", cfg);
            assertNull(nonStoredT.getEncoding());
        }

        {
            Template nonStoredT = Template.createPlainTextTemplate(null, "<#test>", cfg);
            assertNull(nonStoredT.getEncoding());
        }
    }

}
