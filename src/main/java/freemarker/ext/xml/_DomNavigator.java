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

package freemarker.ext.xml;

import java.io.StringWriter;
import java.util.List;

import org.jaxen.Context;
import org.jaxen.NamespaceContext;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import freemarker.template.TemplateModelException;
import freemarker.template.utility.StringUtil;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 */
public class _DomNavigator extends Navigator {
    public _DomNavigator() {
    } 

    void getAsString(Object node, StringWriter sw) {
        outputContent((Node)node, sw.getBuffer());
    }
    
    private void outputContent(Node n, StringBuffer buf) {
        switch(n.getNodeType()) {
            case Node.ATTRIBUTE_NODE: {
                buf.append(' ')
                   .append(getQualifiedName(n))
                   .append("=\"")
                   .append(StringUtil.XMLEncNA(n.getNodeValue())) // XmlEncNA for HTML compatibility
                   .append('"');
                break;
            }
            case Node.CDATA_SECTION_NODE: {
                buf.append("<![CDATA[").append(n.getNodeValue()).append("]]>");
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
                    buf.append('"').append(dt.getSystemId()).append('"');
                }
                if(dt.getInternalSubset() != null) {
                    buf.append(" [").append(dt.getInternalSubset()).append(']');
                }
                buf.append('>');
                break;
            }
            case Node.ELEMENT_NODE: {
                buf.append('<').append(getQualifiedName(n));
                outputContent(n.getAttributes(), buf);
                buf.append('>');
                outputContent(n.getChildNodes(), buf);
                buf.append("</").append(getQualifiedName(n)).append('>');
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
            case Node.TEXT_NODE: {
                buf.append(StringUtil.XMLEncNQG(n.getNodeValue()));
                break;
            }
        }
    }

    private void outputContent(NodeList nodes, StringBuffer buf) {
        for(int i = 0; i < nodes.getLength(); ++i) {
            outputContent(nodes.item(i), buf);
        }
    }
    
    private void outputContent(NamedNodeMap nodes, StringBuffer buf) {
        for(int i = 0; i < nodes.getLength(); ++i) {
            outputContent(nodes.item(i), buf);
        }
    }
    
    void getChildren(Object node, String localName, String namespaceUri, List result) {
        if("".equals(namespaceUri)) {
            namespaceUri = null;
        }
        NodeList children = ((Node)node).getChildNodes();
        for(int i = 0; i < children.getLength(); ++i) {
            Node subnode = children.item(i);
            // IMO, we should get the text nodes as well -- will discuss.
            if(subnode.getNodeType() == Node.ELEMENT_NODE || subnode.getNodeType() == Node.TEXT_NODE) {
                if(localName == null || (equal(subnode.getNodeName(), localName) && equal(subnode.getNamespaceURI(), namespaceUri))) {
                    result.add(subnode);
                }
            }
        }
    }
    
    void getAttributes(Object node, String localName, String namespaceUri, List result) {
        if(node instanceof Element) {
            Element e = (Element)node;
            if(localName == null) {
                NamedNodeMap atts = e.getAttributes();
                for(int i = 0; i < atts.getLength(); ++i) {
                    result.add(atts.item(i));
                }
            }
            else {
                if("".equals(namespaceUri)) {
                    namespaceUri = null;
                }
                Attr attr = e.getAttributeNodeNS(namespaceUri, localName);
                if(attr != null) {
                    result.add(attr);
                }
            }
        }
        else if (node instanceof ProcessingInstruction) {
            ProcessingInstruction pi = (ProcessingInstruction)node;
            if ("target".equals(localName)) {
                result.add(createAttribute(pi, "target", pi.getTarget()));
            }
            else if ("data".equals(localName)) {
                result.add(createAttribute(pi, "data", pi.getData()));
            }
            else {
                // TODO: DOM has no facility for parsing data into
                // name-value pairs...
                ;
            }
        } else if (node instanceof DocumentType) {
            DocumentType doctype = (DocumentType)node;
            if ("publicId".equals(localName)) {
                result.add(createAttribute(doctype, "publicId", doctype.getPublicId()));
            }
            else if ("systemId".equals(localName)) {
                result.add(createAttribute(doctype, "systemId", doctype.getSystemId()));
            }
            else if ("elementName".equals(localName)) {
                result.add(createAttribute(doctype, "elementName", doctype.getNodeName()));
            }
        } 
    }

