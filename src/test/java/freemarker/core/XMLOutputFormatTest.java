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
package freemarker.core;

import static freemarker.core.XMLOutputFormat.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import freemarker.template.TemplateModelException; 

public class XMLOutputFormatTest {
    
    @Test
    public void testOutputTOM() throws TemplateModelException, IOException {
       StringWriter out = new StringWriter();
       INSTANCE.output(INSTANCE.escapePlainText("a'b"), out);
       assertEquals("a&apos;b", out.toString());
    }
    
    @Test
    public void testOutputString() throws TemplateModelException, IOException {
        StringWriter out = new StringWriter();
        INSTANCE.output("a'b", out);
        assertEquals("a&apos;b", out.toString());
    }
    
    @Test
    public void testGetMimeType() {
        assertEquals("text/xml", INSTANCE.getMimeType());
    }
    
}
