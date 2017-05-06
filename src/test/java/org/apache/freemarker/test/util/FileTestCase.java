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

package org.apache.freemarker.test.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.freemarker.core.util._StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Test case that needs to compare a string to a reference (expected) text file, or two text files. 
 */
public abstract class FileTestCase extends TestCase {

    public FileTestCase(String name) {
        super(name);
    }

    protected void assertExpectedFileEqualsString(String expectedFileName, String actualContent) {
        try {
            final URL expectedFile = getExpectedFileFor(expectedFileName);
            
            try {
                multilineAssertEquals(loadTestTextResource(expectedFile), actualContent);
            } catch (AssertionFailedError e) {
                File actualFile = getActualFileFor(expectedFileName);
                if (actualFile == null) {
                    saveString(actualFile, actualContent);
                    reportActualFileSaved(actualFile);
                }

                throw e;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to do assertion", e);
        }
    }

    private void multilineAssertEquals(String expected, String actual) {
        String normExpected = normalizeNewLines(expected);
        final String normActual = normalizeNewLines(actual);
        
        // Ignore final line-break difference:
        if (normActual.endsWith("\n") && !normExpected.endsWith("\n")) {
            normExpected += "\n";
        } else if (!normActual.endsWith("\n") && normExpected.endsWith("\n")) {
            normExpected = normExpected.substring(0, normExpected.length() -  1);
        }
        
        assertEquals(normExpected, normActual);
    }

    private String normalizeNewLines(String s) {
        return _StringUtil.replace(s, "\r\n", "\n").replace('\r', '\n');
    }

    private void saveString(File actualFile, String actualContents) throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(actualFile), StandardCharsets.UTF_8);
        try {
            w.write(actualContents);
        } finally {
            w.close();
        }
    }

    protected URL getExpectedFileFor(String testCaseFileName) throws IOException {
        return new URL(getExpectedFileDirectory(), testCaseFileName);
    }

    /**
     * @return {@code null} if there's no place to write the actual files to
     */
    protected File getActualFileFor(String testCaseFileName) throws IOException {
        File actualFileDirectory = getActualFileDirectory();
        if (actualFileDirectory == null) {
            return null;
        }
        return new File(actualFileDirectory, deduceActualFileName(testCaseFileName));
    }
    
    private String deduceActualFileName(String testCaseFileName) {
        int lastDotIdx = testCaseFileName.lastIndexOf('.');
        return lastDotIdx == -1
                ? testCaseFileName + ".actual" 
                : testCaseFileName.substring(0, lastDotIdx) + "-actual" + testCaseFileName.substring(lastDotIdx);
    }

    /**
     * The URL of the directory that contains the expected files; must end with "/" or "/." or else relative paths won't
     * be resolved correctly.
     */
    protected URL getExpectedFileDirectory() throws IOException {
        return getTestClassDirectory();
    }

    /**
     * @return {@code null} if there's no directory to write the actual files to
     */
    protected File getActualFileDirectory() throws IOException {
        return FileUtils.toFile(getExpectedFileDirectory());
    }

    @SuppressFBWarnings(value="UI_INHERITANCE_UNSAFE_GETRESOURCE", justification="By design relative to subclass")
    protected final URL getTestClassDirectory() throws IOException {
        URL url = getClass().getResource(".");
        if (url == null) throw new IOException("Couldn't get resource URL for \".\"");
        return url;
    }

    protected String loadTestTextResource(URL resource) throws IOException {
        return loadTestTextResource(resource, getTestResourceCharset());
    }
    
    protected String loadTestTextResource(URL resource, Charset charset) throws IOException {
        return TestUtil.removeTxtCopyrightComment(
                IOUtils.toString(resource, charset.name()));
    }
    
    protected Charset getTestResourceCharset() {
        return StandardCharsets.UTF_8;
    }
    
    protected void reportActualFileSaved(File f) {
        System.out.println("Note: Saved actual output of the failed test to here: " + f.getAbsolutePath());
    }
   
    private static String loadString(InputStream in, Charset charset) throws IOException {
        Reader r = new InputStreamReader(in, charset);
        StringBuilder sb = new StringBuilder(1024);
        try {
            char[] buf = new char[4096];
            int ln;
            while ((ln = r.read(buf)) != -1) {
                sb.append(buf, 0, ln);
            }
        } finally {
            r.close();
        }
        return sb.toString();
    }
    
}
