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

import java.beans.IntrospectionException;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.tagext.Tag;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import freemarker.core.BugException;
import freemarker.core.Environment;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.log.Logger;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;

/**
 * A hash model associated with a servlet context that can load JSP tag libraries associated with that servlet context.
 * An instance of this class is made available in the root data model of templates executed by
 * {@link freemarker.ext.servlet.FreemarkerServlet} under key {@code JspTaglibs}. It can be added to custom servlets as
 * well to enable JSP taglib integration in them as well.
 */
public class TaglibFactory implements TemplateHashModel {

    private static final Logger LOG = Logger.getLogger("freemarker.jsp");

    private static final int FULL_URI = 0;
    private static final int ABSOLUTE_URL_PATH = 1;
    private static final int RELATIVE_URL_PATH = 2;

    private static final String STANDARD_TLD_JAR_ENTRY_PATH = "META-INF/taglib.tld";

    private final ServletContext servletContext;
    private final ObjectWrapper objectWrapper;
    private final Pattern classpathTaglibJarsPattern;

    private final Map taglibs = new HashMap();
    private final Map tldLocations = new HashMap();
    private int nextTldLocationLookupPhase = 0;

    /**
     * @deprecated Use {@link TaglibFactory#TaglibFactory(ServletContext, TaglibFactoryConfiguration)} instead,
     *             otherwise custom EL functions defined in the TLD will be ignored.
     */
    public TaglibFactory(ServletContext ctx) {
        this(ctx, null);
    }

    /**
     * Creates a new JSP taglib factory that will be used to load JSP tag libraries and functions for the web
     * application represented by the passed servlet context, using the object wrapper when invoking JSTL functions.
     * 
     * @param ctx
     *            The servlet context whose JSP tag libraries this factory will load.
     * 
     * @param cfg
     *            The configuration settings of this taglib factory. Can be {@code null} for backward compatibility.
     * 
     * @since 2.3.22
     */
    public TaglibFactory(ServletContext ctx, TaglibFactoryConfiguration cfg) {
        this.servletContext = ctx;
        this.objectWrapper = cfg != null ? cfg.getObjectWrapper() : null;
        this.classpathTaglibJarsPattern = cfg != null ? cfg.getAdditionalTaglibJarsPattern() : null;
    }

    /**
     * Retrieves a JSP tag library identified by an URI. The matching of the URI to a JSP taglib is done as described in
     * the JSP 1.2 FCS specification.
     * 
     * @param taglibUri
     *            The URI used in templates to refer to the taglib (like {@code <%@ taglib uri="..." ... %>} in
     *            JSP). It can be any of the three forms allowed by the JSP specification: absolute URI (like
     *            {@code http://example.com/foo}), root relative URI (like {@code /bar/foo.tld}) and non-root relative
     *            URI (like {@code bar/foo.tld}). Note that if a non-root relative URI is used it's resolved relative to
     *            the URL of the current request. In this case, the current request is obtained by looking up a
     *            {@link HttpRequestHashModel} object named <tt>Request</tt> in the root data model.
     *            {@link FreemarkerServlet} provides this object under the expected name, and custom servlets that want
     *            to integrate JSP taglib support should do the same.
     * 
     * @return a {@link TemplateHashModel} representing the JSP taglib. Each element of this hash represents a single
     *         custom tag or EL function from the library, implemented as a {@link TemplateTransformModel} or
     *         {@link TemplateMethodModelEx}, respectively.
     */
    public TemplateModel get(final String taglibUri) throws TemplateModelException {
        synchronized (taglibs) {
            {
                final Taglib taglib = (Taglib) taglibs.get(taglibUri);
                if (taglib != null) {
                    return taglib;
                }
            }

            final TldLocation tldLocation;
            final String normalizedTaglibUri;
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Locating TLD for taglib URI " + StringUtil.jQuoteNoXSS(taglibUri) + ".");
                }
                
                TldLocation explicitlyMappedTldLocation = getExplicitlyMappedTldLocation(taglibUri);
                if (explicitlyMappedTldLocation != null) {
                    tldLocation = explicitlyMappedTldLocation;
                    normalizedTaglibUri = taglibUri;
                } else {
                    // Taglib URI must be directly the path (no mapping).
                    
                    final int urlType = getUriType(taglibUri);
                    if (urlType == RELATIVE_URL_PATH) {
                        // TODO Shouldn't this be done before looking up explicit mappings? Check specs.
                        normalizedTaglibUri = resolveRelativeUri(taglibUri);
                    } else if (urlType == ABSOLUTE_URL_PATH) {
                        normalizedTaglibUri = taglibUri;
                    } else if (urlType == FULL_URI) {
                        // Per spec., absolute URI-s can only be resolved through explicit mapping
                        throw new TemplateModelException("No mapping to TLD was found for this JSP taglib URI: "
                                + taglibUri);
                    } else {
                        throw new BugException();
                    }

                    if (!normalizedTaglibUri.equals(taglibUri)) {
                        // TODO I guess we should only check among non-explicit mappings here. Check specs.
                        final Taglib taglib = (Taglib) taglibs.get(taglibUri);
                        if (taglib != null) {
                            return taglib;
                        }
                    }

                    tldLocation = new ServletContextTldLocation(
                            normalizedTaglibUri,
                            isJarPath(normalizedTaglibUri) ? STANDARD_TLD_JAR_ENTRY_PATH : null);
                }
            } catch (TemplateModelException e) {
                throw e;
            } catch (Exception e) {
                throw new TemplateModelException(
                        "Error while looking for TLD file for " + StringUtil.jQuoteNoXSS(taglibUri) + ".",
                        e);
            }

