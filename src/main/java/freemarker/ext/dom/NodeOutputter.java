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
 
package freemarker.ext.dom;

import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.utility.StringUtil;

class NodeOutputter {
    
    private Element contextNode;
    private Environment env;
    private String defaultNS;
    private boolean hasDefaultNS;
    private boolean explicitDefaultNSPrefix;
    private HashMap namespacesToPrefixLookup = new HashMap();
    private String namespaceDecl;
    
    NodeOutputter(Node node) {
        if (node instanceof Element) {
            setContext((Element) node);
        }
        else if (node instanceof Attr) {
            setContext(((Attr) node).getOwnerElement());
        }
        else if (node instanceof Document) {
            setContext(((Document) node).getDocumentElement());
        }
    }
    
    private void setContext(Element contextNode) {
        this.contextNode = contextNode;
        this.env = Environment.getCurrentEnvironment();
        this.defaultNS = env.getDefaultNS();
        this.hasDefaultNS = defaultNS != null && defaultNS.length() >0;
        namespacesToPrefixLookup.put(null, "");
        namespacesToPrefixLookup.put("", "");
        buildPrefixLookup(contextNode);
        if (!explicitDefaultNSPrefix && hasDefaultNS) {
            namespacesToPrefixLookup.put(defaultNS, "");
        }
        constructNamespaceDecl();
    }
    
    private void buildPrefixLookup(Node n) {
        String nsURI = n.getNamespaceURI();
        if (nsURI != null && nsURI.length() >0) {
            String prefix = env.getPrefixForNamespace(nsURI);
            namespacesToPrefixLookup.put(nsURI, prefix);
        }  else if (hasDefaultNS && n.getNodeType() == Node.ELEMENT_NODE) {
            namespacesToPrefixLookup.put(defaultNS, Template.DEFAULT_NAMESPACE_PREFIX); 
            explicitDefaultNSPrefix = true;
        } else if (n.getNodeType() == Node.ATTRIBUTE_NODE && hasDefaultNS && defaultNS.equals(nsURI)) {
            namespacesToPrefixLookup.put(defaultNS, Template.DEFAULT_NAMESPACE_PREFIX); 
            explicitDefaultNSPrefix = true;
        }
        NodeList childNodes = n.getChildNodes();
        for (int i = 0; i<childNodes.getLength(); i++) {
            buildPrefixLookup(childNodes.item(i));
        }
    }
    
    private void constructNamespaceDecl() {
        StringBuffer buf = new StringBuffer();
        if (explicitDefaultNSPrefix) {
            buf.append(" xmlns=\"");
            buf.append(defaultNS);
            buf.append("\"");
        }
        for (Iterator it = namespacesToPrefixLookup.keySet().iterator(); it.hasNext();) {
            String nsURI = (String) it.next();
            if (nsURI == null || nsURI.length() == 0) {
                continue;
            }
            String prefix = (String) namespacesToPrefixLookup.get(nsURI);
            if (prefix == null) {
                // Okay, let's auto-assign a prefix.
                // Should we do this??? (REVISIT)
                for (int i=0;i<26;i++) {
                    char[] cc = new char[1];
                    cc[0] = (char) ('a' + i);
                    prefix = new String(cc);
                    if (env.getNamespaceForPrefix(prefix) == null) {
                        break;
                    }
                    prefix = null;
                }
                if (prefix == null) {
                    throw new RuntimeException("This will almost never happen!");
                }
                namespacesToPrefixLookup.put(nsURI, prefix);
            }
            buf.append(" xmlns");
            if (prefix.length() >0) {
                buf.append(":");
                buf.append(prefix);
            }
            buf.append("=\"");
            buf.append(nsURI);
            buf.append("\"");
        }
        this.namespaceDecl = buf.toString();
    }
    
    private void outputQualifiedName(Node n, StringBuffer buf) {
        String nsURI = n.getNamespaceURI();
        if (nsURI == null || nsURI.length() == 0) {
            buf.append(n.getNodeName());
        } else {
            String prefix = (String) namespacesToPrefixLookup.get(nsURI);
            if (prefix == null) {
                //REVISIT!
                buf.append(n.getNodeName());
            } else {
                if (prefix.length() > 0) {
                    buf.append(prefix);
                    buf.append(':');
                }
                buf.append(n.getLocalName());
            }
        }
    }
    
