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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.freemarker.core.Environment.LazilyInitializedNamespace;
import org.apache.freemarker.core.Environment.Namespace;
import org.apache.freemarker.core.model.WrappingTemplateModel;
import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@SuppressWarnings("boxing")
public class IncludeAndImportTest extends TemplateTest {

    @Override
    protected void addCommonTemplates() {
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
    }
    
    @Test
    public void importInMainCreatesGlobalBugfix() throws IOException, TemplateException {
        // An import in the main namespace should invoke a global variable, even if the imported library was already
        // initialized elsewhere.
        String ftl = "<#import 'lib3ImportsLib1.ftl' as lib3>${lib1Cnt} ${.main.lib1???c} ${.globals.lib1???c}, "
        + "<#import 'lib1.ftl' as lib1>${lib1Cnt} ${.main.lib1???c} ${.globals.lib1???c}";
        assertOutput(ftl, "1 false false, 1 true true");
    }

    /**
     * Tests the order of auto-includes and auto-imports, also that they only effect the main template directly.
     */
    @Test
    public void autoIncludeAndAutoImport() throws IOException, TemplateException {
        setConfiguration(new TestConfigurationBuilder()
                .autoImports(ImmutableMap.of(
                        "lib1", "lib1.ftl",
                        "lib2", "lib2CallsLib1.ftl"
                ))
                .autoIncludes(ImmutableList.of(
                        "inc1.ftl",
                        "inc2.ftl"))
                .build());
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
        
        setConfiguration(new TestConfigurationBuilder().lazyImports(true).build());
        assertOutput(ftl, "In lib2, In lib1; L2L1");
        
        assertOutput(ftlImports + "<@l3.m/>, " + ftlCalls, "In lib3 (In lib1), In lib2, In lib1; L3L1L2");
    }

    @Test
    public void lazyImportAndLocale() throws IOException, TemplateException {
        setConfiguration(new TestConfigurationBuilder().lazyImports(true).build());
        assertOutput("<#setting locale = 'de_DE'><#import 'lib.ftl' as lib>"
                + "[${history!}] "
                + "<#setting locale = 'en'>"
                + "<@lib.m/> ${lib.initLocale} [${history}]",
                "[] de de_DE [LDe]");
    }

    @Test
    public void lazyAutoImportSettings() throws IOException, TemplateException {
        TestConfigurationBuilder cfgB = new TestConfigurationBuilder()
                .autoImports(ImmutableMap.of(
                        "l1", "lib1.ftl",
                        "l2", "lib2.ftl",
                        "l3", "lib3.ftl"
                ));

        String ftl = "<@l2.m/>, <@l1.m/>; ${history}";
        String expectedEagerOutput = "In lib2, In lib1; L1L2L3";
        String expecedLazyOutput = "In lib2, In lib1; L2L1";

        setConfiguration(cfgB.build());
        assertOutput(ftl, expectedEagerOutput);
        cfgB.setLazyImports(true);
        setConfiguration(cfgB.build());
        assertOutput(ftl, expecedLazyOutput);
        cfgB.setLazyImports(false);
        setConfiguration(cfgB.build());
        assertOutput(ftl, expectedEagerOutput);
        cfgB.setLazyAutoImports(true);
        setConfiguration(cfgB.build());
        assertOutput(ftl, expecedLazyOutput);
        cfgB.setLazyAutoImports(null);
        setConfiguration(cfgB.build());
        assertOutput(ftl, expectedEagerOutput);
        cfgB.setLazyImports(true);
        cfgB.setLazyAutoImports(false);
        setConfiguration(cfgB.build());
        assertOutput(ftl, expectedEagerOutput);
    }
    
    @Test
    public void lazyAutoImportMixedWithManualImport() throws IOException, TemplateException {
        TestConfigurationBuilder cfgB = new TestConfigurationBuilder()
                .autoImports(ImmutableMap.of(
                        "l1", "lib1.ftl",
                        "l2", "/./lib2.ftl",
                        "l3", "lib3.ftl"))
                .lazyAutoImports(true);

        String ftl = "<@l2.m/>, <@l1.m/>; ${history}";
        String expectOutputWithoutHistory = "In lib2, In lib1; ";
        String expecedOutput = expectOutputWithoutHistory + "L2L1";

        setConfiguration(cfgB.build());
        assertOutput(ftl, expecedOutput);
        assertOutput("<#import 'lib1.ftl' as l1>" + ftl, expectOutputWithoutHistory + "L1L2");
        assertOutput("<#import './x/../lib1.ftl' as l1>" + ftl, expectOutputWithoutHistory + "L1L2");
        assertOutput("<#import 'lib2.ftl' as l2>" + ftl, expecedOutput);
        assertOutput("<#import 'lib3.ftl' as l3>" + ftl, expectOutputWithoutHistory + "L3L2L1");

        cfgB.setLazyImports(true);
        setConfiguration(cfgB.build());
        assertOutput("<#import 'lib1.ftl' as l1>" + ftl, expecedOutput);
        assertOutput("<#import './x/../lib1.ftl' as l1>" + ftl, expecedOutput);
        assertOutput("<#import 'lib2.ftl' as l2>" + ftl, expecedOutput);
        assertOutput("<#import 'lib3.ftl' as l3>" + ftl, expecedOutput);
    }

    @Test
    public void lazyImportErrors() throws IOException, TemplateException {
        TestConfigurationBuilder cfgB = new TestConfigurationBuilder();
        cfgB.setLazyImports(true);

        setConfiguration(cfgB.build());
        assertOutput("<#import 'noSuchTemplate.ftl' as wrong>x", "x");
        
        cfgB.addAutoImport("wrong", "noSuchTemplate.ftl");
        setConfiguration(cfgB.build());
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
