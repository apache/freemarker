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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.freemarker.core.outputformat.OutputFormat;
import org.junit.Test;

public class TemplateLanguageTest {
    
    @Test
    public void fileExtensionRestrictionsTest() {
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

}
