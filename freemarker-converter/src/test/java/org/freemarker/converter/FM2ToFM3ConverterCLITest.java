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

package org.freemarker.converter;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.commons.io.FileUtils.*;
import static org.apache.freemarker.converter.FM2ToFM3ConverterCLI.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.freemarker.converter.FM2ToFM3ConverterCLI;
import org.freemarker.converter.test.ConverterTest;
import org.junit.Test;

public class FM2ToFM3ConverterCLITest extends ConverterTest {

    @Override
    protected void createSourceFiles() throws IOException {
        write(new File(srcDir, "1.ftl"), "", UTF_8);
        write(new File(srcDir, "2.ftl"), "", UTF_8);
        write(new File(srcDir, "3.txt"), "", UTF_8);
    }

    @Test
    public void testHelp() {
        assertCLIResult(SUCCESS_EXIT_STATUS, "Options:", null, "-h");
        assertCLIResult(SUCCESS_EXIT_STATUS, "Options:", null, "--help");
        assertCLIResult(COMMAND_LINE_FORMAT_ERROR_EXIT_STATUS, "usage", "Options:");
    }

    @Test
    public void testMissingOptions() {
        assertCLIResult(COMMAND_LINE_FORMAT_ERROR_EXIT_STATUS, "source", null, "-d", dstDir.toString());
        assertCLIResult(COMMAND_LINE_FORMAT_ERROR_EXIT_STATUS, "required", "source", srcDir.toString());
    }

    @Test
    public void testBasic() {
        assertCLIResult(SUCCESS_EXIT_STATUS, "2 file", null,
                srcDir.toString(), "-d", dstDir.toString());
        assertTrue(new File(dstDir, "1.fm3").exists());
        assertTrue(new File(dstDir, "2.fm3").exists());
        assertFalse(new File(dstDir, "3.txt").exists());
    }

    @Test
    public void testExecutionError() {
        assertCLIResult(EXECUTION_ERROR_EXIT_STATUS, "Exception", null,
                new File(srcDir, "doesNotExists").toString(), "-d", dstDir.toString());
    }

    @Test
    public void testIncludeAndExclude() {
        assertCLIResult(SUCCESS_EXIT_STATUS, "2 file", null,
                srcDir.toString(), "-d", dstDir.toString(),
                "--include", ".*", "--exclude", ".*2\\.ftl");
        assertTrue(new File(dstDir, "1.fm3").exists());
        assertFalse(new File(dstDir, "2.fm3").exists());
        assertTrue(new File(dstDir, "3.txt").exists());
    }

    @Test
    public void testExtensionCustomization1() {
        assertCLIResult(SUCCESS_EXIT_STATUS, null, null,
                srcDir.toString(), "-d", dstDir.toString(),
                "--include", ".*", "-Etxt=txt3");
        assertTrue(new File(dstDir, "1.fm3").exists());
        assertTrue(new File(dstDir, "3.txt3").exists());
    }

    @Test
    public void testExtensionCustomization2() {
        assertCLIResult(SUCCESS_EXIT_STATUS, null, null,
                srcDir.toString(), "-d", dstDir.toString(),
                "--include", ".*", "-Eftl=foo");
        assertTrue(new File(dstDir, "1.foo").exists());
        assertTrue(new File(dstDir, "3.txt").exists());
    }

    @Test
    public void testExtensionCustomization3() {
        assertCLIResult(SUCCESS_EXIT_STATUS, null, null,
                srcDir.toString(), "-d", dstDir.toString(),
                "--include", ".*",
                "--no-predef-file-ext-substs",
                "--file-ext-subst", "txt=txt3",
                "--no-predef-file-ext-substs"
        );
        assertTrue(new File(dstDir, "1.ftl").exists());
        assertTrue(new File(dstDir, "3.txt3").exists());
    }

    public void assertCLIResult(int expectedExitStatus, String stdoutContains, String stdoutNotContains, String...
            args) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        int actualExitStatus = FM2ToFM3ConverterCLI.execute(out, args);
        String stdout = sw.toString();
        if (actualExitStatus != expectedExitStatus) {
            assertEquals("Exit status mismatch. The output was:\n" + stdout, actualExitStatus, expectedExitStatus);
        }
        if (stdoutContains != null) {
            assertThat(stdout, containsString(stdoutContains));
        }
        if (stdoutNotContains != null) {
            assertThat(stdout, not(containsString(stdoutNotContains)));
        }
    }

}
