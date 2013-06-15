/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.ext.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.core.Configurable;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.StringUtil;

/**
 * <p>This is a general-purpose FreeMarker view servlet.</p>
 * 
 * <p>The main features are:
 * 
 * <ul>
 * 
 * <li>It makes all request, request parameters, session, and servlet
 * context attributes available to templates through <code>Request</code>,
 * <code>RequestParameters</code>, <code>Session</code>, and <code>Application</code>
 * variables.
 * 
 * <li>The scope variables are also available via automatic scope discovery. That is,
 * writing <code>Application.attrName</code>, <code>Session.attrName</code>,
 * <code>Request.attrName</code> is not mandatory; it's enough to write <code>attrName</code>,
 * and if no such variable was created in the template, it will search the
 * variable in <code>Request</code>, and then in <code>Session</code>,
 * and finally in <code>Application</code>.  
 * 
 * <li>It creates a variable with name <code>JspTaglibs</code>, that can be used
 * to load JSP taglibs. For example:<br>
 * <code>&lt;#assign tiles=JspTaglibs["/WEB-INF/struts-tiles.tld"]></code>
 * 
 * <li>A custom directive named <code>include_page</code> allows you to 
 * include the output of another servlet resource from your servlet container,
 * just as if you used <code>ServletRequest.getRequestDispatcher(path).include()</code>:<br>
 * <code>&lt;@include_page path="/myWebapp/somePage.jsp"/></code>. You can also
 * pass parameters to the newly included page by passing a hash named 'params':
 * <code>&lt;@include_page path="/myWebapp/somePage.jsp" params={lang: "en", q="5"}/></code>.
 * By default, the request parameters of the original request (the one being
 * processed by FreemarkerServlet) are also inherited by the include. You can
 * explicitly control this inheritance using the 'inherit_params' parameter:
 * <code>&lt;@include_page path="/myWebapp/somePage.jsp" params={lang: "en", q="5"} inherit_params=false/></code>.
 * </ul>
 * 
 * <p>The servlet will rethrow the errors occurring during template processing,
 * wrapped into <code>ServletException</code>, except if the class name of the
 * class set by the <code>template_exception_handler</code> contains the
 * substring <code>"Debug"</code>. If it contains <code>"Debug"</code>, then it
 * is assumed that the template exception handler prints the error message to the
 * page, thus <code>FreemarkerServlet</code> will suppress the  exception, and
 * logs the problem with the servlet logger
 * (<code>javax.servlet.GenericServlet.log(...)</code>). 
 * 
 * <p>Supported init-params are:</p>
 * 
 * <ul>
 * 
 * <li><strong>TemplatePath</strong> specifies the location of the templates.
 * By default, this is interpreted as web application directory relative URI.<br>
 * Alternatively, you can prepend it with <tt>file://</tt> to indicate a literal
 * path in the file system (i.e. <tt>file:///var/www/project/templates/</tt>). 
 * Note that three slashes were used to specify an absolute path.<br>
 * Also, you can prepend it with <tt>class://path/to/template/package</tt> to
 * indicate that you want to load templates from the specified package
 * accessible through the classloader of the servlet.<br>
 * Default value is <tt>class://</tt> (that is, the root of the class hierarchy).
 * <i>Note that this default is different than the default in FreeMarker 1.x, when
 * it defaulted <tt>/</tt>, that is to loading from the webapp root directory.</i></li>
 * 
 * <li><strong>NoCache</strong> if set to true, generates headers in the response
 * that advise the HTTP client not to cache the returned page.
 * The default is <tt>false</tt>.</li>
 * 
 * <li><strong>ContentType</strong> if specified, response uses the specified
 * Content-type HTTP header. The value may include the charset (e.g.
 * <tt>"text/html; charset=ISO-8859-1"</tt>). If not specified, <tt>"text/html"</tt>
 * is used. If the charset is not specified in this init-param, then the charset
 * (encoding) of the actual template file will be used (in the response HTTP header
 * and for encoding the output stream). Note that this setting can be overridden
 * on a per-template basis by specifying a custom attribute named 
 * <tt>content_type</tt> in the <tt>attributes</tt> parameter of the 
 * <tt>&lt;#ftl></tt> directive. 
 * </li>
 * 
 * <li>The following init-params are supported only for backward compatibility, and
 * their usage is discouraged: TemplateUpdateInterval, DefaultEncoding,
 * ObjectWrapper, TemplateExceptionHandler. Use setting init-params such as
 * object_wrapper instead. 
 * 
 * <li>Any other init-param will be interpreted as <code>Configuration</code>
 * level setting. See {@link Configuration#setSetting}</li>
 * 
 * </ul>
 * 
 * @author Attila Szegedi
 */

