package freemarker.core;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class IncludeAndImportTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setTemplateLoader(new StringTemplateLoader());
        return cfg;
    }

    @Before
    public void setup() {
        addTemplate("inc1.ftl", "[inc1]<#global inc1Cnt = (inc1Cnt!0) + 1><#global history = (history!) + 'I'>");
        addTemplate("inc2.ftl", "[inc2]");
        addTemplate("inc3.ftl", "[inc3]");
        addTemplate("lib1.ftl", "<#global lib1Cnt = (lib1Cnt!0) + 1><#global history = (history!) + 'L1'>"
                + "<#macro m>In lib1</#macro>");
        addTemplate("lib2.ftl", "<#global history = (history!) + 'L2'>"
                + "<#macro m>In lib2 (<@lib1.m/>)</#macro>");
        addTemplate("lib3.ftl", "<#import 'lib1.ftl' as lib1>");
    }

    @Test
    public void includeSameTwice() throws IOException, TemplateException {
        assertOutput("<#include 'inc1.ftl'>${inc1Cnt}<#include 'inc1.ftl'>${inc1Cnt}", "[inc1]1[inc1]2");
    }

    @Test
    public void importSameTwice() throws IOException, TemplateException {
        assertOutput("<#import 'lib1.ftl' as i1>${lib1Cnt} <#import 'lib1.ftl' as i2>${lib1Cnt}", "1 1");
    }

    @Test
    public void importInMainCreatesGlobal() throws IOException, TemplateException {
        String ftl = "${.main.lib1???c} ${.globals.lib1???c}"
                + "<#import 'lib1.ftl' as lib1> ${.main.lib1???c} ${.globals.lib1???c}";
        String expectedOut = "false false true true";
        assertOutput(ftl, expectedOut);
        // No difference:
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        assertOutput(ftl, expectedOut);
    }
    
    @Test
    public void importInMainCreatesGlobalBugfix() throws IOException, TemplateException {
        // An import in the main namespace should create a global variable, but there's a bug where that doesn't happen
        // if the imported library was already initialized elsewhere.
        String ftl = "<#import 'lib3.ftl' as lib3>${lib1Cnt} ${.main.lib1???c} ${.globals.lib1???c}, "
        + "<#import 'lib1.ftl' as lib1>${lib1Cnt} ${.main.lib1???c} ${.globals.lib1???c}";
        assertOutput(ftl, "1 false false, 1 true false");
        // Activate bugfix:
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        assertOutput(ftl, "1 false false, 1 true true");
    }

    /**
     * Tests the order of auto-includes and auto-imports, also that they only effect the main template directly.
     */
    @Test
    public void autoIncludeAndAutoImport() throws IOException, TemplateException {
        getConfiguration().addAutoInclude("inc1.ftl");
        getConfiguration().addAutoInclude("inc2.ftl");
        getConfiguration().addAutoImport("lib1", "lib1.ftl");
        getConfiguration().addAutoImport("lib2", "lib2.ftl");
        assertOutput(
                "<#include 'inc3.ftl'>[main] ${inc1Cnt}, ${history}, <@lib1.m/>, <@lib2.m/>",
                "[inc1][inc2][inc3][main] 1, L1L2I, In lib1, In lib2 (In lib1)");
    }
    
}
