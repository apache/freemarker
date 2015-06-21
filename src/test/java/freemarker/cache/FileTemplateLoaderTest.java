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
package freemarker.cache;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import freemarker.template.Configuration;
import freemarker.template.TemplateNotFoundException;

public class FileTemplateLoaderTest {
    
    private File templateRootDir;
    
    Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
    
    @Before
    public void setup() throws IOException {
        templateRootDir = Files.createTempDir();
        File sub1Dir = new File(templateRootDir, "sub1");
        File sub2Dir = new File(sub1Dir, "sub2");
        if (!sub2Dir.mkdirs()) {
            throw new IOException("Failed to create subdirectories");
        }
        File tFile = new File(sub2Dir, "t.ftl");
        FileUtils.write(tFile, "foo");
        
        cfg.setDirectoryForTemplateLoading(templateRootDir);
    }
    
    @Test
    public void testSuccessful() throws Exception {
        for (int i = 0; i < 2; i++) {
            assertEquals("foo", cfg.getTemplate("sub1/sub2/t.ftl").toString());
        }
    }

    @Test
    public void testSuccessful2() throws Exception {
        ((FileTemplateLoader) cfg.getTemplateLoader()).setEmulateCaseSensitiveFileSystem(true);
        for (int i = 0; i < 2; i++) {
            cfg.clearTemplateCache();
            assertEquals("foo", cfg.getTemplate("sub1/sub2/t.ftl").toString());
        }
    }
    
    
    @Test
    public void testNotFound() throws Exception {
        for (int i = 0; i < 2; i++) {
            try {
                cfg.getTemplate("sub1X/sub2/t.ftl");
                fail();
            } catch (TemplateNotFoundException e) {
                assertThat(e.getMessage(), containsString("sub1X"));
                assertNull(e.getCause());
            }
        }
    }

    @Test
    public void testCaseSensitivity() throws Exception {
        for (boolean emuCaseSensFS : new boolean[] { false, true }) {
            for (String nameWithBadCase : new String[] { "SUB1/sub2/t.ftl", "sub1/SUB2/t.ftl", "sub1/sub2/T.FTL" }) {
                ((FileTemplateLoader) cfg.getTemplateLoader()).setEmulateCaseSensitiveFileSystem(emuCaseSensFS);
                cfg.clearTemplateCache();
                
                if (SystemUtils.IS_OS_WINDOWS && !emuCaseSensFS) {
                    assertEquals("foo", cfg.getTemplate(nameWithBadCase).toString());
                } else {
                    assertEquals("foo", cfg.getTemplate(nameWithBadCase.toLowerCase()).toString());
                    try {
                        cfg.getTemplate(nameWithBadCase);
                        fail();
                    } catch (TemplateNotFoundException e) {
                        assertThat(e.getMessage(), containsString(nameWithBadCase));
                        assertNull(e.getCause());
                    }
                }
            }
        }
    }
    
    @Test
    public void testDefault() throws IOException {
        assertFalse(new FileTemplateLoader(templateRootDir).getEmulateCaseSensitiveFileSystem());
    }
    
    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(templateRootDir);
    }

}
