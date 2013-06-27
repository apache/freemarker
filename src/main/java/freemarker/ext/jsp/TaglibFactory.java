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

package freemarker.ext.jsp;

import java.beans.IntrospectionException;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import freemarker.core.Environment;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.log.Logger;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;

/**
 * A hash model associated with a servlet context that can load JSP tag 
 * libraries associated with that servlet context. An instance of this class is
 * made available in the root data model of templates executed by 
 * {@link freemarker.ext.servlet.FreemarkerServlet} under key
 * <tt>JspTaglibs</tt>. It can be added to custom servlets as well to enable JSP
 * taglib integration in them as well.
 * @author Attila Szegedi
 */
public class TaglibFactory implements TemplateHashModel {
    private static final Logger logger = Logger.getLogger("freemarker.jsp");
    private static final String DEFAULT_JAR_TLD = "META-INF/taglib.tld";

    // No TLDs have been looked up yet
    private static final int LOOKUP_NONE = 0;
    // Only taglibs defined in web.xml have been looked up
    private static final int LOOKUP_WEB_XML = 1;
    // Both taglibs in web.xml and those in JARs and TLD files have been looked up
    private static final int LOOKUP_WEB_APP = 2;

    private static final int ABS_URI = 0;
    private static final int ROOT_REL_URI = 1;
    private static final int NOROOT_REL_URI = 2;

    private final ServletContext ctx;
    private final Map taglibs = new HashMap();
    private final Map locations = new HashMap();
    private int lookupPhase = LOOKUP_NONE;

