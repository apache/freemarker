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

package org.apache.freemarker.servlet;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Configuration.ExtendableBuilder;
import org.apache.freemarker.core.ConfigurationException;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.MutableProcessingConfiguration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.TemplateExceptionHandler;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.impl.SimpleHash;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.ClassTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.FileTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.MultiTemplateLoader;
import org.apache.freemarker.core.util._SecurityUtil;
import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.servlet.jsp.TaglibFactory;
import org.apache.freemarker.servlet.jsp.TaglibFactory.MetaInfTldSource;
import org.apache.freemarker.servlet.jsp.TaglibFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
 * your servlet container, just as if you used {@code ServletRequest.getRequestDispatcher(path).include()}: {@code 
 * <@include_page path="/myWebapp/somePage.jsp"/>}. You can also pass parameters to the newly included page by passing a
 * hash named {@code params}:
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
 * <li><strong>{@value #INIT_PARAM_TEMPLATE_PATH}</strong>: Specifies the location of the template files. By default,
 * this is interpreted as a {@link ServletContext} resource path, which practically means a web application directory
 * relative path, or a {@code WEB-INF/lib/*.jar/META-INF/resources}-relative path (note that this last haven't always
 * worked before FreeMarker 2.3.23).<br>
 * Alternatively, you can prepend it with <tt>file://</tt> to indicate a literal path in the file system (i.e.
 * <tt>file:///var/www/project/templates/</tt>). Note that three slashes were used to specify an absolute path.<br>
 * Also, you can prepend it with {@code classpath:}, like in <tt>classpath:com/example/templates</tt>, to indicate that
 * you want to load templates from the specified package accessible through the Thread Context Class Loader of the
 * thread that initializes this servlet.<br>
 * If {@code incompatible_improvements} is set to 2.3.22 (or higher), you can specify multiple comma separated locations
 * inside square brackets, like: {@code [ WEB-INF/templates, classpath:com/example/myapp/templates ]}. This internally
 * creates a {@link MultiTemplateLoader}. Note again that if {@code incompatible_improvements} isn't set to at least
 * 2.3.22, the initial {@code [} has no special meaning, and so this feature is unavailable.<br>
 * Any of the above can have a {@code ?setting(name=value, ...)} postfix to set the JavaBeans properties of the
 * {@link TemplateLoader} created. For example,
 * {@code /templates?settings(attemptFileAccess=false, URLConnectionUsesCaches=true)} calls
 * {@link WebAppTemplateLoader#setAttemptFileAccess(boolean)} and
 * {@link WebAppTemplateLoader#setURLConnectionUsesCaches(Boolean)} to tune the {@link WebAppTemplateLoader}. For
 * backward compatibility (not recommended!), you can use the {@code class://} prefix, like in
 * <tt>class://com/example/templates</tt> format, which is similar to {@code classpath:}, except that it uses the
 * defining class loader of this servlet's class. This can cause template-not-found errors, if that class (in
 * {@code freemarer.jar} usually) is not local to the web application, while the templates are.<br>
 * The default value is <tt>class://</tt> (that is, the root of the class hierarchy), which is not recommended anymore,
 * and should be overwritten with the {@value #INIT_PARAM_TEMPLATE_PATH} init-param.</li>
 * 
 * <li><strong>{@value #INIT_PARAM_NO_CACHE}</strong>: If set to {@code true}, generates headers in the response that
 * advise the HTTP client not to cache the returned page. If {@code false}, the HTTP response is not modified for this
 * purpose. The default is {@code false}.</li>
 * 
 * <li><strong>{@value #INIT_PARAM_CONTENT_TYPE}</strong>: The Content-type HTTP header value used in the HTTP responses
 * when nothing else specifies the MIME type. The things that may specify the MIME type (and hence this init-param is
 * ignored), starting with the highest precedence, are:
 * <ol>
 * <li>If the {@value #INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE} init-param is {@value #INIT_PARAM_VALUE_NEVER} (the
 * default is {@value #INIT_PARAM_VALUE_ALWAYS}), then the value of {@link HttpServletResponse#getContentType()} is used
 * if that's non-{@code null}.
 * <li>The template's <tt>content_type</tt> custom setting, usually specified via the <tt>attributes</tt> parameter of
 * the <tt>&lt;#ftl&gt;</tt> directive. This is a legacy feature, deprecated by the {@link OutputFormat} mechanism.
 * <li>The {@linkplain Template#getOutputFormat() output format of the template}, if that has non-{@code null} MIME-type
 * ({@link OutputFormat#getMimeType()}). When a template has no output format specified, {@link UndefinedOutputFormat}
 * is used, which has {@code null} MIME-type. (The output format of a template is deduced from {@link Configuration}
 * settings, or can be specified directly in the template, like {@code <#ftl outputFormat="HTML">}. See the FreeMarker
 * Manual for more about the output format mechanism. Note that setting an output format may turns on auto-escaping, so
 * it's not just about MIME types.)
 * <li>If the {@value #INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE} init-param is not {@value #INIT_PARAM_VALUE_ALWAYS}
 * (the default is {@value #INIT_PARAM_VALUE_ALWAYS}), then the value of {@link HttpServletResponse#getContentType()} is
 * used if that's non-{@code null}.
 * </ol>
 * If none of the above gives a MIME type, then this init-param does. Defaults to <tt>"text/html"</tt>. If and only if
 * the {@value #INIT_PARAM_RESPONSE_CHARACTER_ENCODING} init-param is set to {@value #INIT_PARAM_VALUE_LEGACY} (which is
 * the default of it), the content type may include the charset (as in <tt>"text/html; charset=utf-8"</tt>), in which
 * case that specifies the actual charset of the output. If the the {@value #INIT_PARAM_RESPONSE_CHARACTER_ENCODING}
 * init-param is not set to {@value #INIT_PARAM_VALUE_LEGACY}, then specifying the charset in the
 * {@value #INIT_PARAM_CONTENT_TYPE} init-param is not allowed, and will cause servlet initialization error.</li>
 *
 * <li><strong>{@value #INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE}</strong> (since 2.3.24): Specifies when we should
 * override the {@code contentType} that might be already set (i.e., non-{@code null}) in the
 * {@link HttpServletResponse}. The default is {@value #INIT_PARAM_VALUE_ALWAYS}, which means that we always set the
 * content type. Another possible value is {@value #INIT_PARAM_VALUE_NEVER}, which means that we don't set the content
 * type in the response, unless {@link HttpServletResponse#getContentType()} is {@code null}. The third possible value
 * is {@value #INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE}, which means that we only set the content type if either
 * the template has an associated {@link OutputFormat} with non-{@code null} {@link OutputFormat#getMimeType()}, or it
 * has a custom setting with name <tt>content_type</tt>, or {@link HttpServletResponse#getContentType()} is
 * {@code null}. Setting this init-param allows you to specify the content type before forwarding to
 * {@link FreemarkerServlet}.</li>
 *
 * <li><strong>{@value #INIT_PARAM_OVERRIDE_RESPONSE_LOCALE}</strong> (since 2.3.24): Specifies if we should override
 * the template {@code locale} that might be already set (i.e., non-{@code null}) in the {@link HttpServletRequest}. The
 * default is {@value #INIT_PARAM_VALUE_ALWAYS}, which means that we always deduce the template {@code locale} by
 * invoking {@link #deduceLocale(String, HttpServletRequest, HttpServletResponse)}. Another possible value is
 * {@value #INIT_PARAM_VALUE_NEVER}, which means that we don't deduce the template {@code locale}, unless
 * {@link HttpServletRequest#getLocale()} is {@code null}.
 * 
 * <li><strong>{@value #INIT_PARAM_RESPONSE_CHARACTER_ENCODING}</strong> (since 2.3.24): Specifies how the
 * {@link HttpServletResponse} "character encoding" (as in {@link HttpServletResponse#setCharacterEncoding(String)})
 * will be deduced. The possible modes are:
 * <ul>
 * <li>{@value #INIT_PARAM_VALUE_LEGACY}: This is the default for backward compatibility; in new applications, use
 * {@value #INIT_PARAM_VALUE_FROM_TEMPLATE} (or some of the other options) instead. {@value #INIT_PARAM_VALUE_LEGACY}
 * will use the charset of the template file to set the charset of the servlet response. Except, if the
 * {@value #INIT_PARAM_CONTENT_TYPE} init-param contains a charset, it will use that instead. A quirk of this legacy
 * mode is that it's not aware of the {@link MutableProcessingConfiguration#getOutputEncoding()} FreeMarker setting, and thus never reads
 * or writes it (though very few applications utilize that setting anyway). Also, it sets the charset of the servlet
 * response by adding it to the response content type via calling {@link HttpServletResponse#setContentType(String)} (as
 * that was the only way before Servlet 2.4), not via the more modern
 * {@link HttpServletResponse#setCharacterEncoding(String)} method. Note that the charset of a template usually comes
 * from {@link Configuration#getSourceEncoding()} (i.e., from the {@code encoding} FreeMarker setting),
 * or occasionally from {@link Configuration#getTemplateConfigurations()} (when FreeMarker was
 * configured to use a specific charset for certain templates).
 * <li>{@value #INIT_PARAM_VALUE_FROM_TEMPLATE}: This should be used in most applications, but it's not the default for
 * backward compatibility. It reads the {@link MutableProcessingConfiguration#getOutputEncoding()} setting of the template (note that the
 * template usually just inherits that from the {@link Configuration}), and if that's not set, then reads the source
 * charset of the template, just like {@value #INIT_PARAM_VALUE_LEGACY}, and if that's {@code null} (which happens if
 * the template was loaded from a non-binary source) then it will be UTF-8. Then it passes the charset acquired this way
 * to {@link HttpServletResponse#setCharacterEncoding(String)} and {@link Environment#setOutputEncoding(Charset)}. (It
 * doesn't call the legacy {@link HttpServletResponse#setContentType(String)} API to set the charset.) (Note that if the
 * template has a {@code content_type} template attribute (which is deprecated) that specifies a charset, it will be
 * used as the output charset of that template.)
 * <li>{@value #INIT_PARAM_VALUE_DO_NOT_SET}: {@link FreemarkerServlet} will not set the {@link HttpServletResponse}
 * "character encoding". It will still call {@link Environment#setOutputEncoding(Charset)}, so that the running template
 * will be aware of the charset used for the output.
 * <li>{@value #INIT_PARAM_VALUE_FORCE_PREFIX} + charset name, for example {@code force UTF-8}: The output charset will
 * be the one specified after "force" + space, regardless of everything. The charset specified this way is passed to
 * {@link HttpServletResponse#setCharacterEncoding(String)} and {@link Environment#setOutputEncoding(Charset)}. If the
 * charset name is not recognized by Java, the servlet initialization will fail.
 * </ul>
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
 * optionally followed by colon and a regular expression, or {@value #META_INF_TLD_LOCATION_CLEAR}. For example {@code 
 * <param-value>classpath:.*myoverride.*\.jar$, webInfPerLibJars, classpath:.*taglib.*\.jar$</param-value>}, or {@code 
 * <param-value>classpath</param-value>}. (Whitespace around the commas and list items will be ignored.) See
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
 * Instead, use init-params with the setting names documented at
 * {@link ExtendableBuilder#setSetting(String, String)}, such as {@code object_wrapper}.
 * 
 * <li><strong>Any other init-params</strong> will be interpreted as {@link Configuration}-level FreeMarker setting. See
 * the possible names and values at {@link ExtendableBuilder#setSetting(String, String)}. Note that
 * these init-param names are starting with lower-case letter (upper-case init-params are used for FreemarkerSerlvet
 * settings).</li>
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
 * {@link Logger}), under the "org.apache.freemarker.servlet" category.</li>
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
 * <li>If the template contains parsing errors, it will log it with error level, then the servlet throws
 * {@link ServletException} to the servlet container (with the proper cause exception).</li>
 * 
 * <li>If the template throws exception during its execution, and the value of the {@code template_exception_handler}
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
// [FM3] Lot of things are marked here with "BC" and deprecated
public class FreemarkerServlet extends HttpServlet {
    
    private static final Logger LOG = LoggerFactory.getLogger(FreemarkerServlet.class);

    public static final long serialVersionUID = -2440216393145762479L;

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params. (This init-param
     * has existed long before 2.3.22, but this constant was only added then.)
     */
    public static final String INIT_PARAM_TEMPLATE_PATH = "TemplatePath";
    
    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params. (This init-param
     * has existed long before 2.3.22, but this constant was only added then.)
     */
    public static final String INIT_PARAM_NO_CACHE = "NoCache";

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params. (This init-param
     * has existed long before 2.3.22, but this constant was only added then.)
     */
    public static final String INIT_PARAM_CONTENT_TYPE = "ContentType";

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     */
    public static final String INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE = "OverrideResponseContentType";

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     */
    public static final String INIT_PARAM_RESPONSE_CHARACTER_ENCODING = "ResponseCharacterEncoding";

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     */
    public static final String INIT_PARAM_OVERRIDE_RESPONSE_LOCALE = "OverrideResponseLocale";

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     */
    public static final String INIT_PARAM_BUFFER_SIZE = "BufferSize";
    
    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     */
    public static final String INIT_PARAM_META_INF_TLD_LOCATIONS = "MetaInfTldSources";

    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     */
    public static final String INIT_PARAM_EXCEPTION_ON_MISSING_TEMPLATE = "ExceptionOnMissingTemplate";
    
    /**
     * Init-param name - see the {@link FreemarkerServlet} class documentation about the init-params.
     */
    public static final String INIT_PARAM_CLASSPATH_TLDS = "ClasspathTlds";
    
    private static final String INIT_PARAM_DEBUG = "Debug";

    private static final String DEPR_INITPARAM_TEMPLATE_DELAY = "TemplateDelay";
    private static final String DEPR_INITPARAM_ENCODING = "DefaultEncoding";
    private static final String DEPR_INITPARAM_OBJECT_WRAPPER = "ObjectWrapper";
    private static final String DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER = "TemplateExceptionHandler";
    private static final String DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_RETHROW = "rethrow";
    private static final String DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_DEBUG = "debug";
    private static final String DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_HTML_DEBUG = "htmlDebug";
    private static final String DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_IGNORE = "ignore";
    private static final String DEPR_INITPARAM_DEBUG = "debug";
    
    private static final ContentType DEFAULT_CONTENT_TYPE = new ContentType("text/html");
    
    public static final String INIT_PARAM_VALUE_NEVER = "never";
    public static final String INIT_PARAM_VALUE_ALWAYS = "always";
    public static final String INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE = "whenTemplateHasMimeType";
    public static final String INIT_PARAM_VALUE_FROM_TEMPLATE = "fromTemplate";
    public static final String INIT_PARAM_VALUE_LEGACY = "legacy";
    public static final String INIT_PARAM_VALUE_DO_NOT_SET = "doNotSet";
    public static final String INIT_PARAM_VALUE_FORCE_PREFIX = "force ";

    /**
     * When set, the items defined in it will be added after those coming from the
     * {@value #INIT_PARAM_META_INF_TLD_LOCATIONS} init-param. The value syntax is the same as of the init-param. Note
     * that {@value #META_INF_TLD_LOCATION_CLEAR} can be used to re-start the list, rather than continue it.
     */
    public static final String SYSTEM_PROPERTY_META_INF_TLD_SOURCES = "org.freemarker.jsp.metaInfTldSources";

    /**
     * When set, the items defined in it will be added after those coming from the
     * {@value #INIT_PARAM_CLASSPATH_TLDS} init-param. The value syntax is the same as of the init-param.
     */
    public static final String SYSTEM_PROPERTY_CLASSPATH_TLDS = "org.freemarker.jsp.classpathTlds";
    
    /**
     * Used as part of the value of the {@value #INIT_PARAM_META_INF_TLD_LOCATIONS} init-param.
     */
    public static final String META_INF_TLD_LOCATION_WEB_INF_PER_LIB_JARS = "webInfPerLibJars";
    
    /**
     * Used as part of the value of the {@value #INIT_PARAM_META_INF_TLD_LOCATIONS} init-param.
     */
    public static final String META_INF_TLD_LOCATION_CLASSPATH = "classpath";
    
    /**
     * Used as part of the value of the {@value #INIT_PARAM_META_INF_TLD_LOCATIONS} init-param.
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
    public static final String ATTR_REQUEST_MODEL = ".freemarker.Request";
    public static final String ATTR_REQUEST_PARAMETERS_MODEL = ".freemarker.RequestParameters";
    public static final String ATTR_SESSION_MODEL = ".freemarker.Session";
    
    /** @deprecated We only keeps this attribute for backward compatibility, but actually aren't using it. */
    @Deprecated
    private static final String ATTR_APPLICATION_MODEL = ".freemarker.Application";
    
    /** @deprecated We only keeps this attribute for backward compatibility, but actually aren't using it. */
    @Deprecated
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
    private boolean noCache;
    private Integer bufferSize;
    private boolean exceptionOnMissingTemplate;
    
    /**
     * @deprecated Not used anymore; to enable/disable debug logging, just set the logging level of the logging library
     *             used by {@link Logger}.
     */
    @Deprecated
    protected boolean debug;
    
    @SuppressFBWarnings(value="SE_BAD_FIELD", justification="Not investing into making this Servlet serializable")
    private Configuration config;
    private ContentType contentType;
    private OverrideResponseContentType overrideResponseContentType = initParamValueToEnum(
            getDefaultOverrideResponseContentType(), OverrideResponseContentType.values());
    private ResponseCharacterEncoding responseCharacterEncoding = ResponseCharacterEncoding.LEGACY;
    private Charset forcedResponseCharacterEncoding;
    private OverrideResponseLocale overrideResponseLocale = OverrideResponseLocale.ALWAYS;
    private List/*<MetaInfTldSource>*/ metaInfTldSources;
    private List/*<String>*/ classpathTlds;

    private Object lazyInitFieldsLock = new Object();
    @SuppressFBWarnings(value="SE_BAD_FIELD", justification="Not investing into making this Servlet serializable")
    private ServletContextHashModel servletContextModel;
    @SuppressFBWarnings(value="SE_BAD_FIELD", justification="Not investing into making this Servlet serializable")
    private TaglibFactory taglibFactory;
    
    private boolean objectWrapperMismatchWarnLogged;

    /**
     * Don't override this method to adjust FreeMarker settings! Override the protected methods for that, such as
     * {@link #createConfigurationBuilder()}, {@link #createTemplateLoader(String)},
     * etc. Also note that lot of things can be changed with init-params instead of overriding methods, so if you
     * override settings, usually you should only override their defaults.
     */
    @Override
    public void init() throws ServletException {
        try {
            initialize();
        } catch (Exception e) {
            // At least Jetty doesn't log the ServletException itself, only its cause exception. Thus we add some
            // message here that (re)states the obvious.
            throw new ServletException("Error while initializing " + getClass().getName()
                    + " servlet; see cause exception.", e);
        }
    }
    
    private void initialize() throws InitParamValueException, MalformedWebXmlException, ConflictingInitParamsException,
            ConfigurationException {
        Configuration.ExtendableBuilder<?> cfgB = createConfigurationBuilder();
        
        // Only override what's coming from the config if it was explicitly specified: 
        final String iciInitParamValue = getInitParameter(Configuration.ExtendableBuilder.INCOMPATIBLE_IMPROVEMENTS_KEY);
        if (iciInitParamValue != null) {
            try {
                cfgB.setSetting(Configuration.ExtendableBuilder.INCOMPATIBLE_IMPROVEMENTS_KEY, iciInitParamValue);
            } catch (Exception e) {
                throw new InitParamValueException(Configuration.ExtendableBuilder.INCOMPATIBLE_IMPROVEMENTS_KEY, iciInitParamValue, e);
            }
        }

        contentType = DEFAULT_CONTENT_TYPE;
        
        // Process object_wrapper init-param out of order:
        String objectWrapperInitParamValue = getInitParameter(
                Configuration.Builder.OBJECT_WRAPPER_KEY, DEPR_INITPARAM_OBJECT_WRAPPER);
        if (objectWrapperInitParamValue != null) {
            setObjectWrapperFromInitParam(cfgB, objectWrapperInitParamValue);
        }

        // Process TemplatePath init-param out of order:
        String templatePath = getInitParameter(INIT_PARAM_TEMPLATE_PATH);
        if (templatePath != null) {
            try {
                cfgB.setTemplateLoader(createTemplateLoader(templatePath));
            } catch (Exception e) {
                throw new InitParamValueException(INIT_PARAM_TEMPLATE_PATH, templatePath, e);
            }
        }
        
        metaInfTldSources = createDefaultMetaInfTldSources();
        classpathTlds = createDefaultClassPathTlds();

        // Process all other init-params:
        for (Enumeration initPNames = getServletConfig().getInitParameterNames(); initPNames.hasMoreElements();) {
            final String name = (String) initPNames.nextElement();
            final String value = getInitParameter(name);
            if (name == null) {
                throw new MalformedWebXmlException(
                        "init-param without param-name. "
                        + "Maybe the web.xml is not well-formed?");
            }
            if (value == null) {
                throw new MalformedWebXmlException(
                        "init-param " + _StringUtil.jQuote(name) + " without param-value. "
                        + "Maybe the web.xml is not well-formed?");
            }
            
            try {
                if (name.equals(DEPR_INITPARAM_OBJECT_WRAPPER)
                        || name.equals(Configuration.Builder.OBJECT_WRAPPER_KEY)
                        || name.equals(INIT_PARAM_TEMPLATE_PATH)
                        || name.equals(Configuration.ExtendableBuilder.INCOMPATIBLE_IMPROVEMENTS_KEY)) {
                    // ignore: we have already processed these
                } else if (name.equals(DEPR_INITPARAM_ENCODING)) { // BC
                    if (getInitParameter(Configuration.ExtendableBuilder.SOURCE_ENCODING_KEY) != null) {
                        throw new ConflictingInitParamsException(
                                Configuration.ExtendableBuilder.SOURCE_ENCODING_KEY, DEPR_INITPARAM_ENCODING);
                    }
                    cfgB.setSourceEncoding(Charset.forName(value));
                } else if (name.equals(DEPR_INITPARAM_TEMPLATE_DELAY)) { // BC
                    if (getInitParameter(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY) != null) {
                        throw new ConflictingInitParamsException(
                                Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, DEPR_INITPARAM_TEMPLATE_DELAY);
                    }
                    try {
                        cfgB.setTemplateUpdateDelayMilliseconds(Integer.parseInt(value) * 1000L);
                    } catch (NumberFormatException e) {
                        // Intentionally ignored
                    }
                } else if (name.equals(DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER)) { // BC
                    if (getInitParameter(MutableProcessingConfiguration.TEMPLATE_EXCEPTION_HANDLER_KEY) != null) {
                        throw new ConflictingInitParamsException(
                                MutableProcessingConfiguration.TEMPLATE_EXCEPTION_HANDLER_KEY, DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER);
                    }
    
                    if (DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_RETHROW.equals(value)) {
                        cfgB.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW);
                    } else if (DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_DEBUG.equals(value)) {
                        cfgB.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG);
                    } else if (DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_HTML_DEBUG.equals(value)) {
                        cfgB.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG);
                    } else if (DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER_IGNORE.equals(value)) {
                        cfgB.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE);
                    } else {
                        throw new InitParamValueException(DEPR_INITPARAM_TEMPLATE_EXCEPTION_HANDLER, value,
                                "Not one of the supported values.");
                    }
                } else if (name.equals(INIT_PARAM_NO_CACHE)) {
                    noCache = _StringUtil.getYesNo(value);
                } else if (name.equals(INIT_PARAM_BUFFER_SIZE)) {
                    bufferSize = Integer.valueOf(parseSize(value));
                } else if (name.equals(DEPR_INITPARAM_DEBUG)) { // BC
                    if (getInitParameter(INIT_PARAM_DEBUG) != null) {
                        throw new ConflictingInitParamsException(INIT_PARAM_DEBUG, DEPR_INITPARAM_DEBUG);
                    }
                    debug = _StringUtil.getYesNo(value);
                } else if (name.equals(INIT_PARAM_DEBUG)) {
                    debug = _StringUtil.getYesNo(value);
                } else if (name.equals(INIT_PARAM_CONTENT_TYPE)) {
                    contentType = new ContentType(value);
                } else if (name.equals(INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE)) {
                    overrideResponseContentType = initParamValueToEnum(value, OverrideResponseContentType.values());
                } else if (name.equals(INIT_PARAM_RESPONSE_CHARACTER_ENCODING)) {
                    responseCharacterEncoding = initParamValueToEnum(value, ResponseCharacterEncoding.values());
                    if (responseCharacterEncoding == ResponseCharacterEncoding.FORCE_CHARSET) {
                        String charsetName = value.substring(INIT_PARAM_VALUE_FORCE_PREFIX.length()).trim();
                        forcedResponseCharacterEncoding = Charset.forName(charsetName);
                    }
                } else if (name.equals(INIT_PARAM_OVERRIDE_RESPONSE_LOCALE)) {
                    overrideResponseLocale = initParamValueToEnum(value, OverrideResponseLocale.values());
                } else if (name.equals(INIT_PARAM_EXCEPTION_ON_MISSING_TEMPLATE)) {
                    exceptionOnMissingTemplate = _StringUtil.getYesNo(value);
                } else if (name.equals(INIT_PARAM_META_INF_TLD_LOCATIONS)) {
                    metaInfTldSources = TaglibFactoryBuilder.parseMetaInfTldLocations(InitParamParser.parseCommaSeparatedList(value));
                } else if (name.equals(INIT_PARAM_CLASSPATH_TLDS)) {
                    List newClasspathTlds = new ArrayList();
                    if (classpathTlds != null) {
                        newClasspathTlds.addAll(classpathTlds);
                    }
                    newClasspathTlds.addAll(InitParamParser.parseCommaSeparatedList(value));
                    classpathTlds = newClasspathTlds;
                } else {
                    cfgB.setSetting(name, value);
                }
            } catch (ConflictingInitParamsException e) {
                throw e;
            } catch (Exception e) {
                throw new InitParamValueException(name, value, e);
            }
        } // for initPNames
        
        if (contentType.containsCharset && responseCharacterEncoding != ResponseCharacterEncoding.LEGACY) {
            throw new InitParamValueException(INIT_PARAM_CONTENT_TYPE, contentType.httpHeaderValue,
                    new IllegalStateException("You can't specify the charset in the content type, because the \"" +
                            INIT_PARAM_RESPONSE_CHARACTER_ENCODING + "\" init-param isn't set to "
                            + "\"" + INIT_PARAM_VALUE_LEGACY + "\"."));
        }

        beforeConfigurationBuilt(cfgB);
        config = cfgB.build();
        afterConfigurationBuilt(config);

        if (!(config.getObjectWrapper() instanceof ObjectWrapperAndUnwrapper)) {
            throw new RuntimeException(FreemarkerServlet.class.getSimpleName() + " requires an ObjectWrapper that " +
                    "implements " + ObjectWrapperAndUnwrapper.class.getName() + ", but this class doesn't do that: "
                    + config.getObjectWrapper().getClass().getName());
        }
        LOG.debug("Using object wrapper {}", config.getObjectWrapper());
    }

    /**
     * Create the template loader. The default implementation will create a {@link ClassTemplateLoader} if the template
     * path starts with {@code "class://"}, a {@link FileTemplateLoader} if the template path starts with
     * {@code "file://"}, and a {@link WebAppTemplateLoader} otherwise. Also, if
     * {@link Configuration#getIncompatibleImprovements()}  incompatibleImprovements} is 2.3.22 or higher,
     * it will create a {@link MultiTemplateLoader} if the template path starts with {@code "["}.
     * 
     * @param templatePath
     *            the template path to create a loader for
     * @return a newly created template loader
     */
    protected TemplateLoader createTemplateLoader(String templatePath) throws IOException {
        return InitParamParser.createTemplateLoader(templatePath, getClass(), getServletContext());
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        process(request, response);
    }

    @Override
    public void doPost(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {
        process(request, response);
    }

    private void process(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {
        // Give chance to subclasses to perform preprocessing
        if (preprocessRequest(request, response)) {
            return;
        }
        
        if (bufferSize != null && !response.isCommitted()) {
            try {
                response.setBufferSize(bufferSize.intValue());
            } catch (IllegalStateException e) {
                LOG.debug("Can't set the response buffer size any more", e);
            }
        }

        String templatePath = requestUrlToTemplatePath(request);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Requested template " + _StringUtil.jQuoteNoXSS(templatePath) + ".");
        }

        Locale locale = request.getLocale();
        if (locale == null || overrideResponseLocale != OverrideResponseLocale.NEVER) {
            locale = deduceLocale(templatePath, request, response);
        }

        final Template template;
        try {
            template = config.getTemplate(templatePath, locale);
        } catch (TemplateNotFoundException e) {
            if (exceptionOnMissingTemplate) {
                throw newServletExceptionWithFreeMarkerLogging(
                        "Template not found for name " + _StringUtil.jQuoteNoXSS(templatePath) + ".", e);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Responding HTTP 404 \"Not found\" for missing template "
                            + _StringUtil.jQuoteNoXSS(templatePath) + ".", e);
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Page template not found");
                return;
            }
        } catch (org.apache.freemarker.core.ParseException e) {
            throw newServletExceptionWithFreeMarkerLogging(
                    "Parsing error with template " + _StringUtil.jQuoteNoXSS(templatePath) + ".", e);
        } catch (Exception e) {
            throw newServletExceptionWithFreeMarkerLogging(
                    "Unexpected error when loading template " + _StringUtil.jQuoteNoXSS(templatePath) + ".", e);
        }

        boolean tempSpecContentTypeContainsCharset = false;
        if (response.getContentType() == null || overrideResponseContentType != OverrideResponseContentType.NEVER) {
            ContentType templateSpecificContentType = getTemplateSpecificContentType(template);
            if (templateSpecificContentType != null) {
                // With ResponseCharacterEncoding.LEGACY we should append the charset, but we don't do that for b. c.
                response.setContentType(
                        responseCharacterEncoding != ResponseCharacterEncoding.DO_NOT_SET
                                ? templateSpecificContentType.httpHeaderValue
                                : templateSpecificContentType.getMimeType());
                tempSpecContentTypeContainsCharset = templateSpecificContentType.containsCharset;
            } else if (response.getContentType() == null
                    || overrideResponseContentType == OverrideResponseContentType.ALWAYS) {
                if (responseCharacterEncoding == ResponseCharacterEncoding.LEGACY && !contentType.containsCharset) {
                    // In legacy mode we don't call response.setCharacterEncoding, so the charset must be set here:
                    response.setContentType(
                            contentType.httpHeaderValue + "; charset=" + getOutputEncodingForTemplate(template));
                } else {
                    response.setContentType(contentType.httpHeaderValue);
                }
            }
        }
        
        if (responseCharacterEncoding != ResponseCharacterEncoding.LEGACY
                && responseCharacterEncoding != ResponseCharacterEncoding.DO_NOT_SET) {
            // Using the Servlet 2.4 way of setting character encoding.
            if (responseCharacterEncoding != ResponseCharacterEncoding.FORCE_CHARSET) {
                if (!tempSpecContentTypeContainsCharset) {
                    response.setCharacterEncoding(getOutputEncodingForTemplate(template).name());
                }
            } else {
                response.setCharacterEncoding(forcedResponseCharacterEncoding.name());
            }
        }

        setBrowserCachingPolicy(response);

        ServletContext servletContext = getServletContext();
        try {
            TemplateModel model = createModel(
                    (ObjectWrapperAndUnwrapper) config.getObjectWrapper(), // This is checked in initialize()
                    servletContext, request, response);

            // Give subclasses a chance to hook into preprocessing
            if (preTemplateProcess(request, response, template, model)) {
                try {
                    // Process the template
                    Environment env = template.createProcessingEnvironment(model, response.getWriter());
                    if (responseCharacterEncoding != ResponseCharacterEncoding.LEGACY) {
                        String actualOutputCharsetName = response.getCharacterEncoding();
                        if (actualOutputCharsetName != null) {
                            Charset actualOutputCharset = null;
                            try {
                                actualOutputCharset = Charset.forName(actualOutputCharsetName);
                            } catch (Exception e) {
                                throw new IllegalStateException(
                                        "Failed to resolve charset name returned by "
                                        + " HttpServletResponse.getCharacterEncoding(): "
                                        + _StringUtil.jQuote(actualOutputCharsetName),
                                        e);
                            }
                            env.setOutputEncoding(actualOutputCharset);
                        }
                    }
                    processEnvironment(env, request, response);
                } finally {
                    // Give subclasses a chance to hook into postprocessing
                    postTemplateProcess(request, response, template, model);
                }
            }
        } catch (TemplateException e) {
            final TemplateExceptionHandler teh = config.getTemplateExceptionHandler();
            // Ensure that debug handler responses aren't rolled back:
            if (teh == TemplateExceptionHandler.HTML_DEBUG || teh == TemplateExceptionHandler.DEBUG
                    || teh.getClass().getName().indexOf("Debug") != -1) {
                response.flushBuffer();
            }
            throw newServletExceptionWithFreeMarkerLogging("Error executing FreeMarker template", e);
        }
    }

    /**
     * This is the method that actually executes the template. The original implementation coming from
     * {@link FreemarkerServlet} simply calls {@link Environment#process()}. Overriding this method allows you to
     * prepare the {@link Environment} before the execution, or extract information from the {@link Environment} after
     * the execution. It also allows you to capture exceptions throw by the template.
     * 
     * @param env
     *            The {@link Environment} object already set up to execute the template. You only have to call
     *            {@link Environment#process()} and the output will be produced by the template.
     */
    protected void processEnvironment(Environment env, HttpServletRequest request, HttpServletResponse response)
            throws TemplateException, IOException {
        env.process();
    }

    private Charset getOutputEncodingForTemplate(Template template) {
        Charset outputEncoding = responseCharacterEncoding == ResponseCharacterEncoding.LEGACY ? null
                : template.getOutputEncoding();
        // [FM3] Don't use template.getSourceEncoding() here; it might can't encode the dynamic values inserted.
        return outputEncoding != null ? outputEncoding
                : template.getActualSourceEncoding() != null ? template.getActualSourceEncoding()
                : StandardCharsets.UTF_8;
    }

    private ContentType getTemplateSpecificContentType(final Template template) {
        Object contentTypeAttr = template.getCustomSetting("content_type", null);
        if (contentTypeAttr != null) {
            // Converted with toString() for backward compatibility.
            return new ContentType(contentTypeAttr.toString());
        }
        
        String outputFormatMimeType = template.getOutputFormat().getMimeType();
        if (outputFormatMimeType != null) {
            if (responseCharacterEncoding == ResponseCharacterEncoding.LEGACY) {
                // In legacy mode we won't call serlvetResponse.setCharacterEncoding(...), so:
                return new ContentType(
                        outputFormatMimeType + "; charset=" + getOutputEncodingForTemplate(template),
                        true);
            } else {
                return new ContentType(outputFormatMimeType, false);
            }
        }
            
        return null;
    }

    private ServletException newServletExceptionWithFreeMarkerLogging(String message, Throwable cause) throws ServletException {
        if (cause instanceof TemplateException) {
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
    
    /**
     * Returns the locale used for the {@link Configuration#getTemplate(String, Locale)} call (as far as the
     * {@value #INIT_PARAM_OVERRIDE_RESPONSE_LOCALE} Servlet init-param allows that). The base implementation in
     * {@link FreemarkerServlet} simply returns the {@code locale} setting of the configuration. Override this method to
     * provide different behavior, for example, to use the locale indicated in the HTTP request.
     * 
     * @param templatePath
     *            The template path (template name) as it will be passed to {@link Configuration#getTemplate(String)}.
     *            (Not to be confused with the servlet init-param of identical name; they aren't related.)
     * 
     * @throws ServletException
     *             Can be thrown since 2.3.22, if the locale can't be deduced from the URL.
     */
    protected Locale deduceLocale(String templatePath, HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        return config.getLocale();
    }

    protected TemplateModel createModel(ObjectWrapperAndUnwrapper objectWrapper,
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
            if (session != null) {
                sessionModel = (HttpSessionHashModel) session.getAttribute(ATTR_SESSION_MODEL);
                if (sessionModel == null || sessionModel.isOrphaned(session)) {
                    sessionModel = new HttpSessionHashModel(session, objectWrapper);
                    session.setAttribute(ATTR_SESSION_MODEL, sessionModel);
                    initializeSession(request, response);
                }
            } else {
                sessionModel = new HttpSessionHashModel(this, request, response, objectWrapper);
            }
            params.putUnlistedModel(KEY_SESSION, sessionModel);
    
            // Create hash model wrapper for request
            HttpRequestHashModel requestModel =
                (HttpRequestHashModel) request.getAttribute(ATTR_REQUEST_MODEL);
            if (requestModel == null || requestModel.getRequest() != request) {
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
     */
    @SuppressWarnings("unchecked")
    protected TaglibFactory createTaglibFactory(ObjectWrapper objectWrapper, ServletContext servletContext)
            throws TemplateModelException {

        List<MetaInfTldSource> metaInfTldSourcesFromSysProp = null;
        try {
            final String prop = _SecurityUtil.getSystemProperty(SYSTEM_PROPERTY_META_INF_TLD_SOURCES, null);
            metaInfTldSourcesFromSysProp = (List<MetaInfTldSource>) ((prop != null)
                    ? TaglibFactoryBuilder.parseMetaInfTldLocations(InitParamParser.parseCommaSeparatedList(prop))
                    : Collections.emptyList());
        } catch (ParseException e) {
            throw new TemplateModelException(
                    "Failed to parse system property \"" + SYSTEM_PROPERTY_META_INF_TLD_SOURCES + "\"", e);
        }

        List<Pattern> jettyTaglibJarPatterns = null;
        try {
            final String attrVal = (String) servletContext.getAttribute(ATTR_JETTY_CP_TAGLIB_JAR_PATTERNS);
            jettyTaglibJarPatterns = (attrVal != null) ? InitParamParser.parseCommaSeparatedPatterns(attrVal)
                    : Collections.emptyList();
        } catch (Exception e) {
            LOG.error("Failed to parse application context attribute \"" + ATTR_JETTY_CP_TAGLIB_JAR_PATTERNS
                    + "\" - it will be ignored", e);
        }

        List<String> classpathTldsFromSysProp = null;
        try {
            final String prop = _SecurityUtil.getSystemProperty(SYSTEM_PROPERTY_CLASSPATH_TLDS, null);
            classpathTldsFromSysProp = (prop != null) ? InitParamParser.parseCommaSeparatedList(prop)
                    : Collections.emptyList();
        } catch (ParseException e) {
            throw new TemplateModelException(
                    "Failed to parse system property \"" + SYSTEM_PROPERTY_CLASSPATH_TLDS + "\"", e);
        }

        return new TaglibFactoryBuilder(servletContext, objectWrapper)
                .addAllMetaInfTldSources(metaInfTldSources)
                .addAllMetaInfTldSources(metaInfTldSourcesFromSysProp)
                .addAllJettyMetaInfTldJarPatterns(jettyTaglibJarPatterns)
                .addAllClasspathTlds(classpathTlds)
                .addAllClasspathTlds(classpathTldsFromSysProp).build();
    }

    /**
     * Creates the default of the {@value #INIT_PARAM_CLASSPATH_TLDS} init-param; if this init-param is specified, it
     * will be appended <em>after</em> the default, not replace it.
     * 
     * <p>
     * The implementation in {@link FreemarkerServlet} returns {@link TaglibFactory#DEFAULT_CLASSPATH_TLDS}.
     * 
     * @return A {@link List} of {@link String}-s; not {@code null}.
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
     */
    protected List/*<MetaInfTldSource>*/ createDefaultMetaInfTldSources() {
        return TaglibFactory.DEFAULT_META_INF_TLD_SOURCES;
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
    protected String requestUrlToTemplatePath(HttpServletRequest request) throws ServletException {
        // First, see if it's an included request
        String includeServletPath  = (String) request.getAttribute("javax.servlet.include.servlet_path");
        if (includeServletPath != null) {
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
     * Creates a new FreeMarker {@link Configuration} builder; by providing a custom builder, the configuration
     * setting defaults can be specific to the {@link FreemarkerServlet} subclass.
     * <p>
     * The default implementation creates a new {@link FreemarkerServletConfigurationBuilder} instance (note that it's
     * not the standard {@link Configuration} builder, as some setting defaults differ) with
     * {@link Configuration#getIncompatibleImprovements() incompatibleImprovements}
     * {@link Configuration#VERSION_3_0_0}.
     * <p>
     * By overriding this method you can use your own {@link FreemarkerServletConfigurationBuilder} subclass
     * (or actually any {@link ExtendableBuilder} subclass) and hence specify what the defaults are.
     */
    protected Configuration.ExtendableBuilder<?> createConfigurationBuilder() {
        return new FreemarkerServletConfigurationBuilder(this, Configuration.VERSION_3_0_0);
    }
    
    /**
     * Hook for {@link FreemarkerServlet} subclasses to modify the configuration builder just before the
     * {@link Configuration} is created. Note that to change the defaults of some setting, you meant to use
     * {@link #createConfigurationBuilder()} instead.
     * <p>
     * The implementation in {@link FreemarkerServlet} does nothing here.
     */
    protected void beforeConfigurationBuilt(Configuration.ExtendableBuilder<?> cfgB) {
        // do nothing
    }

    /**
     * Hook for {@link FreemarkerServlet} subclasses to examine {@link Configuration} just after it was created.
     * <p>
     * The implementation in {@link FreemarkerServlet} does nothing here.
     */
    protected void afterConfigurationBuilt(Configuration cfg) {
        // do nothing
    }
    
    /**
     * Called from {@link #init()} to set the {@link ObjectWrapper} in the {@link ExtendableBuilder}
     * from the init-param value.
     * To customize init-param interpretation, see if the init-param value argument is something that you want to
     * interpret yourself, otherwise fall back to the super method. This method won't be called if there's not
     * init-param that specifies the object wrapper.
     * <p>
     * The default implementation interprets the {@code object_wrapper} servlet init-param with
     * calling {@link MutableProcessingConfiguration#setSetting(String, String)}.
     *
     * @param initParamValue Not {@code null}
     */
    protected void setObjectWrapperFromInitParam(Configuration.ExtendableBuilder<?> cb, String initParamValue)
            throws ConfigurationException {
        cb.setSetting(Configuration.Builder.OBJECT_WRAPPER_KEY, initParamValue);
    }

    protected HttpRequestParametersHashModel createRequestParametersHashModel(HttpServletRequest request) {
        return new HttpRequestParametersHashModel(request, config.getObjectWrapper());
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
    protected void initializeSession(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }

    /**
     * Called before the execution is passed to {@link Template#process(Object, java.io.Writer)}. This is a
     * generic hook you might use in subclasses to perform a specific action before the template is processed.
     *
     * @param request
     *            The HTTP request that we will response to.
     * @param response
     *            The HTTP response. The HTTP headers are already initialized here, such as the {@code conteType} and
     *            the {@code responseCharacterEncoding} are already set, but you can do the final adjustments here. The
     *            response {@link Writer} isn't created yet, so changing HTTP headers and buffering parameters works.
     * @param template
     *            The template that will get executed
     * @param model
     *            The data model that will be passed to the template. By default this will be an
     *            {@link AllHttpScopesHashModel} (which is a {@link SimpleHash} subclass). Thus, you can add new
     *            variables to the data-model with the {@link SimpleHash#put(String, Object)} subclass) method. However,
     *            to adjust the data-model, overriding {@link #createModel(ObjectWrapperAndUnwrapper, ServletContext,
     *            HttpServletRequest, HttpServletResponse)} is probably a more appropriate place.
     *
     * @return true to process the template, false to suppress template processing.
     */
    protected boolean preTemplateProcess(
        HttpServletRequest request,
        HttpServletResponse response,
        Template template,
        TemplateModel model)
        throws ServletException, IOException {
        return true;
    }

    /**
     * Called after the execution returns from {@link Template#process(Object, java.io.Writer)}.
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
        throws ServletException, IOException {
    }
    
    /**
     * Returns the {@link org.apache.freemarker.core.Configuration} object used by this servlet.
     * Please don't forget that {@link org.apache.freemarker.core.Configuration} is not thread-safe
     * when you modify it.
     */
    protected Configuration getConfiguration() {
        return config;
    }

    /**
     * Returns the default value of the {@value #INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE} Servlet init-param.
     * The method inherited from {@link FreemarkerServlet} returns {@value #INIT_PARAM_VALUE_ALWAYS}; subclasses my
     * override this.
     */
    protected String getDefaultOverrideResponseContentType() {
        return INIT_PARAM_VALUE_ALWAYS;
    }

    /**
     * If the parameter "nocache" was set to true, generate a set of headers
     * that will advise the HTTP client not to cache the returned page.
     */
    private void setBrowserCachingPolicy(HttpServletResponse res) {
        if (noCache) {
            // HTTP/1.1 + IE extensions
            res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, "
                    + "post-check=0, pre-check=0");
            // HTTP/1.0
            res.setHeader("Pragma", "no-cache");
            // Last resort for those that ignore all of the above
            res.setHeader("Expires", EXPIRATION_DATE);
        }
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
            super("Failed to set the " + _StringUtil.jQuote(initParamName) + " servlet init-param to "
                    + _StringUtil.jQuote(initParamValue) + "; see cause exception.",
                    casue);
        }

        public InitParamValueException(String initParamName, String initParamValue, String cause) {
            super("Failed to set the " + _StringUtil.jQuote(initParamName) + " servlet init-param to "
                    + _StringUtil.jQuote(initParamValue) + ": " + cause);
        }
        
    }
    
    private static class ConflictingInitParamsException extends Exception {
        
        ConflictingInitParamsException(String recommendedName, String otherName) {
            super("Conflicting servlet init-params: "
                    + _StringUtil.jQuote(recommendedName) + " and " + _StringUtil.jQuote(otherName)
                    + ". Only use " + _StringUtil.jQuote(recommendedName) + ".");
        }
    }

    private static class MalformedWebXmlException extends Exception {

        MalformedWebXmlException(String message) {
            super(message);
        }
        
    }
    
    private static class ContentType {
        private final String httpHeaderValue;
        private final boolean containsCharset;
        
        public ContentType(String httpHeaderValue) {
            this(httpHeaderValue, contentTypeContainsCharset(httpHeaderValue));
        }

        public ContentType(String httpHeaderValue, boolean containsCharset) {
            this.httpHeaderValue = httpHeaderValue;
            this.containsCharset = containsCharset;
        }
        
        private static boolean contentTypeContainsCharset(String contentType) {
            int charsetIdx = contentType.toLowerCase().indexOf("charset=");
            if (charsetIdx != -1) {
                char c = 0;
                charsetIdx--;
                while (charsetIdx >= 0) {
                    c = contentType.charAt(charsetIdx);
                    if (!Character.isWhitespace(c)) break;
                    charsetIdx--;
                }
                if (charsetIdx == -1 || c == ';') {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Extracts the MIME type without the charset specifier or other such extras.
         */
        private String getMimeType() {
            int scIdx = httpHeaderValue.indexOf(';');
            return (scIdx == -1 ? httpHeaderValue : httpHeaderValue.substring(0, scIdx)).trim();
        }
        
    }
    
    private <T extends InitParamValueEnum> T initParamValueToEnum(String initParamValue, T[] enumValues) {
        for (T enumValue : enumValues) {
            String enumInitParamValue = enumValue.getInitParamValue();
            if (initParamValue.equals(enumInitParamValue)
                    || enumInitParamValue.endsWith("}") && initParamValue.startsWith(
                            enumInitParamValue.substring(0, enumInitParamValue.indexOf("${")))) {
                return enumValue;
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(_StringUtil.jQuote(initParamValue));
        sb.append(" is not a one of the enumeration values: ");
        boolean first = true;
        for (T value : enumValues) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(_StringUtil.jQuote(value.getInitParamValue()));
        }
        throw new IllegalArgumentException(sb.toString());
    }

    private String getInitParameter(String name1, String name2) {
        String r1 = getServletConfig().getInitParameter(name1);
        String r2 = getInitParameter(name2);
        if (r1 != null && r2 != null) {
            throw new RuntimeException("Conflicting init-params: " + name1 + " and " + name2);
        }
        return r2 != null ? r2 : r1;
    }

    /**
     * Superclass of all (future) init-param value enums.
     * 
     * @see #initParamValueToEnum
     */
    private interface InitParamValueEnum {
        String getInitParamValue();
    }
    
    private enum OverrideResponseContentType implements InitParamValueEnum {
        ALWAYS(INIT_PARAM_VALUE_ALWAYS),
        NEVER(INIT_PARAM_VALUE_NEVER),
        WHEN_TEMPLATE_HAS_MIME_TYPE(INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE);

        private final String initParamValue;
        
        OverrideResponseContentType(String initParamValue) {
            this.initParamValue = initParamValue;
        }

        @Override
        public String getInitParamValue() {
            return initParamValue;
        }
    }
    
    private enum ResponseCharacterEncoding implements InitParamValueEnum {
        // [FM3] Get rid of LEGACY
        LEGACY(INIT_PARAM_VALUE_LEGACY),
        FROM_TEMPLATE(INIT_PARAM_VALUE_FROM_TEMPLATE),
        DO_NOT_SET(INIT_PARAM_VALUE_DO_NOT_SET),
        FORCE_CHARSET(INIT_PARAM_VALUE_FORCE_PREFIX + "${charsetName}");

        private final String initParamValue;
        
        ResponseCharacterEncoding(String initParamValue) {
            this.initParamValue = initParamValue;
        }

        @Override
        public String getInitParamValue() {
            return initParamValue;
        }
    }

    private enum OverrideResponseLocale implements InitParamValueEnum {
        ALWAYS(INIT_PARAM_VALUE_ALWAYS),
        NEVER(INIT_PARAM_VALUE_NEVER);

        private final String initParamValue;

        OverrideResponseLocale(String initParamValue) {
            this.initParamValue = initParamValue;
        }

        @Override
        public String getInitParamValue() {
            return initParamValue;
        }
    }


}
