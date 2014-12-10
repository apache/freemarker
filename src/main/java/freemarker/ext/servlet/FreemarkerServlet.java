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

package freemarker.ext.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
import freemarker.ext.jsp.TaglibFactory.ClasspathMetaInfTldSource;
import freemarker.ext.jsp.TaglibFactory.ClearMetaInfTldSource;
import freemarker.ext.jsp.TaglibFactory.MetaInfTldSource;
import freemarker.ext.jsp.TaglibFactory.WebInfPerLibJarMetaInfTldSource;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.SecurityUtilities;
import freemarker.template.utility.StringUtil;

/**
 * <p>
 * This is a general-purpose FreeMarker view servlet.
 * </p>
 * 
 * <p>
 * The main features are:
 * 
 * <ul>
 * 
 * <li>It makes all request, request parameters, session, and servlet context attributes available to templates through
 * <code>Request</code>, <code>RequestParameters</code>, <code>Session</code>, and <code>Application</code> variables.
 * 
 * <li>The scope variables are also available via automatic scope discovery. That is, writing
 * <code>Application.attrName</code>, <code>Session.attrName</code>, <code>Request.attrName</code> is not mandatory;
 * it's enough to write <code>attrName</code>, and if no such variable was created in the template, it will search the
 * variable in <code>Request</code>, and then in <code>Session</code>, and finally in <code>Application</code>.
 * 
 * <li>It creates a variable with name <code>JspTaglibs</code> that can be used to load JSP taglibs. For example:<br>
 * <code>&lt;#assign dt=JspTaglibs["http://displaytag.sf.net"]&gt;</code> or
 * <code>&lt;#assign tiles=JspTaglibs["/WEB-INF/struts-tiles.tld"]&gt;</code>.
 * 
 * <li>A custom directive named <code>include_page</code> allows you to include the output of another servlet resource
 * from your servlet container, just as if you used <code>ServletRequest.getRequestDispatcher(path).include()</code>:<br>
 * <code>&lt;@include_page path="/myWebapp/somePage.jsp"/&gt;</code>. You can also pass parameters to the newly included
 * page by passing a hash named 'params':
 * <code>&lt;@include_page path="/myWebapp/somePage.jsp" params={lang: "en", q="5"}/&gt;</code>. By default, the request
 * parameters of the original request (the one being processed by FreemarkerServlet) are also inherited by the include.
 * You can explicitly control this inheritance using the 'inherit_params' parameter:
 * <code>&lt;@include_page path="/myWebapp/somePage.jsp" params={lang: "en", q="5"} inherit_params=false/&gt;</code>.
 * </ul>
 * 
 * <p>
 * The servlet will rethrow the errors occurring during template processing, wrapped into <code>ServletException</code>,
 * except if the class name of the class set by the <code>template_exception_handler</code> contains the substring
 * <code>"Debug"</code>. If it contains <code>"Debug"</code>, then it is assumed that the template exception handler
 * prints the error message to the page, thus <code>FreemarkerServlet</code> will suppress the exception, and logs the
 * problem with the servlet logger (<code>javax.servlet.GenericServlet.log(...)</code>).
 * 
 * <p>
 * Supported init-params are:
 * </p>
 * 
 * <ul>
 * 
 * <li><strong>{@value #INIT_PARAM_TEMPLATE_PATH}</strong>: Specifies the location of the templates. By default, this is
 * interpreted as web application directory relative URI.<br>
 * Alternatively, you can prepend it with <tt>file://</tt> to indicate a literal path in the file system (i.e.
 * <tt>file:///var/www/project/templates/</tt>). Note that three slashes were used to specify an absolute path.<br>
 * Also, you can prepend it with <tt>class://path/to/template/package</tt> to indicate that you want to load templates
 * from the specified package accessible through the classloader of the servlet.<br>
 * Default value is <tt>class://</tt> (that is, the root of the class hierarchy). <i>Note that this default is different
 * than the default in FreeMarker 1.x, when it defaulted <tt>/</tt>, that is to loading from the webapp root
 * directory.</i></li>
 * 
 * <li><strong>{@value #INIT_PARAM_NO_CACHE}</strong>: If set to true, generates headers in the response that advise the
 * HTTP client not to cache the returned page. The default is <tt>false</tt>.</li>
 * 
 * <li><strong>{@value #INIT_PARAM_CONTENT_TYPE}</strong>: If specified, response uses the specified Content-type HTTP
 * header. The value may include the charset (e.g. <tt>"text/html; charset=ISO-8859-1"</tt>). If not specified,
 * <tt>"text/html"</tt> is used. If the charset is not specified in this init-param, then the charset (encoding) of the
 * actual template file will be used (in the response HTTP header and for encoding the output stream). Note that this
 * setting can be overridden on a per-template basis by specifying a custom attribute named <tt>content_type</tt> in the
 * <tt>attributes</tt> parameter of the <tt>&lt;#ftl&gt;</tt> directive.</li>
 * 
 * <li><strong>{@value #INIT_PARAM_META_INF_TLD_LOCATIONS}</strong> (since 2.3.22): Comma separated list of items which
 * are either: {@value #META_INF_TLD_LOCATION_WEB_INF_PER_LIB_JARS}, {@value #META_INF_TLD_LOCATION_CLASSPATH}
 * optionally followed by colon and a regular expression, or {@value #META_INF_TLD_LOCATION_CLEAR}. For example
 * {@code <param-value>classpath:.*myoverride.*\.jar$, webInfPerLibJars, classpath:.*taglib.*\.jar$</param-value>}, or
 * {@code <param-value>classpath</param-value>}. (Whitespace around the commas and list items will be ignored.) See
 * {@link TaglibFactory#setMetaInfTldSources(List)} for more information. Defaults to not set, in which case
 * {@link TaglibFactory} will behave as if it was {@value #META_INF_TLD_LOCATION_WEB_INF_PER_LIB_JARS}. Note that this
 * can be also specified with the {@value #SYSTEM_PROPERTY_META_INF_TLD_SOURCES} system property. If both the init-param
 * and the system property exists, the sources listed in system property will be added after those specified by the
 * init-param. This is where the special item, {@value #META_INF_TLD_LOCATION_CLEAR} comes handy, as it will remove all
 * previous list items. (An intended usage of the system property is setting it to {@code clear, classpath} in the
 * Eclipse run configuration if you are running the application without making a WAR out of it.) Also, note that further
 * {@code classpath:<pattern>} items are added automatically at the end of this list based on Jetty's
 * {@code "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern"} servlet context attribute.</li>
 * 
 * <li><strong>{@value #INIT_PARAM_CLASSPATH_TLDS}</strong> (since 2.3.22): Comma separated list of paths; see
 * {@link TaglibFactory#setClasspathTlds(List)}. Whitespace around the list items will be ignored. Defaults to no paths.
 * Note that this can be also specified with the {@value #SYSTEM_PROPERTY_CLASSPATH_TLDS} system property. If both the
 * init-param and the system property exists, the items listed in system property will be added after those specified by
 * the init-param.</li>
 * 
 * <li>The following init-params are supported only for backward compatibility, and their usage is discouraged:
 * {@code TemplateUpdateInterval}, {@code DefaultEncoding}, {@code ObjectWrapper}, {@code TemplateExceptionHandler}. Use
 * setting init-params such as {@code object_wrapper} instead.
 * 
 * <li>Any other init-param will be interpreted as {@link Configuration}-level setting. See the possible names and
 * values at {@link Configuration#setSetting(String, String)}</li>
 * 
 * </ul>
 */

