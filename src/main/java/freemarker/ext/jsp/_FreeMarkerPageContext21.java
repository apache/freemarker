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
import freemarker.template.utility.ClassUtil;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 * 
 * Implementation of PageContext that contains JSP 2.0 and JSP 2.1 specific 
 * methods.
 */
public class _FreeMarkerPageContext21 extends FreeMarkerPageContext {
    private static final Logger LOG = Logger.getLogger("freemarker.jsp");

    static {
        if(JspFactory.getDefaultFactory() == null) {
            JspFactory.setDefaultFactory(new FreeMarkerJspFactory21());
        }
        LOG.debug("Using JspFactory implementation class " + 
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
            } else {
                throw new UnsupportedOperationException(
                        "Can not create an ELContext using a foreign JspApplicationContext (of class "
                        + ClassUtil.getShortClassNameOfObject(jspctx) + ").\n" +
                        "Hint: The cause of this is often that you are trying to use JSTL tags/functions in FTL. "
                        + "In that case, know that that's not really suppored, and you are supposed to use FTL "
                        + "constrcuts instead, like #list instead of JSTL's forEach, etc.");
            }
        }
        return elContext;
    }
}
