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
package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Before;
import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.Environment.LazilyInitializedNamespace;
import freemarker.core.Environment.Namespace;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.WrappingTemplateModel;
import freemarker.test.TemplateTest;

@SuppressWarnings("boxing")
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
                + "<#macro m>In lib2</#macro>");
        addTemplate("lib3.ftl", "<#global history = (history!) + 'L3'>"
                + "<#macro m>In lib3</#macro>");
        
        addTemplate("lib2CallsLib1.ftl", "<#global history = (history!) + 'L2'>"
                + "<#macro m>In lib2 (<@lib1.m/>)</#macro>");
        addTemplate("lib3ImportsLib1.ftl", "<#import 'lib1.ftl' as lib1><#global history = (history!) + 'L3'>"
                + "<#macro m>In lib3 (<@lib1.m/>)</#macro>");
        
        addTemplate("lib_de.ftl", "<#global history = (history!) + 'LDe'><#assign initLocale=.locale>"
                + "<#macro m>de</#macro>");
        addTemplate("lib_en.ftl", "<#global history = (history!) + 'LEn'><#assign initLocale=.locale>"
                + "<#macro m>en</#macro>");
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
        String ftl = "<#import 'lib3ImportsLib1.ftl' as lib3>${lib1Cnt} ${.main.lib1???c} ${.globals.lib1???c}, "
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
        getConfiguration().addAutoImport("lib2", "lib2CallsLib1.ftl");
        assertOutput(
                "<#include 'inc3.ftl'>[main] ${inc1Cnt}, ${history}, <@lib1.m/>, <@lib2.m/>",
                "[inc1][inc2][inc3][main] 1, L1L2I, In lib1, In lib2 (In lib1)");
    }
    
    /**
     * Demonstrates design issue in FreeMarker 2.3.x where the lookupStrategy is not factored in when identifying
     * already existing namespaces.
     */
    @Test
    public void lookupSrategiesAreNotConsideredProperly() throws IOException, TemplateException {
        // As only the name of the template is used for the finding the already existing namespace, the settings that
        // influence the lookup are erroneously ignored.
        assertOutput(
                "<#setting locale='en_US'><#import 'lib.ftl' as ns1>"
                + "<#setting locale='de_DE'><#import 'lib.ftl' as ns2>"
                + "<@ns1.m/> <@ns2.m/> ${history}",
                "en en LEn");
        
        // The opposite of the prevous, where differn names refer to the same template after a lookup: 
        assertOutput(
                "<#setting locale='en_US'>"
                + "<#import '*/lib.ftl' as ns1>"
                + "<#import 'lib.ftl' as ns2>"
                + "<@ns1.m/> <@ns2.m/> ${history}",
                "en en LEnLEn");
    }
    
    @Test
    public void lazyImportBasics() throws IOException, TemplateException {
        String ftlImports = "<#import 'lib1.ftl' as l1><#import 'lib2.ftl' as l2><#import 'lib3ImportsLib1.ftl' as l3>";
        String ftlCalls = "<@l2.m/>, <@l1.m/>; ${history}";
        String ftl = ftlImports + ftlCalls;
        
        assertOutput(ftl, "In lib2, In lib1; L1L2L3");
        
        getConfiguration().setLazyImports(true);
        assertOutput(ftl, "In lib2, In lib1; L2L1");
        
        assertOutput(ftlImports + "<@l3.m/>, " + ftlCalls, "In lib3 (In lib1), In lib2, In lib1; L3L1L2");
    }

    @Test
    public void lazyImportAndLocale() throws IOException, TemplateException {
        getConfiguration().setLazyImports(true);
        assertOutput("<#setting locale = 'de_DE'><#import 'lib.ftl' as lib>"
                + "[${history!}] "
                + "<#setting locale = 'en'>"
                + "<@lib.m/> ${lib.initLocale} [${history}]",
                "[] de de_DE [LDe]");
    }

    @Test
    public void lazyAutoImportSettings() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        cfg.addAutoImport("l1", "lib1.ftl");
        cfg.addAutoImport("l2", "lib2.ftl");
        cfg.addAutoImport("l3", "lib3.ftl");
        
        String ftl = "<@l2.m/>, <@l1.m/>; ${history}";
        String expectedEagerOutput = "In lib2, In lib1; L1L2L3";
        String expecedLazyOutput = "In lib2, In lib1; L2L1";
        
        assertOutput(ftl, expectedEagerOutput);
        cfg.setLazyImports(true);
        assertOutput(ftl, expecedLazyOutput);
        cfg.setLazyImports(false);
        assertOutput(ftl, expectedEagerOutput);
        cfg.setLazyAutoImports(true);
        assertOutput(ftl, expecedLazyOutput);
        cfg.setLazyAutoImports(null);
        assertOutput(ftl, expectedEagerOutput);
        cfg.setLazyImports(true);
        cfg.setLazyAutoImports(false);
        assertOutput(ftl, expectedEagerOutput);
    }
    
    @Test
    public void lazyAutoImportMixedWithManualImport() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        cfg.addAutoImport("l1", "lib1.ftl");
        cfg.addAutoImport("l2", "/./lib2.ftl");
        cfg.addAutoImport("l3", "lib3.ftl");
        cfg.setLazyAutoImports(true);

        String ftl = "<@l2.m/>, <@l1.m/>; ${history}";
        String expectOutputWithoutHistory = "In lib2, In lib1; ";
        String expecedOutput = expectOutputWithoutHistory + "L2L1";
        
        assertOutput(ftl, expecedOutput);
        assertOutput("<#import 'lib1.ftl' as l1>" + ftl, expectOutputWithoutHistory + "L1L2");
        assertOutput("<#import './x/../lib1.ftl' as l1>" + ftl, expectOutputWithoutHistory + "L1L2");
        assertOutput("<#import 'lib2.ftl' as l2>" + ftl, expecedOutput);
        assertOutput("<#import 'lib3.ftl' as l3>" + ftl, expectOutputWithoutHistory + "L3L2L1");
        cfg.setLazyImports(true);
        assertOutput("<#import 'lib1.ftl' as l1>" + ftl, expecedOutput);
        assertOutput("<#import './x/../lib1.ftl' as l1>" + ftl, expecedOutput);
        assertOutput("<#import 'lib2.ftl' as l2>" + ftl, expecedOutput);
        assertOutput("<#import 'lib3.ftl' as l3>" + ftl, expecedOutput);
    }

    @Test
    public void lazyImportErrors() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        cfg.setLazyImports(true);
        
        assertOutput("<#import 'noSuchTemplate.ftl' as wrong>x", "x");
        
        cfg.addAutoImport("wrong", "noSuchTemplate.ftl");
        assertOutput("x", "x");

        try {
            assertOutput("${wrong.x}", "");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("Lazy initialization"), containsString("noSuchTemplate.ftl")));
            assertThat(e.getCause(), instanceOf(TemplateNotFoundException.class));
        }
        
        addTemplate("containsError.ftl", "${noSuchVar}");
        try {
            assertOutput("<#import 'containsError.ftl' as lib>${lib.x}", "");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("Lazy initialization"), containsString("containsError.ftl")));
            assertThat(e.getCause(), instanceOf(InvalidReferenceException.class));
            assertThat(e.getCause().getMessage(), containsString("noSuchVar"));
        }
    }
    
    /**
     * Ensures that all methods are overridden so that they will do the lazy initialization.
     */
    @Test
    public void lazilyInitializingNamespaceOverridesAll() throws SecurityException, NoSuchMethodException {
        for (Method m : Namespace.class.getMethods()) {
            Class<?> declClass = m.getDeclaringClass();
            if (declClass == Object.class || declClass == WrappingTemplateModel.class
                    || (m.getModifiers() & Modifier.STATIC) != 0
                    || m.getName().equals("synchronizedWrapper")) {
                continue;
            }
            Method lazyM = LazilyInitializedNamespace.class.getMethod(m.getName(), m.getParameterTypes());
            if (lazyM.getDeclaringClass() != LazilyInitializedNamespace.class) {
                fail("The " + lazyM + " method wasn't overidden in " + LazilyInitializedNamespace.class.getName());
            }
        }
    }
    
}
