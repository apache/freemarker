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

package freemarker.template;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.Environment;
import freemarker.core.Environment.Namespace;
import freemarker.core.InvalidReferenceException;
import freemarker.core.LibraryLoad;
import freemarker.template.utility.NullWriter;

/**
 * These are things that users shouldn't do, but we shouldn't break backward compatibility without knowing about it.
 */
public class MistakenlyPublicImportAPIsTest {

    @Test
    public void testImportCopying() throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("imp1", "<#macro m>1</#macro>");
        tl.putTemplate("imp2", "<#assign x = 2><#macro m>${x}</#macro>");
        cfg.setTemplateLoader(tl);
        
        Template t1 = new Template(null, "<#import 'imp1' as i1><#import 'imp2' as i2>", cfg);
        List<LibraryLoad> imports = t1.getImports();
        assertEquals(2, imports.size());
        
        {
            Template t2 = new Template(null, "<@i1.m/><@i2.m/>", cfg);
            for (LibraryLoad libLoad : imports) {
                t2.addImport(libLoad);
            }
            
            try {
                t2.process(null, NullWriter.INSTANCE);
                fail();
            } catch (InvalidReferenceException e) {
                // Apparenly, it has never worked like this...
                assertEquals("i1", e.getBlamedExpressionString());
            }
        }
        
        // It works this way, though it has nothing to do with the problematic API-s: 
        Environment env = t1.createProcessingEnvironment(null, NullWriter.INSTANCE);
        env.process();
        TemplateModel i1 = env.getVariable("i1");
        assertThat(i1, instanceOf(Namespace.class));
        TemplateModel i2 = env.getVariable("i2");
        assertThat(i2, instanceOf(Namespace.class));

        {
            Template t2 = new Template(null, "<@i1.m/>", cfg);
            
            StringWriter sw = new StringWriter();
            env = t2.createProcessingEnvironment(null, sw);
            env.setVariable("i1", i1);
            
            env.process();
            assertEquals("1", sw.toString());
        }

        {
            Template t2 = new Template(null, "<@i2.m/>", cfg);
            
            StringWriter sw = new StringWriter();
            env = t2.createProcessingEnvironment(null, sw);
            env.setVariable("i2", i2);
            
            try {
                env.process();
                assertEquals("2", sw.toString());
            } catch (NullPointerException e) {
                // Expected on 2.3.x, because it won't find the namespace for the macro
                // [2.4] Fix this "bug"
            }
        }
    }
    
}
