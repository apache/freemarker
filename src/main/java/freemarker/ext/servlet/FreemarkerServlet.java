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
import freemarker.cache.MultiTemplateLoader;
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
import freemarker.template.TemplateNotFoundException;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.SecurityUtilities;
import freemarker.template.utility.StringUtil;

/**
 * FreeMarker MVC View servlet that can be used similarly to JSP views. That is, you put the variables to expose into
 * HTTP servlet request attributes, then forward to an FTL file (instead of to a JSP file) that's mapped to this servet
 * (usually via the {@code <url-pattern>*.ftl<url-pattern>}). See web.xml example (and more) in the FreeMarker Manual!
 * 
 * 
 * <p>
 * <b>Main features</b>
 * </p>
 *
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
 * <li>A custom directive named {@code include_page} allows you to include the output of another servlet resource from
 * your servlet container, just as if you used {@code ServletRequest.getRequestDispatcher(path).include()}:
 * {@code <@include_page path="/myWebapp/somePage.jsp"/>}. You can also pass parameters to the newly included page by
 * passing a hash named {@code params}:
 * <code>&lt;@include_page path="/myWebapp/somePage.jsp" params= lang: "en", q="5"}/&gt;</code>. By default, the request
 * parameters of the original request (the one being processed by FreemarkerServlet) are also inherited by the include.
 * You can explicitly control this inheritance using the {@code inherit_params} parameter:
 * <code>&lt;@include_page path="/myWebapp/somePage.jsp" params={lang: "en", q="5"} inherit_params=false/&gt;</code>.
 * 
 * </ul>
 * 
 * 
 * <p>
 * <b>Supported {@code init-param}-s</b>
 * </p>
 * 
 * 
 * <ul>
 * 
 * <li><strong>{@value #INIT_PARAM_TEMPLATE_PATH}</strong>: Specifies the location of the templates. By default, this is
 * interpreted as web application directory relative URI.<br>
 * Alternatively, you can prepend it with <tt>file://</tt> to indicate a literal path in the file system (i.e.
 * <tt>file:///var/www/project/templates/</tt>). Note that three slashes were used to specify an absolute path.<br>
 * Also, you can prepend it with {@code classpath:}, like in <tt>classpath:com/example/templates</tt>, to indicate that
 * you want to load templates from the specified package accessible through the Thread Context Class Loader of the
 * thread that initializes this servlet.<br>
 * If {@code incompatible_improvements} is set to 2.3.22 (or higher), you can specify multiple comma separated locations
 * inside square brackets, like: {@code [ WEB-INF/templates, classpath:com/example/myapp/templates ]}.
 * This internally creates a {@link MultiTemplateLoader}. Note again that if {@code incompatible_improvements} isn't
 * set to at least 2.3.22, the initial {@code [} has no special meaning, and so this feature is unavailable.<br>
 * For backward compatibility (not recommended!), you can also use the {@code class://} prefix, like in
 * <tt>class://com/example/templates</tt> format, which is similar to {@code classpath:}, except that it uses the
 * defining class loader of this servlet's class. This can cause template not found errors, if that class (in
 * {@code freemarer.jar} usually) is not local to the web application, while the templates are.<br>
 * The default value is <tt>class://</tt> (that is, the root of the class hierarchy), which is not recommended anymore,
 * and should be overwritten with the init-param.</li>
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
 * <li><strong>{@value #INIT_PARAM_BUFFER_SIZE}</strong>: Sets the size of the output buffer in bytes, or if "KB" or
 * "MB" is written after the number (like {@code <param-value>256 KB</param-value>}) then in kilobytes or megabytes.
 * This corresponds to {@link HttpServletResponse#setBufferSize(int)}. If the {@link HttpServletResponse} state doesn't
 * allow changing the buffer size, it will silently do nothing. If this init param isn't specified, then the buffer size
 * is not set by {@link FreemarkerServlet} in the HTTP response, which usually means that the default buffer size of the
 * servlet container will be used.</li>
 *
 * <li><strong>{@value #INIT_PARAM_EXCEPTION_ON_MISSING_TEMPLATE}</strong> (since 2.3.22): If {@code false} (default,
 * but not recommended), if a template is requested that's missing, this servlet responses with a HTTP 404 "Not found"
 * error, and only logs the problem with debug level. If {@code true} (recommended), the servlet will log the issue with
 * error level, then throws an exception that bubbles up to the servlet container, which usually then creates a HTTP 500
 * "Internal server error" response (and maybe logs the event into the container log). See "Error handling" later for
 * more!</li>
 * 
 * <li><strong>{@value #INIT_PARAM_META_INF_TLD_LOCATIONS}</strong> (since 2.3.22): Comma separated list of items, each
 * is either {@value #META_INF_TLD_LOCATION_WEB_INF_PER_LIB_JARS}, or {@value #META_INF_TLD_LOCATION_CLASSPATH}
 * optionally followed by colon and a regular expression, or {@value #META_INF_TLD_LOCATION_CLEAR}. For example
 * {@code <param-value>classpath:.*myoverride.*\.jar$, webInfPerLibJars, classpath:.*taglib.*\.jar$</param-value>}, or
 * {@code <param-value>classpath</param-value>}. (Whitespace around the commas and list items will be ignored.) See
 * {@link TaglibFactory#setMetaInfTldSources(List)} for more information. Defaults to a list that contains
 * {@value #META_INF_TLD_LOCATION_WEB_INF_PER_LIB_JARS} only (can be overridden with
 * {@link #createDefaultMetaInfTldSources()}). Note that this can be also specified with the
 * {@value #SYSTEM_PROPERTY_META_INF_TLD_SOURCES} system property. If both the init-param and the system property
 * exists, the sources listed in the system property will be added after those specified by the init-param. This is
 * where the special entry, {@value #META_INF_TLD_LOCATION_CLEAR} comes handy, as it will remove all previous list
 * items. (An intended usage of the system property is setting it to {@code clear, classpath} in the Eclipse run
 * configuration if you are running the application without putting the dependency jar-s into {@code WEB-INF/lib}.)
 * Also, note that further {@code classpath:<pattern>} items are added automatically at the end of this list based on
 * Jetty's {@code "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern"} servlet context attribute.</li>
 * 
 * <li><strong>{@value #INIT_PARAM_CLASSPATH_TLDS}</strong> (since 2.3.22): Comma separated list of paths; see
 * {@link TaglibFactory#setClasspathTlds(List)}. Whitespace around the list items will be ignored. Defaults to no paths
 * (can be overidden with {@link #createDefaultClassPathTlds()}). Note that this can also be specified with the
 * {@value #SYSTEM_PROPERTY_CLASSPATH_TLDS} system property. If both the init-param and the system property exists, the
 * items listed in system property will be added after those specified by the init-param.</li>
 * 
 * <li><strong>"Debug"</strong>: Deprecated, has no effect since 2.3.22. (Earlier it has enabled/disabled sending
 * debug-level log messages to the servlet container log, but this servlet doesn't log debug level messages into the
 * servlet container log anymore, only into the FreeMarker log.)</li>
 * 
 * <li>The following init-params are supported only for backward compatibility, and their usage is discouraged:
 * {@code TemplateUpdateInterval}, {@code DefaultEncoding}, {@code ObjectWrapper}, {@code TemplateExceptionHandler}.
 * Instead, use init-params with the setting names documented at {@link Configuration#setSetting(String, String)}, such
 * as {@code object_wrapper}.
 * 
 * <li><strong>Any other init-params</strong> will be interpreted as {@link Configuration}-level FreeMarker setting. See
 * the possible names and values at {@link Configuration#setSetting(String, String)}.</li>
 * 
 * </ul>
 * 
 * 
 * <p>
 * <b>Error handling</b>
 * </p>
 * 
 * 
 * <p>
 * Notes:
 * </p>
 * 
 * <ul>
 *
 * <li>Logging below, where not said otherwise, always refers to logging with FreeMarker's logging facility (see
 * {@link Logger}), under the "freemarker.servlet" category.</li>
 * <li>Throwing a {@link ServletException} to the servlet container is mentioned at a few places below. That in practice
 * usually means HTTP 500 "Internal server error" response, and maybe a log entry in the servlet container's log.</li>
 * </ul>
 *
 * <p>
 * Errors types:
 * </p>
 * 
 * <ul>
 * 
 * <li>If the servlet initialization fails, the servlet won't be started as usual. The cause is usually logged with
 * error level. When it isn't, check the servlet container's log.
 * 
 * <li>If the requested template doesn't exist, by default the servlet returns a HTTP 404 "Not found" response, and logs
 * the problem with <em>debug</em> level. Responding with HTTP 404 is how JSP behaves, but it's actually not a
 * recommended setting anymore. By setting {@value #INIT_PARAM_EXCEPTION_ON_MISSING_TEMPLATE} init-param to {@code true}
 * (recommended), it will instead log the problem with error level, then the servlet throws {@link ServletException} to
 * the servlet container (with the proper cause exception). After all, if the visited URL had an associated "action" but
 * the template behind it is missing, that's an internal server error, not a wrong URL.</li>
 * 
 * <li>
 * If the template contains parsing errors, it will log it with error level, then the servlet throws
 * {@link ServletException} to the servlet container (with the proper cause exception).</li>
 * 
 * <li>
 * If the template throws exception during its execution, and the value of the {@code template_exception_handler}
 * init-param is {@code rethrow} (recommended), it will log it with error level and then the servlet throws
 * {@link ServletException} to the servlet container (with the proper cause exception). But beware, the default value of
 * the {@code template_exception_handler} init-param is {@code html_debug}, which is for development only! Set it to
 * {@code rethrow} for production. The {@code html_debug} (and {@code debug}) handlers will print error details to the
 * page and then commit the HTTP response with response code 200 "OK", thus, the server wont be able roll back the
 * response and send back an HTTP 500 page. This is so that the template developers will see the error without digging
 * the logs.
 * 
 * </ul>
 */
