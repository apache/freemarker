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
package org.apache.freemarker.manual.examples;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.freemarker.core.TemplateConfiguration;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.RTFOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;
import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileExtensionMatcher;
import org.apache.freemarker.core.templateresolver.FirstMatchTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.OrMatcher;
import org.apache.freemarker.core.templateresolver.PathGlobMatcher;
import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class ConfigureOutputFormatExamples extends TemplateTest {
    
    @Test
    public void test() throws Exception {
        addTemplate("mail/t.ftl", "");
        addTemplate("t.html", "");
        addTemplate("t.htm", "");
        addTemplate("t.xml", "");
        addTemplate("t.rtf", "");

        example2(true);
        example2(false);
        example3(true);
        example3(false);
    }

    private void example2(boolean javaCfg) throws IOException {
        setConfiguration(
                javaCfg
                        ? new TestConfigurationBuilder()
                                .templateConfigurations(
                                        new ConditionalTemplateConfigurationFactory(
                                                new PathGlobMatcher("mail/**"),
                                                new TemplateConfiguration.Builder()
                                                        .outputFormat(HTMLOutputFormat.INSTANCE)
                                                        .build()))
                                .build()
                        : new TestConfigurationBuilder()
                                .settings(loadPropertiesFile("ConfigureOutputFormatExamples1.properties"))
                                .build());
        assertEquals(HTMLOutputFormat.INSTANCE, getConfiguration().getTemplate("mail/t.ftl").getOutputFormat());
    }

    private void example3(boolean javaCfg) throws IOException {
        setConfiguration(
                javaCfg
                        ? new TestConfigurationBuilder()
                                .templateConfigurations(
                                        new FirstMatchTemplateConfigurationFactory(
                                                new ConditionalTemplateConfigurationFactory(
                                                        new FileExtensionMatcher("xml"),
                                                        new TemplateConfiguration.Builder()
                                                                .outputFormat(XMLOutputFormat.INSTANCE)
                                                                .build()),
                                                new ConditionalTemplateConfigurationFactory(
                                                        new OrMatcher(
                                                                new FileExtensionMatcher("html"),
                                                                new FileExtensionMatcher("htm")),
                                                        new TemplateConfiguration.Builder()
                                                                .outputFormat(HTMLOutputFormat.INSTANCE)
                                                                .build()),
                                                new ConditionalTemplateConfigurationFactory(
                                                        new FileExtensionMatcher("rtf"),
                                                        new TemplateConfiguration.Builder()
                                                                .outputFormat(RTFOutputFormat.INSTANCE)
                                                                .build()))
                                        .allowNoMatch(true))
                                .build()
                        : new TestConfigurationBuilder()
                                .settings(loadPropertiesFile("ConfigureOutputFormatExamples2.properties"))
                                .build());
        assertEquals(HTMLOutputFormat.INSTANCE, getConfiguration().getTemplate("t.html").getOutputFormat());
        assertEquals(HTMLOutputFormat.INSTANCE, getConfiguration().getTemplate("t.htm").getOutputFormat());
        assertEquals(XMLOutputFormat.INSTANCE, getConfiguration().getTemplate("t.xml").getOutputFormat());
        assertEquals(RTFOutputFormat.INSTANCE, getConfiguration().getTemplate("t.rtf").getOutputFormat());
    }

}
