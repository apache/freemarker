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

import java.io.IOException;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import freemarker.test.TemplateTest;

public class TabSizeTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_22);
        return cfg;
    }

    @Test
    public void testBasics() throws Exception {
        assertErrorColumnNumber(3, "${*}");
        assertErrorColumnNumber(8 + 3, "\t${*}");
        assertErrorColumnNumber(16 + 3, "\t\t${*}");
        assertErrorColumnNumber(16 + 3, "  \t  \t${*}");
        
        getConfiguration().setTabSize(1);
        assertErrorColumnNumber(3, "${*}");
        assertErrorColumnNumber(1 + 3, "\t${*}");
        assertErrorColumnNumber(2 + 3, "\t\t${*}");
        assertErrorColumnNumber(6 + 3, "  \t  \t${*}");
    }
    
    @Test
    public void testEvalBI() throws Exception {
        assertErrorContains("${r'\t~'?eval}", "column 9");
        getConfiguration().setTabSize(4);
        assertErrorContains("${r'\t~'?eval}", "column 5");
    }

    @Test
    public void testInterpretBI() throws Exception {
        assertErrorContains("<@'\\t$\\{*}'?interpret />", "column 11");
        getConfiguration().setTabSize(4);
        assertErrorContains("<@'\\t$\\{*}'?interpret />", "column 7");
    }
    
    @Test
    public void testStringLiteralInterpolation() throws Exception {
        assertErrorColumnNumber(6, "${'${*}'}");
        assertErrorColumnNumber(9, "${'${\t*}'}");
        getConfiguration().setTabSize(16);
        assertErrorColumnNumber(17, "${'${\t*}'}");
    }

    protected void assertErrorColumnNumber(int expectedColumn, String templateSource)
            throws TemplateNotFoundException, MalformedTemplateNameException, IOException {
        addTemplate("t", templateSource);
        try {
            getConfiguration().getTemplate("t");
            fail();
        } catch (ParseException e) {
            assertEquals(expectedColumn, e.getColumnNumber());
        }
        getConfiguration().clearTemplateCache();
        
        try {
            new Template(null, templateSource, getConfiguration());
            fail();
        } catch (ParseException e) {
            assertEquals(expectedColumn, e.getColumnNumber());
        }
    }
    
}
