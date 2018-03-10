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

import java.io.IOException;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class AbsoluteTemplateNameBITest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setTemplateLoader(new StringTemplateLoader());
        return cfg;
    }

    @Test
    public void basicsTest() throws Exception {
        assertOutput("${'a/b'?absolute_template_name}", "/a/b");
        assertOutput("${'a/b/'?absolute_template_name}", "/a/b/");
        assertOutput("${'foo://a/b'?absolute_template_name}", "foo://a/b");
        assertOutput("${'/a/b'?absolute_template_name}", "/a/b");

        assertOutputOfDirPerF("${'a/b'?absolute_template_name}", "/dir/a/b");
        assertOutputOfDirPerF("${'a/b/'?absolute_template_name}", "/dir/a/b/");
        assertOutputOfDirPerF("${'foo://a/b'?absolute_template_name}", "foo://a/b");
        assertOutputOfDirPerF("${'/a/b'?absolute_template_name}", "/a/b");
        
        for (String baseName : new String[] { "dir/f", "/dir/f", "dir/", "/dir/" }) {
            assertOutput("${'a/b'?absolute_template_name('" + baseName + "')}", "/dir/a/b");
            assertOutput("${'a/b/'?absolute_template_name('" + baseName + "')}", "/dir/a/b/");
            assertOutput("${'foo://a/b'?absolute_template_name('" + baseName + "')}", "foo://a/b");
            assertOutput("${'/a/b'?absolute_template_name('" + baseName + "')}", "/a/b");
        }

        assertOutput("${'a/b'?absolute_template_name('schema://dir/f')}", "schema://dir/a/b");
        assertOutput("${'a/b/'?absolute_template_name('schema://dir/f')}", "schema://dir/a/b/");
        assertOutput("${'foo://a/b'?absolute_template_name('schema://dir/f')}", "foo://a/b");
        assertOutput("${'/a/b'?absolute_template_name('schema://dir/f')}", "schema://a/b");
    }
    
    private void assertOutputOfDirPerF(String ftl, String expectedOut)
            throws IOException, TemplateException {
        addTemplate("dir/f", ftl);
        getConfiguration().removeTemplateFromCache("dir/f");
        assertOutputForNamed("dir/f", expectedOut);
    }
    
}
