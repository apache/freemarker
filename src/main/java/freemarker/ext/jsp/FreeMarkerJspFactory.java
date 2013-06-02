package freemarker.ext.jsp;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

/**
 * @author Attila Szegedi
 */
abstract class FreeMarkerJspFactory extends JspFactory
{
    protected abstract String getSpecificationVersion();
    
    public JspEngineInfo getEngineInfo() {
        return new JspEngineInfo() {
            public String getSpecificationVersion() {
                return FreeMarkerJspFactory.this.getSpecificationVersion();
            }
        };
    }

    public PageContext getPageContext(Servlet servlet, ServletRequest request, 
            ServletResponse response, String errorPageURL, 
            boolean needsSession, int bufferSize, boolean autoFlush) {
        // This is never meant to be called. JSP pages compiled to Java 
        // bytecode use this API, but in FreeMarker, we're running templates,
        // and not JSP pages precompiled to bytecode, therefore we have no use
        // for this API.
        throw new UnsupportedOperationException();
    }

    public void releasePageContext(PageContext ctx) {
        // This is never meant to be called. JSP pages compiled to Java 
        // bytecode use this API, but in FreeMarker, we're running templates,
        // and not JSP pages precompiled to bytecode, therefore we have no use
        // for this API.
        throw new UnsupportedOperationException();
    }
}