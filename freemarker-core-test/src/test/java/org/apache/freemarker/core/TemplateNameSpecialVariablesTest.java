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

public class TemplateNameSpecialVariablesTest extends TemplateTest {
    
    private static final String PRINT_ALL_FTL
            = "ct=${.currentTemplateName!'-'}, mt=${.mainTemplateName!'-'}";

    @Test
    public void testMainTemplateName() throws IOException, TemplateException {
        addTemplateNameTestTemplates(".mainTemplateName");
        assertOutputForNamed("main.f3au",
                "In main: main.f3au\n"
                + "In imp: main.f3au\n"
                + "In main: main.f3au\n"
                + "main.f3au\n"
                + "{main.f3au}\n"
                + "In imp call imp:\n"
                + "main.f3au\n"
                + "{main.f3au}\n"
                + "After: main.f3au\n"
                + "In main: main.f3au\n"
                + "In inc: main.f3au\n"
                + "In inc call imp:\n"
                + "main.f3au\n"
                + "{main.f3au}\n"
                + "In main: main.f3au\n"
                + "main.f3au\n"
                + "{main.f3au}\n"
                + "In inc call imp:\n"
                + "main.f3au\n"
                + "{main.f3au}\n"
                + "In main: main.f3au\n");
    }

    @Test
    public void testCurrentTemplateName() throws IOException, TemplateException {
        addTemplateNameTestTemplates(".currentTemplateName");
        assertOutputForNamed("main.f3au",
                "In main: main.f3au\n"
                + "In imp: imp.f3au\n"
                + "In main: main.f3au\n"
                + "imp.f3au\n"
                + "{main.f3au}\n"
                + "In imp call imp:\n"
                + "imp.f3au\n"
                + "{imp.f3au}\n"
                + "After: imp.f3au\n"
                + "In main: main.f3au\n"
                + "In inc: inc.f3au\n"
                + "In inc call imp:\n"
                + "imp.f3au\n"
                + "{inc.f3au}\n"
                + "In main: main.f3au\n"
                + "inc.f3au\n"
                + "{main.f3au}\n"
                + "In inc call imp:\n"
                + "imp.f3au\n"
                + "{inc.f3au}\n"
                + "In main: main.f3au\n");
    }

    private void addTemplateNameTestTemplates(String specVar) {
        addTemplate("main.f3au",
                "In main: ${" + specVar + "}\n"
                        + "<#import 'imp.f3au' as i>"
                        + "In imp: ${inImp}\n"
                        + "In main: ${" + specVar + "}\n"
                        + "<@i.impM>${" + specVar + "}</@>\n"
                        + "<@i.impM2 />\n"
                        + "In main: ${" + specVar + "}\n"
                        + "<#include 'inc.f3au'>"
                        + "In main: ${" + specVar + "}\n"
                        + "<@incM>${" + specVar + "}</@>\n"
                        + "<@incM2 />\n"
                        + "In main: ${" + specVar + "}\n"
        );
        addTemplate("imp.f3au",
                "<#global inImp = " + specVar + ">"
                        + "<#macro impM>"
                        + "${" + specVar + "}\n"
                        + "{<#nested>}"
                        + "</#macro>"
                        + "<#macro impM2>"
                        + "In imp call imp:\n"
                        + "<@impM>${" + specVar + "}</@>\n"
                        + "After: ${" + specVar + "}"
                        + "</#macro>"
        );
        addTemplate("inc.f3au",
                "In inc: ${" + specVar + "}\n"
                        + "In inc call imp:\n"
                        + "<@i.impM>${" + specVar + "}</@>\n"
                        + "<#macro incM>"
                        + "${" + specVar + "}\n"
                        + "{<#nested>}"
                        + "</#macro>"
                        + "<#macro incM2>"
                        + "In inc call imp:\n"
                        + "<@i.impM>${" + specVar + "}</@>"
                        + "</#macro>"
        );
    }

    @Test
    public void testInAdhocTemplate() throws TemplateException, IOException {
        addTemplate("inc.f3au", "Inc: " + PRINT_ALL_FTL);

        // In nameless templates, the deprecated .templateName is "", but the new variables are missing values. 
        assertOutput(new Template(null, PRINT_ALL_FTL + "; <#include 'inc.f3au'>", getConfiguration()),
                "ct=-, mt=-; Inc: ct=inc.f3au, mt=-");
        
        assertOutput(new Template("foo.f3au", PRINT_ALL_FTL + "; <#include 'inc.f3au'>", getConfiguration()),
                "ct=foo.f3au, mt=foo.f3au; Inc: ct=inc.f3au, mt=foo.f3au");
    }

    @Test
    public void testInInterpretTemplate() throws TemplateException, IOException {
        addToDataModel("t", PRINT_ALL_FTL);
        assertOutput(new Template("foo.f3au", PRINT_ALL_FTL + "; <@t?interpret />", getConfiguration()),
                "ct=foo.f3au, mt=foo.f3au; "
                + "ct=foo.f3au->anonymous_interpreted, mt=foo.f3au");
        assertOutput(new Template(null, PRINT_ALL_FTL + "; <@t?interpret />", getConfiguration()),
                "ct=-, mt=-; "
                + "ct=nameless_template->anonymous_interpreted, mt=-");
        assertOutput(new Template("foo.f3au", PRINT_ALL_FTL + "; <@[t,'bar']?interpret />", getConfiguration()),
                "ct=foo.f3au, mt=foo.f3au; "
                + "ct=foo.f3au->bar, mt=foo.f3au");
    }

    @Override
    protected void setupConfigurationBuilder(Configuration.ExtendableBuilder<?> cb) {
        cb.whitespaceStripping(false);
    }

}
