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

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateConfiguration;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.PlainTextOutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;
import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileExtensionMatcher;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.templateresolver.FirstMatchTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.MergingTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.OrMatcher;
import org.apache.freemarker.core.templateresolver.PathGlobMatcher;
import org.apache.freemarker.core.util._DateUtil;
import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class TemplateConfigurationExamples extends TemplateTest {

    @Test
    public void example1JavaCfg() throws Exception {
        example1(true);
    }

    @Test
    public void example1PropertiesCfg() throws Exception {
        example1(false);
    }

    private void example1(boolean javaCfg) throws Exception {
        TestConfigurationBuilder cfgB = new TestConfigurationBuilder(this.getClass());
        if (javaCfg) {
            cfgB.setTemplateConfigurations(new ConditionalTemplateConfigurationFactory(
                    new FileExtensionMatcher("xml"),
                    new TemplateConfiguration.Builder()
                            .sourceEncoding(StandardCharsets.UTF_8)
                            .outputFormat(XMLOutputFormat.INSTANCE)
                            .build()));

        } else {
            cfgB.setTemplateConfigurations(null);
            cfgB.setSettings(loadPropertiesFile("TemplateConfigurationExamples1.properties"));
        }
        setConfiguration(cfgB.build());

        addTemplate("t.xml", "");

        Template t = getConfiguration().getTemplate("t.xml");
        assertEquals(StandardCharsets.UTF_8, t.getActualSourceEncoding());
        assertEquals(XMLOutputFormat.INSTANCE, t.getOutputFormat());
    }

    @Test
    public void example2JavaCfg() throws Exception {
        example2(true);
    }

    @Test
    public void example2PropertiesCfg() throws Exception {
        example2(false);
    }

    private void example2(boolean javaCfg) throws Exception {
        TestConfigurationBuilder cfgB = new TestConfigurationBuilder(this.getClass());
        if (javaCfg) {
            cfgB.setTemplateConfigurations(
                    new ConditionalTemplateConfigurationFactory(
                            new PathGlobMatcher("mail/**"),
                            new FirstMatchTemplateConfigurationFactory(
                                    new ConditionalTemplateConfigurationFactory(
                                            new FileNameGlobMatcher("*.subject.*"),
                                            new TemplateConfiguration.Builder()
                                                    .outputFormat(PlainTextOutputFormat.INSTANCE)
                                                    .build()),
                                    new ConditionalTemplateConfigurationFactory(
                                            new FileNameGlobMatcher("*.body.*"),
                                            new TemplateConfiguration.Builder()
                                                    .outputFormat(HTMLOutputFormat.INSTANCE)
                                                    .build())
                            )
                            .noMatchErrorDetails(
                                    "Mail template names must contain \".subject.\" or \".body.\"!")));
        } else{
            cfgB.setSettings(loadPropertiesFile("TemplateConfigurationExamples2.properties"));
        }
        setConfiguration(cfgB.build());

        addTemplate("t.subject.ftl", "");
        addTemplate("mail/t.subject.ftl", "");
        addTemplate("mail/t.body.ftl", "");

        Configuration cfg = getConfiguration();
        assertEquals(UndefinedOutputFormat.INSTANCE, cfg.getTemplate("t.subject.ftl").getOutputFormat());
        assertEquals(PlainTextOutputFormat.INSTANCE, cfg.getTemplate("mail/t.subject.ftl").getOutputFormat());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("mail/t.body.ftl").getOutputFormat());
    }

    @Test
    public void example3JavaCfg() throws Exception {
        example3(true);
    }

    @Test
    public void example3PropertiesCfg() throws Exception {
        example3(false);
    }

    private void example3(boolean javaCfg) throws Exception {
        TestConfigurationBuilder cfgB = new TestConfigurationBuilder(this.getClass())
                .sourceEncoding(StandardCharsets.ISO_8859_1);
        if (javaCfg) {
            cfgB.setTemplateConfigurations(
                    new MergingTemplateConfigurationFactory(
                            new ConditionalTemplateConfigurationFactory(
                                    new FileNameGlobMatcher("*.stats.*"),
                                    new TemplateConfiguration.Builder()
                                            .dateTimeFormat("iso")
                                            .dateFormat("iso")
                                            .timeFormat("iso")
                                            .timeZone(_DateUtil.UTC)
                                            .build()),
                            new ConditionalTemplateConfigurationFactory(
                                    new PathGlobMatcher("mail/**"),
                                    new TemplateConfiguration.Builder()
                                            .sourceEncoding(StandardCharsets.UTF_8)
                                            .build()),
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
                                                    .build())
                            ).allowNoMatch(true)));
        } else {
            cfgB.setSettings(loadPropertiesFile("TemplateConfigurationExamples3.properties"));
        }
        setConfiguration(cfgB.build());

        addTemplate("t.stats.html", "${ts?datetime} ${ts?date} ${ts?time}");
        addTemplate("t.html", "");
        addTemplate("t.htm", "");
        addTemplate("t.xml", "");
        addTemplate("mail/t.html", "");

        addToDataModel("ts", new Date(1440431606011L));

        Configuration cfg = getConfiguration();
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.html").getOutputFormat());
        assertEquals(StandardCharsets.ISO_8859_1, cfg.getTemplate("t.html").getActualSourceEncoding());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.htm").getOutputFormat());
        assertEquals(XMLOutputFormat.INSTANCE, cfg.getTemplate("t.xml").getOutputFormat());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.stats.html").getOutputFormat());
        assertOutputForNamed("t.stats.html", "2015-08-24T15:53:26.011Z 2015-08-24 15:53:26.011Z");
        assertEquals(StandardCharsets.UTF_8, cfg.getTemplate("mail/t.html").getActualSourceEncoding());
    }
    
}
