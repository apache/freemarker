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

import java.io.IOException;
import java.io.StringWriter;

import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;
import freemarker.test.utility.FileTestCase;

public class CanonicalFormTest extends FileTestCase {

    public CanonicalFormTest(String name) {
        super(name);
    }

    public void testMacrosCanonicalForm() throws Exception {
        assertCanonicalFormOf("cano-macros.ftl");
    }
    
    public void testIdentifierEscapingCanonicalForm() throws Exception {
        assertCanonicalFormOf("cano-identifier-escaping.ftl");
    }

    public void testAssignmentCanonicalForm() throws Exception {
        assertCanonicalFormOf("cano-assignments.ftl");
    }

    public void testBuiltInCanonicalForm() throws Exception {
        assertCanonicalFormOf("cano-builtins.ftl");
    }

    public void testStringLiteralInterpolationCanonicalForm() throws Exception {
        assertCanonicalFormOf("cano-strlitinterpolation.ftl");
    }
    
    private void assertCanonicalFormOf(String ftlFileName)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setClassForTemplateLoading(CanonicalFormTest.class, "");
        StringWriter sw = new StringWriter();
        cfg.getTemplate(ftlFileName).dump(sw);

        int lastDotIdx = ftlFileName.lastIndexOf('.');
        String canonicalFtlName = ftlFileName.substring(0, lastDotIdx) + "-canonical"
                + ftlFileName.substring(lastDotIdx);
        assertExpectedFileEqualsString(canonicalFtlName, sw.toString());
    }

}