    private Attr createAttribute(Node node, String name, String value) {
        Attr attr = node.getOwnerDocument().createAttribute(name);
        attr.setNodeValue(value);
        return attr;
    }
    
    void getDescendants(Object node, List result) {
        NodeList children = ((Node)node).getChildNodes();
        for(int i = 0; i < children.getLength(); ++i) {
            Node subnode = children.item(i);
            if(subnode.getNodeType() == Node.ELEMENT_NODE) {
                result.add(subnode);
                getDescendants(subnode, result);
            }
        }
    }

    Object getParent(Object node) {
        return ((Node)node).getParentNode();
    }

    Object getDocument(Object node) {
        return ((Node)node).getOwnerDocument();
    }

    Object getDocumentType(Object node) {
        return 
            node instanceof Document
            ? ((Document)node).getDoctype()
            : null;
    }

    void getContent(Object node, List result) {
        NodeList children = ((Node)node).getChildNodes();
        for(int i = 0; i < children.getLength(); ++i) {
            result.add(children.item(i));
        }
    }

    String getText(Object node) {
        StringBuffer buf = new StringBuffer();
        if(node instanceof Element) {
            NodeList children = ((Node)node).getChildNodes();
            for(int i = 0; i < children.getLength(); ++i) {
                Node child = children.item(i);
                if(child instanceof Text) { 
                    buf.append(child.getNodeValue());
                }
            }
            return buf.toString();
        }
        else {
            return ((Node)node).getNodeValue();
        }
    }

    String getLocalName(Object node) {
        return ((Node)node).getNodeName();
    }

    String getNamespacePrefix(Object node) {
        return ((Node)node).getPrefix();
    }

    String getNamespaceUri(Object node) {
        return ((Node)node).getNamespaceURI();
    }

    String getType(Object node) {
        switch(((Node)node).getNodeType()) {
            case Node.ATTRIBUTE_NODE: {
                return "attribute";
            }
            case Node.CDATA_SECTION_NODE: {
                return "cdata";
            }
            case Node.COMMENT_NODE: {
                return "comment";
            }
            case Node.DOCUMENT_NODE: {
                return "document";
            }
            case Node.DOCUMENT_TYPE_NODE: {
                return "documentType";
            }
            case Node.ELEMENT_NODE: {
                return "element";
            }
            case Node.ENTITY_NODE: {
                return "entity";
            }
            case Node.ENTITY_REFERENCE_NODE: {
                return "entityReference";
            }
            case Node.PROCESSING_INSTRUCTION_NODE: {
                return "processingInstruction";
            }
            case Node.TEXT_NODE: {
                return "text";
            }
        }
        return "unknown";
    }

    XPathEx createXPathEx(String xpathString) throws TemplateModelException
    {
        try {
            return new DomXPathEx(xpathString);
        }
        catch(Exception e) {
            throw new TemplateModelException(e);
        }
    }

    private static final class DomXPathEx
    extends
        DOMXPath
    implements
        XPathEx
    {
        DomXPathEx(String path)
        throws 
            Exception
        {
            super(path);
        }

        public List selectNodes(Object object, NamespaceContext namespaces)
        throws
            TemplateModelException
        {
            Context context = getContext(object);
            context.getContextSupport().setNamespaceContext(namespaces);
            try {
                return selectNodesForContext(context);
            }
            catch(Exception e) {
                throw new TemplateModelException(e);
            }
        } 
    }
}
