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


package freemarker.ext.jsp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

import javax.el.ELContext;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.tagext.BodyContent;

import freemarker.core.Environment;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.ServletContextHashModel;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.log.Logger;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.ObjectWrapperAndUnwrapper;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template._VersionInts;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.UndeclaredThrowableException;

class FreeMarkerPageContext extends PageContext implements TemplateModel {
    private static final Logger LOG = Logger.getLogger("freemarker.jsp");

    static {
        if (JspFactory.getDefaultFactory() == null) {
            JspFactory.setDefaultFactory(new FreeMarkerJspFactory());
        }
        LOG.debug("Using JspFactory implementation class " +
                JspFactory.getDefaultFactory().getClass().getName());
    }

    private final Environment environment;
    private final int incompatibleImprovements;
    private List tags = new ArrayList();
    private List outs = new ArrayList();
    private final GenericServlet servlet;
    private HttpSession session;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ObjectWrapper wrapper;
    private final ObjectWrapperAndUnwrapper unwrapper;
    private JspWriter jspOut;
    private ELContext elContext;

    protected FreeMarkerPageContext() throws TemplateModelException {
        environment = Environment.getCurrentEnvironment();
        incompatibleImprovements = environment.getConfiguration().getIncompatibleImprovements().intValue();

        TemplateModel appModel = environment.getGlobalVariable(
                FreemarkerServlet.KEY_APPLICATION_PRIVATE);
        if (!(appModel instanceof ServletContextHashModel)) {
            appModel = environment.getGlobalVariable(
                    FreemarkerServlet.KEY_APPLICATION);
        }
        if (appModel instanceof ServletContextHashModel) {
            this.servlet = ((ServletContextHashModel) appModel).getServlet();
        } else {
            throw new TemplateModelException("Could not find an instance of " +
                    ServletContextHashModel.class.getName() +
                    " in the data model under either the name " +
                    FreemarkerServlet.KEY_APPLICATION_PRIVATE + " or " +
                    FreemarkerServlet.KEY_APPLICATION);
        }

        TemplateModel requestModel =
                environment.getGlobalVariable(FreemarkerServlet.KEY_REQUEST_PRIVATE);
        if (!(requestModel instanceof HttpRequestHashModel)) {
            requestModel = environment.getGlobalVariable(
                    FreemarkerServlet.KEY_REQUEST);
        }
        if (requestModel instanceof HttpRequestHashModel) {
            HttpRequestHashModel reqHash = (HttpRequestHashModel) requestModel;
            request = reqHash.getRequest();
            session = request.getSession(false);
            response = reqHash.getResponse();
            wrapper = reqHash.getObjectWrapper();
            unwrapper = this.wrapper instanceof ObjectWrapperAndUnwrapper
                    ? (ObjectWrapperAndUnwrapper) this.wrapper : null;
        } else {
            throw new TemplateModelException("Could not find an instance of " +
                    HttpRequestHashModel.class.getName() +
                    " in the data model under either the name " +
                    FreemarkerServlet.KEY_REQUEST_PRIVATE + " or " +
                    FreemarkerServlet.KEY_REQUEST);
        }

        // Register page attributes as per spec
        setAttribute(REQUEST, request);
        setAttribute(RESPONSE, response);
        if (session != null)
            setAttribute(SESSION, session);
        setAttribute(PAGE, servlet);
        setAttribute(CONFIG, servlet.getServletConfig());
        setAttribute(PAGECONTEXT, this);
        setAttribute(APPLICATION, servlet.getServletContext());
    }

    ObjectWrapper getObjectWrapper() {
        return wrapper;
    }

    @Override
    public void initialize(
            Servlet servlet, ServletRequest request, ServletResponse response,
            String errorPageURL, boolean needsSession, int bufferSize,
            boolean autoFlush) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release() {
    }

