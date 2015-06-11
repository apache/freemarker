package freemarker.template;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class MockServletContext implements ServletContext {

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