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

import java.io.IOException;

import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class AbsoluteTemplateNameBITest extends TemplateTest {

    @Test
    public void basicsTest() throws Exception {
        assertOutput("${'a/b'?absoluteTemplateName}", "/a/b");
        assertOutput("${'a/b/'?absoluteTemplateName}", "/a/b/");
        assertOutput("${'foo://a/b'?absoluteTemplateName}", "foo://a/b");
        assertOutput("${'/a/b'?absoluteTemplateName}", "/a/b");

        assertOutputOfDirPerF("${'a/b'?absoluteTemplateName}", "/dir/a/b");
        assertOutputOfDirPerF("${'a/b/'?absoluteTemplateName}", "/dir/a/b/");
        assertOutputOfDirPerF("${'foo://a/b'?absoluteTemplateName}", "foo://a/b");
        assertOutputOfDirPerF("${'/a/b'?absoluteTemplateName}", "/a/b");
        
        for (String baseName : new String[] { "dir/f", "/dir/f", "dir/", "/dir/" }) {
            assertOutput("${'a/b'?absoluteTemplateName('" + baseName + "')}", "/dir/a/b");
            assertOutput("${'a/b/'?absoluteTemplateName('" + baseName + "')}", "/dir/a/b/");
            assertOutput("${'foo://a/b'?absoluteTemplateName('" + baseName + "')}", "foo://a/b");
            assertOutput("${'/a/b'?absoluteTemplateName('" + baseName + "')}", "/a/b");
        }

        assertOutput("${'a/b'?absoluteTemplateName('schema://dir/f')}", "schema://dir/a/b");
        assertOutput("${'a/b/'?absoluteTemplateName('schema://dir/f')}", "schema://dir/a/b/");
        assertOutput("${'foo://a/b'?absoluteTemplateName('schema://dir/f')}", "foo://a/b");
        assertOutput("${'/a/b'?absoluteTemplateName('schema://dir/f')}", "schema://a/b");
    }
    
    private void assertOutputOfDirPerF(String ftl, String expectedOut)
            throws IOException, TemplateException {
        addTemplate("dir/f", ftl);
        Configuration cfg = getConfiguration();
        cfg.removeTemplateFromCache("dir/f", getConfiguration().getLocale(), null);
        assertOutputForNamed("dir/f", expectedOut);
    }
    
}