public class FreemarkerServlet extends HttpServlet
{
    private static final Logger logger = Logger.getLogger("freemarker.servlet");
    
    public static final long serialVersionUID = -2440216393145762479L;

    private static final String INITPARAM_TEMPLATE_PATH = "TemplatePath";
    private static final String INITPARAM_NOCACHE = "NoCache";
    private static final String INITPARAM_CONTENT_TYPE = "ContentType";
    private static final String DEFAULT_CONTENT_TYPE = "text/html";
    private static final String INITPARAM_DEBUG = "Debug";
    
    private static final String DEPR_INITPARAM_TEMPLATE_DELAY = "TemplateDelay";
    private static final String DEPR_INITPARAM_ENCODING = "DefaultEncoding";
    private static final String DEPR_INITPARAM_OBJECT_WRAPPER = "ObjectWrapper";
    private static final String DEPR_INITPARAM_WRAPPER_SIMPLE = "simple";
    private static final String DEPR_INITPARAM_WRAPPER_BEANS = "beans";
    private static final String DEPR_INITPARAM_WRAPPER_JYTHON = "jython";
    private static final String DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER = "TemplateExceptionHandler";
    private static final String DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_RETHROW = "rethrow";
    private static final String DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_DEBUG = "debug";
    private static final String DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_HTML_DEBUG = "htmlDebug";
    private static final String DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_IGNORE = "ignore";
    private static final String DEPR_INITPARAM_DEBUG = "debug";

    public static final String KEY_REQUEST = "Request";
    public static final String KEY_INCLUDE = "include_page";
    public static final String KEY_REQUEST_PRIVATE = "__FreeMarkerServlet.Request__";
    public static final String KEY_REQUEST_PARAMETERS = "RequestParameters";
    public static final String KEY_SESSION = "Session";
    public static final String KEY_APPLICATION = "Application";
    public static final String KEY_APPLICATION_PRIVATE = "__FreeMarkerServlet.Application__";
    public static final String KEY_JSP_TAGLIBS = "JspTaglibs";

    // Note these names start with dot, so they're essentially invisible from
    // a freemarker script.
    private static final String ATTR_REQUEST_MODEL = ".freemarker.Request";
    private static final String ATTR_REQUEST_PARAMETERS_MODEL =
        ".freemarker.RequestParameters";
    private static final String ATTR_SESSION_MODEL = ".freemarker.Session";
    private static final String ATTR_APPLICATION_MODEL =
        ".freemarker.Application";
    private static final String ATTR_JSP_TAGLIBS_MODEL =
        ".freemarker.JspTaglibs";

    private static final String EXPIRATION_DATE;

