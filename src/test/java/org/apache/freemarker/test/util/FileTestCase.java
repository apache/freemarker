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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Test case that needs to compare a string to a reference (expected) text file, or two text files. 
 */
public abstract class FileTestCase extends TestCase {

    public static final Logger LOG = LoggerFactory.getLogger(FileTestCase.class);

    public FileTestCase(String name) {
        super(name);
    }

    protected void assertExpectedFileEqualsString(String expectedFileName, String actualContent) {
        try {
            final URL expectedFile = getExpectedContentFileURL(expectedFileName);
            
            try {
                multilineAssertEquals(loadTestTextResource(expectedFile), actualContent);
            } catch (AssertionFailedError | FileNotFoundException e) {
                File actualFile = getActualContentFileFor(expectedFile);
                if (actualFile != null) {
                    FileUtils.write(actualFile, actualContent);
                    reportActualFileSaved(actualFile);
                }

                throw e;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to do assertion", e);
        }
    }

    private void multilineAssertEquals(String expected, String actual) {
        String normExpected = _StringUtil.normalizeEOLs(expected);
        final String normActual = _StringUtil.normalizeEOLs(actual);
        
        // Ignore final line-break difference:
        if (normActual.endsWith("\n") && !normExpected.endsWith("\n")) {
            normExpected += "\n";
        } else if (!normActual.endsWith("\n") && normExpected.endsWith("\n")) {
            normExpected = normExpected.substring(0, normExpected.length() -  1);
        }
        
        assertEquals(normExpected, normActual);
    }

    protected void reportActualFileSaved(File actualContentFile) {
        LOG.info("Saved actual output of the failed test to here: {}", actualContentFile.getAbsolutePath());
    }

    /**
     * Convenience method for calling {@link #getTestFileURL(String, String)} with {@link
     * #getExpectedContentFileDirectoryResourcePath()} as the first argument.
     */
    protected final URL getExpectedContentFileURL(String expectedContentFileName) throws IOException {
        return getTestFileURL(getExpectedContentFileDirectoryResourcePath(), expectedContentFileName);
    }

    /**
     * Gets the URL of the test file that contains the expected result.
     *
     * @param directoryResourcePath
     *         The class-loader resource path of the containing directory; if relative, it's interpreted relatively to
     *         the package of {@link #getTestResourcesBaseClass()}.
     *
     * @return Not {@code null}; if the file isn't found, throw {@link FileNotFoundException}
     *
     * @throws FileNotFoundException
     *         If the requested file wasn't found.
     */
    protected final URL getTestFileURL(String directoryResourcePath, String fileName) throws IOException {
        _NullArgumentException.check("directoryResourcePath", directoryResourcePath);
        _NullArgumentException.check("testCaseFileName", fileName);

        Class baseClass = getTestResourcesBaseClass();
        String resourcePath = joinResourcePaths(directoryResourcePath, fileName);
        // It's important that we only query an URL for the file (not for the parent package directory), because the
        // parent URL can depend on the file name if the class loader uses multiple directories/jars.
        URL resource = baseClass.getResource(resourcePath);
        if (resource == null) {
            throw new FileNotFoundException("Class-loader resource not found for: "
                    + "baseClass: " + baseClass.getName() + "; "
                    + "resourcePath (shown quoted): " + _StringUtil.jQuote(resourcePath));
        }
        return resource;
    }

    /**
     * Concatenates two resource paths, taking care of the edge cases due to leading and trailing "/"
     * characters in them.
     */
    protected final String joinResourcePaths(String dirPath, String tailPath) {
        if (tailPath.startsWith("/") || dirPath.isEmpty()) {
            return tailPath;
        }
        return dirPath.endsWith("/") ? dirPath + tailPath : dirPath + "/" + tailPath;
    }

    /**
     * Gets the actual content file to create which belongs to an expected content file. Actual content files are
     * created when the expected and the actual content differs.
     *
     * @return {@code null} if there's no place to write the files that contain the actual content
     */
    protected File getActualContentFileFor(URL expectedContentFile) throws IOException {
        _NullArgumentException.check("expectedContentFile", expectedContentFile);

        File actualContentFileDir = getActualContentFileDirectory(expectedContentFile);
        if (actualContentFileDir == null) {
            return null;
        }

        String expectedContentFileName = expectedContentFile.getPath();
        int lastSlashIdx = expectedContentFileName.lastIndexOf('/');
        if (lastSlashIdx != -1) {
            expectedContentFileName = expectedContentFileName.substring(lastSlashIdx + 1);
        }

        return new File(actualContentFileDir, deduceActualContentFileName(expectedContentFileName));
    }

    /**
     * Deduces the actual content file name from the expected content file name.
     *
     * @return Not {@code null}
     */
    protected String deduceActualContentFileName(String expectedContentFileName) {
        _NullArgumentException.check("expectedContentFileName", expectedContentFileName);

        int lastDotIdx = expectedContentFileName.lastIndexOf('.');
        return lastDotIdx == -1
                ? expectedContentFileName + ".actual"
                : expectedContentFileName.substring(0, lastDotIdx) + "-actual" + expectedContentFileName.substring(lastDotIdx);
    }

    /**
     * The class loader resource path of the directory that contains the expected files; must start and end with "/"!
     */
    protected String getExpectedContentFileDirectoryResourcePath() throws IOException {
        return getTestClassDirectoryResourcePath();
    }

    /**
     * @return {@code null} if there's no directory to write the actual files to
     */
    protected File getActualContentFileDirectory(URL expectedFile) throws IOException {
        return FileUtils.toFile(expectedFile).getParentFile();
    }

    /**
     * The class loader resource path of the directory that contains the test files; must not end with "/"!
     */
    protected String getTestClassDirectoryResourcePath() throws IOException {
        return "";
    }

    /**
     * Resource paths are loaded using this class's {@link Class#getResourceAsStream(String)} method; thus, if
     * {@link #getTestClassDirectoryResourcePath()} and such return a relative paths, they will be relative to the
     * package of this class.
     */
    protected Class getTestResourcesBaseClass() {
        return getClass();
    }

    protected String loadTestTextResource(URL resource) throws IOException {
        return loadTestTextResource(resource, getTestResourceDefaultCharset());
    }
    
    protected String loadTestTextResource(URL resource, Charset charset) throws IOException {
        return TestUtil.removeTxtCopyrightComment(
                IOUtils.toString(resource, charset.name()));
    }
    
    protected Charset getTestResourceDefaultCharset() {
        return StandardCharsets.UTF_8;
    }

}
