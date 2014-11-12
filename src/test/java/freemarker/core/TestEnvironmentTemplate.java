package freemarker.core;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.Version;
import freemarker.test.TemplateTest;

public class TestEnvironmentTemplate extends TemplateTest {

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
                );
        TEMPLATES.putTemplate("inc",
                "<@tNames />\n"
                + "<#macro incM>"
                    + "[incM: <@tNames /> {<#nested>}]"
                + "</#macro>"
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
            "<t=main>\n"
            + "---1---\n"
            + "[imp: <t=imp>]\n"
            + "---2---\n"
            + "[impM: <t=main>\n"
                + "{<t=main>}\n"
                + "[inc: <t=inc>\n"
                    + "[incM: <t=inc> {<t=imp>}]\n"
                    + "[incInc: <t=inc>\n"
                        + "[incM: <t=inc> {<t=imp>}]\n"
                    + "]\n"
                + "]\n"
                + "[incM: <t=main> {<t=imp>}]\n"
            + "]\n"
            + "---3---\n"
            + "[inc: <t=inc>\n"
                + "[incM: <t=inc> {<t=main>}]\n"
                + "[incInc: <t=inc>\n"
                    + "[incM: <t=inc> {<t=main>}]\n"
                + "]\n"
            + "]\n"
            + "---4---\n"
            + "[incM: <t=main> {<t=main>}]\n"
            + "---5---\n"
            + "[inc2: <t=inc2>\n"
                + "[impM: <t=inc2>\n"
                    + "{<t=main>}\n"
                    + "[inc: <t=inc>\n"
                        + "[incM: <t=inc> {<t=imp>}]\n"
                    + "]\n"
                    + "[incM: <t=inc2> {<t=imp>}]\n"
                + "]\n"
            + "]\n"
            + "---6---\n"
            + "[impM2: <t=main>\n"
                + "{<t=main>}\n"
                + "[imp2M: <t=main> {<t=imp>}]\n"
            + "]\n"
            + "---7---\n"
            + "[inc3: <t=inc3>\n"
                + "[mainM: <t=inc3> {<t=main>} <t=inc3>]\n"
            + "]\n"
            + "[mainM: <t=main> {<t=main> <t=inc4> <t=main>} <t=main>]\n"
            + "<t=main>\n";            

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
    
    private Configuration createConfiguration(Version version2321) {
        Configuration cfg = new Configuration(version2321);
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
                out.write("<t=" + env.getTemplate().getName() + ">");
            }
            
        });
    }

}