public class FreemarkerServlet extends HttpServlet
{
    private static final Logger LOG = Logger.getLogger("freemarker.servlet");
    private static final Logger LOG_RT = Logger.getLogger("freemarker.runtime");

    private static final String TEMPLATE_PATH_PREFIX_CLASSPATH = "classpath:";
    private static final String TEMPLATE_PATH_PREFIX_CLASS = "class://";
    private static final String TEMPLATE_PATH_PREFIX_FILE = "file://";
    
    public static final long serialVersionUID = -2440216393145762479L;

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params. (This init-param
     * has existed long before 2.3.22, but this constant was only added then.)
     * 
     * @since 2.3.22
     */
    public static final String INIT_PARAM_TEMPLATE_PATH = "TemplatePath";
    
    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params. (This init-param
     * has existed long before 2.3.22, but this constant was only added then.)
     * 
     * @since 2.3.22
     */
    public static final String INIT_PARAM_NO_CACHE = "NoCache";

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params. (This init-param
     * has existed long before 2.3.22, but this constant was only added then.)
     * 
     * @since 2.3.22
     */
    public static final String INIT_PARAM_CONTENT_TYPE = "ContentType";

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     * 
     * @since 2.3.22
     */
    public static final String INIT_PARAM_BUFFER_SIZE = "BufferSize";
    
    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     * 
     * @since 2.3.22
     */
    public static final String INIT_PARAM_META_INF_TLD_LOCATIONS = "MetaInfTldSources";

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     * 
     * @since 2.3.22
     */
    public static final String INIT_PARAM_EXCEPTION_ON_MISSING_TEMPLATE = "ExceptionOnMissingTemplate";
    
    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     * 
     * @since 2.3.22
     */
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
     * When set, the items defined in it will be added after those coming from the
     * {@value #INIT_PARAM_META_INF_TLD_LOCATIONS} init-param. The value syntax is the same as of the init-param. Note
     * that {@value #META_INF_TLD_LOCATION_CLEAR} can be used to re-start the list, rather than continue it.
     * 
     * @since 2.3.22
     */
    public static final String SYSTEM_PROPERTY_META_INF_TLD_SOURCES = "org.freemarker.jsp.metaInfTldSources";

    /**
     * When set, the items defined in it will be added after those coming from the
     * {@value #INIT_PARAM_CLASSPATH_TLDS} init-param. The value syntax is the same as of the init-param.
     * 
     * @since 2.3.22
     */
    public static final String SYSTEM_PROPERTY_CLASSPATH_TLDS = "org.freemarker.jsp.classpathTlds";
    
    /**
     * Used as part of the value of the {@value #INIT_PARAM_META_INF_TLD_LOCATIONS} init-param.
     * 
     * @since 2.3.22
     */
    public static final String META_INF_TLD_LOCATION_WEB_INF_PER_LIB_JARS = "webInfPerLibJars";
    
    /**
     * Used as part of the value of the {@value #INIT_PARAM_META_INF_TLD_LOCATIONS} init-param.
     * 
     * @since 2.3.22
     */
    public static final String META_INF_TLD_LOCATION_CLASSPATH = "classpath";
    
    /**
     * Used as part of the value of the {@value #INIT_PARAM_META_INF_TLD_LOCATIONS} init-param.
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
    private static final String ATTR_REQUEST_PARAMETERS_MODEL = ".freemarker.RequestParameters";
    private static final String ATTR_SESSION_MODEL = ".freemarker.Session";
    
    /** @deprecated We only keeps this attribute for backward compatibility, but actually aren't using it. */
    private static final String ATTR_APPLICATION_MODEL = ".freemarker.Application";
    
    /** @deprecated We only keeps this attribute for backward compatibility, but actually aren't using it. */
    private static final String ATTR_JSP_TAGLIBS_MODEL = ".freemarker.JspTaglibs";

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

    // Init-param values:
    private String templatePath;
    private boolean noCache;
    private Integer bufferSize;
    private boolean exceptionOnMissingTemplate;
    
    /**
     * @deprecated Not used anymore; to enable/disable debug logging, just set the logging level of the logging library
     *             used by {@link Logger}.
     */
    protected boolean debug;
    
    private Configuration config;
    private ObjectWrapper wrapper;
    private String contentType;
    private boolean noCharsetInContentType;
    private List/*<MetaInfTldSource>*/ metaInfTldSources;
    private List/*<String>*/ classpathTlds;

    private Object lazyInitFieldsLock = new Object();
    private ServletContextHashModel servletContextModel;
    private TaglibFactory taglibFactory;
    
    private boolean objectWrapperMismatchWarnLogged;

    /**
     * Don't override this method to adjust FreeMarker settings! Override the protected methods for that, such as
     * {@link #createConfiguration()}, {@link #createTemplateLoader(String)}, {@link #createDefaultObjectWrapper()},
     * etc. Also note that lot of things can be changed with init-params instead of overriding methods, so if you
     * override settings, usually you should only override their defaults.
     */
    public void init() throws ServletException {
        try {
            initialize();
        } catch (Exception e) {
            // At least Jetty doesn't log the ServletException itself, only its cause exception. Thus we add some
            // message here that (re)states the obvious.
            throw new ServletException("Error while initializing " + this.getClass().getName()
                    + " servlet; see cause exception.", e);
        }
    }
    
    private void initialize() throws InitParamValueException, MalformedWebXmlException, ConflictingInitParamsException {
        config = createConfiguration();
        
        // Only override what's coming from the config if it was explicitly specified: 
        final String iciInitParamValue = getInitParameter(Configuration.INCOMPATIBLE_IMPROVEMENTS_KEY);
        if (iciInitParamValue != null) {
            try {
                config.setSetting(Configuration.INCOMPATIBLE_IMPROVEMENTS_KEY, iciInitParamValue);
            } catch (Exception e) {
                throw new InitParamValueException(Configuration.INCOMPATIBLE_IMPROVEMENTS_KEY, iciInitParamValue, e);
            }
        }
        
        // Set FreemarkerServlet-specific defaults, except where createConfiguration() has already set them:
        if (!config.isTemplateExceptionHandlerExplicitlySet()) {
            config.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        }
        if (!config.isLogTemplateExceptionsExplicitlySet()) {
            config.setLogTemplateExceptions(false);
        }
        
        contentType = DEFAULT_CONTENT_TYPE;
        
        // Process object_wrapper init-param out of order: 
        wrapper = createObjectWrapper();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using object wrapper: " + wrapper);
        }
        config.setObjectWrapper(wrapper);
        
        // Process TemplatePath init-param out of order:
        templatePath = getInitParameter(INIT_PARAM_TEMPLATE_PATH);
        if (templatePath == null && !config.isTemplateLoaderExplicitlySet()) {
            templatePath = TEMPLATE_PATH_PREFIX_CLASS;
        }
        if (templatePath != null) {
            try {
                config.setTemplateLoader(createTemplateLoader(templatePath));
            } catch (Exception e) {
                throw new InitParamValueException(INIT_PARAM_TEMPLATE_PATH, templatePath, e);
            }
        }
        
        metaInfTldSources = createDefaultMetaInfTldSources();
        classpathTlds = createDefaultClassPathTlds();

        // Process all other init-params:
        Enumeration initpnames = getServletConfig().getInitParameterNames();
        while (initpnames.hasMoreElements()) {
            final String name = (String) initpnames.nextElement();
            final String value = getInitParameter(name);
            if (name == null) {
                throw new MalformedWebXmlException(
                        "init-param without param-name. "
                        + "Maybe the web.xml is not well-formed?");
            }
            if (value == null) {
                throw new MalformedWebXmlException(
                        "init-param " + StringUtil.jQuote(name) + " without param-value. "
                        + "Maybe the web.xml is not well-formed?");
            }
            
            try {
                if (name.equals(DEPR_INITPARAM_OBJECT_WRAPPER)
                        || name.equals(Configurable.OBJECT_WRAPPER_KEY)
                        || name.equals(INIT_PARAM_TEMPLATE_PATH)
                        || name.equals(Configuration.INCOMPATIBLE_IMPROVEMENTS)) {
                    // ignore: we have already processed these
                } else if (name.equals(DEPR_INITPARAM_ENCODING)) { // BC
                    if (getInitParameter(Configuration.DEFAULT_ENCODING_KEY) != null) {
                        throw new ConflictingInitParamsException(
                                Configuration.DEFAULT_ENCODING_KEY, DEPR_INITPARAM_ENCODING);
                    }
                    config.setDefaultEncoding(value);
                } else if (name.equals(DEPR_INITPARAM_TEMPLATE_DELAY)) { // BC
                    if (getInitParameter(Configuration.TEMPLATE_UPDATE_DELAY_KEY) != null) {
                        throw new ConflictingInitParamsException(
                                Configuration.TEMPLATE_UPDATE_DELAY_KEY, DEPR_INITPARAM_TEMPLATE_DELAY);
                    }
                    try {
                        config.setTemplateUpdateDelay(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // Intentionally ignored
                    }
                } else if (name.equals(DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER)) { // BC
                    if (getInitParameter(Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY) != null) {
                        throw new ConflictingInitParamsException(
                                Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY, DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER);
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
                        throw new InitParamValueException(DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER, value,
                                "Not one of the supported values.");
                    }
                } else if (name.equals(INIT_PARAM_NO_CACHE)) {
                    noCache = StringUtil.getYesNo(value);
                } else if (name.equals(INIT_PARAM_BUFFER_SIZE)) {
                    bufferSize = new Integer(parseSize(value));
                } else if (name.equals(DEPR_INITPARAM_DEBUG)) { // BC
                    if (getInitParameter(INIT_PARAM_DEBUG) != null) {
                        throw new ConflictingInitParamsException(INIT_PARAM_DEBUG, DEPR_INITPARAM_DEBUG);
                    }
                    debug = StringUtil.getYesNo(value);
                } else if (name.equals(INIT_PARAM_DEBUG)) {
                    debug = StringUtil.getYesNo(value);
                } else if (name.equals(INIT_PARAM_CONTENT_TYPE)) {
                    contentType = value;
                } else if (name.equals(INIT_PARAM_EXCEPTION_ON_MISSING_TEMPLATE)) {
                    exceptionOnMissingTemplate = StringUtil.getYesNo(value);
                } else if (name.equals(INIT_PARAM_META_INF_TLD_LOCATIONS)) {;
                    metaInfTldSources = parseAsMetaInfTldLocations(value);
                } else if (name.equals(INIT_PARAM_CLASSPATH_TLDS)) {;
                    List newClasspathTlds = new ArrayList();
                    if (classpathTlds != null) {
                        newClasspathTlds.addAll(classpathTlds);
                    }
                    newClasspathTlds.addAll(parseCommaSeparatedList(value));
                    classpathTlds = newClasspathTlds;
                } else {
                    config.setSetting(name, value);
                }
            } catch (ConflictingInitParamsException e) {
                throw e;
            } catch (Exception e) {
                throw new InitParamValueException(name, value, e);
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
                throw new ParseException("Item has no recognized source type prefix: " + itemStr, -1);
            }
            if (metaInfTldSources == null) {
                metaInfTldSources = new ArrayList();
            }
            metaInfTldSources.add(metaInfTldSource);
        }
        
        return metaInfTldSources;
    }

    /**
     * Create the template loader. The default implementation will create a {@link ClassTemplateLoader} if the template
     * path starts with {@code "class://"}, a {@link FileTemplateLoader} if the template path starts with
     * {@code "file://"}, and a {@link WebappTemplateLoader} otherwise. Also, if
     * {@link Configuration#Configuration(freemarker.template.Version) incompatible_improvements} is 2.3.22 or higher,
     * it will create a {@link MultiTemplateLoader} if the template path starts with {@code "["}.
     * 
     * @param templatePath
     *            the template path to create a loader for
     * @return a newly created template loader
     */
    protected TemplateLoader createTemplateLoader(String templatePath) throws IOException
    {
        templatePath = templatePath.trim();
        
        if (templatePath.startsWith(TEMPLATE_PATH_PREFIX_CLASS)) {
            String packagePath = templatePath.substring(TEMPLATE_PATH_PREFIX_CLASS.length());
            packagePath = normalizeToAbsolutePackagePath(packagePath);
            return new ClassTemplateLoader(getClass(), packagePath);
        } else if (templatePath.startsWith(TEMPLATE_PATH_PREFIX_CLASSPATH)) {
            // To be similar to Spring resource paths, we don't require "//":
            String packagePath = templatePath.substring(TEMPLATE_PATH_PREFIX_CLASSPATH.length());
            packagePath = normalizeToAbsolutePackagePath(packagePath);
            
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                LOG.warn("No Thread Context Class Loader was found. Falling back to the class loader of "
                        + this.getClass().getName() + ".");
                classLoader = getClass().getClassLoader();
            }
            
            return new ClassTemplateLoader(classLoader, packagePath);
        } else if (templatePath.startsWith(TEMPLATE_PATH_PREFIX_FILE)) {
                String filePath = templatePath.substring(TEMPLATE_PATH_PREFIX_FILE.length());
                return new FileTemplateLoader(new File(filePath));
        } else if (templatePath.startsWith("[")
                && getConfiguration().getIncompatibleImprovements().intValue() >= _TemplateAPI.VERSION_INT_2_3_22) {
            if (!templatePath.endsWith("]")) {
                // B.C. constraint: Can't throw any checked exceptions.
                throw new RuntimeException("Failed to parse template path; closing \"]\" is missing.");
            }
            List pathItems;
            try {
                pathItems = parseCommaSeparatedList(templatePath.substring(1, templatePath.length() - 1));
            } catch (ParseException e) {
                // B.C. constraint: Can't throw any checked exceptions.
                throw new RuntimeException("Failed to parse template path; see cause exception.", e);
            }
            TemplateLoader[] templateLoaders = new TemplateLoader[pathItems.size()];
            for (int i = 0; i < pathItems.size(); i++) {
                String pathItem = (String) pathItems.get(i);
                templateLoaders[i] = createTemplateLoader(pathItem);
            }
            return new MultiTemplateLoader(templateLoaders);
        } else if (templatePath.startsWith("{")
                && getConfiguration().getIncompatibleImprovements().intValue() >= _TemplateAPI.VERSION_INT_2_3_22) {
            throw new RuntimeException("Template paths startin with \"{\" are reseved for future purposes");
        } else {
            return new WebappTemplateLoader(this.getServletContext(), templatePath);
        }
    }

    private String normalizeToAbsolutePackagePath(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return "/" + path;
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
        
        if (bufferSize != null && !response.isCommitted()) {
            try {
                response.setBufferSize(bufferSize.intValue());
            } catch (IllegalStateException e) {
                LOG.debug("Can't set buffer size any more,", e);
            }
        }

        String templatePath = requestUrlToTemplatePath(request);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Requested template " + StringUtil.jQuoteNoXSS(templatePath) + ".");
        }

        final Locale locale = deduceLocale(templatePath, request, response);
        
        final Template template;
        try {
            template = config.getTemplate(templatePath, locale);
        } catch (TemplateNotFoundException e) {
            if (exceptionOnMissingTemplate) {
                throw newServletExceptionWithFreeMarkerLogging(
                        "Template not found for name " + StringUtil.jQuoteNoXSS(templatePath) + ".", e);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Responding HTTP 404 \"Not found\" for missing template "
                            + StringUtil.jQuoteNoXSS(templatePath) + ".", e);
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Page template not found");
                return;
            }
        } catch (freemarker.core.ParseException e) {
            throw newServletExceptionWithFreeMarkerLogging(
                    "Parsing error with template " + StringUtil.jQuoteNoXSS(templatePath) + ".", e);
        } catch (Exception e) {
            throw newServletExceptionWithFreeMarkerLogging(
                    "Unexpected error when loading template " + StringUtil.jQuoteNoXSS(templatePath) + ".", e);
        }
        
