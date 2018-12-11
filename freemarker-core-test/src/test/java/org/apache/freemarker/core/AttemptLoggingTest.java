/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class AttemptLoggingTest extends TemplateTest {

    @Test
    public void standardConfigTest() throws IOException, TemplateException {
        assertOutput("<#attempt>${missingVar1}<#recover>r</#attempt>", "r");
        // Here, we should have an ERROR entry in the log that refers to an exception in an #attempt block. But we can't
        // easily assert that automatically, so it has to be checked manually...

        setConfiguration(new TestConfigurationBuilder()
                .attemptExceptionReporter(AttemptExceptionReporter.LOG_WARN)
                .build());
        assertOutput("<#attempt>${missingVar2}<#recover>r</#attempt>", "r");
        // Again, it must be checked manually if there's a WARN entry
    }

    @Test
    public void customConfigTest() throws IOException, TemplateException {
        List<String> reports = new ArrayList<String>();
        setConfiguration(new TestConfigurationBuilder()
                .attemptExceptionReporter(new TestAttemptExceptionReporter(reports))
                .build());

        assertOutput(
                "<#attempt>${missingVar1}<#recover>r</#attempt>"
                        + "<#attempt>${missingVar2}<#recover>r</#attempt>",
                "rr");
        assertEquals(2, reports.size());
        assertThat(reports.get(0), containsString("missingVar1"));
        assertThat(reports.get(1), containsString("missingVar2"));
    }

    @Test
    public void dontReportSuppressedExceptionsTest() throws IOException, TemplateException {
        List<String> reports = new ArrayList<String>();
        setConfiguration(new TestConfigurationBuilder()
                .attemptExceptionReporter(new TestAttemptExceptionReporter(reports))
                .templateExceptionHandler(new TemplateExceptionHandler() {
                    public void handleTemplateException(TemplateException te, Environment env, Writer out)
                            throws TemplateException {
                        try {
                            out.write("[E]");
                        } catch (IOException e) {
                            throw new TemplateException("Failed to write to the output", e, env);
                        }
                    }
                })
                .build());

        assertOutput("<#attempt>${missingVar1}t<#recover>r</#attempt>", "[E]t");

        assertEquals(0, reports.size());
    }

    private static final class TestAttemptExceptionReporter implements AttemptExceptionReporter {
        private final List<String> reports;

        private TestAttemptExceptionReporter(List<String> reports) {
            this.reports = reports;
        }

        public void report(TemplateException te, Environment env) {
            reports.add(te.getMessage());
        }
    }

}