    @Override
    public void setAttribute(String name, Object value) {
        setAttribute(name, value, PAGE_SCOPE);
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        switch (scope) {
            case PAGE_SCOPE: {
                try {
                    environment.setGlobalVariable(name, wrapper.wrap(value));
                    break;
                } catch (TemplateModelException e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
            case REQUEST_SCOPE: {
                getRequest().setAttribute(name, value);
                break;
            }
            case SESSION_SCOPE: {
                getSession(true).setAttribute(name, value);
                break;
            }
            case APPLICATION_SCOPE: {
                getServletContext().setAttribute(name, value);
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid scope " + scope);
            }
        }
    }

    @Override
    public Object getAttribute(String name) {
        return getAttribute(name, PAGE_SCOPE);
    }

    @Override
    public Object getAttribute(String name, int scope) {
        switch (scope) {
            case PAGE_SCOPE: {
                try {
                    final TemplateModel tm = environment.getGlobalNamespace().get(name);
                    if (incompatibleImprovements >= _VersionInts.V_2_3_22 && unwrapper != null) {
                        return unwrapper.unwrap(tm);
                    } else { // Legacy behavior branch
                        if (tm instanceof AdapterTemplateModel) {
                            return ((AdapterTemplateModel) tm).getAdaptedObject(Object.class);
                        }
                        if (tm instanceof WrapperTemplateModel) {
                            return ((WrapperTemplateModel) tm).getWrappedObject();
                        }
                        if (tm instanceof TemplateScalarModel) {
                            return ((TemplateScalarModel) tm).getAsString();
                        }
                        if (tm instanceof TemplateNumberModel) {
                            return ((TemplateNumberModel) tm).getAsNumber();
                        }
                        if (tm instanceof TemplateBooleanModel) {
                            return Boolean.valueOf(((TemplateBooleanModel) tm).getAsBoolean());
                        }
                        if (incompatibleImprovements >= _VersionInts.V_2_3_22
                                && tm instanceof TemplateDateModel) {
                            return ((TemplateDateModel) tm).getAsDate();
                        }
                        return tm;
                    }
                } catch (TemplateModelException e) {
                    throw new UndeclaredThrowableException("Failed to unwrapp FTL global variable", e);
                }
            }
            case REQUEST_SCOPE: {
                return getRequest().getAttribute(name);
            }
            case SESSION_SCOPE: {
                HttpSession session = getSession(false);
                if (session == null) {
                    return null;
                }
                return session.getAttribute(name);
            }
            case APPLICATION_SCOPE: {
                return getServletContext().getAttribute(name);
            }
            default: {
                throw new IllegalArgumentException("Invalid scope " + scope);
            }
        }
    }

    @Override
    public Object findAttribute(String name) {
        Object retval = getAttribute(name, PAGE_SCOPE);
        if (retval != null) return retval;
        retval = getAttribute(name, REQUEST_SCOPE);
        if (retval != null) return retval;
        retval = getAttribute(name, SESSION_SCOPE);
        if (retval != null) return retval;
        return getAttribute(name, APPLICATION_SCOPE);
    }

    @Override
    public void removeAttribute(String name) {
        removeAttribute(name, PAGE_SCOPE);
        removeAttribute(name, REQUEST_SCOPE);
        removeAttribute(name, SESSION_SCOPE);
        removeAttribute(name, APPLICATION_SCOPE);
    }

    @Override
    public void removeAttribute(String name, int scope) {
        switch (scope) {
            case PAGE_SCOPE: {
                environment.getGlobalNamespace().remove(name);
                break;
            }
            case REQUEST_SCOPE: {
                getRequest().removeAttribute(name);
                break;
            }
            case SESSION_SCOPE: {
                HttpSession session = getSession(false);
                if (session != null) {
                    session.removeAttribute(name);
                }
                break;
            }
            case APPLICATION_SCOPE: {
                getServletContext().removeAttribute(name);
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid scope: " + scope);
            }
        }
    }

    @Override
    public int getAttributesScope(String name) {
        if (getAttribute(name, PAGE_SCOPE) != null) return PAGE_SCOPE;
        if (getAttribute(name, REQUEST_SCOPE) != null) return REQUEST_SCOPE;
        if (getAttribute(name, SESSION_SCOPE) != null) return SESSION_SCOPE;
        if (getAttribute(name, APPLICATION_SCOPE) != null) return APPLICATION_SCOPE;
        return 0;
    }

    @Override
    public Enumeration getAttributeNamesInScope(int scope) {
        switch (scope) {
            case PAGE_SCOPE: {
                try {
                    return
                            new TemplateHashModelExEnumeration(environment.getGlobalNamespace());
                } catch (TemplateModelException e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
            case REQUEST_SCOPE: {
                return getRequest().getAttributeNames();
            }
            case SESSION_SCOPE: {
                HttpSession session = getSession(false);
                if (session != null) {
                    return session.getAttributeNames();
                }
                return Collections.enumeration(Collections.EMPTY_SET);
            }
            case APPLICATION_SCOPE: {
                return getServletContext().getAttributeNames();
            }
            default: {
                throw new IllegalArgumentException("Invalid scope " + scope);
            }
        }
    }

    @Override
    public JspWriter getOut() {
        return jspOut;
    }

    private HttpSession getSession(boolean create) {
        if (session == null) {
            session = request.getSession(create);
            if (session != null) {
                setAttribute(SESSION, session);
            }
        }
        return session;
    }

    @Override
    public HttpSession getSession() {
        return getSession(false);
    }

    @Override
    public Object getPage() {
        return servlet;
    }

    @Override
    public ServletRequest getRequest() {
        return request;
    }

    @Override
    public ServletResponse getResponse() {
        return response;
    }

    @Override
    public Exception getException() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletConfig getServletConfig() {
        return servlet.getServletConfig();
    }

    @Override
    public ServletContext getServletContext() {
        return servlet.getServletContext();
    }

    @Override
    public void forward(String url) throws ServletException, IOException {
        //TODO: make sure this is 100% correct by looking at Jasper output 
        request.getRequestDispatcher(url).forward(request, response);
    }

    @Override
    public void include(String url) throws ServletException, IOException {
        jspOut.flush();
        request.getRequestDispatcher(url).include(request, response);
    }

    @Override
    public void include(String url, boolean flush) throws ServletException, IOException {
        if (flush) {
            jspOut.flush();
        }
        final PrintWriter pw = new PrintWriter(jspOut);
        request.getRequestDispatcher(url).include(request, new HttpServletResponseWrapper(response) {
            @Override
            public PrintWriter getWriter() {
                return pw;
            }

            @Override
            public ServletOutputStream getOutputStream() {
                throw new UnsupportedOperationException("JSP-included resource must use getWriter()");
            }
        });
        pw.flush();
    }

    @Override
    public void handlePageException(Exception e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handlePageException(Throwable e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BodyContent pushBody() {
        return (BodyContent) pushWriter(new TagTransformModel.BodyContentImpl(getOut(), true));
    }

    @Override
    public JspWriter pushBody(Writer w) {
        return pushWriter(new JspWriterAdapter(w));
    }

    @Override
    public JspWriter popBody() {
        popWriter();
        return (JspWriter) getAttribute(OUT);
    }

    Object peekTopTag(Class tagClass) {
        for (ListIterator iter = tags.listIterator(tags.size()); iter.hasPrevious(); ) {
            Object tag = iter.previous();
            if (tagClass.isInstance(tag)) {
                return tag;
            }
        }
        return null;
    }

    void popTopTag() {
        tags.remove(tags.size() - 1);
    }

    void popWriter() {
        jspOut = (JspWriter) outs.remove(outs.size() - 1);
        setAttribute(OUT, jspOut);
    }

    void pushTopTag(Object tag) {
        tags.add(tag);
    }

    JspWriter pushWriter(JspWriter out) {
        outs.add(jspOut);
        jspOut = out;
        setAttribute(OUT, jspOut);
        return out;
    }

    /**
     * Attempts to locate and manufacture an expression evaulator instance. For this to work you <b>must</b> have the
     * Apache Commons-EL package in the classpath. If Commons-EL is not available, this method will throw an
     * UnsupportedOperationException.
     */
    @Override
    public ExpressionEvaluator getExpressionEvaluator() {
        try {
            Class type = ((ClassLoader) AccessController.doPrivileged(
                    new PrivilegedAction() {
                        @Override
                        public Object run() {
                            return Thread.currentThread().getContextClassLoader();
                        }
                    })).loadClass
                    ("org.apache.commons.el.ExpressionEvaluatorImpl");
            return (ExpressionEvaluator) type.newInstance();
        } catch (Exception e) {
            throw new UnsupportedOperationException("In order for the getExpressionEvaluator() " +
                    "method to work, you must have downloaded the apache commons-el jar and " +
                    "made it available in the classpath.");
        }
    }

    /**
     * Returns a variable resolver that will resolve variables by searching through the page scope, request scope,
     * session scope and application scope for an attribute with a matching name.
     */
    @Override
    public VariableResolver getVariableResolver() {
        final PageContext ctx = this;

        return new VariableResolver() {
            @Override
            public Object resolveVariable(String name) throws ELException {
                return ctx.findAttribute(name);
            }
        };
    }

    @Override
    public ELContext getELContext() {
        if (elContext == null) {
            JspApplicationContext jspctx = JspFactory.getDefaultFactory().getJspApplicationContext(getServletContext());
            if (jspctx instanceof FreeMarkerJspApplicationContext) {
                elContext = ((FreeMarkerJspApplicationContext) jspctx).createNewELContext(this);
                elContext.putContext(JspContext.class, this);
            } else {
                throw new UnsupportedOperationException(
                        "Can not create an ELContext using a foreign JspApplicationContext (of class "
                                + ClassUtil.getShortClassNameOfObject(jspctx) + ").\n" +
                                "Hint: The cause of this is often that you are trying to use JSTL tags/functions in " +
                                "FTL. "
                                + "In that case, know that that's not really suppored, and you are supposed to use FTL "
                                + "constrcuts instead, like #list instead of JSTL's forEach, etc.");
            }
        }
        return elContext;
    }

    private static class TemplateHashModelExEnumeration implements Enumeration {
        private final TemplateModelIterator it;

        private TemplateHashModelExEnumeration(TemplateHashModelEx hashEx) throws TemplateModelException {
            it = hashEx.keys().iterator();
        }

        @Override
        public boolean hasMoreElements() {
            try {
                return it.hasNext();
            } catch (TemplateModelException tme) {
                throw new UndeclaredThrowableException(tme);
            }
        }

        @Override
        public Object nextElement() {
            try {
                return ((TemplateScalarModel) it.next()).getAsString();
            } catch (TemplateModelException tme) {
                throw new UndeclaredThrowableException(tme);
            }
        }
    }
}
