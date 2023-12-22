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

import static freemarker.core.XMLOutputFormat.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import freemarker.template.TemplateModelException; 

public class XMLOutputFormatTest {
    
    @Test
    public void testOutputMO() throws TemplateModelException, IOException {
       StringWriter out = new StringWriter();
       INSTANCE.output(INSTANCE.fromPlainTextByEscaping("a'b"), out);
       assertEquals("a&apos;b", out.toString());
    }
    
    @Test
    public void testOutputString() throws TemplateModelException, IOException {
        StringWriter out = new StringWriter();
        INSTANCE.output("a'b", out);
        assertEquals("a&apos;b", out.toString());
    }
    
    @Test
    public void testEscaplePlainText() {
        assertEquals("", INSTANCE.escapePlainText(""));
        assertEquals("a", INSTANCE.escapePlainText("a"));
        assertEquals("&lt;a&amp;b&apos;c&quot;d&gt;", INSTANCE.escapePlainText("<a&b'c\"d>"));
        assertEquals("&lt;&gt;", INSTANCE.escapePlainText("<>"));
    }
    
    @Test
    public void testGetMimeType() {
        assertEquals("application/xml", INSTANCE.getMimeType());
    }
    
}
