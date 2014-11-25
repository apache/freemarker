package freemarker.test.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAppTestCase {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebAppTestCase.class);

    private static Server server;
    private static ContextHandlerCollection contextHandlers;
    private static Set<String> deployedWebApps = new HashSet<String>(); 
    
    @BeforeClass
    public static void beforeClass() throws Exception {
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
        server.join(); // TODO redundant?
    }
    
    protected final String getResponseContent(String webAppName, String webAppRelURL) throws Exception {
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
            
            final int httpStatusCode = httpCon.getResponseCode();
            if (httpStatusCode != HttpURLConnection.HTTP_OK) {
                fail("Expected HTTP status " + HttpURLConnection.HTTP_OK + ", but got "
                        + httpStatusCode + " (message: " + httpCon.getResponseMessage() + ") for URI "
                        + uri);
            }
            
            InputStream in = httpCon.getInputStream();
            try {            
                return IOUtils.toString(in, "UTF-8");
            } finally {
                in.close();
            }
        } finally {
            httpCon.disconnect();
        }
    }
    
    /**
     * Compares the output of the JSP and the FTL version of the same page, ignoring some of the whitespace differences.
     * @param webAppRelURLWithoutExt something like {@code "tester?view=foo"}, which will be extended to
     *          {@code "tester?view=foo.jsp"} and {@code "tester?view=foo.ftl"}, and then the output of these extended
     *          URL-s will be compared.
     */
    protected void assertJSPAndFTLOutputEquals(String webAppName, String webAppRelURLWithoutExt) throws Exception {
        String jspOutput = normalizeWS(getResponseContent(webAppName, webAppRelURLWithoutExt + ".jsp"));
        String ftlOutput = normalizeWS(getResponseContent(webAppName, webAppRelURLWithoutExt + ".ftl"));
        assertEquals(jspOutput, ftlOutput);
    }

    private Pattern TRIM_NL = Pattern.compile("^[\\n\\r]*+(.*?)[\\n\\r]*$", Pattern.DOTALL); 
    
    private String normalizeWS(String s) {
        Matcher m = TRIM_NL.matcher(s);
        m.matches();
        s = m.group(1);
        return s;
    }

    private synchronized void ensureWebAppIsDeployed(String webAppName) throws Exception {
        if (deployedWebApps.contains(webAppName)) {
            return;
        }
        
        final String webAppDirURL = getWebAppDirURL(webAppName);
        
        WebAppContext context = new WebAppContext(webAppDirURL, "/" + webAppName);
        context.setParentLoaderPriority(true);
        // Pattern of jar file names scanned for META-INF/*.tld:
        context.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*taglib.*standard.*\\.jar$");

        contextHandlers.addHandler(context);
        // As we add this after the Server was started, it has to be started manually:
        context.start();
        
        deployedWebApps.add(webAppName);
        LOG.info("Deployed web app.: {}", webAppName);
    }

    private String getWebAppDirURL(String webAppName) throws IOException {
        final URL webXmlURL;
        {
            final String relResPath = "webapps/" + webAppName + "/WEB-INF/web.xml";
            
            Class<?> baseClass = this.getClass();
            findWebXmlURL: do {
                URL r = baseClass.getResource(relResPath);
                if (r != null) {
                    webXmlURL = r;
                    break findWebXmlURL;
                }
                
                baseClass = baseClass.getSuperclass();
                if (!WebAppTestCase.class.isAssignableFrom(baseClass)) {
                    throw new IOException("Can't find test class relative resource: " + relResPath);
                }
            } while (true);
        }
        
        try {
            return webXmlURL.toURI().resolve("..").toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to get grandparent URL for " + webXmlURL, e);
        }
    }
    
}
