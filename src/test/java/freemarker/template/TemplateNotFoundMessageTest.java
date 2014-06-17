package freemarker.template;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.Test;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;

public class TemplateNotFoundMessageTest {

    @Test
    public void testFileTemplateLoader() throws IOException {
        final File baseDir = new File(System.getProperty("user.home"));
        final String errMsg = failWith(new FileTemplateLoader(baseDir));
        showErrorMessage(errMsg);
        assertTrue(errMsg.contains(baseDir.toString()));
        assertTrue(errMsg.contains("FileTemplateLoader"));
    }

    @Test
    public void testClassTemplateLoader() throws IOException {
        final String errMsg = failWith(new ClassTemplateLoader(this.getClass(), "foo/bar"));
        showErrorMessage(errMsg);
        assertTrue(errMsg.contains("ClassTemplateLoader"));
        assertTrue(errMsg.contains("foo/bar"));
    }

    @Test
    public void testWebappTemplateLoader() throws IOException {
        final String errMsg = failWith(new WebappTemplateLoader(new MockServletContext(), "WEB-INF/templates"));
        showErrorMessage(errMsg);
        assertTrue(errMsg.contains("WebappTemplateLoader"));
        assertTrue(errMsg.contains("MyApp"));
        assertTrue(errMsg.contains("WEB-INF/templates"));
    }

    @Test
    public void testStringTemplateLoader() throws IOException {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("aaa", "A");
        tl.putTemplate("bbb", "B");
        tl.putTemplate("ccc", "C");
        final String errMsg = failWith(tl);
        showErrorMessage(errMsg);
        assertTrue(errMsg.contains("StringTemplateLoader"));
        assertTrue(errMsg.contains("aaa"));
        assertTrue(errMsg.contains("bbb"));
        assertTrue(errMsg.contains("ccc"));
    }
    
    @Test
    public void testMultiTemplateLoader() throws IOException {
        final String errMsg = failWith(new MultiTemplateLoader(new TemplateLoader[] {
                new WebappTemplateLoader(new MockServletContext(), "WEB-INF/templates"),
                new ClassTemplateLoader(this.getClass(), "foo/bar")
        }));
        showErrorMessage(errMsg);
        assertTrue(errMsg.contains("MultiTemplateLoader"));
        assertTrue(errMsg.contains("WebappTemplateLoader"));
        assertTrue(errMsg.contains("MyApp"));
        assertTrue(errMsg.contains("WEB-INF/templates"));
        assertTrue(errMsg.contains("ClassTemplateLoader"));
        assertTrue(errMsg.contains("foo/bar"));
    }

    private void showErrorMessage(String errMsg) {
        System.out.println(errMsg);
    }

    private String failWith(TemplateLoader tl) {
        Configuration cfg = new Configuration(new Version(2, 3, 21));
        cfg.setTemplateLoader(tl);
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (FileNotFoundException e) {
            return e.getMessage();
        } catch (IOException e) {
            fail();
        }
        return null;
    }
    
    private static class MockServletContext implements ServletContext {

        public Object getAttribute(String arg0) {
            return null;
        }

        public Enumeration getAttributeNames() {
            return null;
        }

        public ServletContext getContext(String arg0) {
            return null;
        }

        public String getContextPath() {
            return "/myapp";
        }

        public String getInitParameter(String arg0) {
            return null;
        }

        public Enumeration getInitParameterNames() {
            return null;
        }

        public int getMajorVersion() {
            return 0;
        }

        public String getMimeType(String arg0) {
            return null;
        }

        public int getMinorVersion() {
            return 0;
        }

        public RequestDispatcher getNamedDispatcher(String arg0) {
            return null;
        }

        public String getRealPath(String arg0) {
            return null;
        }

        public RequestDispatcher getRequestDispatcher(String arg0) {
            return null;
        }

        public URL getResource(String arg0) throws MalformedURLException {
            return null;
        }

        public InputStream getResourceAsStream(String arg0) {
            return null;
        }

        public Set getResourcePaths(String arg0) {
            return null;
        }

        public String getServerInfo() {
            return null;
        }

        public Servlet getServlet(String arg0) throws ServletException {
            return null;
        }

        public String getServletContextName() {
            return "MyApp";
        }

        public Enumeration getServletNames() {
            return null;
        }

        public Enumeration getServlets() {
            return null;
        }

        public void log(String arg0) {
            
        }

        public void log(Exception arg0, String arg1) {
            
        }

        public void log(String arg0, Throwable arg1) {
            
        }

        public void removeAttribute(String arg0) {
        }

        public void setAttribute(String arg0, Object arg1) {
        }
        
    }

}
