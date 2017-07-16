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

import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

/**
 * Test template names returned by special variables and relative path resolution in {@code ?interpret}-ed and
 * {@code ?eval}-ed parts.  
 */
public class InterpretAndEvalTemplateNameTest extends TemplateTest {
    
    @Test
    public void testInterpret() throws IOException, TemplateException {
        for (String getTemplateNames : new String[] {
                "c=${.currentTemplateName}, m=${.mainTemplateName}",
                "c=${\".currentTemplateName\"?eval}, m=${\".mainTemplateName\"?eval}"
                }) {
            StringTemplateLoader tl = new StringTemplateLoader();
            tl.putTemplate(
                    "main.ftl",
                    getTemplateNames + " "
                    + "{<#include 'sub/t.ftl'>}");
            tl.putTemplate(
                    "sub/t.ftl",
                    getTemplateNames + " "
                    + "i{<@r'" + getTemplateNames + " {<#include \"a.ftl\">'?interpret />}} "
                    + "i{<@[r'" + getTemplateNames + " {<#include \"a.ftl\">','named_interpreted']?interpret />}}");
            tl.putTemplate("sub/a.ftl", "In sub/a.ftl, " + getTemplateNames);
            tl.putTemplate("a.ftl", "In a.ftl");

            setConfiguration(new TestConfigurationBuilder().templateLoader(tl).build());
            
            assertOutputForNamed("main.ftl",
                    "c=main.ftl, m=main.ftl "
                    + "{"
                        + "c=sub/t.ftl, m=main.ftl "
                        + "i{c=sub/t.ftl->anonymous_interpreted, m=main.ftl {In sub/a.ftl, c=sub/a.ftl, m=main.ftl}} "
                        + "i{c=sub/t.ftl->named_interpreted, m=main.ftl {In sub/a.ftl, c=sub/a.ftl, m=main.ftl}}"
                    + "}");
            
            assertOutputForNamed("sub/t.ftl",
                    "c=sub/t.ftl, m=sub/t.ftl "
                    + "i{c=sub/t.ftl->anonymous_interpreted, m=sub/t.ftl {In sub/a.ftl, c=sub/a.ftl, m=sub/t.ftl}} "
                    + "i{c=sub/t.ftl->named_interpreted, m=sub/t.ftl {In sub/a.ftl, c=sub/a.ftl, m=sub/t.ftl}}");
        }
    }
    
}
