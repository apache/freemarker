package freemarker.test.servlet;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebApplicationTestCase {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebApplicationTestCase.class);

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
        
        ensureWebAppDeployed(webAppName);
        
        final URI uri = new URI("http://localhost:" + server.getConnectors()[0].getLocalPort()
                + "/" + webAppName + "/" + webAppRelURL);

        LOG.debug("Connecting to {}", uri);
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

    private synchronized void ensureWebAppDeployed(String webAppName) throws Exception {
        if (deployedWebApps.contains(webAppName)) {
            return;
        }
        
        final String webAppDirURL = getWebAppDirURL(webAppName);
        
        WebAppContext context = new WebAppContext(webAppDirURL, "/" + webAppName);
        context.setParentLoaderPriority(true);

        contextHandlers.addHandler(context);
        // As we add this after the Server was started, it has to be started manually:
        context.start();
        
        deployedWebApps.add(webAppName);
        LOG.info("Installed web application: {}", webAppName);
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
                if (!WebApplicationTestCase.class.isAssignableFrom(baseClass)) {
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
