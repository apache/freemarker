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

package freemarker.test.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import freemarker.template.utility.StringUtil;

/**
 * Test case that needs to compare a string to a reference (expected) text file, or two text files. 
 */
public abstract class FileTestCase extends TestCase {

    public FileTestCase(String name) {
        super(name);
    }

    protected void assertFilesEqual(String testCaseFileName) throws FileNotFoundException, IOException {
        try {
            multilineAssertEquals(
                    loadFile(getExpectedFileFor(testCaseFileName)),
                    loadFile(getActualFileFor(testCaseFileName)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to do assertion", e);
        }
    }

    protected void assertExpectedFileEqualsString(String expectedFileName, String actualContent) {
        try {
            final File expectedFile = getExpectedFileFor(expectedFileName);
            
            AssertionFailedError assertionExcepton = null;
            boolean successful = false;
            try {
                if (expectedFile.exists()) {
                    multilineAssertEquals(loadFile(expectedFile), actualContent);
                    successful = true;
                }
            } catch (AssertionFailedError e) {
                assertionExcepton = e;
            }
            
            if (!successful) {
                File actualFile = getActualFileFor(expectedFileName);
                saveString(actualFile, actualContent);
                reportActualFileSaved(actualFile);
                
                if (assertionExcepton != null) {
                    throw assertionExcepton;
                } else {
                    throw new FileNotFoundException(expectedFile.getPath());
                }
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
        return StringUtil.replace(s, "\r\n", "\n").replace('\r', '\n');
    }

    private void saveString(File actualFile, String actualContents) throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(actualFile), "UTF-8");
        try {
            w.write(actualContents);
        } finally {
            w.close();
        }
    }

    protected File getExpectedFileFor(String testCaseFileName) throws IOException {
        return new File(getExpectedFileDirectory(), testCaseFileName);
    }
    
    protected File getActualFileFor(String testCaseFileName) throws IOException {
        return new File(getActualFileDirectory(), deduceActualFileName(testCaseFileName));
    }
    
    private String deduceActualFileName(String testCaseFileName) {
        int lastDotIdx = testCaseFileName.lastIndexOf('.');
        return lastDotIdx == -1
                ? testCaseFileName + ".actual" 
                : testCaseFileName.substring(0, lastDotIdx) + "-actual" + testCaseFileName.substring(lastDotIdx);
    }

    protected File getExpectedFileDirectory() throws IOException {
        return getTestClassDirectory();
    }

    protected File getActualFileDirectory() throws IOException {
        return getExpectedFileDirectory();
    }

    // Findbugs annotation, try to put back on Java 1.6:
    // @SuppressFBWarnings(value="UI_INHERITANCE_UNSAFE_GETRESOURCE")
    protected final File getTestClassDirectory() throws IOException {
        URL url = this.getClass().getResource(".");  // Deliberately gets the package URL of the subclass
        if (url == null) throw new IOException("Couldn't get resource URL for \".\"");
        return new File(url.getFile());
    }

    protected String loadFile(File f) throws FileNotFoundException, IOException {
        return loadFile(f, getDefaultCharset());
    }
    
    protected String loadFile(File f, String charset) throws FileNotFoundException, IOException {
        return loadString(new FileInputStream(f), charset);
    }

    protected String loadResource(String resourceName) throws FileNotFoundException, IOException {
        return loadResource(resourceName, getDefaultCharset());
    }
    
    protected String loadResource(String resourceName, String charset) throws FileNotFoundException, IOException {
        return loadString(new FileInputStream(new File(getTestClassDirectory(), resourceName)), charset);
    }
    
    protected String getDefaultCharset() {
        return "UTF-8";
    }
    
    protected void reportActualFileSaved(File f) {
        System.out.println("Note: Saved actual output of the failed test to here: " + f.getAbsolutePath());
    }
   
    private static String loadString(InputStream in, String charset) throws IOException {
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
