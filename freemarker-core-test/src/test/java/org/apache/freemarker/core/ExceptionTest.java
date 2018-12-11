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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Locale;

import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.core.util._NullWriter;
import org.apache.freemarker.test.TestConfigurationBuilder;

import junit.framework.TestCase;
public class ExceptionTest extends TestCase {
    
    public ExceptionTest(String name) {
        super(name);
    }

    public void testParseExceptionSerializable() throws IOException, ClassNotFoundException {
        try {
            new Template(null, new StringReader("<@>"), new TestConfigurationBuilder().build());
            fail();
        } catch (ParseException e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(e);
            new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
        }
    }

    public void testTemplateErrorSerializable() throws IOException, ClassNotFoundException {
        Template tmp = new Template(null, new StringReader("${noSuchVar}"),
                new TestConfigurationBuilder().build());
        try {
            tmp.process(Collections.EMPTY_MAP, new StringWriter());
            fail();
        } catch (TemplateException e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(e);
            new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
        }
    }
    
    @SuppressWarnings("boxing")
    public void testTemplateExceptionLocationInformation() throws IOException {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("foo_en.f3ah", "\n\nxxx${noSuchVariable}");

        Template t = new TestConfigurationBuilder().templateLoader(tl).build()
                .getTemplate("foo.f3ah", Locale.US);
        try {
            t.process(null, _NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertEquals("foo.f3ah", t.getLookupName());
            assertEquals("foo.f3ah", e.getTemplateLookupName());
            assertEquals("foo_en.f3ah", e.getTemplateSourceName());
            assertEquals(3, (int) e.getLineNumber());
            assertEquals(6, (int) e.getColumnNumber());
            assertEquals(3, (int) e.getEndLineNumber());
            assertEquals(19, (int) e.getEndColumnNumber());
            assertThat(e.getMessage(), containsString("foo_en.f3ah"));
            assertThat(e.getMessage(), containsString("noSuchVariable"));
        }
    }

    @SuppressWarnings("cast")
    public void testParseExceptionLocationInformation() throws IOException {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("foo_en.f3ah", "\n\nxxx<#noSuchDirective>");

        try {
            new TestConfigurationBuilder().templateLoader(tl).build()
                    .getTemplate("foo.f3ah", Locale.US);
            fail();
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            assertEquals("foo_en.f3ah", e.getTemplateSourceName());
            assertEquals("foo.f3ah", e.getTemplateLookupName());
            assertEquals(3, e.getLineNumber());
            assertEquals(5, e.getColumnNumber());
            assertEquals(3, e.getEndLineNumber());
            assertEquals(20, e.getEndColumnNumber());
            assertThat(e.getMessage(), containsString("foo_en.f3ah"));
            assertThat(e.getMessage(), containsString("noSuchDirective"));
        }
    }
    
}
