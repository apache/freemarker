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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
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
 * @version $Id: TaglibFactory.java,v 1.26.2.1 2007/05/16 12:13:04 szegedia Exp $
 * @author Attila Szegedi
 */
public class TaglibFactory implements TemplateHashModel {
    private static final Logger logger = Logger.getLogger("freemarker.jsp");

    // No TLDs have been looked up yet
    private static final int LOOKUP_NONE = 0;
    // Only explicit TLDs in web.xml have been looked up
    private static final int LOOKUP_WEB_XML = 1;
    // Both explicit TLDs and those in JARs have been looked up
    private static final int LOOKUP_JARS = 2;
    
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
     * used it is resolved relative to the URL of the current request. In this
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
            Taglib taglib = null;
            taglib = (Taglib) taglibs.get(uri);
            if(taglib != null) {
                return taglib;
            }
            
            taglib = new Taglib();
            try {
                do {
                    if(taglib.load(uri, ctx, locations)) {
                        taglibs.put(uri, taglib);
                        return taglib;
                    }
                }
                while(getMoreTaglibLocations());

                // Not found -- in case of NOROOT_REL_URI, let's resolve it and
                // try again.
                String resolvedUri = resolveRelativeUri(uri);
                if(resolvedUri != uri) {
                    taglib = (Taglib) taglibs.get(resolvedUri);
                    if(taglib != null) {
                        return taglib;
                    }
                    taglib = new Taglib();
                    if(taglib.load(resolvedUri, ctx, locations)) {
                        taglibs.put(resolvedUri, taglib);
                        return taglib;
                    }
                }
            }
            catch(TemplateModelException e) {
                throw e;
            }
            catch(Exception e) {
                throw new TemplateModelException("Could not load taglib information", e);
            }
            return null;
        }
    }

    /**
     * Returns false.
     */
    public boolean isEmpty() {
        return false;
    }

    private boolean getMoreTaglibLocations() throws MalformedURLException, ParserConfigurationException, IOException, SAXException
    {
        switch(lookupPhase) {
            case LOOKUP_NONE: {
                getLocationsFromWebXml();
                lookupPhase = LOOKUP_WEB_XML;
                return true;
            }
            case LOOKUP_WEB_XML: {
                getLocationsFromLibJars();
                lookupPhase = LOOKUP_JARS;
                return true;
            }
            default : {
                return false;
            }
        }
    }
    
    private void getLocationsFromWebXml() throws MalformedURLException, ParserConfigurationException, IOException, SAXException
    {
        WebXmlParser webXmlParser = new WebXmlParser(locations);
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
    
    private static class WebXmlParser extends DefaultHandler {
        private final Map locations;
        
        private StringBuffer buf;
        private String uri;
        private String location;

        WebXmlParser(Map locations) {
            this.locations = locations;
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

        public void endElement(String nsuri, String localName, String qName) {
            if ("taglib-uri".equals(qName)) {
                uri = buf.toString().trim();
                buf = null;
            }
            else if ("taglib-location".equals(qName)) {
                location = buf.toString().trim();
                if(location.indexOf("://") == -1 && !location.startsWith("/")) {
                    location = "/WEB-INF/" + location;
                }
                buf = null;
            }
            else if ("taglib".equals(qName)) {
                String[] loc = new String[2];
                loc[0] = location;
                if(location.endsWith(".jar") || location.endsWith(".zip")) {
                    loc[1] = "META-INF/taglib.tld";
                }
                locations.put(uri, loc);
                if(logger.isDebugEnabled()) {
                    logger.debug("web.xml assigned URI " + StringUtil.jQuote(uri) +
                            " to location " + StringUtil.jQuote(loc[0] + (loc[1] != null ? "!" + loc[1] : "")));
                }
            }
        }
    }


    private void getLocationsFromLibJars() throws ParserConfigurationException, IOException, SAXException
    {
        Set libs = ctx.getResourcePaths("/WEB-INF/lib");
        for (Iterator iter = libs.iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            if(path.endsWith(".jar") || path.endsWith(".zip")) {
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
                            String loc = getTldUri(uin, url);
                            if(loc != null) {
                                locations.put(loc, new String[] { path, zname });
                                if(logger.isDebugEnabled()) {
                                    logger.debug("libjar assigned URI " + StringUtil.jQuote(loc) +
                                            " to location " + StringUtil.jQuote(path+ "!" + zname));
                                }
                            } 
                        }
                    }
                }
                finally {
                    zin.close();
                }
            }
        }
    }

    private String getTldUri(InputStream in, String url) throws ParserConfigurationException, IOException, SAXException 
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
    throws 
        ParserConfigurationException, IOException, SAXException
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
        private Map tags;

        Taglib() {
        }

        public TemplateModel get(String key) {
            return (TemplateModel)tags.get(key);
        }

        public boolean isEmpty() {
            return false;
        }
        
        boolean load(String uri, ServletContext ctx, Map locations)
        throws 
            ParserConfigurationException, 
            IOException, 
            SAXException,
            TemplateModelException
        {
            String[] tldPath = getTldPath(uri, locations);
            if(tldPath != null) {
                if(logger.isDebugEnabled()) {
                    logger.debug("Loading taglib " + StringUtil.jQuote(uri) + " from location " + 
                            StringUtil.jQuote(tldPath[0] + (tldPath[1] != null ? "!" + tldPath[1] : "")));
                }
                tags = loadTaglib(tldPath, ctx);
                if(tags != null) {
                    locations.remove(uri);
                    return true;
                }
            }
            return false;
        }
    }

    private static final Map loadTaglib(String[] tldPath, ServletContext ctx)
    throws
        ParserConfigurationException, IOException, SAXException, TemplateModelException
    {
        String filePath = tldPath[0]; 
        TldParser tldParser = new TldParser();
        InputStream in = ctx.getResourceAsStream(filePath);
        if(in == null) {
            throw new TemplateModelException("Could not find webapp resource " + filePath);
        }
        String url = ctx.getResource(filePath).toExternalForm();
        try {
            String jarPath = tldPath[1];
            if(jarPath != null) {
                ZipInputStream zin = new ZipInputStream(in);
                for(;;) {
                    ZipEntry ze = zin.getNextEntry();
                    if(ze == null) {
                        throw new TemplateModelException("Could not find JAR entry " + jarPath + " inside webapp resource " + filePath);
                    }
                    String zname = ze.getName(); 
                    if(zname.equals(jarPath)) {
                        parseXml(zin, "jar:" + url + "!" + zname, tldParser);
                        break;
                    }
                }
            }
            else {
                parseXml(in, url, tldParser);
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

    private static final String[] getTldPath(String uri, Map locations)
    {
        String[] path = (String[])locations.get(uri);
        // If location was explicitly defined in web.xml, or discovered in a
        // JAR file, use it. (Hopefully this is 99% of the cases)
        if(path != null) {
            return path;
        }
        
        // If there was no explicit mapping in web.xml, but URI is a 
        // ROOT_REL_URI, return it (JSP.7.6.3.2)
        if(uri.startsWith("/")) {
            path = new String[2];
            path[0] = uri;
            if(uri.endsWith(".jar") || uri.endsWith(".zip")) {
                path[1] = "META-INF/taglib.tld";
            }
            return path;
        }

        // Unmapped NOROOT_REL_URI - do nothing with it, so eventually get() 
        // will resolve it and try again
        return null;
    }

    private static String resolveRelativeUri(String uri)
    throws
        TemplateModelException
    {
        // Absolute and root-relative URIs are left as they are.
        if(uri.startsWith("/") || uri.indexOf("://") != -1) {
            return uri;
        }
        
        // Otherwise it is a NOROOT_REL_URI, and has to be resolved relative
        // to current page... We have to obtain the request object to know what
        // is the URL of the current page (this assumes there's a 
        // HttpRequestHashModel under name FreemarkerServlet.KEY_REQUEST in the
        // environment...) (JSP.7.6.3.2)
        TemplateModel reqHash = 
            Environment.getCurrentEnvironment().getVariable(
                FreemarkerServlet.KEY_REQUEST_PRIVATE);
        if(reqHash instanceof HttpRequestHashModel) {
            HttpServletRequest req  = 
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
