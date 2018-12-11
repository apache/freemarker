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

package org.apache.freemarker.servlet.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.freemarker.test.ResourcesExtractor;
import org.apache.freemarker.test.TestUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class WebAppTestCase {

    public static final String IGNORED_MASK = "[IGNORED]";

    private static final Logger LOG = LoggerFactory.getLogger(WebAppTestCase.class);

    private static final String ATTR_JETTY_CONTAINER_INCLUDE_JAR_PATTERN
            = "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern";

    private static final String EXPECTED_DIR = "WEB-INF/expected/";

    private static Server server;
    private static ContextHandlerCollection contextHandlers;
    private static Map<String, WebAppContext> deployedWebApps = new HashMap<>();
    private static volatile File testTempDirectory;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Work around Java 5 bug(?) that causes Jasper to fail with "zip file closed" when it reads the JSTL jar:
        org.eclipse.jetty.util.resource.Resource.setDefaultUseCaches(false);

        LOG.info("Starting embedded Jetty...");

        server = new Server(0);

        contextHandlers = new ContextHandlerCollection();
        server.setHandler(contextHandlers);

        server.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        LOG.info("Stopping embedded Jetty...");
        server.stop();
        server.join();
        LOG.info("Jetty stopped.");
        deleteTemporaryDirectories();
    }

    protected final String getResponseContent(String webAppName, String webAppRelURL) throws Exception {
        HTTPResponse resp = getHTTPResponse(webAppName, webAppRelURL);
        if (resp.getStatusCode() != HttpURLConnection.HTTP_OK) {
            fail("Expected HTTP status " + HttpURLConnection.HTTP_OK + ", but got "
                    + resp.getStatusCode() + " (message: " + resp.getStatusMessage() + ") for URI "
                    + resp.getURI());
        }
        return resp.getContent();
    }

    protected final int getResponseStatusCode(String webAppName, String webAppRelURL) throws Exception {
        HTTPResponse resp = getHTTPResponse(webAppName, webAppRelURL);
        return resp.getStatusCode();
    }

    protected final HTTPResponse getHTTPResponse(String webAppName, String webAppRelURL) throws Exception {
        if (webAppName.startsWith("/") || webAppName.endsWith("/")) {
            throw new IllegalArgumentException("\"webAppName\" can't start or end with \"/\": " + webAppName);
        }
        if (webAppRelURL.startsWith("/") || webAppRelURL.endsWith("/")) {
            throw new IllegalArgumentException("\"webappRelURL\" can't start or end with \"/\": " + webAppRelURL);
        }

        ensureWebAppIsDeployed(webAppName);

        final URI uri = new URI("http://localhost:" + server.getConnectors()[0].getLocalPort()
                + "/" + webAppName + "/" + webAppRelURL);

        final HttpURLConnection httpCon = (HttpURLConnection) uri.toURL().openConnection();
        httpCon.connect();
        try {
            LOG.debug("HTTP GET: {}", uri);

            final int responseCode = httpCon.getResponseCode();

            final String content;
            if (responseCode == 200) {
                InputStream in = httpCon.getInputStream();
                try {
                    content = IOUtils.toString(in, StandardCharsets.UTF_8.name());
                } finally {
                    in.close();
                }
            } else {
                content = null;
            }

            return new HTTPResponse(
                    responseCode, httpCon.getResponseMessage(),
                    content,
                    uri);
        } finally {
            httpCon.disconnect();
        }
    }

    /**
     * Compares the output of the JSP and the FTL version of the same page, ignoring some of the whitespace differences.
     * 
     * @param webAppRelURLWithoutExt
     *            something like {@code "tester?view=foo"}, which will be extended to {@code "tester?view=foo.jsp"} and
     *            {@code "tester?view=foo.f3ah"}, and then the output of these extended URL-s will be compared.
     */
    protected void assertJSPAndFTLOutputEquals(String webAppName, String webAppRelURLWithoutExt) throws Exception {
        assertOutputsEqual(webAppName, webAppRelURLWithoutExt + ".jsp", webAppRelURLWithoutExt + ".f3ah");
    }

    protected void assertOutputsEqual(String webAppName, String webAppRelURL1, final String webAppRelURL2)
            throws Exception {
        String jspOutput = normalizeWS(getResponseContent(webAppName, webAppRelURL1), true);
        String ftlOutput = normalizeWS(getResponseContent(webAppName, webAppRelURL2), true);
        assertEquals(jspOutput, ftlOutput);
    }

    protected void assertExpectedEqualsOutput(String webAppName, String expectedFileName, String webAppRelURL)
            throws Exception {
        assertExpectedEqualsOutput(webAppName, expectedFileName, webAppRelURL, true);
    }

    protected void assertExpectedEqualsOutput(String webAppName, String expectedFileName, String webAppRelURL,
            boolean compressWS) throws Exception {
        assertExpectedEqualsOutput(webAppName, expectedFileName, webAppRelURL, compressWS, null);
    }

    /**
     * @param expectedFileName
     *            The name of the file that stores the expected content, relatively to
     *            {@code servketContext:/WEB-INF/expected}.
     * @param ignoredParts
     *            Parts that will be search-and-replaced with {@value #IGNORED_MASK} with both in the expected and
     *            actual outputs.
     */
    protected void assertExpectedEqualsOutput(String webAppName, String expectedFileName, String webAppRelURL,
            boolean compressWS, List<Pattern> ignoredParts) throws Exception {
        final String actual = normalizeWS(getResponseContent(webAppName, webAppRelURL), compressWS);
        final String expected;
        {
            ClassPathResource cpResource = findWebAppDirectoryResource(webAppName);
            String expectedResource = cpResource.path + EXPECTED_DIR + expectedFileName;
            final InputStream in = cpResource.resolverClass.getResourceAsStream(
                    expectedResource);
            if (in == null) {
                throw new IOException("Test resource not found: " + expectedResource);
            }
            try {
                expected = TestUtils.removeTxtCopyrightComment(normalizeWS(
                        IOUtils.toString(in, StandardCharsets.UTF_8.name()),
                        compressWS));
            } finally {
                in.close();
            }
        }
        assertEquals(maskIgnored(expected, ignoredParts), maskIgnored(actual, ignoredParts));
    }

    private String maskIgnored(String s, List<Pattern> ignoredParts) {
        if (ignoredParts == null) return s;

        for (Pattern ignoredPart : ignoredParts) {
            s = ignoredPart.matcher(s).replaceAll(IGNORED_MASK);
        }
        return s;
    }

    protected synchronized void restartWebAppIfStarted(String webAppName) throws Exception {
        WebAppContext context = deployedWebApps.get(webAppName);
        if (context != null) {
            context.stop();
            context.start();
        }
    }

    private Pattern BR = Pattern.compile("\r\n|\r");
    private Pattern MULTI_LINE_WS = Pattern.compile("[\t ]*[\r\n][\t \r\n]*", Pattern.DOTALL);
    private Pattern SAME_LINE_WS = Pattern.compile("[\t ]+", Pattern.DOTALL);

    private String normalizeWS(String s, boolean compressWS) {
        if (compressWS) {
            return SAME_LINE_WS.matcher(
                    MULTI_LINE_WS.matcher(s).replaceAll("\n"))
                    .replaceAll(" ")
                    .trim();
        } else {
            return BR.matcher(s).replaceAll("\n");
        }
    }

    private synchronized void ensureWebAppIsDeployed(String webAppName) throws Exception {
        if (deployedWebApps.containsKey(webAppName)) {
            return;
        }

        final String webAppDirURL = createWebAppDirAndGetURI(webAppName);

        WebAppContext context = new WebAppContext(webAppDirURL, "/" + webAppName);

        // Pattern of jar file names scanned for META-INF/*.tld:
        context.setAttribute(
                ATTR_JETTY_CONTAINER_INCLUDE_JAR_PATTERN,
                ".*taglib.*\\.jar$");
        contextHandlers.addHandler(context);
        // As we add this after the Server was started, it has to be started manually:
        context.start();

        deployedWebApps.put(webAppName, context);
        LOG.info("Deployed web app.: {}", webAppName);
    }

    private static void deleteTemporaryDirectories() throws IOException {
        if (testTempDirectory.getParentFile() == null) {
            throw new IOException("Won't delete the root directory");
        }
        try {
            FileUtils.forceDelete(testTempDirectory);
        } catch (IOException e) {
            LOG.warn("Failed to delete temporary file or directory; will re-try on JVM shutdown: "
                    + testTempDirectory);
            FileUtils.forceDeleteOnExit(testTempDirectory);
        }
    }

    @SuppressFBWarnings(value = "UI_INHERITANCE_UNSAFE_GETRESOURCE", justification = "By design relative to subclass")
    private String createWebAppDirAndGetURI(String webAppName) throws IOException {
        ClassPathResource resourceDir = findWebAppDirectoryResource(webAppName);
        File temporaryDir = ResourcesExtractor.extract(
                resourceDir.resolverClass, resourceDir.path, new File(getTestTempDirectory(), webAppName));
        return temporaryDir.toURI().toString();
    }

    @SuppressFBWarnings("UI_INHERITANCE_UNSAFE_GETRESOURCE")
    private ClassPathResource findWebAppDirectoryResource(String webAppName) throws IOException {
        final String appRelResPath = "webapps/" + webAppName + "/";
        final String relResPath = appRelResPath + "WEB-INF/web.xml";

        Class<?> baseClass = getClass();
        do {
            URL r = baseClass.getResource(relResPath);
            if (r != null) {
                return new ClassPathResource(baseClass, appRelResPath);
            }

            baseClass = baseClass.getSuperclass();
            if (!WebAppTestCase.class.isAssignableFrom(baseClass)) {
                throw new IOException("Can't find test class relative resource: " + relResPath);
            }
        } while (true);
    }
    
    private File getTestTempDirectory() throws IOException {
        if (testTempDirectory == null) {
            // As at least on Windows we have problem deleting the directories once Jetty has used them (the file
            // handles of the jars remain open), we always use the same name, so that at most one will remain there.
            File d = new File(
                    new File(System.getProperty("java.io.tmpdir")),
                    "freemarker-jetty-junit-tests-(delete-it)");
            if (d.exists()) {
                FileUtils.deleteDirectory(d);
            }
            if (!d.mkdirs()) {
               throw new IOException("Failed to invoke Jetty temp directory: " + d);
            }
            testTempDirectory = d;
        }
        return testTempDirectory;
    }

    private static class ClassPathResource {

        private final Class<?> resolverClass;
        private final String path;

        public ClassPathResource(Class<?> resolverClass, String path) {
            this.resolverClass = resolverClass;
            this.path = path;
        }

    }

    private static class HTTPResponse {

        private final int statusCode;
        private final String content;
        private final String statusMessage;
        private final URI uri;

        public HTTPResponse(int statusCode, String statusMessage, String content, URI uri) {
            this.statusCode = statusCode;
            this.content = content;
            this.statusMessage = statusMessage;
            this.uri = uri;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getContent() {
            return content;
        }

        public URI getURI() {
            return uri;
        }

    }

}