public class FreemarkerServlet extends HttpServlet
{
    private static final Logger LOG = Logger.getLogger("freemarker.servlet");
    
    public static final long serialVersionUID = -2440216393145762479L;

    /**
     * The name of the servlet init-param with similar name. (This init-param has existed long before 2.3.22, but this
     * constant was only added then.)
     * 
     * @since 2.3.22
     */
    public static final String INIT_PARAM_TEMPLATE_PATH = "TemplatePath";
    
    /**
     * The name of the servlet init-param with similar name. (This init-param has existed long before 2.3.22, but this
     * constant was only added then.)
     * 
     * @since 2.3.22
     */
    public static final String INIT_PARAM_NO_CACHE = "NoCache";
    
    /**
     * The name of the servlet init-param with similar name. (This init-param has existed long before 2.3.22, but this
     * constant was only added then.)
     * 
     * @since 2.3.22
     */
    public static final String INIT_PARAM_CONTENT_TYPE = "ContentType";

    /** @since 2.3.22 */
    public static final String INIT_PARAM_META_INF_TLD_LOCATIONS = "MetaInfTldSources";
    
    /** @since 2.3.22 */
    public static final String INIT_PARAM_CLASSPATH_TLDS = "ClasspathTlds";
    
    private static final String INIT_PARAM_DEBUG = "Debug";

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
    
