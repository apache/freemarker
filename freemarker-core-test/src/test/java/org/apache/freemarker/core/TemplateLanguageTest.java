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
import java.io.InputStream;
import java.io.Reader;

import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.RTFOutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class TemplateLanguageTest {
    
    @Test
    public void testFileExtensionRestrictions() {
        new DummyTemplateLanguage("xfoo", null, null);
        
        new DummyTemplateLanguage("ffoo", true, null, null);
        try {
            new DummyTemplateLanguage("ffoo", null, null);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        try {
            new DummyTemplateLanguage("xfOo", null, null);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        try {
            new DummyTemplateLanguage("xf.o", null, null);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    public static class DummyTemplateLanguage extends TemplateLanguage {

        public DummyTemplateLanguage(String fileExtension, OutputFormat outputFormat,
                AutoEscapingPolicy autoEscapingPolicy) {
            super(fileExtension, outputFormat, autoEscapingPolicy);
        }

        DummyTemplateLanguage(String fileExtension, boolean allowExtensionStartingWithF, OutputFormat outputFormat,
                AutoEscapingPolicy autoEscapingPolicy) {
            super(fileExtension, allowExtensionStartingWithF, outputFormat, autoEscapingPolicy);
        }

        @Override
        public boolean getCanSpecifyEncodingInContent() {
            return false;
        }

        @Override
        public ASTElement parse(Template template, Reader reader, ParsingConfiguration pCfg,
                OutputFormat contextOutputFormat, AutoEscapingPolicy contextAutoEscapingPolicy,
                InputStream streamToUnmarkWhenEncEstabd) throws IOException, ParseException {
            throw new RuntimeException("Not implemented");
        }
        
    }
    
    @Test
    public void testDefaultFileExtensionsRecognized() throws Exception {
        Configuration cfg = new TestConfigurationBuilder()
                .outputFormat(RTFOutputFormat.INSTANCE)
                .build();
        
        assertLanguageAndOutputFormat(
                new Template("foo.f3ac", "", cfg),
                DefaultTemplateLanguage.F3AC, RTFOutputFormat.INSTANCE);
        assertLanguageAndOutputFormat(
                new Template("foo.notRecognized", "", cfg),
                DefaultTemplateLanguage.F3AC, RTFOutputFormat.INSTANCE);
        assertLanguageAndOutputFormat(
                new Template("foo.f3au", "", cfg),
                DefaultTemplateLanguage.F3AU, UndefinedOutputFormat.INSTANCE);
        assertLanguageAndOutputFormat(
                new Template("foo.f3ah", "", cfg),
                DefaultTemplateLanguage.F3AH, HTMLOutputFormat.INSTANCE);
        assertLanguageAndOutputFormat(
                new Template("foo.f3ax", "", cfg),
                DefaultTemplateLanguage.F3AX, XMLOutputFormat.INSTANCE);
        
        assertLanguageAndOutputFormat(
                new Template("foo.f3sc", "", cfg),
                DefaultTemplateLanguage.F3SC, RTFOutputFormat.INSTANCE);
        assertLanguageAndOutputFormat(
                new Template("foo.f3su", "", cfg),
                DefaultTemplateLanguage.F3SU, UndefinedOutputFormat.INSTANCE);
        assertLanguageAndOutputFormat(
                new Template("foo.f3sh", "", cfg),
                DefaultTemplateLanguage.F3SH, HTMLOutputFormat.INSTANCE);
        assertLanguageAndOutputFormat(
                new Template("foo.f3sx", "", cfg),
                DefaultTemplateLanguage.F3SX, XMLOutputFormat.INSTANCE);
        
        assertLanguageAndOutputFormat(
                new Template("foo.f3uu", "", cfg),
                UnparsedTemplateLanguage.F3UU, UndefinedOutputFormat.INSTANCE);
        
       try {
           new Template("foo.ftl", "", cfg);
           fail();
       } catch (ParseException e) {
           assertThat(e.getMessage(), allOf(containsString("FreeMarker 2"), containsString("*.ftl")));
       }
       try {
           new Template("foo.ftlh", "", cfg);
           fail();
       } catch (ParseException e) {
           assertThat(e.getMessage(), allOf(containsString("FreeMarker 2"), containsString("*.ftlh")));
       }
       try {
           new Template("foo.ftlx", "", cfg);
           fail();
       } catch (ParseException e) {
           assertThat(e.getMessage(), allOf(containsString("FreeMarker 2"), containsString("*.ftlx")));
       }
    }

    private void assertLanguageAndOutputFormat(Template template, TemplateLanguage expectedLanguage,
            OutputFormat expectedOutputFromat) {
        assertEquals(expectedLanguage, template.getParsingConfiguration().getTemplateLanguage());
        assertEquals(expectedOutputFromat, template.getOutputFormat());
    }

}
