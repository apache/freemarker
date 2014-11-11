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
                + "</@>\n");
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
                + "</#macro>");
        // TODO call mInc from inc
        // TODO call mImp from inc
        // TODO i2.macro call from imp macro
    }

    @Test
    public void test2321() throws IOException, TemplateException {
        setConfiguration(cfg2321);
        assertOutputForNamed("main",
                "<t=main>\n"
                + "---1---\n"
                + "[imp: <t=imp>]\n"
                + "---2---\n"
                + "[impM: <t=main>\n"
                    + "{<t=main>}\n"
                    + "[inc: <t=inc>\n"
                        + "[incM: <t=inc> {<t=inc>}]\n"
                        + "[incInc: <t=inc>\n"
                            + "[incM: <t=inc> {<t=inc>}]\n"
                        + "]\n"
                    + "]\n"
                    + "[incM: <t=main> {<t=imp>}]\n"
                + "]\n"
                + "---3---\n"
                + "[inc: <t=inc>\n"
                + "[incM: <t=inc> {<t=inc>}]\n"
                + "[incInc: <t=inc>\n"
                    + "[incM: <t=inc> {<t=inc>}]\n"
                + "]\n"
                + "---4---\n"
                + "[incM: <t=main> {<t=main>}]\n");
    }
    
    private final Configuration cfg2321 = createConfiguration(Configuration.VERSION_2_3_21);
    private final Configuration cfg2322 = createConfiguration(Configuration.VERSION_2_3_22);

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