        Object attrContentType = template.getCustomAttribute("content_type");
        if(attrContentType != null) {
            response.setContentType(attrContentType.toString());
        } else {
            if (noCharsetInContentType) {
                response.setContentType(contentType + "; charset=" + template.getEncoding());
            } else {
                response.setContentType(contentType);
            }
        }

        setBrowserCachingPolicy(response);

        ServletContext servletContext = getServletContext();
        try {
            logWarnOnObjectWrapperMismatch();
            
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
        } catch (TemplateException e) {
            final TemplateExceptionHandler teh = config.getTemplateExceptionHandler();
            // Ensure that debug handler responses aren't rolled back:
            if (teh == TemplateExceptionHandler.HTML_DEBUG_HANDLER || teh == TemplateExceptionHandler.DEBUG_HANDLER
                    || teh.getClass().getName().indexOf("Debug") != -1) {
                response.flushBuffer();
            }
            throw newServletExceptionWithFreeMarkerLogging("Error executing FreeMarker template", e);
        }
    }

    private ServletException newServletExceptionWithFreeMarkerLogging(String message, Throwable cause) throws ServletException {
        if (cause instanceof TemplateException) {
            // For backward compatibility, we log into the same category as Environment did when
            // log_template_exceptions was true.
            LOG_RT.error(message, cause);
        } else {
            LOG.error(message, cause);
        }

        ServletException e = new ServletException(message, cause);
        try {
            // Prior to Servlet 2.5, the cause exception wasn't set by the above constructor.
            // If we are on 2.5+ then this will throw an exception as the cause was already set.
            e.initCause(cause);
        } catch (Exception ex) {
            // Ignored; see above
        }
        throw e;
    }
    
    private void logWarnOnObjectWrapperMismatch() {
        // Deliberately uses double check locking.
        if (wrapper != config.getObjectWrapper() && !objectWrapperMismatchWarnLogged && LOG.isWarnEnabled()) {
            final boolean logWarn;
            synchronized (this) {
                logWarn = !objectWrapperMismatchWarnLogged;
                if (logWarn) {
                    objectWrapperMismatchWarnLogged = true;
                }
            }
            if (logWarn) {
                LOG.warn(
                        this.getClass().getName()
                        + ".wrapper != config.getObjectWrapper(); possibly the result of incorrect extension of "
                        + FreemarkerServlet.class.getName() + ".");
            }
        }
    }
    
    /**
     * Returns the locale used for the {@link Configuration#getTemplate(String, Locale)} call. The base implementation
     * simply returns the locale setting of the configuration. Override this method to provide different behaviour, i.e.
     * to use the locale indicated in the request.
     * 
     * @param templatePath
     *            The template path (templat name) as it will be passed to {@link Configuration#getTemplate(String)}.
     *            (Not to be confused with the servlet init-param of identical name; they aren't related.)
     * 
     * @throws ServletException
     *             Can be thrown since 2.3.22, if the locale can't be deduced from the URL.
     */
    protected Locale deduceLocale(String templatePath, HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        return config.getLocale();
    }

    protected TemplateModel createModel(ObjectWrapper objectWrapper,
                                        ServletContext servletContext,
                                        final HttpServletRequest request,
                                        final HttpServletResponse response) throws TemplateModelException {
        try {
            AllHttpScopesHashModel params = new AllHttpScopesHashModel(objectWrapper, servletContext, request);
    
            // Create hash model wrapper for servlet context (the application)
            final ServletContextHashModel servletContextModel;
            final TaglibFactory taglibFactory;
            synchronized (lazyInitFieldsLock) {
                if (this.servletContextModel == null) {
                    servletContextModel = new ServletContextHashModel(this, objectWrapper);
                    taglibFactory = createTaglibFactory(objectWrapper, servletContext);
                    
                    // For backward compatibility only. We don't use these:
                    servletContext.setAttribute(ATTR_APPLICATION_MODEL, servletContextModel);
                    servletContext.setAttribute(ATTR_JSP_TAGLIBS_MODEL, taglibFactory);
                    
                    initializeServletContext(request, response);

                    this.taglibFactory = taglibFactory;
                    this.servletContextModel = servletContextModel;
                } else {
                    servletContextModel = this.servletContextModel;
                    taglibFactory = this.taglibFactory;
                }
            }
            
            params.putUnlistedModel(KEY_APPLICATION, servletContextModel);
            params.putUnlistedModel(KEY_APPLICATION_PRIVATE, servletContextModel);
            params.putUnlistedModel(KEY_JSP_TAGLIBS, taglibFactory);
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
            List/*<MetaInfTldSource>*/ mergedMetaInfTldSources = new ArrayList();

            if (metaInfTldSources != null) {
                mergedMetaInfTldSources.addAll(metaInfTldSources);
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

    /**
     * Creates the default of the {@value #INIT_PARAM_CLASSPATH_TLDS} init-param; if this init-param is specified, it
     * will be appended <em>after</em> the default, not replace it.
     * 
     * <p>
     * The implementation in {@link FreemarkerServlet} returns {@link TaglibFactory#DEFAULT_CLASSPATH_TLDS}.
     * 
     * @return A {@link List} of {@link String}-s; not {@code null}.
     * 
     * @since 2.3.22
     */
    protected List/*<MetaInfTldSource>*/ createDefaultClassPathTlds() {
        return TaglibFactory.DEFAULT_CLASSPATH_TLDS;
    }

    /**
     * Creates the default of the {@value #INIT_PARAM_META_INF_TLD_LOCATIONS} init-param; if this init-param is
     * specified, it will completelly <em>replace</em> the default value.
     * 
     * <p>
     * The implementation in {@link FreemarkerServlet} returns {@link TaglibFactory#DEFAULT_META_INF_TLD_SOURCES}.
     * 
     * @return A {@link List} of {@link MetaInfTldSource}-s; not {@code null}.
     * 
     * @since 2.3.22
     */
    protected List/*<MetaInfTldSource>*/ createDefaultMetaInfTldSources() {
        return TaglibFactory.DEFAULT_META_INF_TLD_SOURCES;
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
     * Maps the request URL to a template path (template name) that is passed to
     * {@link Configuration#getTemplate(String, Locale)}. You can override it (i.e. to provide advanced rewriting
     * capabilities), but you are strongly encouraged to call the overridden method first, then only modify its return
     * value.
     * 
     * @param request
     *            The currently processed HTTP request
     * @return The template path (template name); can't be {@code null}. This is what's passed to
     *         {@link Configuration#getTemplate(String)} later. (Not to be confused with the {@code templatePath}
     *         servlet init-param of identical name; that basically specifies the "virtual file system" to which this
     *         will be relative to.)
     * 
     * @throws ServletException
     *             Can be thrown since 2.3.22, if the template path can't be deduced from the URL.
     */
    protected String requestUrlToTemplatePath(HttpServletRequest request) throws ServletException
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
     * Creates the FreeMarker {@link Configuration} singleton and (when overidden) maybe sets its defaults. Servlet
     * init-params will be applied later, and thus can overwrite the settings specified here.
     * 
     * <p>
     * By overriding this method you can set your preferred {@link Configuration} setting defaults, as only the settings
     * for which an init-param was specified will be overwritten later. (Note that {@link FreemarkerServlet} also has
     * its own defaults for a few settings, but since 2.3.22, the servlet detects if those settings were already set
     * here and then it won't overwrite them.)
     * 
     * <p>
     * The default implementation simply creates a new instance with {@link Configuration#Configuration()} and returns
     * it.
     */
    protected Configuration createConfiguration() {
        // We can only set incompatible_improvements later, so ignore the deprecation warning here.
        return new Configuration();
    }
    
    /**
     * Sets the defaults of the configuration that are specific to the {@link FreemarkerServlet} subclass.
     * This is called after the common (wired in) {@link FreemarkerServlet} setting defaults was set, also the 
     */
    protected void setConfigurationDefaults() {
        // do nothing
    }
    
    /**
     * Called from {@link #init()} to create the {@link ObjectWrapper}; to customzie this aspect, in most cases you
     * should override {@link #createDefaultObjectWrapper()} instead. Overriding this method is necessary when you want
     * to customize how the {@link ObjectWrapper} is created <em>from the init-param values</em>, or you want to do some
     * post-processing (like checking) on the created {@link ObjectWrapper}. To customize init-param interpretation,
     * call {@link #getInitParameter(String)} with {@link Configurable#OBJECT_WRAPPER_KEY} as argument, and see if it
     * returns a value that you want to interpret yourself. If was {@code null} or you don't want to interpret the
     * value, fall back to the super method.
     * 
     * <p>
     * The default implementation interprets the {@code object_wrapper} servlet init-param with
     * calling {@link Configurable#setSetting(String, String)} (see valid values there), or if there's no such servlet
     * init-param, then it calls {@link #createDefaultObjectWrapper()}.
     * 
     * @return The {@link ObjectWrapper} that will be used for adapting request, session, and servlet context attributes
     *         to {@link TemplateModel}-s, and also as the object wrapper setting of {@link Configuration}.
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
            return createDefaultObjectWrapper();
        } else {
            wrapper = getInitParameter(Configurable.OBJECT_WRAPPER_KEY);
            if (wrapper == null) {
                if (!config.isObjectWrapperExplicitlySet()) {
                    return createDefaultObjectWrapper();
                } else {
                    return config.getObjectWrapper();
                }
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
     * Override this to specify what the default {@link ObjectWrapper} will be when the
     * {@code object_wrapper} Servlet init-param wasn't specified. Note that this is called by
     * {@link #createConfiguration()}, and so if that was also overidden but improperly then this method might won't be
     * ever called. Also note that if you set the {@code object_wrapper} in {@link #createConfiguration()}, then this
     * won't be called, since then that has already specified the default.
     * 
     * <p>
     * The default implementation calls {@link Configuration#getDefaultObjectWrapper(freemarker.template.Version)}. You
     * should also pass in the version paramter when creating an {@link ObjectWrapper} that supports that. You can get
     * the version by calling {@link #getConfiguration()} and then {@link Configuration#getIncompatibleImprovements()}.
     * 
     * @since 2.3.22
     */
    protected ObjectWrapper createDefaultObjectWrapper() {
        return Configuration.getDefaultObjectWrapper(config.getIncompatibleImprovements());
    }
    
    /**
     * Should be final; don't override it. Override {@link #createObjectWrapper()} instead.
     */
    // [2.4] Make it final
    protected ObjectWrapper getObjectWrapper() {
        return wrapper;
    }
    
    /**
     * The value of the {@code TemplatePath} init-param. {@code null} if the {@code template_loader} setting was set in
     * a custom {@link #createConfiguration()}.
     * 
     * @deprecated Not called by FreeMarker code, and there's no point to override this (unless to cause confusion).
     */
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
        if (noCache)
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
                throw new ParseException("Missing list item after a comma", -1);
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
    
    private int parseSize(String value) throws ParseException {
        int lastDigitIdx;
        for (lastDigitIdx = value.length() - 1; lastDigitIdx >= 0; lastDigitIdx--) {
            char c = value.charAt(lastDigitIdx);
            if (c >= '0' && c <= '9') {
                break;
            }
        }
        
        final int n = Integer.parseInt(value.substring(0, lastDigitIdx + 1).trim());
        
        final String unitStr = value.substring(lastDigitIdx + 1).trim().toUpperCase();
        final int unit;
        if (unitStr.length() == 0 || unitStr.equals("B")) {
            unit = 1;
        } else if (unitStr.equals("K") || unitStr.equals("KB") || unitStr.equals("KIB")) {
            unit = 1024;
        } else if (unitStr.equals("M") || unitStr.equals("MB") || unitStr.equals("MIB")) {
            unit = 1024 * 1024;
        } else {
            throw new ParseException("Unknown unit: " + unitStr, lastDigitIdx + 1);
        }
        
        long size = (long) n * unit;
        if (size < 0) {
            throw new IllegalArgumentException("Buffer size can't be negative");
        }
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Buffer size can't bigger than " + Integer.MAX_VALUE);
        }
        return (int) size;
    }

    private static class InitParamValueException extends Exception {
        
        InitParamValueException(String initParamName, String initParamValue, Throwable casue) {
            super("Failed to set the " + StringUtil.jQuote(initParamName) + " servlet init-param to "
                    + StringUtil.jQuote(initParamValue) + "; see cause exception.",
                    casue);
        }

        public InitParamValueException(String initParamName, String initParamValue, String cause) {
            super("Failed to set the " + StringUtil.jQuote(initParamName) + " servlet init-param to "
                    + StringUtil.jQuote(initParamValue) + ": " + cause);
        }
        
    }
    
    private static class ConflictingInitParamsException extends Exception {
        
        ConflictingInitParamsException(String recommendedName, String otherName) {
            super("Conflicting servlet init-params: "
                    + StringUtil.jQuote(recommendedName) + " and " + StringUtil.jQuote(otherName)
                    + ". Only use " + StringUtil.jQuote(recommendedName) + ".");
        }
    }

    private static class MalformedWebXmlException extends Exception {

        MalformedWebXmlException(String message) {
            super(message);
        }
        
    }
    
}
