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

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Test;

import freemarker.cache.ConditionalTemplateConfigurationFactory;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.test.TemplateTest;

public class IncludeAndImportConfigurableLayersTest extends TemplateTest {

    @Test
    public void test3LayerImportNoClashes() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.addAutoImport("t1", "t1.ftl");

        TemplateConfiguration tc = new TemplateConfiguration();
        tc.addAutoImport("t2", "t2.ftl");
        cfg.setTemplateConfigurations(
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("main.ftl"), tc));

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoImport("t3", "t3.ftl");
    
            env.process();
            assertEquals("In main: t1;t2;t3;", sw.toString());
        }

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
    
            env.process();
            assertEquals("In main: t1;t2;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main2.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoImport("t3", "t3.ftl");
    
            env.process();
            assertEquals("In main2: t1;t3;", sw.toString());
        }
        
        cfg.removeAutoImport("t1");
        
        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoImport("t3", "t3.ftl");
    
            env.process();
            assertEquals("In main: t2;t3;", sw.toString());
        }
    }
    
    @Test
    public void test3LayerImportClashes() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.addAutoImport("t1", "t1.ftl");
        cfg.addAutoImport("t2", "t2.ftl");
        cfg.addAutoImport("t3", "t3.ftl");

        TemplateConfiguration tc = new TemplateConfiguration();
        tc.addAutoImport("t2", "t2b.ftl");
        cfg.setTemplateConfigurations(
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("main.ftl"), tc));

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoImport("t3", "t3b.ftl");
    
            env.process();
            assertEquals("In main: t1;t2b;t3b;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main2.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoImport("t3", "t3b.ftl");
    
            env.process();
            assertEquals("In main2: t1;t2;t3b;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
    
            env.process();
            assertEquals("In main: t1;t3;t2b;", sw.toString());
        }
    }

    @Test
    public void test3LayerIncludesNoClashes() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.addAutoInclude("t1.ftl");

        TemplateConfiguration tc = new TemplateConfiguration();
        tc.addAutoInclude("t2.ftl");
        cfg.setTemplateConfigurations(
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("main.ftl"), tc));

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoInclude("t3.ftl");
    
            env.process();
            assertEquals("T1;T2;T3;In main: t1;t2;t3;", sw.toString());
        }

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
    
            env.process();
            assertEquals("T1;T2;In main: t1;t2;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main2.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoInclude("t3.ftl");
    
            env.process();
            assertEquals("T1;T3;In main2: t1;t3;", sw.toString());
        }
        
        cfg.removeAutoInclude("t1.ftl");
        
        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoInclude("t3.ftl");
    
            env.process();
            assertEquals("T2;T3;In main: t2;t3;", sw.toString());
        }
    }

    @Test
    public void test3LayerIncludeClashes() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.addAutoInclude("t1.ftl");
        cfg.addAutoInclude("t2.ftl");
        cfg.addAutoInclude("t3.ftl");

        TemplateConfiguration tc = new TemplateConfiguration();
        tc.addAutoInclude("t2.ftl");
        cfg.setTemplateConfigurations(
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("main.ftl"), tc));

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoInclude("t3.ftl");
    
            env.process();
            assertEquals("T1;T2;T3;In main: t1;t2;t3;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main2.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoInclude("t3.ftl");
    
            env.process();
            assertEquals("T1;T2;T3;In main2: t1;t2;t3;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
    
            env.process();
            assertEquals("T1;T3;T2;In main: t1;t3;t2;", sw.toString());
        }
        
        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoInclude("t1.ftl");
    
            env.process();
            assertEquals("T3;T2;T1;In main: t3;t2;t1;", sw.toString());
        }
    }
    
    @Test
    public void test3LayerIncludesClashes2() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.addAutoInclude("t1.ftl");
        cfg.addAutoInclude("t1.ftl");

        TemplateConfiguration tc = new TemplateConfiguration();
        tc.addAutoInclude("t2.ftl");
        tc.addAutoInclude("t2.ftl");
        cfg.setTemplateConfigurations(
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("main.ftl"), tc));

        {
            Template t = cfg.getTemplate("main.ftl");
            StringWriter sw = new StringWriter();
            Environment env = t.createProcessingEnvironment(null, sw);
            env.addAutoInclude("t3.ftl");
            env.addAutoInclude("t3.ftl");
            env.addAutoInclude("t1.ftl");
            env.addAutoInclude("t1.ftl");
    
            env.process();
            assertEquals("T2;T3;T1;In main: t2;t3;t1;", sw.toString());
        }
    }
    
    @Test
    public void test3LayerLazyness() throws Exception {
        for (Class<?> layer : new Class<?>[] { Configuration.class, Template.class, Environment.class }) {
            test3LayerLazyness(layer, null, null, false, "t1;t2;");
            test3LayerLazyness(layer, null, null, true, "t1;t2;");
            test3LayerLazyness(layer, null, false, true, "t1;t2;");
            test3LayerLazyness(layer, null, true, true, "t2;");
            
            test3LayerLazyness(layer, false, null, false, "t1;t2;");
            test3LayerLazyness(layer, false, null, true, "t1;t2;");
            test3LayerLazyness(layer, false, false, true, "t1;t2;");
            test3LayerLazyness(layer, false, true, true, "t2;");

            test3LayerLazyness(layer, true, null, false, "");
            test3LayerLazyness(layer, true, null, true, "");
            test3LayerLazyness(layer, true, false, true, "t1;");
            test3LayerLazyness(layer, true, true, true, "");
        }
    }
    
    private void test3LayerLazyness(
            Class<?> layer,
            Boolean lazyImports,
            Boolean lazyAutoImports, boolean setLazyAutoImports,
            String expectedOutput)
            throws Exception {
        dropConfiguration();
        Configuration cfg = getConfiguration();
        cfg.addAutoImport("t1", "t1.ftl");
        Template t = new Template(null, "<#import 't2.ftl' as t2>${loaded!}", cfg);

        StringWriter sw = new StringWriter();
        Environment env = t.createProcessingEnvironment(null, sw);
        
        if (layer == Configuration.class) {
            setLazynessOfConfigurable(cfg, lazyImports, lazyAutoImports, setLazyAutoImports);
        } else if (layer == Template.class) {
            setLazynessOfConfigurable(t, lazyImports, lazyAutoImports, setLazyAutoImports);
        } else if (layer == Environment.class) {
            setLazynessOfConfigurable(env, lazyImports, lazyAutoImports, setLazyAutoImports);
        } else {
            throw new IllegalArgumentException();
        }
        
        env.process();
        assertEquals(expectedOutput, sw.toString());
    }

    private void setLazynessOfConfigurable(Configurable cfg, Boolean lazyImports, Boolean lazyAutoImports,
            boolean setLazyAutoImports) {
        if (lazyImports != null) {
            cfg.setLazyImports(lazyImports);
        }
        if (setLazyAutoImports) {
            cfg.setLazyAutoImports(lazyAutoImports);
        }
    }
    
    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_24);
        cfg.setTemplateLoader(new StringTemplateLoader());
        return cfg;
    }

    @Override
    protected void addCommonTemplates() {
        addTemplate("main.ftl", "In main: ${loaded}");
        addTemplate("main2.ftl", "In main2: ${loaded}");
        addTemplate("t1.ftl", "<#global loaded = (loaded!) + 't1;'>T1;");
        addTemplate("t2.ftl", "<#global loaded = (loaded!) + 't2;'>T2;");
        addTemplate("t3.ftl", "<#global loaded = (loaded!) + 't3;'>T3;");
        addTemplate("t1b.ftl", "<#global loaded = (loaded!) + 't1b;'>T1b;");
        addTemplate("t2b.ftl", "<#global loaded = (loaded!) + 't2b;'>T2b;");
        addTemplate("t3b.ftl", "<#global loaded = (loaded!) + 't3b;'>T3b;");
    }

}
