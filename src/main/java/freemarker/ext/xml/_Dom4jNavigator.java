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
import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.ProcessingInstruction;
import org.dom4j.tree.DefaultAttribute;
import org.jaxen.Context;
import org.jaxen.NamespaceContext;
import org.jaxen.dom4j.Dom4jXPath;

import freemarker.template.TemplateModelException;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 */
public class _Dom4jNavigator extends Navigator {

    public _Dom4jNavigator() {
    } 

    void getAsString(Object node, StringWriter sw) {
        sw.getBuffer().append(((Node)node).asXML());
    }

    void getChildren(Object node, String localName, String namespaceUri, List result) {
        if(node instanceof Element) {
            Element e = (Element)node;
            if(localName == null) {
                result.addAll(e.elements());
            }
            else {
                result.addAll(e.elements(e.getQName().getDocumentFactory().createQName(localName, "", namespaceUri)));
            }
        }
        else if(node instanceof Document) {
            Element root = ((Document)node).getRootElement();
            if(localName == null || (equal(root.getName(), localName) && equal(root.getNamespaceURI(), namespaceUri))) {
                result.add(root);
            }
        }
    }
    
    void getAttributes(Object node, String localName, String namespaceUri, List result) {
        if(node instanceof Element) {
            Element e = (Element)node;
            if(localName == null) {
                result.addAll(e.attributes());
            }
            else {
                Attribute attr = e.attribute(e.getQName().getDocumentFactory().createQName(localName, "", namespaceUri)); 
                if(attr != null) {
                    result.add(attr);
                }
            }
        }
        else if (node instanceof ProcessingInstruction) {
            ProcessingInstruction pi = (ProcessingInstruction)node;
            if ("target".equals(localName)) {
                result.add(new DefaultAttribute("target", pi.getTarget()));
            }
            else if ("data".equals(localName)) {
                result.add(new DefaultAttribute("data", pi.getText()));
            }
            else {
                result.add(new DefaultAttribute(localName, pi.getValue(localName)));
            }
        } else if (node instanceof DocumentType) {
            DocumentType doctype = (DocumentType)node;
            if ("publicId".equals(localName)) {
                result.add(new DefaultAttribute("publicId", doctype.getPublicID()));
            }
            else if ("systemId".equals(localName)) {
                result.add(new DefaultAttribute("systemId", doctype.getSystemID()));
            }
            else if ("elementName".equals(localName)) {
                result.add(new DefaultAttribute("elementName", doctype.getElementName()));
            }
        } 
    }

    void getDescendants(Object node, List result) {
        if(node instanceof Branch) {
            getDescendants((Branch)node, result);
        }
    }
    
    private void getDescendants(Branch node, List result) {
        List content = node.content();
        for (Iterator iter = content.iterator(); iter.hasNext();) {
            Node subnode = (Node) iter.next();
            if(subnode instanceof Element) {
                result.add(subnode);
                getDescendants(subnode, result);
            }
        }
    }

    Object getParent(Object node) {
        return ((Node)node).getParent();
    }

    Object getDocument(Object node) {
        return ((Node)node).getDocument();
    }

    Object getDocumentType(Object node) {
        return 
            node instanceof Document 
            ? ((Document)node).getDocType()
            : null; 
    }
    
    void getContent(Object node, List result) {
        if(node instanceof Branch) {
            result.addAll(((Branch)node).content());
        }
    }

    String getText(Object node) {
        return ((Node)node).getText();
    }

    String getLocalName(Object node) {
        return ((Node)node).getName();
    }

    String getNamespacePrefix(Object node) {
        if(node instanceof Element) {
            return ((Element)node).getNamespacePrefix();
        }
        if(node instanceof Attribute) {
            return ((Attribute)node).getNamespacePrefix();
        }
        return null;
    }

    String getNamespaceUri(Object node) {
        if(node instanceof Element) {
            return ((Element)node).getNamespaceURI();
        }
        if(node instanceof Attribute) {
            return ((Attribute)node).getNamespaceURI();
        }
        return null;
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
            case Node.ENTITY_REFERENCE_NODE: {
                return "entityReference";
            }
            case Node.NAMESPACE_NODE: {
                return "namespace";
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
            return new Dom4jXPathEx(xpathString);
        }
        catch(Exception e) {
            throw new TemplateModelException(e);
        }
    }

    private static final class Dom4jXPathEx
    extends
        Dom4jXPath
    implements
        XPathEx
    {
        Dom4jXPathEx(String path)
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
