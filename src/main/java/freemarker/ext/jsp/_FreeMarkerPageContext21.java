package freemarker.ext.jsp;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.el.ELContext;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;

import freemarker.log.Logger;
import freemarker.template.TemplateModelException;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 * 
 * Implementation of PageContext that contains JSP 2.0 and JSP 2.1 specific 
 * methods.
 * 
 * @author Attila Szegedi
 */
public class _FreeMarkerPageContext21 extends FreeMarkerPageContext {
    private static final Logger logger = Logger.getLogger("freemarker.jsp");

    static {
        if(JspFactory.getDefaultFactory() == null) {
            JspFactory.setDefaultFactory(new FreeMarkerJspFactory21());
        }
        logger.debug("Using JspFactory implementation class " + 
                JspFactory.getDefaultFactory().getClass().getName());
    }

    public _FreeMarkerPageContext21() throws TemplateModelException {
        super();
    }

    /**
     * Attempts to locate and manufacture an expression evaulator instance. For this
     * to work you <b>must</b> have the Apache Commons-EL package in the classpath. If
     * Commons-EL is not available, this method will throw an UnsupportedOperationException. 
     */
    public ExpressionEvaluator getExpressionEvaluator() {
        try {
            Class type = ((ClassLoader)AccessController.doPrivileged(
                    new PrivilegedAction() {
                        public Object run() {
                            return Thread.currentThread().getContextClassLoader();
                        }
                    })).loadClass
                    ("org.apache.commons.el.ExpressionEvaluatorImpl");
            return (ExpressionEvaluator) type.newInstance();
        }
        catch (Exception e) {
            throw new UnsupportedOperationException("In order for the getExpressionEvaluator() " +
                "method to work, you must have downloaded the apache commons-el jar and " +
                "made it available in the classpath.");
        }
    }

    /**
     * Returns a variable resolver that will resolve variables by searching through
     * the page scope, request scope, session scope and application scope for an
     * attribute with a matching name.
     */
    public VariableResolver getVariableResolver() {
        final PageContext ctx = this;

        return new VariableResolver() {
            public Object resolveVariable(String name) throws ELException {
                return ctx.findAttribute(name);
            }
        };
    }

    private ELContext elContext;
    
    public ELContext getELContext() {
        if(elContext == null) { 
            JspApplicationContext jspctx = JspFactory.getDefaultFactory().getJspApplicationContext(getServletContext());
            if(jspctx instanceof FreeMarkerJspApplicationContext) {
                elContext = ((FreeMarkerJspApplicationContext)jspctx).createNewELContext(this);
                elContext.putContext(JspContext.class, this);
            }
            else {
                throw new UnsupportedOperationException(
                        "Can not create an ELContext using a foreign JspApplicationContext\n" +
                        "Consider dropping a private instance of JSP 2.1 API JAR file in\n" +
                        "your WEB-INF/lib directory and then try again.");
            }
        }
        return elContext;
    }
}
