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
import java.io.StringWriter;
import java.util.List;

import org.apache.freemarker.core.Environment.Namespace;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.core.util._NullWriter;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

/**
 * These are things that users shouldn't do, but we shouldn't break backward compatibility without knowing about it.
 * 
 * TODO [FM3] Now we should make this illegal, but I'm not sure how to catch when the user does this.
 */
public class MistakenlyPublicImportAPIsTest {

    
    @Test
    public void testImportCopying() throws IOException, TemplateException {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("imp1", "<#macro m>1</#macro>");
        tl.putTemplate("imp2", "<#assign x = 2><#macro m>${x}</#macro>");

        Configuration cfg = new TestConfigurationBuilder().templateLoader(tl).build();
        
        Template t1 = new Template(null, "<#import 'imp1' as i1><#import 'imp2' as i2>", cfg);
        List<ASTDirImport> imports = t1.getImports();
        assertEquals(2, imports.size());
        
        {
            Template t2 = new Template(null, "<@i1.m/><@i2.m/>", cfg);
            for (ASTDirImport libLoad : imports) {
                t2.addImport(libLoad);
            }
            
            try {
                t2.process(null, _NullWriter.INSTANCE);
                fail();
            } catch (InvalidReferenceException e) {
                // Apparently, it has never worked like this...
                assertEquals("i1", e.getBlamedExpressionString());
            }
        }
        
        // It works this way, though it has nothing to do with the problematic API-s: 
        Environment env = t1.createProcessingEnvironment(null, _NullWriter.INSTANCE);
        env.process();
        TemplateModel i1 = env.getVariable("i1");
        assertThat(i1, instanceOf(Namespace.class));
        TemplateModel i2 = env.getVariable("i2");
        assertThat(i2, instanceOf(Namespace.class));
        Environment originalEnv = env;

        {
            Template t2 = new Template(null, "<@i1.m/>", cfg);
            
            StringWriter sw = new StringWriter();
            env = t2.createProcessingEnvironment(null, sw);
            env.setVariable("i1", i1);
            
            originalEnv.setOut(sw); // The imported macros are still bound to and will use this.
            env.process();
            assertEquals("1", sw.toString());
        }

        {
            Template t2 = new Template(null, "<@i2.m/>", cfg);
            
            StringWriter sw = new StringWriter();
            env.setOut(sw); // In the old Environment instance, to which the imported macros are bound.
            env = t2.createProcessingEnvironment(null, sw);
            env.setVariable("i2", i2);
            
            try {
                originalEnv.setOut(sw); // The imported macros are still bound to and will use this.
                env.process();
                assertEquals("2", sw.toString());
            } catch (NullPointerException e) {
                // Expected on 2.3.x, because it won't find the namespace for the macro
                // [2.4] Fix this "bug"
            }
        }
    }
    
}