    static {
        // Generate expiration date that is one year from now in the past
        GregorianCalendar expiration = new GregorianCalendar();
        expiration.roll(Calendar.YEAR, -1);
        SimpleDateFormat httpDate =
            new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z",
                java.util.Locale.US);
        EXPIRATION_DATE = httpDate.format(expiration.getTime());
    }

    private String templatePath;
    private boolean nocache;
    protected boolean debug;
    private Configuration config;
    private ObjectWrapper wrapper;
    private String contentType;
    private boolean noCharsetInContentType;
    
    public void init() throws ServletException {
        try {
            config = createConfiguration();
            
            // Set defaults:
            config.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
            contentType = DEFAULT_CONTENT_TYPE;
            
            // Process object_wrapper init-param out of order: 
            wrapper = createObjectWrapper();
            if (logger.isDebugEnabled()) {
                logger.debug("Using object wrapper of class " + wrapper.getClass().getName());
            }
            config.setObjectWrapper(wrapper);
            
            // Process TemplatePath init-param out of order:
            templatePath = getInitParameter(INITPARAM_TEMPLATE_PATH);
            if (templatePath == null)
                templatePath = "class://";
            config.setTemplateLoader(createTemplateLoader(templatePath));

            // Process all other init-params:
            Enumeration initpnames = getServletConfig().getInitParameterNames();
            while (initpnames.hasMoreElements()) {
                String name = (String) initpnames.nextElement();
                String value = getInitParameter(name);
                
                if (name == null) {
                    throw new ServletException(
                            "init-param without param-name. "
                            + "Maybe the web.xml is not well-formed?");
                }
                if (value == null) {
                    throw new ServletException(
                            "init-param without param-value. "
                            + "Maybe the web.xml is not well-formed?");
                }
                
                if (name.equals(DEPR_INITPARAM_OBJECT_WRAPPER)
                        || name.equals(Configurable.OBJECT_WRAPPER_KEY)
                        || name.equals(INITPARAM_TEMPLATE_PATH)) {
                    // ignore: we have already processed these
                } else if (name.equals(DEPR_INITPARAM_ENCODING)) { // BC
                    if (getInitParameter(Configuration.DEFAULT_ENCODING_KEY) != null) {
                        throw new ServletException(
                                "Conflicting init-params: "
                                + Configuration.DEFAULT_ENCODING_KEY + " and "
                                + DEPR_INITPARAM_ENCODING);
                    }
                    config.setDefaultEncoding(value);
                } else if (name.equals(DEPR_INITPARAM_TEMPLATE_DELAY)) { // BC
                    if (getInitParameter(Configuration.TEMPLATE_UPDATE_DELAY_KEY) != null) {
                        throw new ServletException(
                                "Conflicting init-params: "
                                + Configuration.TEMPLATE_UPDATE_DELAY_KEY + " and "
                                + DEPR_INITPARAM_TEMPLATE_DELAY);
                    }
                    try {
                        config.setTemplateUpdateDelay(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // Intentionally ignored
                    }
                } else if (name.equals(DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER)) { // BC
                    if (getInitParameter(Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY) != null) {
                        throw new ServletException(
                                "Conflicting init-params: "
                                + Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY + " and "
                                + DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER);
                    }

                    if (DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_RETHROW.equals(value)) {
                        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
                    } else if (DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_DEBUG.equals(value)) {
                        config.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);
                    } else if  (DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_HTML_DEBUG.equals(value)) {
                        config.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
                    } else if  (DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_IGNORE.equals(value)) {
                        config.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
                    } else {
                        throw new ServletException(
                                "Invalid value for servlet init-param "
                                + DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER + ": " + value);
                    }
                } else if (name.equals(INITPARAM_NOCACHE)) {
                    nocache = StringUtil.getYesNo(value);
                } else if (name.equals(DEPR_INITPARAM_DEBUG)) { // BC
                    if (getInitParameter(INITPARAM_DEBUG) != null) {
                        throw new ServletException(
                                "Conflicting init-params: "
                                + INITPARAM_DEBUG + " and "
                                + DEPR_INITPARAM_DEBUG);
                    }
                    debug = StringUtil.getYesNo(value);
                } else if (name.equals(INITPARAM_DEBUG)) {
                    debug = StringUtil.getYesNo(value);
                } else if (name.equals(INITPARAM_CONTENT_TYPE)) {
                    contentType = value;
                } else {
                    config.setSetting(name, value);
                }
            } // while initpnames
            
            noCharsetInContentType = true;
            int i = contentType.toLowerCase().indexOf("charset=");
            if (i != -1) {
                char c = ' ';
                i--;
                while (i >= 0) {
                    c = contentType.charAt(i);
                    if (!Character.isWhitespace(c)) break;
                    i--;
                }
                if (i == -1 || c == ';') {
                    noCharsetInContentType = false;
                }
            }
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Create the template loader. The default implementation will create a
     * {@link ClassTemplateLoader} if the template path starts with "class://",
     * a {@link FileTemplateLoader} if the template path starts with "file://",
     * and a {@link WebappTemplateLoader} otherwise.
     * @param templatePath the template path to create a loader for
     * @return a newly created template loader
     * @throws IOException
     */
    protected TemplateLoader createTemplateLoader(String templatePath) throws IOException
    {
        if (templatePath.startsWith("class://")) {
            // substring(7) is intentional as we "reuse" the last slash
            return new ClassTemplateLoader(getClass(), templatePath.substring(7));
        } else {
            if (templatePath.startsWith("file://")) {
                templatePath = templatePath.substring(7);
                return new FileTemplateLoader(new File(templatePath));
            } else {
                return new WebappTemplateLoader(this.getServletContext(), templatePath);
            }
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        process(request, response);
    }

    public void doPost(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException
    {
        process(request, response);
    }

    private void process(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException
    {
        // Give chance to subclasses to perform preprocessing
        if (preprocessRequest(request, response)) {
            return;
        }

        String path = requestUrlToTemplatePath(request);

        if (debug) {
            log("Requested template: " + StringUtil.jQuoteNoXSS(path));
        }

        Template template = null;
        try {
            template = config.getTemplate(
                    path,
                    deduceLocale(path, request, response));
        } catch (FileNotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        Object attrContentType = template.getCustomAttribute("content_type");
        if(attrContentType != null) {
            response.setContentType(attrContentType.toString());
        }
        else {
            if (noCharsetInContentType) {
                response.setContentType(
                        contentType + "; charset=" + template.getEncoding());
            } else {
                response.setContentType(contentType);
            }
        }

        // Set cache policy
        setBrowserCachingPolicy(response);

        ServletContext servletContext = getServletContext();
        try {
            TemplateModel model = createModel(wrapper, servletContext, request, response);

            // Give subclasses a chance to hook into preprocessing
            if (preTemplateProcess(request, response, template, model)) {
                try {
                    // Process the template
                    template.process(model, response.getWriter());
                } finally {
                    // Give subclasses a chance to hook into postprocessing
                    postTemplateProcess(request, response, template, model);
                }
            }
        } catch (TemplateException te) {
            if (config.getTemplateExceptionHandler()
                        .getClass().getName().indexOf("Debug") != -1) {
                this.log("Error executing FreeMarker template", te);
            } else {
                ServletException e = new ServletException(
                        "Error executing FreeMarker template", te);
                // Attempt to set init cause, but don't freak out if the method
                // is not available (i.e. pre-1.4 JRE). This is required as the
                // constructor-passed throwable won't show up automatically in
                // stack traces.
                try {
                    e.getClass().getMethod("initCause",
                            new Class[] { Throwable.class }).invoke(e,
                            new Object[] { te });
                } catch (Exception ex) {
                    // Can't set init cause, we're probably running on a pre-1.4
                    // JDK, oh well...
                }
                throw e;
            }
        }
    }
    
    /**
     * Returns the locale used for the 
     * {@link Configuration#getTemplate(String, Locale)} call.
     * The base implementation simply returns the locale setting of the
     * configuration. Override this method to provide different behaviour, i.e.
     * to use the locale indicated in the request.
     */
    protected Locale deduceLocale(
            String templatePath, HttpServletRequest request, HttpServletResponse response) {
        return config.getLocale();
    }

    protected TemplateModel createModel(ObjectWrapper wrapper,
                                        ServletContext servletContext,
                                        final HttpServletRequest request,
                                        final HttpServletResponse response) throws TemplateModelException {
        try {
            AllHttpScopesHashModel params = new AllHttpScopesHashModel(wrapper, servletContext, request);
    
            // Create hash model wrapper for servlet context (the application)
            ServletContextHashModel servletContextModel =
                (ServletContextHashModel) servletContext.getAttribute(
                    ATTR_APPLICATION_MODEL);
            if (servletContextModel == null)
            {
                servletContextModel = new ServletContextHashModel(this, wrapper);
                servletContext.setAttribute(
                    ATTR_APPLICATION_MODEL,
                    servletContextModel);
                TaglibFactory taglibs = new TaglibFactory(servletContext);
                servletContext.setAttribute(
                    ATTR_JSP_TAGLIBS_MODEL,
                    taglibs);
                initializeServletContext(request, response);
            }
            params.putUnlistedModel(KEY_APPLICATION, servletContextModel);
            params.putUnlistedModel(KEY_APPLICATION_PRIVATE, servletContextModel);
            params.putUnlistedModel(KEY_JSP_TAGLIBS, (TemplateModel)servletContext.getAttribute(ATTR_JSP_TAGLIBS_MODEL));
            // Create hash model wrapper for session
            HttpSessionHashModel sessionModel;
            HttpSession session = request.getSession(false);
            if(session != null) {
                sessionModel = (HttpSessionHashModel) session.getAttribute(ATTR_SESSION_MODEL);
                if (sessionModel == null || sessionModel.isOrphaned(session)) {
                    sessionModel = new HttpSessionHashModel(session, wrapper);
                    initializeSessionAndInstallModel(request, response, 
                            sessionModel, session);
                }
            }
            else {
                sessionModel = new HttpSessionHashModel(this, request, response, wrapper);
            }
            params.putUnlistedModel(KEY_SESSION, sessionModel);
    
            // Create hash model wrapper for request
            HttpRequestHashModel requestModel =
                (HttpRequestHashModel) request.getAttribute(ATTR_REQUEST_MODEL);
            if (requestModel == null || requestModel.getRequest() != request)
            {
                requestModel = new HttpRequestHashModel(request, response, wrapper);
                request.setAttribute(ATTR_REQUEST_MODEL, requestModel);
                request.setAttribute(
                    ATTR_REQUEST_PARAMETERS_MODEL,
                    createRequestParametersHashModel(request));
            }
            params.putUnlistedModel(KEY_REQUEST, requestModel);
            params.putUnlistedModel(KEY_INCLUDE, new IncludePage(request, response));
            params.putUnlistedModel(KEY_REQUEST_PRIVATE, requestModel);
    
            // Create hash model wrapper for request parameters
            HttpRequestParametersHashModel requestParametersModel =
                (HttpRequestParametersHashModel) request.getAttribute(
                    ATTR_REQUEST_PARAMETERS_MODEL);
            params.putUnlistedModel(KEY_REQUEST_PARAMETERS, requestParametersModel);
            return params;
        } catch (ServletException e) {
            throw new TemplateModelException(e);
        } catch (IOException e) {
            throw new TemplateModelException(e);
        }
    }

    void initializeSessionAndInstallModel(HttpServletRequest request,
            HttpServletResponse response, HttpSessionHashModel sessionModel, 
            HttpSession session)
            throws ServletException, IOException
    {
        session.setAttribute(ATTR_SESSION_MODEL, sessionModel);
        initializeSession(request, response);
    }

    /**
     * Maps the request URL to a template path that is passed to 
     * {@link Configuration#getTemplate(String, Locale)}. You can override it
     * (i.e. to provide advanced rewriting capabilities), but you are strongly
     * encouraged to call the overridden method first, then only modify its
     * return value. 
     * @param request the currently processed request
     * @return a String representing the template path
     */
    protected String requestUrlToTemplatePath(HttpServletRequest request)
    {
        // First, see if it's an included request
        String includeServletPath  = (String) request.getAttribute("javax.servlet.include.servlet_path");
        if(includeServletPath != null)
        {
            // Try path info; only if that's null (servlet is mapped to an
            // URL extension instead of to prefix) use servlet path.
            String includePathInfo = (String) request.getAttribute("javax.servlet.include.path_info");
            return includePathInfo == null ? includeServletPath : includePathInfo;
        } 
        // Seems that the servlet was not called as the result of a 
        // RequestDispatcher.include(...). Try pathInfo then servletPath again,
        // only now directly on the request object:
        String path = request.getPathInfo();
        if (path != null) return path;
        path = request.getServletPath();
        if (path != null) return path;
        // Seems that it's a servlet mapped with prefix, and there was no extra path info.
        return "";
    }

    /**
     * Called as the first step in request processing, before the templating mechanism
     * is put to work. By default does nothing and returns false. This method is
     * typically overridden to manage serving of non-template resources (i.e. images)
     * that reside in the template directory.
     * @param request the HTTP request
     * @param response the HTTP response
     * @return true to indicate this method has processed the request entirely,
     * and that the further request processing should not take place.
     */
    protected boolean preprocessRequest(
        HttpServletRequest request,
        HttpServletResponse response)
            throws ServletException, IOException {
        return false;
    }

    /**
     * This method is called from {@link #init()} to create the
     * FreeMarker configuration object that this servlet will use
     * for template loading. This is a hook that allows you
     * to custom-configure the configuration object in a subclass.
     * The default implementation returns a new {@link Configuration}
     * instance.
     */
    protected Configuration createConfiguration() {
        return new Configuration();
    }
    
    /**
     * This method is called from {@link #init()} to create the
     * FreeMarker object wrapper object that this servlet will use
     * for adapting request, session, and servlet context attributes into 
     * template models.. This is a hook that allows you
     * to custom-configure the wrapper object in a subclass.
     * The default implementation returns a wrapper that depends on the value
     * of <code>ObjectWrapper</code> init parameter. If <code>simple</code> is
     * specified, {@link ObjectWrapper#SIMPLE_WRAPPER} is used; if <code>jython</code>
     * is specified, {@link freemarker.ext.jython.JythonWrapper} is used. In
     * every other case {@link ObjectWrapper#DEFAULT_WRAPPER} is used.
     */
    protected ObjectWrapper createObjectWrapper() {
        String wrapper = getServletConfig().getInitParameter(DEPR_INITPARAM_OBJECT_WRAPPER);
        if (wrapper != null) { // BC
            if (getInitParameter(Configurable.OBJECT_WRAPPER_KEY) != null) {
                throw new RuntimeException("Conflicting init-params: "
                        + Configurable.OBJECT_WRAPPER_KEY + " and "
                        + DEPR_INITPARAM_OBJECT_WRAPPER);
            }
            if (DEPR_INITPARAM_WRAPPER_BEANS.equals(wrapper)) {
                return ObjectWrapper.BEANS_WRAPPER;
            }
            if(DEPR_INITPARAM_WRAPPER_SIMPLE.equals(wrapper)) {
                return ObjectWrapper.SIMPLE_WRAPPER;
            }
            if(DEPR_INITPARAM_WRAPPER_JYTHON.equals(wrapper)) {
                // Avoiding compile-time dependency on Jython package
                try {
                    return (ObjectWrapper) Class.forName("freemarker.ext.jython.JythonWrapper")
                            .newInstance();
                } catch (InstantiationException e) {
                    throw new InstantiationError(e.getMessage());
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                } catch (ClassNotFoundException e) {
                    throw new NoClassDefFoundError(e.getMessage());
                }
            }
//            return BeansWrapper.getDefaultInstance();
            return ObjectWrapper.DEFAULT_WRAPPER;
        } else {
            wrapper = getInitParameter(Configurable.OBJECT_WRAPPER_KEY);
            if (wrapper == null) {
//                return BeansWrapper.getDefaultInstance();
                return ObjectWrapper.DEFAULT_WRAPPER;
            } else {
                try {
                    config.setSetting(Configurable.OBJECT_WRAPPER_KEY, wrapper);
                } catch (TemplateException e) {
                    throw new RuntimeException(e.toString());
                }
                return config.getObjectWrapper();
            }
        }
    }
    
    protected ObjectWrapper getObjectWrapper() {
        return wrapper;
    }
    
    protected final String getTemplatePath() {
        return templatePath;
    }

    protected HttpRequestParametersHashModel createRequestParametersHashModel(HttpServletRequest request) {
        return new HttpRequestParametersHashModel(request);
    }

    /**
     * Called when servlet detects in a request processing that
     * application-global (that is, ServletContext-specific) attributes are not yet
     * set.
     * This is a generic hook you might use in subclasses to perform a specific
     * action on first request in the context. By default it does nothing.
     * @param request the actual HTTP request
     * @param response the actual HTTP response
     */
    protected void initializeServletContext(
        HttpServletRequest request,
        HttpServletResponse response)
            throws ServletException, IOException {
    }

    /**
     * Called when servlet detects in a request processing that session-global 
     * (that is, HttpSession-specific) attributes are not yet set.
     * This is a generic hook you might use in subclasses to perform a specific
     * action on first request in the session. By default it does nothing. It
     * is only invoked on newly created sessions; it's not invoked when a
     * replicated session is reinstantiated in another servlet container.
     * 
     * @param request the actual HTTP request
     * @param response the actual HTTP response
     */
    protected void initializeSession(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException
    {
    }

    /**
     * Called before the execution is passed to template.process().
     * This is a generic hook you might use in subclasses to perform a specific
     * action before the template is processed. By default does nothing.
     * A typical action to perform here is to inject application-specific
     * objects into the model root
     *
     * <p>Example: Expose the Serlvet context path as "baseDir" for all templates:
     *
     *<pre>
     *    ((SimpleHash) data).put("baseDir", request.getContextPath() + "/");
     *    return true;
     *</pre>
     *
     * @param request the actual HTTP request
     * @param response the actual HTTP response
     * @param template the template that will get executed
     * @param data the data that will be passed to the template. By default this will be
     *        an {@link AllHttpScopesHashModel} (which is a {@link freemarker.template.SimpleHash} subclass).
     *        Thus, you can add new variables to the data-model with the
     *        {@link freemarker.template.SimpleHash#put(String, Object)} subclass) method.
     * @return true to process the template, false to suppress template processing.
     */
    protected boolean preTemplateProcess(
        HttpServletRequest request,
        HttpServletResponse response,
        Template template,
        TemplateModel data)
        throws ServletException, IOException
    {
        return true;
    }

    /**
     * Called after the execution returns from template.process().
     * This is a generic hook you might use in subclasses to perform a specific
     * action after the template is processed. It will be invoked even if the
     * template processing throws an exception. By default does nothing.
     * @param request the actual HTTP request
     * @param response the actual HTTP response
     * @param template the template that was executed
     * @param data the data that was passed to the template
     */
    protected void postTemplateProcess(
        HttpServletRequest request,
        HttpServletResponse response,
        Template template,
        TemplateModel data)
        throws ServletException, IOException
    {
    }
    
    /**
     * Returns the {@link freemarker.template.Configuration} object used by this servlet.
     * Please don't forget that {@link freemarker.template.Configuration} is not thread-safe
     * when you modify it.
     */
    protected Configuration getConfiguration() {
        return config;
    }

    /**
     * If the parameter "nocache" was set to true, generate a set of headers
     * that will advise the HTTP client not to cache the returned page.
     */
    private void setBrowserCachingPolicy(HttpServletResponse res)
    {
        if (nocache)
        {
            // HTTP/1.1 + IE extensions
            res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, "
                    + "post-check=0, pre-check=0");
            // HTTP/1.0
            res.setHeader("Pragma", "no-cache");
            // Last resort for those that ignore all of the above
            res.setHeader("Expires", EXPIRATION_DATE);
        }
    }
}
