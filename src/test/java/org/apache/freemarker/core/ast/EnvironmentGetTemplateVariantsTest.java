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

package org.apache.freemarker.core.ast;

import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.model.TemplateDirectiveBody;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class EnvironmentGetTemplateVariantsTest extends TemplateTest {

    private static final StringTemplateLoader TEMPLATES = new StringTemplateLoader();
    static {
        TEMPLATES.putTemplate("main",
                "<@tNames />\n"
                + "---1---\n"
                + "[imp: <#import 'imp' as i>${i.impIni}]\n"
                + "---2---\n"
                + "<@i.impM>"
                    + "<@tNames />"
                + "</@>\n"
                + "---3---\n"
                + "[inc: <#include 'inc'>]\n"
                + "---4---\n"
                + "<@incM>"
                    + "<@tNames />"
                + "</@>\n"
                + "---5---\n"
                + "[inc2: <#include 'inc2'>]\n"
                + "---6---\n"
                + "<#import 'imp2' as i2>"
                + "<@i.impM2><@tNames /></@>\n"
                + "---7---\n"
                + "<#macro mainM>"
                    + "[mainM: <@tNames /> {<#nested>} <@tNames />]"
                + "</#macro>"
                + "[inc3: <#include 'inc3'>]\n"
                + "<@mainM><@tNames /> <#include 'inc4'> <@tNames /></@>\n"
                + "<@tNames />\n"
                + "---8---\n"
                + "<#function mainF>"
                    + "<@tNames />"
                    + "<#return lastTNamesResult>"
                + "</#function>"
                + "mainF: ${mainF()}, impF: ${i.impF()}, incF: ${incF()}\n"
                );
        TEMPLATES.putTemplate("inc",
                "<@tNames />\n"
                + "<#macro incM>"
                    + "[incM: <@tNames /> {<#nested>}]"
                + "</#macro>"
                + "<#function incF>"
                    + "<@tNames />"
                    + "<#return lastTNamesResult>"
                + "</#function>"
                + "<@incM><@tNames /></@>\n"
                + "<#if !included!false>[incInc: <#assign included=true><#include 'inc'>]\n</#if>"
                );
        TEMPLATES.putTemplate("imp",
                "<#assign impIni><@tNames /></#assign>\n"
                + "<#macro impM>"
                    + "[impM: <@tNames />\n"
                        + "{<#nested>}\n"
                        + "[inc: <#include 'inc'>]\n"
                        + "<@incM><@tNames /></@>\n"
                    + "]"
                + "</#macro>"
                + "<#macro impM2>"
                    + "[impM2: <@tNames />\n"
                    + "{<#nested>}\n"
                    + "<@i2.imp2M><@tNames /></@>\n"
                    + "]"
                + "</#macro>"
                + "<#function impF>"
                    + "<@tNames />"
                    + "<#return lastTNamesResult>"
                + "</#function>"
                );
        TEMPLATES.putTemplate("inc2",
                "<@tNames />\n"
                + "<@i.impM><@tNames /></@>\n"
                );
        TEMPLATES.putTemplate("imp2",
                "<#macro imp2M>"
                    + "[imp2M: <@tNames /> {<#nested>}]"
                + "</#macro>");
        TEMPLATES.putTemplate("inc3",
                "<@tNames />\n"
                + "<@mainM><@tNames /></@>\n"
                );
        TEMPLATES.putTemplate("inc4",
                "<@tNames />"
                );
    }
    
    @Test
    public void test() throws IOException, TemplateException {
        setConfiguration(createConfiguration(Configuration.VERSION_3_0_0));
        assertOutputForNamed(
                "main",
                "<t=main ct=main mt=main>\n"
                + "---1---\n"
                + "[imp: <t=main ct=imp mt=main>]\n"
                + "---2---\n"
                + "[impM: <t=main ct=imp mt=main>\n"
                    + "{<t=main ct=main mt=main>}\n"
                    + "[inc: <t=main ct=inc mt=main>\n"
                        + "[incM: <t=main ct=inc mt=main> {<t=main ct=inc mt=main>}]\n"
                        + "[incInc: <t=main ct=inc mt=main>\n"
                            + "[incM: <t=main ct=inc mt=main> {<t=main ct=inc mt=main>}]\n"
                        + "]\n"
                    + "]\n"
                    + "[incM: <t=main ct=inc mt=main> {<t=main ct=imp mt=main>}]\n"
                + "]\n"
                + "---3---\n"
                + "[inc: <t=main ct=inc mt=main>\n"
                    + "[incM: <t=main ct=inc mt=main> {<t=main ct=inc mt=main>}]\n"
                    + "[incInc: <t=main ct=inc mt=main>\n"
                        + "[incM: <t=main ct=inc mt=main> {<t=main ct=inc mt=main>}]\n"
                    + "]\n"
                + "]\n"
                + "---4---\n"
                + "[incM: <t=main ct=inc mt=main> {<t=main ct=main mt=main>}]\n"
                + "---5---\n"
                + "[inc2: <t=main ct=inc2 mt=main>\n"
                    + "[impM: <t=main ct=imp mt=main>\n"
                        + "{<t=main ct=inc2 mt=main>}\n"
                        + "[inc: <t=main ct=inc mt=main>\n"
                            + "[incM: <t=main ct=inc mt=main> {<t=main ct=inc mt=main>}]\n"
                        + "]\n"
                        + "[incM: <t=main ct=inc mt=main> {<t=main ct=imp mt=main>}]\n"
                    + "]\n"
                + "]\n"
                + "---6---\n"
                + "[impM2: <t=main ct=imp mt=main>\n"
                    + "{<t=main ct=main mt=main>}\n"
                    + "[imp2M: <t=main ct=imp2 mt=main> {<t=main ct=imp mt=main>}]\n"
                + "]\n"
                + "---7---\n"
                + "[inc3: <t=main ct=inc3 mt=main>\n"
                    + "[mainM: <t=main ct=main mt=main> {<t=main ct=inc3 mt=main>} <t=main ct=main mt=main>]\n"
                + "]\n"
                + "[mainM: "
                    + "<t=main ct=main mt=main> "
                    + "{<t=main ct=main mt=main> <t=main ct=inc4 mt=main> <t=main ct=main mt=main>} "
                    + "<t=main ct=main mt=main>"
                + "]\n"
                + "<t=main ct=main mt=main>\n"
                + "---8---\n"
                + "mainF: <t=main ct=main mt=main>, impF: <t=main ct=imp mt=main>, incF: <t=main ct=inc mt=main>\n"
                .replaceAll("<t=\\w+", "<t=main"));
    }

    @Test
    public void testNotStarted() throws IOException, TemplateException {
        Template t = new Template("foo", "", createConfiguration(Configuration.VERSION_3_0_0));
        final Environment env = t.createProcessingEnvironment(null, null);
        assertSame(t, env.getMainTemplate());
        assertSame(t, env.getCurrentTemplate());
    }
    
    private Configuration createConfiguration(Version iciVersion) {
        Configuration cfg = new Configuration(iciVersion);
        cfg.setTemplateLoader(TEMPLATES);
        cfg.setWhitespaceStripping(false);
        return cfg;
    }

    @Override
    protected Object createDataModel() {
        return Collections.singletonMap("tNames", new TemplateDirectiveModel() {

            @Override
            public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                    throws TemplateException, IOException {
                Writer out = env.getOut();
                final String r = "<t=" + env.getTemplate().getName() + " ct=" + env.getCurrentTemplate().getName() + " mt="
                        + env.getMainTemplate().getName() + ">";
                out.write(r);
                env.setGlobalVariable("lastTNamesResult", new SimpleScalar(r));
            }
            
        });
    }

}