    /**
     * Creates a new JSP taglib factory that will be used to load JSP taglibs
     * for the web application represented by the passed servlet context.
     * @param ctx the servlet context whose JSP tag libraries will this factory
     * load.
     */
    public TaglibFactory(ServletContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Retrieves a JSP tag library identified by an URI. The matching of the URI
     * to a JSP taglib is done as described in the JSP 1.2 FCS specification.
     * @param uri the URI that describes the JSP taglib. It can be any of the
     * three forms allowed by the JSP specification: absolute URI, root relative
     * URI and non-root relative URI. Note that if a non-root relative URI is
     * used it's resolved relative to the URL of the current request. In this
     * case, the current request is obtained by looking up a
     * {@link HttpRequestHashModel} object named <tt>Request</tt> in the root
     * data model. FreemarkerServlet provides this object under the expected
     * name, and custom servlets that want to integrate JSP taglib support
     * should do the same.
     * @return a hash model representing the JSP taglib. Each element of this
     * hash model represents a single custom tag from the library, implemented
     * as a {@link freemarker.template.TemplateTransformModel}.
     */
    public TemplateModel get(String uri) throws TemplateModelException {
        synchronized (taglibs) {
            final Taglib taglib = (Taglib) taglibs.get(uri);
            if(taglib != null) {
                return taglib;
            }
            try {
                // Make sure we have mappings from at least web.xml
                if(lookupPhase == LOOKUP_NONE) {
                    addLocationsFromWebXml();
                    lookupPhase = LOOKUP_WEB_XML;
                }
                // Try explicit mapping
                TldPath path = (TldPath)locations.get(uri);
                if(path != null) {
                    return loadTaglib(path, uri);
                }
                // Make sure we have mappings from .jar and .tld files, too.
                // Note that this delays scanning the WEB-INF directory as long
                // as taglibs can be found based on web.xml mapping.
                if(lookupPhase == LOOKUP_WEB_XML) {
                    addLocationsFromWebApp();
                    lookupPhase = LOOKUP_WEB_APP;
                    // Try newly found explicit mappings
                    path = (TldPath)locations.get(uri);
                    if(path != null) {
                        return loadTaglib(path, uri);
                    }
                }
                // No mappings found, try treating the path as explicit path
                switch(getUriType(uri)) {
                    case ABS_URI: {
                        // Absolute URIs can only be resolved through mapping
                        throw new TemplateModelException("No mapping defined for " + uri);
                    }
                    case NOROOT_REL_URI: {
                        // Resolve URI relative to the current page
                        uri = resolveRelativeUri(uri);
                        // Intentional fallthrough
                    }
                    case ROOT_REL_URI: {
                        // If it's a .jar or .zip, add default TLD entry within it.
                        if(uri.endsWith(".jar") || uri.endsWith(".zip")) {
                            return loadTaglib(new TldPath(uri, DEFAULT_JAR_TLD), uri);
                        }
                        // Treat the URI verbatim.
                        return loadTaglib(new TldPath(uri), uri);
                    }
                    default: {
                        throw new RuntimeException("Cannot happen");
                    }
                }
            }
            catch(TemplateModelException e) {
                throw e;
            }
            catch(RuntimeException e) {
                throw e;
            }
            catch(Exception e) {
                throw new TemplateModelException("Could not load taglib information for " + uri, e);
            }
        }
    }

    private static class TldPath {
        final String filePath;
        final String jarItemPath;

        TldPath(String filePath) {
            this(filePath, null);
        }

        TldPath(String filePath, String jarItemPath) {
            this.filePath = filePath;
            this.jarItemPath = jarItemPath;
        }
        
        public String toString() {
            if(jarItemPath == null) {
                return filePath;
            }
            return filePath + "!" + jarItemPath;
        }
    }

    private TemplateModel loadTaglib(TldPath tldPath, String uri) throws Exception {
        if(logger.isDebugEnabled()) {
            logger.debug("Loading taglib " + StringUtil.jQuoteNoXSS(uri) + 
                " from location " + StringUtil.jQuoteNoXSS(tldPath));
        }
        final Taglib taglib = new Taglib(ctx, tldPath, uri);
        taglibs.put(uri, taglib);
        locations.remove(uri);
        return taglib;
    }

    private static int getUriType(String uri) throws TemplateModelException {
        if(uri == null) {
            throw new TemplateModelException("null is not a valid URI");
        }
        if(uri.length() == 0) {
          throw new TemplateModelException("empty string is not a valid URI");
        }
        final char c0 = uri.charAt(0);
        if(c0 == '/') {
            return ROOT_REL_URI;
        }
        // Check if it conforms to RFC 3986 3.1 in order to qualify as ABS_URI
        if(c0 < 'a' || c0 > 'z') { // First char of scheme must be alpha
          return NOROOT_REL_URI;
        }
        final int colon = uri.indexOf(':');
        if(colon == -1) { // Must have a colon
            return NOROOT_REL_URI;
        }
        // Subsequent chars must be [a-z,0-9,+,-,.]
        for(int i = 1; i < colon; ++i) {
            final char c = uri.charAt(i);
            if((c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '+' && c != '-' && c != '.') {
                return NOROOT_REL_URI;
            }
        }
        return ABS_URI;
    }
    /**
     * Returns false.
     */
    public boolean isEmpty() {
        return false;
    }

    private void addLocationsFromWebXml() throws Exception
    {
        WebXmlParser webXmlParser = new WebXmlParser();
        InputStream in = ctx.getResourceAsStream("/WEB-INF/web.xml");
        if (in == null) {
            // No /WEB-INF/web.xml - do nothing
            return;
        }
        try {
            parseXml(in, ctx.getResource("/WEB-INF/web.xml").toExternalForm(), webXmlParser);
        }
        finally {
            in.close();
        }
    }
    
    private class WebXmlParser extends DefaultHandler {
        private StringBuffer buf;
        private String uri;
        private String location;
        private Locator locator;

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        public void startElement(
            String nsuri,
            String localName,
            String qName,
            Attributes atts) {
            if ("taglib-uri".equals(qName)
                || "taglib-location".equals(qName)) {
                buf = new StringBuffer();
            }
        }

        public void characters(char[] chars, int off, int len) {
            if (buf != null) {
                buf.append(chars, off, len);
            }
        }

        public void endElement(String nsuri, String localName, String qName) throws SAXParseException {
            if ("taglib-uri".equals(qName)) {
                uri = buf.toString().trim();
                buf = null;
            }
            else if ("taglib-location".equals(qName)) {
                location = buf.toString().trim();
                try {
                    if(getUriType(location) == NOROOT_REL_URI) {
                        location = "/WEB-INF/" + location;
                    }
                }
                catch(TemplateModelException e) {
                    throw new SAXParseException(e.getMessage(), locator, e);
                }
                buf = null;
            }
            else if ("taglib".equals(qName)) {
                final String zname;
                if(location.endsWith(".jar") || location.endsWith(".zip")) {
                    zname = DEFAULT_JAR_TLD;
                }
                else {
                    zname = null;
                }
                addLocation("web.xml", location, zname, uri);
            }
        }
    }

    private void addLocationsFromWebApp() throws Exception
    {
        Set libs = ctx.getResourcePaths("/WEB-INF/lib");
        if (libs != null) {
          for (Iterator iter = libs.iterator(); iter.hasNext();) {
              String path = (String) iter.next();
              if(path.endsWith(".jar") || path.endsWith(".zip")) {
                  addLocationsFromJarFile(path);
              }
              else if(path.endsWith(".tld")) {
                  addLocationFromTldFile(path);
              }
          }
        }
        libs = ctx.getResourcePaths("/WEB-INF");
        if (libs != null) {
          for (Iterator iter = libs.iterator(); iter.hasNext();) {
              String path = (String) iter.next();
              if(path.endsWith(".tld")) {
                  addLocationFromTldFile(path);
              }
          }
        }
    }

    private void addLocationsFromJarFile(String path) throws Exception {
        ZipInputStream zin = new ZipInputStream(ctx.getResourceAsStream(path));
        // Make stream uncloseable by XML parsers
        InputStream uin = new FilterInputStream(zin) {
            public void close() {
            }
        };
        try {
            for(;;) {
                ZipEntry ze = zin.getNextEntry();
                if(ze == null) {
                    break;
                }
                String zname = ze.getName();
                if(zname.startsWith("META-INF/") && zname.endsWith(".tld")) {
                    String url = "jar:" + 
                        ctx.getResource(path).toExternalForm() + 
                        "!" + zname;
                    addLocationFromTldResource(uin, path, zname, url); 
                }
            }
        }
        finally {
            zin.close();
        }
    }

    private void addLocationFromTldFile(String path) throws Exception {
        InputStream in = ctx.getResourceAsStream(path);
        try {
            addLocationFromTldResource(in, path, null, ctx.getResource(path).toExternalForm());
        }
        finally {
            in.close();
        }
    }

    private void addLocationFromTldResource(InputStream uin, String path, String zname, String url)
    throws Exception {
        String uri = getTldUri(uin, url);
        if(uri != null) {
            addLocation(zname == null ? "tld file" : "jar file", path, zname, uri);
        }
    }

    private void addLocation(String source, String filePath, String jarItemPath, String uri) {
        final TldPath tldPath = new TldPath(filePath, jarItemPath);
        if(locations.containsKey(uri)) {
            logger.debug("Ignored duplicate URI " + StringUtil.jQuoteNoXSS(uri) +
                    " in " + source + " " + StringUtil.jQuoteNoXSS(tldPath));
        } else {
            locations.put(uri, tldPath);
            if(logger.isDebugEnabled()) {
                logger.debug(source + " assigned URI " + StringUtil.jQuoteNoXSS(uri) +
                        " to location " + StringUtil.jQuoteNoXSS(tldPath));
            }
        }
    }

    private String getTldUri(InputStream in, String url) throws Exception
    {
        TldUriReader tur = new TldUriReader(); 
        parseXml(in, url, tur);
        return tur.getUri();
    }
    
    private static class TldUriReader extends DefaultHandler {
        private StringBuffer buf;
        private String uri;

        TldUriReader() {
        }

        String getUri() {
            return uri;
        }
        
        public void startElement(
            String nsuri,
            String localName,
            String qName,
            Attributes atts) {
            if ("uri".equals(qName)) {
                buf = new StringBuffer();
            }
        }

        public void characters(char[] chars, int off, int len) {
            if (buf != null) {
                buf.append(chars, off, len);
            }
        }

        public void endElement(String nsuri, String localName, String qName) {
            if ("uri".equals(qName)) {
                uri = buf.toString().trim();
                buf = null;
            }
        }
    }

    private static void parseXml(InputStream in, String url, DefaultHandler handler)
    throws Exception
    {
        InputSource is = new InputSource();
        is.setByteStream(in);
        is.setSystemId(url);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setEntityResolver(new LocalTaglibDtds());
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);
        reader.parse(is);
    }

    private static final class Taglib implements TemplateHashModel {
        private final Map tags;

        Taglib(ServletContext ctx, TldPath tldPath, String uri) throws Exception {
            tags = loadTaglib(ctx, tldPath, uri);
        }

        public TemplateModel get(String key) {
            return (TemplateModel)tags.get(key);
        }

        public boolean isEmpty() {
            return tags.isEmpty();
        }

        private static final Map loadTaglib(ServletContext ctx, TldPath tldPath, String uri)
        throws Exception
        {
            final TldParser tldParser = new TldParser();
            final String filePath = tldPath.filePath;
            final InputStream in = ctx.getResourceAsStream(filePath);
            if(in == null) {
                throw new TemplateModelException("Could not find webapp resource " +
                    filePath + " for URI " + uri);
            }
            final String fileUrl = ctx.getResource(filePath).toExternalForm();
            try {
                final String jarItemPath = tldPath.jarItemPath;
                if(jarItemPath != null) {
                    final ZipInputStream zin = new ZipInputStream(in);
                    for(;;) {
                        final ZipEntry ze = zin.getNextEntry();
                        if(ze == null) {
                            throw new TemplateModelException(
                                "Could not find JAR entry " + jarItemPath + 
                                " inside webapp resource " + filePath + 
                                " for URI " + uri);
                        }
                        final String zname = ze.getName();
                        if(zname.equals(jarItemPath)) {
                            parseXml(zin, "jar:" + fileUrl + "!" + zname, tldParser);
                            break;
                        }
                    }
                }
                else {
                    parseXml(in, fileUrl, tldParser);
                }
            }
            finally {
                in.close();
            }
            EventForwarding eventForwarding = EventForwarding.getInstance(ctx);
            if(eventForwarding != null) {
                eventForwarding.addListeners(tldParser.getListeners());
            }
            else if(tldParser.getListeners().size() > 0) {
                throw new TemplateModelException(
                    "Event listeners specified in the TLD could not be " +
                    " registered since the web application doesn't have a" +
                    " listener of class " + EventForwarding.class.getName() + 
                    ". To remedy this, add this element to web.xml:\n" +
                    "| <listener>\n" +
                    "|   <listener-class>" + EventForwarding.class.getName() + "</listener-class>\n" +
                    "| </listener>");
            }
            return tldParser.getTags();
        }
    }

    private static String resolveRelativeUri(String uri)
    throws
        TemplateModelException
    {
        TemplateModel reqHash =
            Environment.getCurrentEnvironment().getVariable(
                FreemarkerServlet.KEY_REQUEST_PRIVATE);
        if(reqHash instanceof HttpRequestHashModel) {
            HttpServletRequest req =
                ((HttpRequestHashModel)reqHash).getRequest();
            String pi = req.getPathInfo();
            String reqPath = req.getServletPath();
            if(reqPath == null) {
                reqPath = "";
            }
            reqPath += (pi == null ? "" : pi);
            // We don't care about paths with ".." in them. If the container
            // wishes to resolve them on its own, let it be.
            int lastSlash = reqPath.lastIndexOf('/');
            if(lastSlash != -1) {
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
    
    private static final class TldParser extends DefaultHandler {
        private final Map tags = new HashMap();
        private final List listeners = new ArrayList();
        
        private Locator locator;
        private StringBuffer buf;
        private String tagName;
        private String tagClassName;

        Map getTags() {
            return tags;
        }

        List getListeners() {
            return listeners;
        }
                
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        public void startElement(
            String nsuri,
            String localName,
            String qName,
            Attributes atts) {
            if ("name".equals(qName) || "tagclass".equals(qName) || "tag-class".equals(qName) || "listener-class".equals(qName)) {
                buf = new StringBuffer();
            }
        }

        public void characters(char[] chars, int off, int len) {
            if (buf != null) {
                buf.append(chars, off, len);
            }
        }

        public void endElement(String nsuri, String localName, String qName)
            throws SAXParseException {
            if ("name".equals(qName)) {
                if(tagName == null) {
                    tagName = buf.toString().trim();
                }
                buf = null;
            }
            else if ("tagclass".equals(qName) || "tag-class".equals(qName)) {
                tagClassName = buf.toString().trim();
                buf = null;
            }
            else if ("tag".equals(qName)) {
                try {
                    Class tagClass = ClassUtil.forName(tagClassName);
                    TemplateModel impl;
                    if(Tag.class.isAssignableFrom(tagClass)) {
                        impl = new TagTransformModel(tagClass); 
                    }
                    else {
                        impl = new SimpleTagDirectiveModel(tagClass); 
                    }
                    tags.put(tagName, impl);
                    tagName = null;
                    tagClassName = null;
                }
                catch (IntrospectionException e) {
                    throw new SAXParseException(
                        "Can't introspect tag class " + tagClassName,
                        locator,
                        e);
                }
                catch (ClassNotFoundException e) {
                    throw new SAXParseException(
                        "Can't find tag class " + tagClassName,
                        locator,
                        e);
                }
            }
            else if ("listener-class".equals(qName)) {
                String listenerClass = buf.toString().trim();
                buf = null;
                try {
                    listeners.add(ClassUtil.forName(listenerClass).newInstance());
                }
                catch(Exception e) {
                    throw new SAXParseException(
                        "Can't instantiate listener class " + listenerClass,
                        locator,
                        e);
                }
            }
        }
    }

    private static final Map dtds = new HashMap();
    static
    {
        // JSP taglib 2.1
        dtds.put("http://java.sun.com/xml/ns/jee/web-jsptaglibrary_2_1.xsd", "web-jsptaglibrary_2_1.xsd");
        // JSP taglib 2.0
        dtds.put("http://java.sun.com/xml/ns/j2ee/web-jsptaglibrary_2_0.xsd", "web-jsptaglibrary_2_0.xsd");
        // JSP taglib 1.2
        dtds.put("-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN", "web-jsptaglibrary_1_2.dtd");
        dtds.put("http://java.sun.com/dtd/web-jsptaglibrary_1_2.dtd", "web-jsptaglibrary_1_2.dtd");
        // JSP taglib 1.1
        dtds.put("-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN", "web-jsptaglibrary_1_1.dtd");
        dtds.put("http://java.sun.com/j2ee/dtds/web-jsptaglibrary_1_1.dtd", "web-jsptaglibrary_1_1.dtd");
        // Servlet 2.5
        dtds.put("http://java.sun.com/xml/ns/jee/web-app_2_5.xsd", "web-app_2_5.xsd");
        // Servlet 2.4
        dtds.put("http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd", "web-app_2_4.xsd");
        // Servlet 2.3
        dtds.put("-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN", "web-app_2_3.dtd");
        dtds.put("http://java.sun.com/dtd/web-app_2_3.dtd", "web-app_2_3.dtd");
        // Servlet 2.2
        dtds.put("-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN", "web-app_2_2.dtd");
        dtds.put("http://java.sun.com/j2ee/dtds/web-app_2_2.dtd", "web-app_2_2.dtd");
    }
    private static final class LocalTaglibDtds implements EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId)
        {
            String resourceName = (String)dtds.get(publicId);
            if(resourceName == null)
            {
                resourceName = (String)dtds.get(systemId);
            }
            InputStream resourceStream;
            if(resourceName != null)
            {
                resourceStream = getClass().getResourceAsStream(resourceName);
            }
            else
            {
                // Fake an empty stream for unknown DTDs 
                resourceStream = new ByteArrayInputStream(new byte[0]);
            }
            InputSource is = new InputSource();
            is.setPublicId(publicId);
            is.setSystemId(systemId);
            is.setByteStream(resourceStream);
            return is;
        }
    }
}