    private static final String DEFAULT_CONTENT_TYPE = "text/html";

    /**
     * When set, the items defined in it will be add after those come from the
     * {@value #INIT_PARAM_META_INF_TLD_LOCATIONS} init-param. The value syntax is the same as of the init-param.
     * 
     * @since 2.3.22
     */
    public static final String SYSTEM_PROPERTY_META_INF_TLD_SOURCES = "org.freemarker.jsp.metaInfTldSources";

    /**
     * When set, the items defined in it will be add after those come from the
     * {@value #INIT_PARAM_CLASSPATH_TLDS} init-param. The value syntax is the same as of the init-param.
     * 
     * @since 2.3.22
     */
    public static final String SYSTEM_PROPERTY_CLASSPATH_TLDS = "org.freemarker.jsp.classpathTlds";
    
    /**
     * Used as part of the value of the {@value #INIT_PARAM_CLASSPATH_TLDS} init-param.
     * 
     * @since 2.3.22
     */
    public static final String META_INF_TLD_LOCATION_WEB_INF_PER_LIB_JARS = "webInfPerLibJars";
    
    /**
     * Used as part of the value of the {@value #INIT_PARAM_CLASSPATH_TLDS} init-param.
     * 
     * @since 2.3.22
     */
    public static final String META_INF_TLD_LOCATION_CLASSPATH = "classpath";
    
    /**
     * Used as part of the value of the {@value #INIT_PARAM_CLASSPATH_TLDS} init-param.
     * 
     * @since 2.3.22
     */
    public static final String META_INF_TLD_LOCATION_CLEAR = "clear";
    

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

    private static final String ATTR_JETTY_CP_TAGLIB_JAR_PATTERNS
            = "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern";
    
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
    private List/*<MetaInfTldSource>*/ metaInfTldSources;
    private List/*<String>*/ classpathTlds;
    