    void outputContent(Node n, StringBuffer buf) {
        switch(n.getNodeType()) {
            case Node.ATTRIBUTE_NODE: {
                if (((Attr) n).getSpecified()) {
                    buf.append(' ');
                    outputQualifiedName(n, buf);
                    buf.append("=\"")
                       .append(StringUtil.XMLEncQAttr(n.getNodeValue()))
                       .append('"');
                }
                break;
            }
            case Node.COMMENT_NODE: {
                buf.append("<!--").append(n.getNodeValue()).append("-->");
                break;
            }
            case Node.DOCUMENT_NODE: {
                outputContent(n.getChildNodes(), buf);
                break;
            }
            case Node.DOCUMENT_TYPE_NODE: {
                buf.append("<!DOCTYPE ").append(n.getNodeName());
                DocumentType dt = (DocumentType)n;
                if(dt.getPublicId() != null) {
                    buf.append(" PUBLIC \"").append(dt.getPublicId()).append('"');
                }
                if(dt.getSystemId() != null) {
                    buf.append(" \"").append(dt.getSystemId()).append('"');
                }
                if(dt.getInternalSubset() != null) {
                    buf.append(" [").append(dt.getInternalSubset()).append(']');
                }
                buf.append('>');
                break;
            }
            case Node.ELEMENT_NODE: {
                buf.append('<');
                outputQualifiedName(n, buf);
                if (n == contextNode) {
                    buf.append(namespaceDecl);
                }
                outputContent(n.getAttributes(), buf);
                NodeList children = n.getChildNodes();
                if (children.getLength() == 0) {
                    buf.append(" />");
                } else {
                    buf.append('>');
                    outputContent(n.getChildNodes(), buf);
                    buf.append("</");
                    outputQualifiedName(n, buf);
                    buf.append('>');
                }
                break;
            }
            case Node.ENTITY_NODE: {
                outputContent(n.getChildNodes(), buf);
                break;
            }
            case Node.ENTITY_REFERENCE_NODE: {
                buf.append('&').append(n.getNodeName()).append(';');
                break;
            }
            case Node.PROCESSING_INSTRUCTION_NODE: {
                buf.append("<?").append(n.getNodeName()).append(' ').append(n.getNodeValue()).append("?>");
                break;
            }
            /*            
                        case Node.CDATA_SECTION_NODE: {
                            buf.append("<![CDATA[").append(n.getNodeValue()).append("]]>");
                            break;
                        }*/
            case Node.CDATA_SECTION_NODE:
            case Node.TEXT_NODE: {
                buf.append(StringUtil.XMLEncNQG(n.getNodeValue()));
                break;
            }
        }
    }

    void outputContent(NodeList nodes, StringBuffer buf) {
        for(int i = 0; i < nodes.getLength(); ++i) {
            outputContent(nodes.item(i), buf);
        }
    }
    
    void outputContent(NamedNodeMap nodes, StringBuffer buf) {
        for(int i = 0; i < nodes.getLength(); ++i) {
            Node n = nodes.item(i);
            if (n.getNodeType() != Node.ATTRIBUTE_NODE 
                || (!n.getNodeName().startsWith("xmlns:") && !n.getNodeName().equals("xmlns"))) 
            { 
                outputContent(n, buf);
            }
        }
    }
    
    String getOpeningTag(Element element) {
        StringBuffer buf = new StringBuffer();
        buf.append('<');
        outputQualifiedName(element, buf);
        buf.append(namespaceDecl);
        outputContent(element.getAttributes(), buf);
        buf.append('>');
        return buf.toString();
    }
    
    String getClosingTag(Element element) {
        StringBuffer buf = new StringBuffer();
        buf.append("</");
        outputQualifiedName(element, buf);
        buf.append('>');
        return buf.toString();
    }
}
