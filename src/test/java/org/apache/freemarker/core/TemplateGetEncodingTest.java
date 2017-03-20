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

import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.templateresolver.impl.StrongCacheStorage;
import org.apache.freemarker.test.MonitoredTemplateLoader;
import org.junit.Test;

public class TemplateGetEncodingTest {

    @Test
    public void test() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        {
            cfg.setDefaultEncoding("ISO-8859-2");
            MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
            tl.putBinaryTemplate("bin", "test");
            tl.putBinaryTemplate("bin-static", "<#test>");
            tl.putTextTemplate("text", "test");
            tl.putTextTemplate("text-static", "<#test>");
            TemplateConfiguration staticTextTC = new TemplateConfiguration();
            staticTextTC.setTemplateLanguage(TemplateLanguage.STATIC_TEXT);
            cfg.setTemplateConfigurations(
                    new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*-static*"), staticTextTC));
            cfg.setTemplateLoader(tl);
            cfg.setCacheStorage(new StrongCacheStorage());
        }

        assertEquals("ISO-8859-2", cfg.getTemplate("bin").getEncoding());
        assertEquals("ISO-8859-2", cfg.getTemplate("bin-static").getEncoding());
        assertNull(cfg.getTemplate("text").getEncoding());
        assertNull(cfg.getTemplate("text-static").getEncoding());
        assertNull(new Template(null, "test", cfg).getEncoding());
        assertNull(Template.createPlainTextTemplate(null, "<#test>", cfg).getEncoding());
    }

}