    public void init() throws ServletException {
        try {
            config = createConfiguration();
            
            // Only override what's coming from the config if it was explicitly specified: 
            final String iciInitParamValue = getInitParameter(Configuration.INCOMPATIBLE_IMPROVEMENTS);
            if (iciInitParamValue != null) {
                config.setSetting(Configuration.INCOMPATIBLE_IMPROVEMENTS, iciInitParamValue);
            }
            
            // Set defaults:
            config.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
            contentType = DEFAULT_CONTENT_TYPE;
            
            // Process object_wrapper init-param out of order: 
            wrapper = createObjectWrapper();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using object wrapper: " + wrapper);
            }
            config.setObjectWrapper(wrapper);
            
            // Process TemplatePath init-param out of order:
            templatePath = getInitParameter(INIT_PARAM_TEMPLATE_PATH);
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
                        || name.equals(INIT_PARAM_TEMPLATE_PATH)
                        || name.equals(Configuration.INCOMPATIBLE_IMPROVEMENTS)) {
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
                } else if (name.equals(INIT_PARAM_NO_CACHE)) {
                    nocache = StringUtil.getYesNo(value);
                } else if (name.equals(DEPR_INITPARAM_DEBUG)) { // BC
                    if (getInitParameter(INIT_PARAM_DEBUG) != null) {
                        throw new ServletException(
                                "Conflicting init-params: "
                                + INIT_PARAM_DEBUG + " and "
                                + DEPR_INITPARAM_DEBUG);
                    }
                    debug = StringUtil.getYesNo(value);
                } else if (name.equals(INIT_PARAM_DEBUG)) {
                    debug = StringUtil.getYesNo(value);
                } else if (name.equals(INIT_PARAM_CONTENT_TYPE)) {
                    contentType = value;
                } else if (name.equals(INIT_PARAM_META_INF_TLD_LOCATIONS)) {;
                    metaInfTldSources = parseAsMetaInfTldLocations(value);
                } else if (name.equals(INIT_PARAM_CLASSPATH_TLDS)) {;
                    classpathTlds = parseCommaSeparatedList(value);
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
            throw new ServletException("Error during servlet initialization", e);
        }
    }

    private List/*<MetaInfTldSource>*/ parseAsMetaInfTldLocations(String value) throws ParseException {
        List/*<MetaInfTldSource>*/ metaInfTldSources = null;
        
        List/*<String>*/ values = parseCommaSeparatedList(value);
        for (Iterator it = values.iterator(); it.hasNext();) {
            final String itemStr = (String) it.next();
            final MetaInfTldSource metaInfTldSource;
            if (itemStr.equals(META_INF_TLD_LOCATION_WEB_INF_PER_LIB_JARS)) {
                metaInfTldSource = WebInfPerLibJarMetaInfTldSource.INSTANCE;
            } else if (itemStr.startsWith(META_INF_TLD_LOCATION_CLASSPATH)) {
                String itemRightSide = itemStr.substring(META_INF_TLD_LOCATION_CLASSPATH.length()).trim();
                if (itemRightSide.length() == 0) {
                    metaInfTldSource = new ClasspathMetaInfTldSource(Pattern.compile(".*", Pattern.DOTALL));
                } else if (itemRightSide.startsWith(":")) {
                    final String regexpStr = itemRightSide.substring(1).trim();
                    if (regexpStr.length() == 0) {
                        throw new ParseException("Empty regular expression after \""
                                + META_INF_TLD_LOCATION_CLASSPATH + ":\"", -1);
                    }
                    metaInfTldSource = new ClasspathMetaInfTldSource(Pattern.compile(regexpStr));   
                } else {
                    throw new ParseException("Invalid \"" + META_INF_TLD_LOCATION_CLASSPATH
                            + "\" value syntax: " + value, -1);
                }
            } else if (itemStr.startsWith(META_INF_TLD_LOCATION_CLEAR)) {
                metaInfTldSource = ClearMetaInfTldSource.INSTANCE;
            } else {
                throw new ParseException("Item has no recognized source type prefix: " + value, -1);
            }
            if (metaInfTldSources == null) {
                metaInfTldSources = new ArrayList();
            }
            metaInfTldSources.add(metaInfTldSource);
        }
        
        return metaInfTldSources;
    }

    /**
     * Create the template loader. The default implementation will create a
     * {@link ClassTemplateLoader} if the template path starts with "class://",
     * a {@link FileTemplateLoader} if the template path starts with "file://",
     * and a {@link WebappTemplateLoader} otherwise.
     * @param templatePath the template path to create a loader for
     * @return a newly created template loader
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
                try {
                    // Prior to Servlet 2.5, the cause exception wasn't set by the above constructor.
                    // If we are on 2.5+ then this will throw an exception as the cause was already set.
                    e.initCause(te);
                } catch (Exception ex) {
                    // Ignored; see above
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

    protected TemplateModel createModel(ObjectWrapper objectWrapper,
                                        ServletContext servletContext,
                                        final HttpServletRequest request,
                                        final HttpServletResponse response) throws TemplateModelException {
        try {
            AllHttpScopesHashModel params = new AllHttpScopesHashModel(objectWrapper, servletContext, request);
    
            // Create hash model wrapper for servlet context (the application)
            ServletContextHashModel servletContextModel =
                    (ServletContextHashModel) servletContext.getAttribute(ATTR_APPLICATION_MODEL);
            if (servletContextModel == null)
            {
                servletContextModel = new ServletContextHashModel(this, objectWrapper);
                servletContext.setAttribute(ATTR_APPLICATION_MODEL, servletContextModel);
                
                TaglibFactory taglibFactory = createTaglibFactory(objectWrapper, servletContext);
                servletContext.setAttribute(ATTR_JSP_TAGLIBS_MODEL, taglibFactory);
                
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
                    sessionModel = new HttpSessionHashModel(session, objectWrapper);
                    initializeSessionAndInstallModel(request, response, 
                            sessionModel, session);
                }
            }
            else {
                sessionModel = new HttpSessionHashModel(this, request, response, objectWrapper);
            }
            params.putUnlistedModel(KEY_SESSION, sessionModel);
    
            // Create hash model wrapper for request
            HttpRequestHashModel requestModel =
                (HttpRequestHashModel) request.getAttribute(ATTR_REQUEST_MODEL);
            if (requestModel == null || requestModel.getRequest() != request)
            {
                requestModel = new HttpRequestHashModel(request, response, objectWrapper);
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

    /**
     * Called to create the {@link TaglibFactory} once per servlet context.
     * The default implementation configures it based on the servlet-init parameters and various other environmental
     * settings, so if you override this method, you should call super, then adjust the result.
     * 
     * @since 2.3.22
     */
    protected TaglibFactory createTaglibFactory(ObjectWrapper objectWrapper,
            ServletContext servletContext) throws TemplateModelException {
        TaglibFactory taglibFactory = new TaglibFactory(servletContext);
        
        taglibFactory.setObjectWrapper(objectWrapper);
        
        {
            List/*<MetaInfTldSources>*/ mergedMetaInfTldSources = new ArrayList();

            if (metaInfTldSources != null) {
                mergedMetaInfTldSources.addAll(metaInfTldSources);
            } else {
                // Needed so that if we will merge in Jetty stuff, the source list will contain this.
                mergedMetaInfTldSources.add(WebInfPerLibJarMetaInfTldSource.INSTANCE);
            }
            
            String sysPropVal = SecurityUtilities.getSystemProperty(SYSTEM_PROPERTY_META_INF_TLD_SOURCES, null);
            if (sysPropVal != null) {
                try {
                    List metaInfTldSourcesSysProp = parseAsMetaInfTldLocations(sysPropVal);
                    if (metaInfTldSourcesSysProp != null) {
                        mergedMetaInfTldSources.addAll(metaInfTldSourcesSysProp);
                    }
                } catch (ParseException e) {
                    throw new TemplateModelException("Failed to parse system property \""
                            + SYSTEM_PROPERTY_META_INF_TLD_SOURCES + "\"", e);
                }
            }

            List/*<Pattern>*/ jettyTaglibJarPatterns = null;
            try {
                final String attrVal = (String) servletContext.getAttribute(ATTR_JETTY_CP_TAGLIB_JAR_PATTERNS);
                jettyTaglibJarPatterns = attrVal != null ? parseCommaSeparatedPatterns(attrVal) : null;
            } catch (Exception e) {
                LOG.error("Failed to parse application context attribute \""
                        + ATTR_JETTY_CP_TAGLIB_JAR_PATTERNS + "\" - it will be ignored", e);
            }
            if (jettyTaglibJarPatterns != null) {
                for (Iterator/*<Pattern>*/ it = jettyTaglibJarPatterns.iterator(); it.hasNext();) {
                    Pattern pattern = (Pattern) it.next();
                    mergedMetaInfTldSources.add(new ClasspathMetaInfTldSource(pattern));
                }
            }
            
            taglibFactory.setMetaInfTldSources(mergedMetaInfTldSources);
        }
        
        {
            List/*<String>*/ mergedClassPathTlds = new ArrayList();
            if (classpathTlds != null) {
                mergedClassPathTlds.addAll(classpathTlds);
            }
            
            String sysPropVal = SecurityUtilities.getSystemProperty(SYSTEM_PROPERTY_CLASSPATH_TLDS, null);
            if (sysPropVal != null) {
                try {
                    List/*<String>*/ classpathTldsSysProp = parseCommaSeparatedList(sysPropVal);
                    if (classpathTldsSysProp != null) {
                        mergedClassPathTlds.addAll(classpathTldsSysProp);
                    }
                } catch (ParseException e) {
                    throw new TemplateModelException("Failed to parse system property \""
                            + SYSTEM_PROPERTY_CLASSPATH_TLDS + "\"", e);
                }
            }
            
            taglibFactory.setClasspathTlds(mergedClassPathTlds);
        }
        
        return taglibFactory;        
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
     * Called from {@link #init()} to create the FreeMarker object wrapper that this servlet will use for adapting
     * request, session, and servlet context attributes to {@link TemplateModel}-s. This is a hook that allows you
     * customize the object wrapper creation in a subclass. You should call {@link #getInitParameter(String)}
     * with {@link Configurable#OBJECT_WRAPPER_KEY} as argument, and see if it returns {@code null} or some other
     * value that you want to interpret yourself. If it wasn't {@code null} and you don't want to interpret the value,
     * fall back to the super method.
     * 
     * <p>The default implementation interprets the {@value Configurable#OBJECT_WRAPPER_KEY} servlet init-param
     * with {@link Configurable#setSetting(String, String)} (see valid values there), or if there's no such servlet
     * init-param, then calls {@link Configuration#getDefaultObjectWrapper(freemarker.template.Version)}. 
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
            return Configuration.getDefaultObjectWrapper(config.getIncompatibleImprovements());
        } else {
            wrapper = getInitParameter(Configurable.OBJECT_WRAPPER_KEY);
            if (wrapper == null) {
                return Configuration.getDefaultObjectWrapper(config.getIncompatibleImprovements());
            } else {
                try {
                    config.setSetting(Configurable.OBJECT_WRAPPER_KEY, wrapper);
                } catch (TemplateException e) {
                    throw new RuntimeException("Failed to set " + Configurable.OBJECT_WRAPPER_KEY, e);
                }
                return config.getObjectWrapper();
            }
        }
    }
    
    /**
     * Should be final; don't override it. Override {@link #createObjectWrapper()} instead.
     */
    // [2.4] Make it final
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

    private List/*<String>*/ parseCommaSeparatedList(String value) throws ParseException {
        List/*<String>*/ valuesList = new ArrayList();
        String[] values = StringUtil.split(value, ',');
        for (int i = 0; i < values.length; i++) {
            final String s = values[i].trim();
            if (s.length() != 0) {
                valuesList.add(s);
            } else if (i != values.length - 1) {
                throw new ParseException("Missing list item at index " + i, -1);
            }
        }
        return valuesList;
    }

    private List parseCommaSeparatedPatterns(String value) throws ParseException {
        List/*<String>*/ values = parseCommaSeparatedList(value);
        List/*<Pattern>*/ patterns = new ArrayList(values.size());
        for (int i = 0; i < values.size(); i++) {
            patterns.add(Pattern.compile((String) values.get(i)));
        }
        return patterns;
    }
}