            try {
                return loadTaglib(tldLocation, normalizedTaglibUri);
            } catch (TemplateModelException e) {
                throw e;
            } catch (Exception e) {
                throw new TemplateModelException("Error while loading taglib for URI "
                        + StringUtil.jQuoteNoXSS(normalizedTaglibUri) + " using TLD location "
                        + StringUtil.jQuoteNoXSS(tldLocation) + ".",
                        e);
            }
        }
    }

    private TldLocation getExplicitlyMappedTldLocation(final String uri) throws Exception {
        while (true) {
            final TldLocation tldLocation = (TldLocation) tldLocations.get(uri);
            if (tldLocation != null) {
                return tldLocation;
            }

            switch (nextTldLocationLookupPhase) {
            case 0:
                addTldLocationsFromWebXml();
                break;
            case 1:
                addTldLocationsFromWebinfPerLib();
                break;
            case 2:
                addTldLocationsFromWebinf();
                break;
            case 3:
                addTldLocationsFromClasspath();
                break;
            case 4:
                return null;
            default:
                throw new BugException();
            }
            nextTldLocationLookupPhase++;
        }
    }

    /**
     * @param tldLocation
     *            The physical location of the TLD file
     * @param taglibUri
     *            The URI used in templates to refer to the taglib (like {@code <%@ taglib uri="..." ... %>} in JSP).
     */
    private TemplateHashModel loadTaglib(TldLocation tldLocation, String taglibUri) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading taglib for URI " + StringUtil.jQuoteNoXSS(taglibUri)
                    + " from TLD location " + StringUtil.jQuoteNoXSS(tldLocation));
        }
        final Taglib taglib = new Taglib(servletContext, tldLocation, objectWrapper);
        taglibs.put(taglibUri, taglib);
        tldLocations.remove(taglibUri);  // TODO Why?
        return taglib;
    }

    private static int getUriType(String uri) throws TemplateModelException {
        if (uri == null) {
            throw new TemplateModelException("null is not a valid URI");
        }
        if (uri.length() == 0) {
            throw new TemplateModelException("empty string is not a valid URI");
        }
        final char c0 = uri.charAt(0);
        if (c0 == '/') {
            return ABSOLUTE_URL_PATH;
        }
        // Check if it conforms to RFC 3986 3.1 in order to qualify as ABS_URI
        if (c0 < 'a' || c0 > 'z') { // First char of scheme must be alpha
            return RELATIVE_URL_PATH;
        }
        final int colon = uri.indexOf(':');
        if (colon == -1) { // Must have a colon
            return RELATIVE_URL_PATH;
        }
        // Subsequent chars must be [a-z,0-9,+,-,.]
        for (int i = 1; i < colon; ++i) {
            final char c = uri.charAt(i);
            if ((c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '+' && c != '-' && c != '.') {
                return RELATIVE_URL_PATH;
            }
        }
        return FULL_URI;
    }

    /**
     * Returns false.
     */
    public boolean isEmpty() {
        return false;
    }

    private void addTldLocationsFromWebXml() throws Exception
    {
        LOG.debug("Looking for TLD locations in servletContext:/WEB-INF/web.xml");

        WebXmlParser webXmlParser = new WebXmlParser();
        InputStream in = servletContext.getResourceAsStream("/WEB-INF/web.xml");
        if (in == null) {
            LOG.debug("No web.xml was found in servlet context");
            return;
        }
        try {
            parseXml(in, servletContext.getResource("/WEB-INF/web.xml").toExternalForm(), webXmlParser);
        } finally {
            in.close();
        }
    }

    private void addTldLocationsFromWebinf() throws Exception {
        LOG.debug("Looking for TLD locations in servletContext:/WEB-INF/**.tld");

        Set webinfEntryPaths = servletContext.getResourcePaths("/WEB-INF");
        if (webinfEntryPaths != null) {
            for (Iterator it = webinfEntryPaths.iterator(); it.hasNext();) {
                String webinfEntryPath = (String) it.next();
                if (webinfEntryPath.endsWith(".tld")) {
                    addTldLocationFromTld(new ServletContextTldLocation(webinfEntryPath, null));
                }
            }
        }
    }

    private void addTldLocationsFromWebinfPerLib() throws Exception {
        LOG.debug("Looking for TLD locations in servletContext:/WEB-INF/lib/**.{jar,zip,tld}");

        Set libEntPaths = servletContext.getResourcePaths("/WEB-INF/lib");
        if (libEntPaths != null) {
            for (Iterator iter = libEntPaths.iterator(); iter.hasNext();) {
                String libEntryPath = (String) iter.next();
                if (isJarPath(libEntryPath)) {
                    addTldLocationsFromJarMetaInf(libEntryPath);
                } else if (libEntryPath.endsWith(".tld")) {
                    addTldLocationFromTld(new ServletContextTldLocation(libEntryPath, null));
                }
            }
        }
    }

    private void addTldLocationsFromClasspath() {
        LOG.debug("Looking for TLD locations in classpath");
        if (classpathTaglibJarsPattern == null) return;
        
        // TODO Not implemented
    }

    // TODO Can't be used for non-SCTX
    private void addTldLocationsFromJarMetaInf(String jarServletContextPath) throws Exception {
        final InputStream jarResourceIn = servletContext.getResourceAsStream(jarServletContextPath);
        try {
            ZipInputStream zipIn = new ZipInputStream(jarResourceIn);
            try {
                while (true) {
                    ZipEntry jarEntry = zipIn.getNextEntry();
                    if (jarEntry == null) break;

                    String jarEntryPath = jarEntry.getName();
                    if (jarEntryPath.startsWith("META-INF/") && jarEntryPath.endsWith(".tld")) {
                        final ServletContextTldLocation tldLocation = new ServletContextTldLocation(jarServletContextPath, jarEntryPath);
                        addTldLocationFromTld(zipIn, tldLocation);                                
                    }
                }
            } finally {
                zipIn.close();
            }
        } finally {
            jarResourceIn.close();
        }
    }

    /**
     * Adds the TLD location mapping from the TLD itself.
     */
    private void addTldLocationFromTld(TldLocation tldLocation) throws Exception {
        InputStream in = tldLocation.getInputStream();
        try {
            addTldLocationFromTld(in, tldLocation);
        } finally {
            in.close();
        }
    }

    /**
     * @param reusedIn
     *            The stream that we already had (so we don't have to open a new one from the {@code tldLocation}).
     */
    private void addTldLocationFromTld(InputStream reusedIn, TldLocation tldLocation)
            throws Exception {
        String taglibUri = getTaglibUriFromTld(reusedIn, tldLocation.getURL());
        if (taglibUri != null) {
            addTldLocation(tldLocation, taglibUri);
        }
    }

    private void addTldLocation(TldLocation tldLocation, String taglibUri) {
        if (tldLocations.containsKey(taglibUri)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignored duplicate mapping of taglib URI " + StringUtil.jQuoteNoXSS(taglibUri)
                        + " to TLD location " + StringUtil.jQuoteNoXSS(tldLocation)
                        + " (mapping source type: \"" + "\")"); // TODO
            }
        } else {
            tldLocations.put(taglibUri, tldLocation);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Mapped taglib URI " + StringUtil.jQuoteNoXSS(taglibUri)
                        + " to TLD location " + StringUtil.jQuoteNoXSS(tldLocation)
                        + " (mapping source type: \"" + "\")"); // TODO
            }
        }
    }

    private String getTaglibUriFromTld(InputStream tldFileIn, String tldFileUrl) throws Exception {
        TldParserForTaglibUriExtration tldParser = new TldParserForTaglibUriExtration();
        parseXml(tldFileIn, tldFileUrl, tldParser);
        return tldParser.getTaglibUri();
    }

    private static void parseXml(InputStream in, String systemId, DefaultHandler handler) throws Exception {
        InputSource inSrc = new InputSource();
        inSrc.setSystemId(systemId);
        inSrc.setByteStream(new FilterInputStream(in) {
            public void close() {
                // Ignores the XML parser's attempt to close the stream.
            }
        });
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setEntityResolver(new LocalDtdEntityResolver());
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);
        
        reader.parse(inSrc);
    }

    private static String resolveRelativeUri(String uri)
            throws
            TemplateModelException
    {
        TemplateModel reqHash =
                Environment.getCurrentEnvironment().getVariable(
                        FreemarkerServlet.KEY_REQUEST_PRIVATE);
        if (reqHash instanceof HttpRequestHashModel) {
            HttpServletRequest req =
                    ((HttpRequestHashModel) reqHash).getRequest();
            String pi = req.getPathInfo();
            String reqPath = req.getServletPath();
            if (reqPath == null) {
                reqPath = "";
            }
            reqPath += (pi == null ? "" : pi);
            // We don't care about paths with ".." in them. If the container
            // wishes to resolve them on its own, let it be.
            int lastSlash = reqPath.lastIndexOf('/');
            if (lastSlash != -1) {
                return reqPath.substring(0, lastSlash + 1) + uri;
            }
            else {
                return '/' + uri;
            }
        }
        throw new TemplateModelException(
                "Can't resolve relative URI " + uri +
                        " as request URL information is unavailable.");
    }

    private boolean isJarPath(final String uriPath) {
        return uriPath.endsWith(".jar") || uriPath.endsWith(".zip");
    }
    
    private interface TldLocation {
        public abstract InputStream getInputStream() throws IOException;
        public abstract String getURL() throws IOException;
    }

    private static abstract class ZipAwareTldLocation implements TldLocation {
        final String jarEntryPath;
    
        ZipAwareTldLocation(String jarEntryPath) {
            this.jarEntryPath = jarEntryPath;
        }
    
        public final InputStream getInputStream() throws IOException {
            final InputStream outerIn = getZipUnawareInputStream();
            if (jarEntryPath != null) {
                final ZipInputStream zipIn = new ZipInputStream(outerIn);
                while (true) {
                    final ZipEntry macthedJarEntry = zipIn.getNextEntry();
                    if (macthedJarEntry == null) {
                        throw new IOException(
                                "Could not find JAR entry " + StringUtil.jQuoteNoXSS(jarEntryPath)
                                + " inside " + StringUtil.jQuoteNoXSS(getOuterURLExternalForm()) + ".");
                    }
                    if (jarEntryPath.equals(macthedJarEntry.getName())) {
                        return zipIn;
                    }
                }
            } else {
                return outerIn;
            }
        }

        private String getOuterURLExternalForm() throws IOException {
            return getZipUnawareURL().toExternalForm();
        }

        private String getURLExternalForm(String outerExternalForm) throws IOException {
            return jarEntryPath != null ? "jar:" + outerExternalForm + "!" + jarEntryPath : outerExternalForm;
        }

        public String getURL() throws IOException {
            return getURLExternalForm(getOuterURLExternalForm());
        }

        /**
         * @return Not {@code null};
         */
        protected abstract InputStream getZipUnawareInputStream() throws IOException;
        
        /**
         * @return Not {@code null};
         */
        protected abstract URL getZipUnawareURL() throws IOException;
    
    }
    
    private class ServletContextTldLocation extends ZipAwareTldLocation {
        
        private final String fileResourcePath;
    
        public ServletContextTldLocation(String fileResourcePath, String jarEntryPath) {
            super(jarEntryPath);
            this.fileResourcePath = fileResourcePath;
        }
    
        public String toString() {
            return "servletContext:" + fileResourcePath + (jarEntryPath != null ? "!" + jarEntryPath : "");
        }
    
        protected InputStream getZipUnawareInputStream() throws IOException {
            final InputStream in = servletContext.getResourceAsStream(fileResourcePath);
            if (in == null) {
                throw newResourceNotFoundException();
            }
            return in;
        }
    
        protected URL getZipUnawareURL() throws IOException {
            final URL url = servletContext.getResource(fileResourcePath);
            if (url == null) {
                throw newResourceNotFoundException();
            }
            return url;
        }
        
        private IOException newResourceNotFoundException() {
            return new IOException("Resource not found: servletContext:" + fileResourcePath);
        }
    
    }

    private static class ClassLoaderTldLocation extends ZipAwareTldLocation {
    
        private final String resourcePath;
        
        public ClassLoaderTldLocation(String filePath, String jarItemPath) {
            super(jarItemPath);
            this.resourcePath = filePath;
        }
    
        public String toString() {
            return "classpath:" + resourcePath + (jarEntryPath != null ? "!" + jarEntryPath : "");
        }
    
        protected InputStream getZipUnawareInputStream() throws IOException {
            ClassLoader tccl = tryGetThreadContextClassLoader();
            if (tccl != null) {
                final InputStream in = getClass().getResourceAsStream(resourcePath);
                if (in != null) { 
                    return in;
                }
            }
            
            final InputStream in = getClass().getResourceAsStream(resourcePath);
            if (in == null) {
                throw newResourceNotFoundException();
            }
            return in;
        }

        private ClassLoader tryGetThreadContextClassLoader() {
            ClassLoader tccl;
            try {
                tccl = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                // Suppress
                tccl = null;
                LOG.warn("Can't access Thread Context ClassLoader", e);
            }
            return tccl;
        }
    
        protected URL getZipUnawareURL() throws IOException {
            ClassLoader tccl = tryGetThreadContextClassLoader();
            if (tccl != null) {
                final URL url = getClass().getResource(resourcePath);
                if (url != null) { 
                    return url;
                }
            }
            
            final URL url = this.getClass().getResource(resourcePath);
            if (url == null) {
                throw newResourceNotFoundException();
            }
            return url;
        }
        
        private IOException newResourceNotFoundException() {
            return new IOException("Resource not found: classpath:" + resourcePath);
        }
    
    }

    private static final class Taglib implements TemplateHashModel {
        private final Map tagsAndFunctions;

        Taglib(ServletContext ctx, TldLocation tldPath, ObjectWrapper wrapper) throws Exception {
            tagsAndFunctions = parseToTagsAndFunctions(ctx, tldPath, wrapper);
        }

        public TemplateModel get(String key) {
            return (TemplateModel) tagsAndFunctions.get(key);
        }

        public boolean isEmpty() {
            return tagsAndFunctions.isEmpty();
        }

        private static final Map parseToTagsAndFunctions(
                ServletContext ctx, TldLocation tldLocation, ObjectWrapper objectWrapper) throws Exception {
            final TldParserForTaglibBuilding tldParser = new TldParserForTaglibBuilding(objectWrapper);
            
            InputStream in = tldLocation.getInputStream();
            try {
                parseXml(in, tldLocation.getURL(), tldParser);
            } finally {
                in.close();
            }
            
            EventForwarding eventForwarding = EventForwarding.getInstance(ctx);
            if (eventForwarding != null) {
                eventForwarding.addListeners(tldParser.getListeners());
            } else if (tldParser.getListeners().size() > 0) {
                throw new TemplateModelException(
                        "Event listeners specified in the TLD could not be " +
                                " registered since the web application doesn't have a" +
                                " listener of class " + EventForwarding.class.getName() +
                                ". To remedy this, add this element to web.xml:\n" +
                                "| <listener>\n" +
                                "|   <listener-class>" + EventForwarding.class.getName() + "</listener-class>\n" +
                                "| </listener>");
            }
            return tldParser.getTagsAndFunctions();
        }
    }

    private class WebXmlParser extends DefaultHandler {
        private static final String E_TAGLIB = "taglib";
        private static final String E_TAGLIB_LOCATION = "taglib-location";
        private static final String E_TAGLIB_URI = "taglib-uri";

        private StringBuffer cDataCollector;
        private String taglibUriCData;
        private String taglibLocationCData;
        private Locator locator;

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        public void startElement(
                String nsuri,
                String localName,
                String qName,
                Attributes atts) {
            if (E_TAGLIB_URI.equals(qName) || E_TAGLIB_LOCATION.equals(qName)) {
                cDataCollector = new StringBuffer();
            }
        }

        public void characters(char[] chars, int off, int len) {
            if (cDataCollector != null) {
                cDataCollector.append(chars, off, len);
            }
        }

        public void endElement(String nsUri, String localName, String qName) throws TldParsingException {
            if (E_TAGLIB_URI.equals(qName)) {
                taglibUriCData = cDataCollector.toString().trim();
                cDataCollector = null;
            } else if (E_TAGLIB_LOCATION.equals(qName)) {
                taglibLocationCData = cDataCollector.toString().trim();
                try {
                    if (getUriType(taglibLocationCData) == RELATIVE_URL_PATH) {
                        taglibLocationCData = "/WEB-INF/" + taglibLocationCData;
                    }
                } catch (TemplateModelException e) {
                    throw new TldParsingException("Failed to detect URI type for: " + taglibLocationCData, locator, e);
                }
                cDataCollector = null;
            } else if (E_TAGLIB.equals(qName)) {
                final String jarEntryPath = isJarPath(taglibLocationCData) ? STANDARD_TLD_JAR_ENTRY_PATH : null;
                addTldLocation(new ServletContextTldLocation(taglibLocationCData, jarEntryPath), taglibUriCData);
            }
        }
    }

    private static class TldParserForTaglibUriExtration extends DefaultHandler {
        private static final String E_URI = "uri";

        private StringBuffer cDataCollector;
        private String uri;

        TldParserForTaglibUriExtration() {
        }

        String getTaglibUri() {
            return uri;
        }

        public void startElement(
                String nsuri,
                String localName,
                String qName,
                Attributes atts) {
            if (E_URI.equals(qName)) {
                cDataCollector = new StringBuffer();
            }
        }

        public void characters(char[] chars, int off, int len) {
            if (cDataCollector != null) {
                cDataCollector.append(chars, off, len);
            }
        }

        public void endElement(String nsuri, String localName, String qName) {
            if (E_URI.equals(qName)) {
                uri = cDataCollector.toString().trim();
                cDataCollector = null;
            }
        }
    }

    static final class TldParserForTaglibBuilding extends DefaultHandler {
        private static final String E_TAG = "tag";
        private static final String E_NAME = "name";
        private static final String E_TAG_CLASS = "tag-class";
        private static final String E_TAG_CLASS_LEGACY = "tagclass";

        private static final String E_FUNCTION = "function";
        private static final String E_FUNCTION_CLASS = "function-class";
        private static final String E_FUNCTION_SIGNATURE = "function-signature";

        private static final String E_LISTENER = "listener";
        private static final String E_LISTENER_CLASS = "listener-class";

        private final BeansWrapper beansWrapper;

        private final Map tagsAndFunctions = new HashMap();
        private final List listeners = new ArrayList();

        private Locator locator;
        private StringBuffer cDataCollector;

        private Stack stack = new Stack();

        private String tagNameCData;
        private String tagClassCData;
        private String functionNameCData;
        private String functionClassCData;
        private String functionSignatureCData;
        private String listenerClassCData;

        TldParserForTaglibBuilding(ObjectWrapper wrapper) {
            if (wrapper instanceof BeansWrapper) {
                beansWrapper = (BeansWrapper) wrapper;
            }
            else {
                beansWrapper = null;
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Custom EL functions won't be loaded because "
                            + (wrapper == null
                                    ? "no ObjectWarpper was specified "
                                    : "the ObjectWrapper wasn't instance of " + BeansWrapper.class.getName())
                            + ".");
                }
            }
        }

        Map getTagsAndFunctions() {
            return tagsAndFunctions;
        }

        List getListeners() {
            return listeners;
        }

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        public void startElement(String nsUri, String localName, String qName, Attributes atts) {
            stack.push(qName);
            if (stack.size() == 3) {
                if (E_NAME.equals(qName) || E_TAG_CLASS_LEGACY.equals(qName) || E_TAG_CLASS.equals(qName)
                        || E_LISTENER_CLASS.equals(qName) || E_FUNCTION_CLASS.equals(qName)
                        || E_FUNCTION_SIGNATURE.equals(qName)) {
                    cDataCollector = new StringBuffer();
                }
            }
        }

        public void characters(char[] chars, int off, int len) {
            if (cDataCollector != null) {
                cDataCollector.append(chars, off, len);
            }
        }

        public void endElement(String nsuri, String localName, String qName) throws TldParsingException {
            if (!stack.peek().equals(qName)) {
                throw new TldParsingException("Unbalanced tag nesting at \"" + qName + "\" end-tag.", locator);
            }

            if (stack.size() == 3) {
                if (E_NAME.equals(qName)) {
                    if (E_TAG.equals(stack.get(1))) {
                        tagNameCData = cDataCollector.toString();
                        cDataCollector = null;
                    }
                    else if (E_FUNCTION.equals(stack.get(1))) {
                        functionNameCData = cDataCollector.toString();
                        cDataCollector = null;
                    }
                }
                else if (E_TAG_CLASS_LEGACY.equals(qName) || E_TAG_CLASS.equals(qName)) {
                    tagClassCData = cDataCollector.toString();
                    cDataCollector = null;
                }
                else if (E_LISTENER_CLASS.equals(qName)) {
                    listenerClassCData = cDataCollector.toString();
                    cDataCollector = null;
                }
                else if (E_FUNCTION_CLASS.equals(qName)) {
                    functionClassCData = cDataCollector.toString();
                    cDataCollector = null;
                }
                else if (E_FUNCTION_SIGNATURE.equals(qName)) {
                    functionSignatureCData = cDataCollector.toString();
                    cDataCollector = null;
                }
            } else if (stack.size() == 2) {
                if (E_TAG.equals(qName)) {
                    checkChildElementNotNull(qName, E_NAME, tagNameCData);
                    checkChildElementNotNull(qName, E_TAG_CLASS, tagClassCData);

                    final Class tagClass = resoveClassFromTLD(tagClassCData, "custom tag", tagNameCData);

                    final TemplateModel impl;
                    try {
                        if (Tag.class.isAssignableFrom(tagClass)) {
                            impl = new TagTransformModel(tagClass);
                        }
                        else {
                            impl = new SimpleTagDirectiveModel(tagClass);
                        }
                    } catch (IntrospectionException e) {
                        throw new TldParsingException(
                                "JavaBean introspection failed on custom tag class " + tagClassCData,
                                locator,
                                e);
                    }

                    tagsAndFunctions.put(tagNameCData, impl);

                    tagNameCData = null;
                    tagClassCData = null;
                } else if (E_FUNCTION.equals(qName) && beansWrapper != null) {
                    checkChildElementNotNull(qName, E_FUNCTION_CLASS, functionClassCData);
                    checkChildElementNotNull(qName, E_FUNCTION_SIGNATURE, functionSignatureCData);
                    checkChildElementNotNull(qName, E_NAME, functionNameCData);

                    final Class functionClass = resoveClassFromTLD(
                            functionClassCData, "custom EL function", functionNameCData);

                    final Method functionMethod;
                    try {
                        functionMethod = TaglibMethodUtil.getMethodByFunctionSignature(
                                functionClass, functionSignatureCData);
                    } catch (Exception e) {
                        throw new TldParsingException(
                                "Error while trying to resolve signature " + StringUtil.jQuote(functionSignatureCData)
                                        + " on class " + StringUtil.jQuote(functionClass.getName())
                                        + " for custom EL function " + StringUtil.jQuote(functionNameCData) + ".",
                                locator,
                                e);
                    }

                    final int modifiers = functionMethod.getModifiers();
                    if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)) {
                        throw new TldParsingException(
                                "The custom EL function method must be public and static: " + functionMethod,
                                locator);
                    }

                    final TemplateMethodModelEx methodModel;
                    try {
                        methodModel = beansWrapper.wrap(null, functionMethod);
                    } catch (Exception e) {
                        throw new TldParsingException(
                                "FreeMarker object wrapping failed on method : " + functionMethod,
                                locator);
                    }

                    tagsAndFunctions.put(functionNameCData, methodModel);

                    functionNameCData = null;
                    functionClassCData = null;
                    functionSignatureCData = null;
                } else if (E_LISTENER.equals(qName)) {
                    checkChildElementNotNull(qName, E_LISTENER_CLASS, listenerClassCData);

                    final Class listenerClass = resoveClassFromTLD(listenerClassCData, E_LISTENER, null);

                    final Object listener;
                    try {
                        listener = listenerClass.newInstance();
                    } catch (Exception e) {
                        throw new TldParsingException(
                                "Failed to create new instantiate from listener class " + listenerClassCData,
                                locator,
                                e);
                    }

                    listeners.add(listener);

                    listenerClassCData = null;
                }
            }

            stack.pop();
        }

        private void checkChildElementNotNull(String parentElementName, String childElementName, String value)
                throws TldParsingException {
            if (value == null) {
                throw new TldParsingException(
                        "Missing required \"" + childElementName + "\" element inside the \""
                                + parentElementName + "\" element.", locator);
            }
        }

        private Class resoveClassFromTLD(String className, String entryType, String entryName)
                throws TldParsingException {
            try {
                return ClassUtil.forName(className);
            } catch (LinkageError e) {
                throw newTLDEntryClassLoadingException(e, className, entryType, entryName);
            } catch (ClassNotFoundException e) {
                throw newTLDEntryClassLoadingException(e, className, entryType, entryName);
            }
        }

        private TldParsingException newTLDEntryClassLoadingException(Throwable e, String className,
                String entryType, String entryName)
                throws TldParsingException {
            int dotIdx = className.lastIndexOf('.');
            if (dotIdx != -1) {
                dotIdx = className.lastIndexOf('.', dotIdx - 1);
            }
            boolean looksLikeNestedClass =
                    dotIdx != -1 && className.length() > dotIdx + 1
                            && Character.isUpperCase(className.charAt(dotIdx + 1));
            return new TldParsingException(
                    (e instanceof ClassNotFoundException ? "Not found class " : "Can't load class ")
                            + StringUtil.jQuote(className) + " for " + entryType
                            + (entryName != null ? " " + StringUtil.jQuote(entryName) : "") + "."
                            + (looksLikeNestedClass
                                    ? " Hint: Before nested classes, use \"$\", not \".\"."
                                    : ""),
                    locator,
                    e);
        }

    }

    /**
     * Redefines {@code SAXParseException#toString()} and {@code SAXParseException#getCause()} because it's broken on
     * Java 1.6 and earlier.
     */
    private static class TldParsingException extends SAXParseException {

        private final Throwable cause;

        TldParsingException(String message, Locator locator) {
            this(message, locator, null);
        }

        TldParsingException(String message, Locator locator, Throwable e) {
            super(message, locator, e instanceof Exception ? (Exception) e : new Exception(
                    "Unchecked exception; see cause", e));
            cause = e;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer(getClass().getName());
            sb.append(": ");
            int startLn = sb.length();

            String systemId = getSystemId();
            String publicId = getPublicId();
            if (systemId != null || publicId != null) {
                sb.append("In ");
                if (systemId != null) {
                    sb.append(systemId);
                }
                if (publicId != null) {
                    if (systemId != null) {
                        sb.append(" (public ID: ");
                    }
                    sb.append(publicId);
                    if (systemId != null) {
                        sb.append(')');
                    }
                }
            }

            int line = getLineNumber();
            if (line != -1) {
                sb.append(sb.length() != startLn ? ", at " : "At ");
                sb.append("line ");
                sb.append(line);
                int col = getColumnNumber();
                if (col != -1) {
                    sb.append(", column ");
                    sb.append(col);
                }
            }

            String message = getLocalizedMessage();
            if (message != null) {
                if (sb.length() != startLn) {
                    sb.append(":\n");
                }
                sb.append(message);
            }

            return sb.toString();
        }

        public Throwable getCause() {
            Throwable superCause = super.getCause();
            return superCause == null ? this.cause : superCause;
        }

    }

    private static final class LocalDtdEntityResolver implements EntityResolver {
        
        private static final Map DTDS = new HashMap();
        static
        {
            // JSP taglib 2.1
            DTDS.put("http://java.sun.com/xml/ns/jee/web-jsptaglibrary_2_1.xsd", "web-jsptaglibrary_2_1.xsd");
            // JSP taglib 2.0
            DTDS.put("http://java.sun.com/xml/ns/j2ee/web-jsptaglibrary_2_0.xsd", "web-jsptaglibrary_2_0.xsd");
            // JSP taglib 1.2
            DTDS.put("-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN", "web-jsptaglibrary_1_2.dtd");
            DTDS.put("http://java.sun.com/dtd/web-jsptaglibrary_1_2.dtd", "web-jsptaglibrary_1_2.dtd");
            // JSP taglib 1.1
            DTDS.put("-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN", "web-jsptaglibrary_1_1.dtd");
            DTDS.put("http://java.sun.com/j2ee/dtds/web-jsptaglibrary_1_1.dtd", "web-jsptaglibrary_1_1.dtd");
            // Servlet 2.5
            DTDS.put("http://java.sun.com/xml/ns/jee/web-app_2_5.xsd", "web-app_2_5.xsd");
            // Servlet 2.4
            DTDS.put("http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd", "web-app_2_4.xsd");
            // Servlet 2.3
            DTDS.put("-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN", "web-app_2_3.dtd");
            DTDS.put("http://java.sun.com/dtd/web-app_2_3.dtd", "web-app_2_3.dtd");
            // Servlet 2.2
            DTDS.put("-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN", "web-app_2_2.dtd");
            DTDS.put("http://java.sun.com/j2ee/dtds/web-app_2_2.dtd", "web-app_2_2.dtd");
        }
        
        public InputSource resolveEntity(String publicId, String systemId)
        {
            String resourceName = (String) DTDS.get(publicId);
            if (resourceName == null)
            {
                resourceName = (String) DTDS.get(systemId);
            }
            InputStream resourceStream;
            if (resourceName != null)
            {
                resourceStream = getClass().getResourceAsStream(resourceName);
            }
            else
            {
                // Fake an empty stream for unknown DTDs
                resourceStream = new ByteArrayInputStream(new byte[0]);
            }
            InputSource is = new InputSource(resourceStream);
            is.setPublicId(publicId);
            is.setSystemId(systemId);
            return is;
        }
    }

}
