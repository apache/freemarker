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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

@RunWith(Enclosed.class)
public class NormalizedEolTest {

    /**
     * Tests that the {@code normalized_eol} setting normalizes the line breaks of the static text of the template,
     * whatever line breaks the template file itself uses.
     */
    @RunWith(Parameterized.class)
    public static class StaticTextTest extends TemplateTest {

        @Parameters(name = "templateEol={0}")
        public static Collection<Object[]> parameters() {
            return Arrays.asList(new Object[][] { { "LF", "\n" }, { "CRLF", "\r\n" }, { "CR", "\r" } });
        }

        private final String templateEol;

        public StaticTextTest(String templateEolName, String templateEol) {
            this.templateEol = templateEol;
        }

        /** The same template content, but with the line breaks of the template file we are simulating. */
        private String tpl() {
            return ("line 1\nline 2\n").replace("\n", templateEol);
        }

        @Test
        public void testUnsetKeepsTemplateEol() throws Exception {
            // This is how FreeMarker behaved before 2.3.36, and still does if the setting isn't set.
            assertEquals(tpl(), processToString(tpl()));
        }

        @Test
        public void testNormalizesToLf() throws Exception {
            getConfiguration().setNormalizedEol("\n");
            assertEquals("line 1\nline 2\n", processToString(tpl()));
        }

        @Test
        public void testNormalizesToCrLf() throws Exception {
            getConfiguration().setNormalizedEol("\r\n");
            assertEquals("line 1\r\nline 2\r\n", processToString(tpl()));
        }

        @Test
        public void testNormalizesToCr() throws Exception {
            getConfiguration().setNormalizedEol("\r");
            assertEquals("line 1\rline 2\r", processToString(tpl()));
        }

        private String processToString(String template) throws IOException, TemplateException {
            return getOutput(template);
        }
    }

    /**
     * Tests of the <code>\R</code> escape, and of what the setting does and doesn't affect.
     */
    public static class EscapeAndScopeTest extends TemplateTest {

        @Test
        public void testEscapeDefaultsToLf() throws Exception {
            assertOutput("${'a\\Rb'}", "a\nb");
        }

        @Test
        public void testEscapeFollowsSetting() throws Exception {
            getConfiguration().setNormalizedEol("\r\n");
            assertOutput("${'a\\Rb'}", "a\r\nb");
            getConfiguration().setNormalizedEol("\r");
            assertOutput("${'a\\Rb'}", "a\rb");
        }

        @Test
        public void testEscapeInInterpolatedLiteral() throws Exception {
            getConfiguration().setNormalizedEol("\r\n");
            addToDataModel("x", "X");
            assertOutput("${'a\\R${x}\\Rb'}", "a\r\nX\r\nb");
        }

        @Test
        public void testLiteralBackslashNIsNotAffected() throws Exception {
            // \n always gives a line feed; that's the whole difference between it and \R.
            getConfiguration().setNormalizedEol("\r\n");
            assertOutput("${'a\\nb'}", "a\nb");
        }

        @Test
        public void testLiteralBackslashRIsNotAffected() throws Exception {
            // \R is case-sensitively distinct from \r, which still means carriage return only.
            getConfiguration().setNormalizedEol("\n");
            assertOutput("${'a\\rb'}", "a\rb");
        }

        @Test
        public void testInterpolatedValuesAreNotAffected() throws Exception {
            // The setting is about the template, not about the data.
            getConfiguration().setNormalizedEol("\r\n");
            addToDataModel("x", "p\nq");
            assertOutput("${x}", "p\nq");
        }

        @Test
        public void testStringConcatenationOfEscape() throws Exception {
            getConfiguration().setNormalizedEol("\r\n");
            assertOutput("${'a' + '\\R' + 'b'}", "a\r\nb");
        }

        @Test
        public void testUnknownEscapeIsStillRejected() throws Exception {
            // Sanity check that adding "R" to the accepted escapes didn't make all letters pass:
            assertErrorContains("${'a\\qb'}", "Lexical error");
        }
    }

    /**
     * Tests of the setting itself. It's a parser-level setting, so it's applied when the template is parsed: it
     * can't be changed from inside a template, but it can differ per template.
     */
    public static class SettingTest extends TemplateTest {

        @Test
        public void testDefaultIsNull() throws Exception {
            assertNull(getConfiguration().getNormalizedEol());
        }

        @Test
        public void testNotSettableFromInsideATemplate() throws Exception {
            // A parser setting can't be changed while the template runs.
            assertErrorContains("<#setting normalized_eol='\\r\\n'>", "normalized_eol");
        }

        @Test
        public void testAppliedWhenTheTemplateIsParsed() throws Exception {
            Configuration cfg = getConfiguration();
            cfg.setNormalizedEol("\r\n");
            assertEquals("a\r\nb", getOutput("a\nb"));
            assertEquals("a\r\nb", getOutput("${'a\\Rb'}"));
        }

        @Test
        public void testChangingItAfterParsingHasNoEffect() throws Exception {
            // The template was already parsed with the old value, so it keeps it.
            Configuration cfg = getConfiguration();
            cfg.setNormalizedEol("\r\n");
            Template t = new Template("t", new StringReader("a\nb"), cfg);
            cfg.setNormalizedEol("\n");
            StringWriter sw = new StringWriter();
            t.process(new HashMap<String, Object>(), sw);
            assertEquals("a\r\nb", sw.toString());
        }

        @Test
        public void testCanDifferPerTemplate() throws Exception {
            Configuration cfg = getConfiguration();
            TemplateConfiguration tc = new TemplateConfiguration();
            tc.setParentConfiguration(cfg);
            tc.setNormalizedEol("\r\n");

            StringWriter sw = new StringWriter();
            new Template("crlf", null, new StringReader("a\nb"), cfg, tc, null)
                    .process(new HashMap<String, Object>(), sw);
            assertEquals("a\r\nb", sw.toString());

            // The same source, without that TemplateConfiguration, is unaffected.
            assertEquals("a\nb", getOutput("a\nb"));
        }
    }
}
