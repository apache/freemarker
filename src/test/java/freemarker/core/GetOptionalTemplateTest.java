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

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import freemarker.cache.ByteArrayTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.test.TemplateTest;

public class GetOptionalTemplateTest extends TemplateTest {

    private ByteArrayTemplateLoader byteArrayTemplateLoader = new ByteArrayTemplateLoader();
    
    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setTemplateLoader(
                new MultiTemplateLoader(new TemplateLoader[] {
                        new StringTemplateLoader(), byteArrayTemplateLoader
                }));
        return cfg;
    }
    
    @Test
    public void testBasicsWhenTemplateExists() throws Exception {
        addTemplate("inc.ftl", "<#assign x = (x!0) + 1>inc ${x}");
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('inc.ftl')>"
                + "Exists: ${t.exists?c}; "
                + "Include: <@t.include />, <@t.include />; "
                + "Import: <#assign ns1 = t.import()><#assign ns2 = t.import()>${ns1.x}, ${ns2.x}; "
                + "Aliased: <#assign x = 9 in ns1>${ns1.x}, ${ns2.x}, <#import 'inc.ftl' as ns3>${ns3.x}",
                "Exists: true; "
                + "Include: inc 1, inc 2; "
                + "Import: 1, 1; "
                + "Aliased: 9, 9, 9"
                );
    }

    @Test
    public void testBasicsWhenTemplateIsMissing() throws Exception {
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('missing.ftl')>"
                + "Exists: ${t.exists?c}; "
                + "Include: ${t.include???c}; "
                + "Import: ${t.import???c}",
                "Exists: false; "
                + "Include: false; "
                + "Import: false"
                );
    }
    
    @Test
    public void testOptions() throws Exception {
        addTemplate("inc.ftl", "${1}");
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('inc.ftl', { 'parse': false })>"
                + "<@t.include />",
                "${1}");
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('inc.ftl')>"
                + "<@t.include />",
                "1");
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('inc.ftl', {})>"
                + "<@t.include />",
                "1");
        
        byteArrayTemplateLoader.putTemplate("inc-u16.ftl", "foo".getBytes(StandardCharsets.UTF_16BE));
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('inc-u16.ftl', { 'encoding': 'utf-16be' })>"
                + "<@t.include />",
                "foo");
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('inc-u16.ftl')>"
                + "<@t.include />",
                "\u0000f\u0000o\u0000o");
        
        byteArrayTemplateLoader.putTemplate("inc-u16.ftl", "foo${1}".getBytes(StandardCharsets.UTF_16BE));
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('inc-u16.ftl', { 'parse': false, 'encoding': 'utf-16be' })>"
                + "<@t.include />",
                "foo${1}");
    }

    @Test
    public void testRelativeAndAbsolutePath() throws Exception {
        addTemplate("lib/inc.ftl", "included");
        
        addTemplate("test1.ftl", "<@.getOptionalTemplate('lib/inc.ftl').include />");
        assertOutputForNamed("test1.ftl", "included");
        
        addTemplate("lib/test2.ftl", "<@.getOptionalTemplate('/lib/inc.ftl').include />");
        assertOutputForNamed("lib/test2.ftl", "included");
        
        addTemplate("lib/test3.ftl", "<@.getOptionalTemplate('inc.ftl').include />");
        assertOutputForNamed("lib/test3.ftl", "included");
        
        addTemplate("sub/test4.ftl", "<@.getOptionalTemplate('../lib/inc.ftl').include />");
        assertOutputForNamed("sub/test4.ftl", "included");
    }

    @Test
    public void testUseCase1() throws Exception {
        addTemplate("lib/inc.ftl", "included");
        assertOutput(""
                + "<#macro test templateName>"
                + "<#local t = .getOptionalTemplate(templateName)>"
                + "<#if t.exists>"
                + "before <@t.include /> after"
                + "<#else>"
                + "missing"
                + "</#if>"
                + "</#macro>"
                + "<@test 'lib/inc.ftl' />; "
                + "<@test 'inc.ftl' />",
                "before included after; missing");
    }

    @Test
    public void testUseCase2() throws Exception {
        addTemplate("found.ftl", "found");
        assertOutput(""
                + "<@("
                + ".getOptionalTemplate('missing1.ftl').include!"
                + ".getOptionalTemplate('missing2.ftl').include!"
                + ".getOptionalTemplate('found.ftl').include!"
                + ".getOptionalTemplate('missing3.ftl').include"
                + ") />",
                "found");
        assertOutput(""
                + "<#macro fallback>fallback</#macro>"
                + "<@("
                + ".getOptionalTemplate('missing1.ftl').include!"
                + ".getOptionalTemplate('missing2.ftl').include!"
                + "fallback"
                + ") />",
                "fallback");
    }
    
    @Test
    public void testWrongArguments() throws Exception {
        assertErrorContains("<#assign t = .getOptionalTemplate()>", ".getOptionalTemplate", "arguments", "none");
        assertErrorContains("<#assign t = .get_optional_template()>", ".get_optional_template", "arguments", "none");
        assertErrorContains("<#assign t = .getOptionalTemplate(1, 2, 3)>", "arguments", "3");
        assertErrorContains("<#assign t = .getOptionalTemplate(1)>", "#1", "string", "number");
        assertErrorContains("<#assign t = .getOptionalTemplate('x', 1)>", "#2", "hash", "number");
        assertErrorContains("<#assign t = .getOptionalTemplate('x', { 'foo': 1 })>",
                "#2", "foo", "encoding", "parse");
        assertErrorContains("<#assign t = .getOptionalTemplate('x', { 'parse': 1 })>",
                "#2", "parse", "number", "boolean");
        assertErrorContains("<#assign t = .getOptionalTemplate('x', { 'encoding': 1 })>",
                "#2", "encoding", "number", "string");
        
        addTemplate("inc.ftl", "Exists...");
        assertErrorContains("<@.getOptionalTemplate('inc.ftl').include x=1 />", "no parameters");
        assertErrorContains("<@.getOptionalTemplate('inc.ftl').include>x</@>", "no nested content");
        assertErrorContains("<@.getOptionalTemplate('inc.ftl').include; x />", "no loop variables");
        assertErrorContains("<#assign x = .getOptionalTemplate('inc.ftl').import(1)>", "no parameters");
    }
    
}
