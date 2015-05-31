/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.Version;
import freemarker.test.TemplateTest;

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
    
    private final static String EXPECTED_2_3_21 =
            "<t=main ct=main mt=main>\n"
            + "---1---\n"
            + "[imp: <t=imp ct=imp mt=main>]\n"
            + "---2---\n"
            + "[impM: <t=main ct=imp mt=main>\n"
                + "{<t=main ct=main mt=main>}\n"
                + "[inc: <t=inc ct=inc mt=main>\n"
                    + "[incM: <t=inc ct=inc mt=main> {<t=imp ct=inc mt=main>}]\n"
                    + "[incInc: <t=inc ct=inc mt=main>\n"
                        + "[incM: <t=inc ct=inc mt=main> {<t=imp ct=inc mt=main>}]\n"
                    + "]\n"
                + "]\n"
                + "[incM: <t=main ct=inc mt=main> {<t=imp ct=imp mt=main>}]\n"
            + "]\n"
            + "---3---\n"
            + "[inc: <t=inc ct=inc mt=main>\n"
                + "[incM: <t=inc ct=inc mt=main> {<t=main ct=inc mt=main>}]\n"
                + "[incInc: <t=inc ct=inc mt=main>\n"
                    + "[incM: <t=inc ct=inc mt=main> {<t=main ct=inc mt=main>}]\n"
                + "]\n"
            + "]\n"
            + "---4---\n"
            + "[incM: <t=main ct=inc mt=main> {<t=main ct=main mt=main>}]\n"
            + "---5---\n"
            + "[inc2: <t=inc2 ct=inc2 mt=main>\n"
                + "[impM: <t=inc2 ct=imp mt=main>\n"
                    + "{<t=main ct=inc2 mt=main>}\n"
                    + "[inc: <t=inc ct=inc mt=main>\n"
                        + "[incM: <t=inc ct=inc mt=main> {<t=imp ct=inc mt=main>}]\n"
                    + "]\n"
                    + "[incM: <t=inc2 ct=inc mt=main> {<t=imp ct=imp mt=main>}]\n"
                + "]\n"
            + "]\n"
            + "---6---\n"
            + "[impM2: <t=main ct=imp mt=main>\n"
                + "{<t=main ct=main mt=main>}\n"
                + "[imp2M: <t=main ct=imp2 mt=main> {<t=imp ct=imp mt=main>}]\n"
            + "]\n"
            + "---7---\n"
            + "[inc3: <t=inc3 ct=inc3 mt=main>\n"
                + "[mainM: <t=inc3 ct=main mt=main> {<t=main ct=inc3 mt=main>} <t=inc3 ct=main mt=main>]\n"
            + "]\n"
            + "[mainM: "
                + "<t=main ct=main mt=main> "
                + "{<t=main ct=main mt=main> <t=inc4 ct=inc4 mt=main> <t=main ct=main mt=main>} "
                + "<t=main ct=main mt=main>"
            + "]\n"
            + "<t=main ct=main mt=main>\n"
            + "---8---\n"
            + "mainF: <t=main ct=main mt=main>, impF: <t=main ct=imp mt=main>, incF: <t=main ct=inc mt=main>\n"
            ;

    @Test
    public void test2321() throws IOException, TemplateException {
        setConfiguration(createConfiguration(Configuration.VERSION_2_3_21));
        assertOutputForNamed("main", EXPECTED_2_3_21);
    }

    @Test
    public void test2322() throws IOException, TemplateException {
        setConfiguration(createConfiguration(Configuration.VERSION_2_3_22));
        assertOutputForNamed("main", EXPECTED_2_3_21.replaceAll("<t=\\w+", "<t=main"));
    }

    @Test
    public void testNotStarted() throws IOException, TemplateException {
        Template t = new Template("foo", "", createConfiguration(Configuration.VERSION_2_3_21));
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
