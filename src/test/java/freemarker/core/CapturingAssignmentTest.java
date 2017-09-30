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

public class CapturingAssignmentTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setTemplateLoader(new StringTemplateLoader());
        return cfg;
    }

    @Test
    public void testAssign() throws IOException, TemplateException {
        assertOutput("<#assign x></#assign>[${x}]", "[]");
        assertOutput("<#assign x><p>${1 + 1}</#assign>${x + '&'}", "<p>2&");
        assertOutput("<#ftl outputFormat='HTML'><#assign x><p>${1 + 1}</#assign>${x + '&'}", "<p>2&amp;");
    }

    @Test
    public void testAssignNs() throws IOException, TemplateException {
        addTemplate("lib.ftl", "");
        assertOutput("<#import 'lib.ftl' as lib>"
                + "<#assign x in lib></#assign>[${lib.x}]", "[]");
        assertOutput("<#import 'lib.ftl' as lib>"
                + "<#assign x in lib><p>${1 + 1}</#assign>${lib.x + '&'}", "<p>2&");
        assertOutput("<#ftl outputFormat='HTML'>"
                + "<#import 'lib.ftl' as lib>"
                + "<#assign x in lib><p>${1 + 1}</#assign>${lib.x + '&'}", "<p>2&amp;");
    }
    
    @Test
    public void testGlobal() throws IOException, TemplateException {
        assertOutput("<#global x></#global>[${.globals.x}]", "[]");
        assertOutput("<#global x><p>${1 + 1}</#global>${.globals.x + '&'}", "<p>2&");
        assertOutput("<#ftl outputFormat='HTML'><#global x><p>${1 + 1}</#global>${.globals.x + '&'}", "<p>2&amp;");
    }

    @Test
    public void testLocal() throws IOException, TemplateException {
        assertOutput("<#macro m><#local x></#local>[${x}]</#macro><@m/>${x!}", "[]");
        assertOutput("<#macro m><#local x><p>${1 + 1}</#local>${x + '&'}</#macro><@m/>${x!}", "<p>2&");
        assertOutput("<#ftl outputFormat='HTML'>"
                + "<#macro m><#local x><p>${1 + 1}</#local>${x + '&'}</#macro><@m/>${x!}", "<p>2&amp;");
    }
    
}
