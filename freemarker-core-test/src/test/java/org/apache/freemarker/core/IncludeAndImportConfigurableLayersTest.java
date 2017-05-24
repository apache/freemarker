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

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class IncludeAndImportConfigurableLayersTest {

    @Test
    public void test3LayerImportNoClashes() throws Exception {
        TestConfigurationBuilder cfgB = createConfigurationBuilder()
                .autoImports(ImmutableMap.of("t1", "t1.ftl"))
                .templateConfigurations(
                        new ConditionalTemplateConfigurationFactory(
                                new FileNameGlobMatcher("main.ftl"),
                                new TemplateConfiguration.Builder()
                                        .autoImports(ImmutableMap.of("t2", "t2.ftl"))
                                        .build()));
        Configuration cfg = cfgB.build();

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .autoImports(ImmutableMap.of("t3", "t3.ftl"))
                    .process();
            assertEquals("In main: t1;t2;t3;", sw.toString());
        }

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .process();
            assertEquals("In main: t1;t2;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main2.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .autoImports(ImmutableMap.of("t3", "t3.ftl"))
                    .process();
            assertEquals("In main2: t1;t3;", sw.toString());
        }
    }
    
    @Test
    public void test3LayerImportClashes() throws Exception {
        Configuration cfg = createConfigurationBuilder()
                .autoImports(ImmutableMap.of(
                        "t1", "t1.ftl",
                        "t2", "t2.ftl",
                        "t3", "t3.ftl"))
                .templateConfigurations(
                        new ConditionalTemplateConfigurationFactory(
                                new FileNameGlobMatcher("main.ftl"),
                                new TemplateConfiguration.Builder()
                                        .autoImports(ImmutableMap.of("t2", "t2b.ftl"))
                                        .build()))
                .build();

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .autoImports(ImmutableMap.of("t3", "t3b.ftl"))
                    .process();
            assertEquals("In main: t1;t2b;t3b;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main2.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .autoImports(ImmutableMap.of("t3", "t3b.ftl"))
                    .process();
            assertEquals("In main2: t1;t2;t3b;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .process();
            assertEquals("In main: t1;t3;t2b;", sw.toString());
        }
    }

    @Test
    public void test3LayerIncludesNoClashes() throws Exception {
        Configuration cfg = createConfigurationBuilder()
                .autoIncludes("t1.ftl")
                .templateConfigurations(
                        new ConditionalTemplateConfigurationFactory(
                                new FileNameGlobMatcher("main.ftl"),
                                new TemplateConfiguration.Builder()
                                        .autoIncludes("t2.ftl")
                                        .build()))
                .build();

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .autoIncludes("t3.ftl")
                    .process();
            assertEquals("T1;T2;T3;In main: t1;t2;t3;", sw.toString());
        }

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .process();
            assertEquals("T1;T2;In main: t1;t2;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main2.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .autoIncludes("t3.ftl")
                    .process();
            assertEquals("T1;T3;In main2: t1;t3;", sw.toString());
        }
    }

    @Test
    public void test3LayerIncludeClashes() throws Exception {
        Configuration cfg = createConfigurationBuilder()
                .autoIncludes("t1.ftl", "t2.ftl", "t3.ftl")
                .templateConfigurations(new ConditionalTemplateConfigurationFactory(
                        new FileNameGlobMatcher("main.ftl"),
                        new TemplateConfiguration.Builder()
                                .autoIncludes("t2.ftl")
                                .build()))
                .build();

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .autoIncludes("t3.ftl")
                    .process();
            assertEquals("T1;T2;T3;In main: t1;t2;t3;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main2.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .autoIncludes("t3.ftl")
                    .process();
            assertEquals("T1;T2;T3;In main2: t1;t2;t3;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .process();
            assertEquals("T1;T3;T2;In main: t1;t3;t2;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .autoIncludes("t1.ftl")
                    .process();
            assertEquals("T3;T2;T1;In main: t3;t2;t1;", sw.toString());
        }
    }
    
    @Test
    public void test3LayerIncludesClashes2() throws Exception {
        Configuration cfg = createConfigurationBuilder()
                .autoIncludes("t1.ftl", "t1.ftl")
                .templateConfigurations(
                        new ConditionalTemplateConfigurationFactory(
                                new FileNameGlobMatcher("main.ftl"),
                                new TemplateConfiguration.Builder()
                                        .autoIncludes("t2.ftl", "t2.ftl")
                                        .build()))
                .build();

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            t.createProcessingEnvironment(null, sw)
                    .autoIncludes("t3.ftl", "t3.ftl", "t1.ftl", "t1.ftl")
                    .process();
            assertEquals("T2;T3;T1;In main: t2;t3;t1;", sw.toString());
        }
    }
    
    @Test
    public void test3LayerLaziness() throws Exception {
        for (Class<?> layer : new Class<?>[] { Configuration.class, Template.class, Environment.class }) {
            test3LayerLaziness(layer, null, null, false, "t1;t2;");
            test3LayerLaziness(layer, null, null, true, "t1;t2;");
            test3LayerLaziness(layer, null, false, true, "t1;t2;");
            test3LayerLaziness(layer, null, true, true, "t2;");
            
            test3LayerLaziness(layer, false, null, false, "t1;t2;");
            test3LayerLaziness(layer, false, null, true, "t1;t2;");
            test3LayerLaziness(layer, false, false, true, "t1;t2;");
            test3LayerLaziness(layer, false, true, true, "t2;");

            test3LayerLaziness(layer, true, null, false, "");
            test3LayerLaziness(layer, true, null, true, "");
            test3LayerLaziness(layer, true, false, true, "t1;");
            test3LayerLaziness(layer, true, true, true, "");
        }
    }
    
    private void test3LayerLaziness(
            Class<?> layer,
            Boolean lazyImports,
            Boolean lazyAutoImports, boolean setLazyAutoImports,
            String expectedOutput)
            throws Exception {
        Configuration cfg;
        {
            TestConfigurationBuilder cfgB = createConfigurationBuilder()
                    .autoImports(ImmutableMap.of("t1", "t1.ftl"));
            if (layer == Configuration.class) {
                setLazinessOfConfigurable(cfgB, lazyImports, lazyAutoImports, setLazyAutoImports);
            }
            cfg = cfgB.build();
        }

        TemplateConfiguration tc;
        if (layer == Template.class) {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            setLazinessOfConfigurable(tcb, lazyImports, lazyAutoImports, setLazyAutoImports);
            tc = tcb.build();
        } else {
            tc = null;
        }

        Template t = new Template(null, "<#import 't2.ftl' as t2>${loaded!}", cfg, tc);
        StringWriter sw = new StringWriter();

        Environment env = t.createProcessingEnvironment(null, sw);
        if (layer == Environment.class) {
            setLazinessOfConfigurable(env, lazyImports, lazyAutoImports, setLazyAutoImports);
        }

        env.process();
        assertEquals(expectedOutput, sw.toString());
    }

    private void setLazinessOfConfigurable(
            MutableProcessingConfiguration<?> cfg,
            Boolean lazyImports, Boolean lazyAutoImports, boolean setLazyAutoImports) {
        if (lazyImports != null) {
            cfg.setLazyImports(lazyImports);
        }
        if (setLazyAutoImports) {
            cfg.setLazyAutoImports(lazyAutoImports);
        }
    }
    
    private TestConfigurationBuilder createConfigurationBuilder() {
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("main.ftl", "In main: ${loaded}");
        loader.putTemplate("main2.ftl", "In main2: ${loaded}");
        loader.putTemplate("t1.ftl", "<#global loaded = (loaded!) + 't1;'>T1;");
        loader.putTemplate("t2.ftl", "<#global loaded = (loaded!) + 't2;'>T2;");
        loader.putTemplate("t3.ftl", "<#global loaded = (loaded!) + 't3;'>T3;");
        loader.putTemplate("t1b.ftl", "<#global loaded = (loaded!) + 't1b;'>T1b;");
        loader.putTemplate("t2b.ftl", "<#global loaded = (loaded!) + 't2b;'>T2b;");
        loader.putTemplate("t3b.ftl", "<#global loaded = (loaded!) + 't3b;'>T3b;");

        return new TestConfigurationBuilder().templateLoader(loader);
    }

}
