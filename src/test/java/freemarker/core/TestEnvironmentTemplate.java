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
                "<@tNames /> "
                + "[imp: <#import 'imp' as i>${i.ini}] "
                + "<@i.m><@tNames /></@> "
                + "[inc: <#include 'inc'>] "
                + "<@incM><@tNames /></@>");
        TEMPLATES.putTemplate("inc", "<@tNames />"
                + "<#if !included!false> [incInc: <#assign included=true><#include 'inc'>]</#if>"
                + "<#macro incM>[incM: <@tNames /> {<#nested>}]</#macro>");
        TEMPLATES.putTemplate("imp", "<#assign ini><@tNames /></#assign> "
                + "<#macro m>[m: <@tNames /> {<#nested>} [inc: <#include 'inc'>] <@incM><@tNames /></@>]</#macro>");
        // TODO call mInc from inc
        // TODO call mImp from inc
        // TODO i2.macro call from imp macro
    }
    
    
    private final Configuration cfg2321 = createConfiguration(Configuration.VERSION_2_3_21);
    private final Configuration cfg2322 = createConfiguration(Configuration.VERSION_2_3_22);

    private Configuration createConfiguration(Version version2321) {
        Configuration cfg = new Configuration(version2321);
        cfg.setTemplateLoader(TEMPLATES);
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
    
    @Test
    public void test2321() throws IOException, TemplateException {
        setConfiguration(cfg2321);
        assertOutputForNamed("main",
                "<t=main> "
                + "[imp: <t=imp>] "
                + "[m: <t=main> {<t=main>} [inc: <t=inc> [incInc: <t=inc>]] [incM: <t=main> {<t=imp>}]] "
                + "[inc: <t=inc> [incInc: <t=inc>]] "
                + "[incM: <t=main> {<t=main>}]");
    }

}
