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

package org.apache.freemarker.dom.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.dom.NodeModel;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utility to load XML resources into {@link Node}-s or {@link NodeModel}-s.
 */
public final class DOMLoader {

    private static final Object STATIC_LOCK = new Object();
    
    static private DocumentBuilderFactory docBuilderFactory;
    
    private DOMLoader() {
        //
    }
    
    /**
     * Convenience method to invoke a {@link NodeModel} from a SAX {@link InputSource}.
     */
    static public NodeModel toModel(InputSource is, boolean simplify)
        throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilder builder = getDocumentBuilderFactory().newDocumentBuilder();
        final Document doc;
        try {
            doc = builder.parse(is);
        } catch (MalformedURLException e) {
            // This typical error has an error message that is hard to understand, so let's translate it:
            if (is.getSystemId() == null && is.getCharacterStream() == null && is.getByteStream() == null) {
                throw new MalformedURLException(
                        "The SAX InputSource has systemId == null && characterStream == null && byteStream == null. "
                        + "This is often because it was created with a null InputStream or Reader, which is often because "
                        + "the XML file it should point to was not found. "
                        + "(The original exception was: " + e + ")");
            } else {
                throw e;
            }
        }
        if (simplify) {
            NodeModel.simplify(doc);
        }
        return NodeModel.wrap(doc);
    }
    
    /**
     * Same as {@link #toModel(InputSource, boolean) parse(is, true)}.
     */
    static public NodeModel toModel(InputSource is) throws SAXException, IOException, ParserConfigurationException {
        return toModel(is, true);
    }

    static public NodeModel toModel(Class<?> baseClass, String resourcePath)
            throws ParserConfigurationException, SAXException, IOException {
        InputStream in = baseClass.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new FileNotFoundException("Class loader resource not found: baseClass=" + baseClass.getName()
                    + "; path=" + _StringUtils.jQuote(resourcePath));
        }
        return toModel(new InputSource(in));
    }

    /**
     * Same as {@link #toModel(InputSource, boolean)}, but loads from a {@link File}; don't miss the security
     * warnings documented there.
     */
    static public NodeModel toModel(String content, boolean simplify) 
    throws SAXException, IOException, ParserConfigurationException {
        return toModel(toInputSource(content));
    }
    
    /**
     * Same as {@link #toModel(InputSource, boolean) parse(source, true)}, but loads from a {@link String}.
     */
    static public NodeModel toModel(String content) throws SAXException, IOException, ParserConfigurationException {
        return toModel(content, true);
    }
    
    public static Document toDOM(String content) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilder builder =  getDocumentBuilderFactory().newDocumentBuilder();
        return builder.parse(toInputSource(content));
    }

    public static Document toDOM(Class<?> baseClass, String resourcePath) throws SAXException, IOException,
            ParserConfigurationException {
        DocumentBuilder builder =  getDocumentBuilderFactory().newDocumentBuilder();
        InputStream in = baseClass.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new FileNotFoundException("Class loader resource not found: baseClass="
                    + baseClass.getName() + "; " + "path=" + _StringUtils.jQuote(resourcePath));
        }
        return builder.parse(new InputSource(in));
    }

    static private DocumentBuilderFactory getDocumentBuilderFactory() {
        synchronized (STATIC_LOCK) {
            if (docBuilderFactory == null) {
                DocumentBuilderFactory newFactory = DocumentBuilderFactory.newInstance();
                newFactory.setNamespaceAware(true);
                newFactory.setIgnoringElementContentWhitespace(true);
                docBuilderFactory = newFactory;  // We only write it out when the initialization was full 
            }
            return docBuilderFactory;
        }
    }

    private static InputSource toInputSource(String content) {
        return new InputSource(new StringReader(content));
    }
    
}
