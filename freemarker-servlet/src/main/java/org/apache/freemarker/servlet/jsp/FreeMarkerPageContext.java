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


package org.apache.freemarker.servlet.jsp;

import jakarta.el.ELContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.*;
import jakarta.servlet.jsp.el.ELException;
import jakarta.servlet.jsp.el.ExpressionEvaluator;
import jakarta.servlet.jsp.el.VariableResolver;
import jakarta.servlet.jsp.tagext.BodyContent;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.*;
import org.apache.freemarker.core.util.UndeclaredThrowableException;
import org.apache.freemarker.core.util._ClassUtils;
import org.apache.freemarker.servlet.FreemarkerServlet;
import org.apache.freemarker.servlet.HttpRequestHashModel;
import org.apache.freemarker.servlet.ServletContextHashModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

class FreeMarkerPageContext extends PageContext implements TemplateModel {
    private static final Logger LOG = LoggerFactory.getLogger(FreeMarkerPageContext.class);

    private final Environment environment;
    private List tags = new ArrayList();
    private List outs = new ArrayList();
    private final GenericServlet servlet;
    private HttpSession session;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ObjectWrapperAndUnwrapper wrapper;
    private JspWriter jspOut;

    static {
        if (JspFactory.getDefaultFactory() == null) {
            JspFactory.setDefaultFactory(new FreeMarkerJspFactory());
        }
        LOG.debug("Using JspFactory implementation class {}",
                JspFactory.getDefaultFactory().getClass().getName());
    }

    protected FreeMarkerPageContext() throws TemplateException {
        environment = Environment.getCurrentEnvironment();

        TemplateModel appModel = environment.getGlobalVariable(
                FreemarkerServlet.KEY_APPLICATION_PRIVATE);
        if (!(appModel instanceof ServletContextHashModel)) {
            appModel = environment.getGlobalVariable(
                    FreemarkerServlet.KEY_APPLICATION);
        }
        if (appModel instanceof ServletContextHashModel) {
            servlet = ((ServletContextHashModel) appModel).getServlet();
        } else {
            throw new  TemplateException("Could not find an instance of " +
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
            ObjectWrapperAndUnwrapper ow = reqHash.getObjectWrapper();
            wrapper = ow;
        } else {
            throw new  TemplateException("Could not find an instance of " +
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
            
    ObjectWrapperAndUnwrapper getObjectWrapper() {
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
        switch(scope) {
            case PAGE_SCOPE: {
                try {
                    environment.setGlobalVariable(name, wrapper.wrap(value));
                    break;
                } catch (ObjectWrappingException e) {
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
                    return wrapper.unwrap(environment.getGlobalNamespace().get(name));
                } catch (TemplateException e) {
                    throw new UndeclaredThrowableException("Failed to unwrap FTL global variable", e);
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
        switch(scope) {
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
        switch(scope) {
            case PAGE_SCOPE: {
                try {
                    return 
                        new TemplateHashModelExEnumeration(environment.getGlobalNamespace());
                } catch (TemplateException e) {
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
      return (BodyContent) pushWriter(new BodyContentImpl(getOut(), true));
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
     * Attempts to locate and manufacture an expression evaulator instance. For this
     * to work you <b>must</b> have the Apache Commons-EL package in the classpath. If
     * Commons-EL is not available, this method will throw an UnsupportedOperationException.
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
     * Returns a variable resolver that will resolve variables by searching through
     * the page scope, request scope, session scope and application scope for an
     * attribute with a matching name.
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

    private ELContext elContext;

    @Override
    public ELContext getELContext() {
        if (elContext == null) {
            JspApplicationContext jspctx = JspFactory.getDefaultFactory().getJspApplicationContext(getServletContext());
            if (jspctx instanceof FreeMarkerJspApplicationContext) {
                elContext = ((FreeMarkerJspApplicationContext) jspctx).createNewELContext(this);
                elContext.putContext(JspContext.class, this);
            } else {
                throw new UnsupportedOperationException(
                        "Can not invoke an ELContext using a foreign JspApplicationContext (of class "
                                + _ClassUtils.getShortClassNameOfObject(jspctx) + ").\n" +
                                "Hint: The cause of this is often that you are trying to use JSTL tags/functions in FTL. "
                                + "In that case, know that that's not really suppored, and you are supposed to use FTL "
                                + "constrcuts instead, like #list instead of JSTL's forEach, etc.");
            }
        }
        return elContext;
    }

    private static class TemplateHashModelExEnumeration implements Enumeration {
        private final TemplateModelIterator it;
            
        private TemplateHashModelExEnumeration(TemplateHashModelEx hashEx) throws TemplateException {
            it = hashEx.keys().iterator();
        }
        
        @Override
        public boolean hasMoreElements() {
            try {
                return it.hasNext();
            } catch (TemplateException tme) {
                throw new UndeclaredThrowableException(tme);
            }
        }
        
        @Override
        public Object nextElement() {
            try {
                if (!it.hasNext()) {
                    throw new NoSuchElementException();
                }
                return ((TemplateStringModel) it.next()).getAsString();
            } catch (TemplateException tme) {
                throw new UndeclaredThrowableException(tme);
            }
        }
    }
}
