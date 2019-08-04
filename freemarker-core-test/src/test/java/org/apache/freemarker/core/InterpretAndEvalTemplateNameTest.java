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
                    "main.f3au",
                    getTemplateNames + " "
                    + "{<#include 'sub/t.f3au'>}");
            tl.putTemplate(
                    "sub/t.f3au",
                    getTemplateNames + " "
                    + "i{<@r'" + getTemplateNames + " {<#include \"a.f3au\">'?interpret />}} "
                    + "i{<@[r'" + getTemplateNames + " {<#include \"a.f3au\">','named_interpreted']?interpret />}}");
            tl.putTemplate("sub/a.f3au", "In sub/a.f3au, " + getTemplateNames);
            tl.putTemplate("a.f3au", "In a.f3au");

            setConfiguration(newConfigurationBuilder().templateLoader(tl));
            
            assertOutputForNamed("main.f3au",
                    "c=main.f3au, m=main.f3au "
                    + "{"
                    + "c=sub/t.f3au, m=main.f3au "
                    + "i{c=sub/t.f3au->anonymous_interpreted, m=main.f3au {In sub/a.f3au, c=sub/a.f3au, m=main.f3au}} "
                    + "i{c=sub/t.f3au->named_interpreted, m=main.f3au {In sub/a.f3au, c=sub/a.f3au, m=main.f3au}}"
                    + "}");
            
            assertOutputForNamed("sub/t.f3au",
                    "c=sub/t.f3au, m=sub/t.f3au "
                    + "i{c=sub/t.f3au->anonymous_interpreted, m=sub/t.f3au {In sub/a.f3au, c=sub/a.f3au, m=sub/t.f3au}} "
                    + "i{c=sub/t.f3au->named_interpreted, m=sub/t.f3au {In sub/a.f3au, c=sub/a.f3au, m=sub/t.f3au}}");
        }
    }
    
}
