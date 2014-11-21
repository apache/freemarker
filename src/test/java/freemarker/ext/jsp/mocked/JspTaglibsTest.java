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

package freemarker.ext.jsp.mocked;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.security.Principal;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import junit.framework.TestCase;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.test.utility.FileTestCase;

/**
 */
public class JspTaglibsTest extends FileTestCase {
    
    public JspTaglibsTest(String name) {
        super(name);
    }
    
    public void test() throws Exception {
        ServletConfig cfg = new MockServletConfig();
        FreemarkerServlet servlet = new FreemarkerServlet();
        servlet.init(cfg);
        MockRequest req = new MockRequest("test-jsptaglibs.txt");
        MockResponse resp = new MockResponse();
        servlet.doGet(req, resp);
        
        assertExpectedFileEqualsString("reference/test-jsptaglibs.txt", resp.toString());
    }

    private static class MockServletConfig
        implements ServletConfig, ServletContext {
        private final Properties initParams = new Properties();
        private final Hashtable attributes = new Hashtable();

        MockServletConfig() {
            initParams.setProperty("TemplatePath", "/template/");
            initParams.setProperty("NoCache", "true");
            initParams.setProperty("TemplateUpdateInterval", "0");
            initParams.setProperty("DefaultEncoding", "UTF-8");
            initParams.setProperty("ObjectWrapper", "beans");
        }

        public String getInitParameter(String name) {
            return initParams.getProperty(name);
        }

        public Enumeration getInitParameterNames() {
            return initParams.keys();
        }

        public ServletContext getServletContext() {
            return this;
        }
        
        public String getContextPath() {
            throw new UnsupportedOperationException();
        }

        public String getServletName() {
            return "freemarker";
        }

        public Object getAttribute(String arg0) {
            return attributes.get(arg0);
        }

        public Enumeration getAttributeNames() {
            return attributes.keys();
        }

        public ServletContext getContext(String arg0) {
            throw new UnsupportedOperationException();
        }

        public int getMajorVersion() {
            return 0;
        }

        public String getMimeType(String arg0) {
            throw new UnsupportedOperationException();
        }

        public int getMinorVersion() {
            return 0;
        }

        public RequestDispatcher getNamedDispatcher(String arg0) {
            throw new UnsupportedOperationException();
        }

        public String getRealPath(String arg0) {
            return null;
        }

        public RequestDispatcher getRequestDispatcher(String arg0) {
            throw new UnsupportedOperationException();
        }

        public URL getResource(String url) {
            if (url.startsWith("/")) {
                url = url.substring(1);
            }
            return getClass().getResource(url);
        }

        public InputStream getResourceAsStream(String url) {
            if (url.startsWith("/")) {
                url = url.substring(1);
            }
            return getClass().getResourceAsStream(url);
        }

        public Set getResourcePaths(String path) {
            if(path.equals("/WEB-INF")) {
                return new HashSet(Arrays.asList(new String[] { 
                    "/WEB-INF/fmtesttag2.tld",
                    "/WEB-INF/lib/"
                }));
            } else if(path.equals("/WEB-INF/lib")) {
                return new HashSet(Arrays.asList(new String[] { 
                    "/WEB-INF/lib/taglib-foo.jar",
                }));
            }
            else {
                return null;
            }
        }

        public String getServerInfo() {
            return "FreeMarker/JUnit";
        }

        /**
         * @deprecated No mock implementation exists for this.
         */
        public Servlet getServlet(String arg0) {
            throw new UnsupportedOperationException();
        }

        public String getServletContextName() {
            return "freemarker";
        }

        /**
         * @deprecated No mock implementation exists for this.
         */
        public Enumeration getServletNames() {
            throw new UnsupportedOperationException();
        }

        /**
         * @deprecated No mock implementation exists for this.
         */
        public Enumeration getServlets() {
            throw new UnsupportedOperationException();
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public void log(Exception arg0, String arg1) {
        }

        public void log(String arg0, Throwable arg1) {
        }

        public void log(String arg0) {
        }

        public void removeAttribute(String arg0) {
            attributes.remove(arg0);
        }

        public void setAttribute(String arg0, Object arg1) {
            attributes.put(arg0, arg1);
        }
    }

    private static final class MockRequest
    implements 
        HttpServletRequest
    {
        private final String pathInfo;
        private HttpSession session;
                   
        MockRequest(String pathInfo) {
            this.pathInfo = pathInfo;
        }

        public int getLocalPort() {
            throw new UnsupportedOperationException();
        }
        
        public String getLocalAddr() {
            throw new UnsupportedOperationException();
        }
        
        public String getLocalName() {
            throw new UnsupportedOperationException();
        }
        
        public int getRemotePort() {
            throw new UnsupportedOperationException();
        }

        public String getAuthType() {
            return null;
        }

        public String getContextPath() {
            return null;
        }

        public Cookie[] getCookies() {
            return null;
        }

        public long getDateHeader(String arg0) {
            return 0;
        }

        public String getHeader(String arg0) {
            return null;
        }

        public Enumeration getHeaderNames() {
            return null;
        }

        public Enumeration getHeaders(String arg0) {
            return null;
        }

        public int getIntHeader(String arg0) {
            return 0;
        }

        public String getMethod() {
            return null;
        }

        public String getPathInfo() {
            return pathInfo;
        }

        public String getPathTranslated() {
            return null;
        }

        public String getQueryString() {
            return null;
        }

        public String getRemoteUser() {
            return null;
        }

        public String getRequestedSessionId() {
            return null;
        }

        public String getRequestURI() {
            return null;
        }

        public StringBuffer getRequestURL() {
            return null;
        }

        public String getServletPath() {
            return null;
        }

        public HttpSession getSession() {
            return getSession(true);
        }

        public HttpSession getSession(boolean arg0) {
            if(session == null && arg0) session = new MockSession();
            return session;
        }

        public Principal getUserPrincipal() {
            return null;
        }

        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public boolean isRequestedSessionIdFromUrl() {
            return false;
        }

        public boolean isRequestedSessionIdFromURL() {
            return false;
        }

        public boolean isRequestedSessionIdValid() {
            return false;
        }

        public boolean isUserInRole(String arg0) {
            return false;
        }

        public Object getAttribute(String arg0) {
            return null;
        }

        public Enumeration getAttributeNames() {
            return null;
        }

        public String getCharacterEncoding() {
            return null;
        }

        public int getContentLength() {
            return 0;
        }

        public String getContentType() {
            return null;
        }

        public ServletInputStream getInputStream() {
            return null;
        }

        public Locale getLocale() {
            return Locale.getDefault();
        }

        public Enumeration getLocales() {
            return null;
        }

        public String getParameter(String arg0) {
            return null;
        }

        public Map getParameterMap() {
            return null;
        }

        public Enumeration getParameterNames() {
            return null;
        }

        public String[] getParameterValues(String arg0) {
            return null;
        }

        public String getProtocol() {
            return null;
        }

        public BufferedReader getReader() {
            return null;
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public String getRealPath(String arg0) {
            return null;
        }

        public String getRemoteAddr() {
            return null;
        }

        public String getRemoteHost() {
            return null;
        }

        public RequestDispatcher getRequestDispatcher(String arg0) {
            return null;
        }

        public String getScheme() {
            return null;
        }

        public String getServerName() {
            return null;
        }

        public int getServerPort() {
            return 0;
        }

        public boolean isSecure() {
            return false;
        }

        public void removeAttribute(String arg0) {
        }

        public void setAttribute(String arg0, Object arg1) {
        }

        public void setCharacterEncoding(String arg0) {
        }
    }

    private static final class MockResponse
    implements 
        HttpServletResponse
    {
        private final StringWriter writer = new StringWriter();
        private final PrintWriter pwriter = new PrintWriter(writer);
        
        public void addCookie(Cookie arg0) {
        }

        public void addDateHeader(String arg0, long arg1) {
        }

        public void addHeader(String arg0, String arg1) {
        }

        public void addIntHeader(String arg0, int arg1) {
        }

        public boolean containsHeader(String arg0) {
            return false;
        }
        
        public void setCharacterEncoding(String arg0) {
            throw new UnsupportedOperationException();
        }

        public String getContentType() {
            throw new UnsupportedOperationException();
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public String encodeRedirectUrl(String arg0) {
            return null;
        }

        public String encodeRedirectURL(String arg0) {
            return null;
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public String encodeUrl(String arg0) {
            return null;
        }

        public String encodeURL(String arg0) {
            return null;
        }

        public void sendError(int arg0, String arg1) {
        }

        public void sendError(int arg0) {
        }

        public void sendRedirect(String arg0) {
        }

        public void setDateHeader(String arg0, long arg1) {
        }

        public void setHeader(String arg0, String arg1) {
        }

        public void setIntHeader(String arg0, int arg1) {
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public void setStatus(int arg0, String arg1) {
        }

        public void setStatus(int arg0) {
        }

        public void flushBuffer() {
        }

        public int getBufferSize() {
            return 0;
        }

        public String getCharacterEncoding() {
            return null;
        }

        public Locale getLocale() {
            return null;
        }

        public ServletOutputStream getOutputStream() {
            return null;
        }

        public PrintWriter getWriter() {
            return pwriter;
        }

        public boolean isCommitted() {
            return false;
        }

        public void reset() {
        }

        public void resetBuffer() {
        }

        public void setBufferSize(int arg0) {
        }

        public void setContentLength(int arg0) {
        }

        public void setContentType(String arg0) {
        }

        public void setLocale(Locale arg0) {
        }
        
        public String toString() {
            pwriter.flush();
            return writer.toString();
        }
    }

    private static final class MockSession implements HttpSession
    {
        public Object getAttribute(String arg0) {
            return null;
        }

        public Enumeration getAttributeNames() {
            return null;
        }

        public long getCreationTime() {
            return 0;
        }

        public String getId() {
            return null;
        }

        public long getLastAccessedTime() {
            return 0;
        }

        public int getMaxInactiveInterval() {
            return 0;
        }

        public ServletContext getServletContext() {
            return null;
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public HttpSessionContext getSessionContext() {
            return null;
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public Object getValue(String arg0) {
            return null;
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public String[] getValueNames() {
            return null;
        }

        public void invalidate() {
        }

        public boolean isNew() {
            return false;
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public void putValue(String arg0, Object arg1) {
        }

        public void removeAttribute(String arg0) {
        }

        /**
         * @deprecated Deprecated in the overidden API.
         */
        public void removeValue(String arg0) {
        }

        public void setAttribute(String arg0, Object arg1) {
        }

        public void setMaxInactiveInterval(int arg0) {
        }
}
    
    /** Bootstrap for the self-test code.
     */
    public static void main( String[] argc ) throws Exception {
        TestCase test = new JspTaglibsTest( "test-jsptaglibs.txt" );
        test.run();
    }
}
