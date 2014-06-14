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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import org.jaxen.Context;
import org.jaxen.NamespaceContext;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.EntityRef;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;

import freemarker.template.TemplateModelException;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 */
public class _JdomNavigator extends Navigator {
    private static final XMLOutputter OUTPUT = new XMLOutputter();
    
    public _JdomNavigator() {
    } 

    void getAsString(Object node, StringWriter sw)
    throws
        TemplateModelException
    {
        try {
            if (node instanceof Element) {
                OUTPUT.output((Element)node, sw);
            }
            else if (node instanceof Attribute) {
                Attribute attribute = (Attribute)node;
                sw.write(" ");
                sw.write(attribute.getQualifiedName());
                sw.write("=\"");
                sw.write(OUTPUT.escapeAttributeEntities(attribute.getValue()));
                sw.write("\"");
            }
            else if (node instanceof Text) {
                OUTPUT.output((Text)node, sw);
            }
            else if (node instanceof Document) {
                OUTPUT.output((Document)node, sw);
            }
            else if (node instanceof ProcessingInstruction) {
                OUTPUT.output((ProcessingInstruction)node, sw);
            }
            else if (node instanceof Comment) {
                OUTPUT.output((Comment)node, sw);
            }
            else if (node instanceof CDATA) {
                OUTPUT.output((CDATA)node, sw);
            }
            else if (node instanceof DocType) {
                OUTPUT.output((DocType)node, sw);
            }
            else if (node instanceof EntityRef) {
                OUTPUT.output((EntityRef)node, sw);
            }
            else {
                throw new TemplateModelException(node.getClass().getName() + " is not a core JDOM class");
            }
        }
        catch(IOException e) {
            throw new TemplateModelException(e);
        }
    }

    void getChildren(Object node, String localName, String namespaceUri, List result) {
        if(node instanceof Element) {
            Element e = (Element)node;
            if(localName == null) {
                result.addAll(e.getChildren());
            }
            else {
                result.addAll(e.getChildren(localName, Namespace.getNamespace("", namespaceUri)));
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
                result.addAll(e.getAttributes());
            }
            else {
                Attribute attr = e.getAttribute(localName, Namespace.getNamespace("", namespaceUri)); 
                if(attr != null) {
                    result.add(attr);
                } 
            }
        } else if (node instanceof ProcessingInstruction) {
            ProcessingInstruction pi = (ProcessingInstruction)node;
            if ("target".equals(localName)) {
                result.add(new Attribute("target", pi.getTarget()));
            }
            else if ("data".equals(localName)) {
                result.add(new Attribute("data", pi.getData()));
            }
            else {
                result.add(new Attribute(localName, pi.getValue(localName)));
            }
        } else if (node instanceof DocType) {
            DocType doctype = (DocType)node;
            if ("publicId".equals(localName)) {
                result.add(new Attribute("publicId", doctype.getPublicID()));
            }
            else if ("systemId".equals(localName)) {
                result.add(new Attribute("systemId", doctype.getSystemID()));
            }
            else if ("elementName".equals(localName)) {
                result.add(new Attribute("elementName", doctype.getElementName()));
            }
        } 
    }

    void getDescendants(Object node, List result) {
        if(node instanceof Document) {
            Element root = ((Document)node).getRootElement();
            result.add(root);
            getDescendants(root, result);
        }
        else if(node instanceof Element) {
            getDescendants((Element)node, result);
        }
    }
    
    private void getDescendants(Element node, List result) {
        for (Iterator iter = node.getChildren().iterator(); iter.hasNext();) {
            Element subnode = (Element)iter.next();
            result.add(subnode);
            getDescendants(subnode, result);
        }
    }

    Object getParent(Object node) {
        if (node instanceof Element) {
            return((Element)node).getParent();
        }
        if (node instanceof Attribute) {
            return((Attribute)node).getParent();
        }
        if (node instanceof Text) {
            return((Text)node).getParent();
        }
        if (node instanceof ProcessingInstruction) {
            return((ProcessingInstruction)node).getParent();
        }
        if (node instanceof Comment) {
            return((Comment)node).getParent();
        }
        if (node instanceof EntityRef) {
            return((EntityRef)node).getParent();
        }
        return null;
    }

    Object getDocument(Object node) {
        if (node instanceof Element) {
            return ((Element)node).getDocument();
        }
        else if (node instanceof Attribute) {
            Element parent = ((Attribute)node).getParent();
            return parent == null ? null : parent.getDocument();
        } 
        else if (node instanceof Text) {
            Element parent = ((Text)node).getParent();
            return parent == null ? null : parent.getDocument();
        } 
        else if (node instanceof Document)
            return node;
        else if (node instanceof ProcessingInstruction) {
            return ((ProcessingInstruction)node).getDocument();
        }
        else if (node instanceof EntityRef) {
            return ((EntityRef)node).getDocument();
        }
        else if (node instanceof Comment) {
            return ((Comment)node).getDocument();
        }
        return null;
    }

    Object getDocumentType(Object node) {
        return 
            node instanceof Document 
            ? ((Document)node).getDocType()
            : null; 
    }
    
    void getContent(Object node, List result) {
        if (node instanceof Element)
            result.addAll(((Element)node).getContent());
        else if (node instanceof Document)
            result.addAll(((Document)node).getContent());
    }

    String getText(Object node) {
        if (node instanceof Element) {
            return ((Element)node).getTextTrim();
        }
        if (node instanceof Attribute) {
            return ((Attribute)node).getValue();
        }
        if (node instanceof CDATA) {
            return ((CDATA)node).getText();
        }
        if (node instanceof Comment) {
            return ((Comment)node).getText();
        }
        if (node instanceof ProcessingInstruction) {
            return ((ProcessingInstruction)node).getData();
        }
        return null;
    }

    String getLocalName(Object node) {
        if (node instanceof Element) {
            return ((Element)node).getName();
        }
        if (node instanceof Attribute) {
            return ((Attribute)node).getName();
        }
        if (node instanceof EntityRef) {
            return ((EntityRef)node).getName();
        }
        if (node instanceof ProcessingInstruction) {
            return ((ProcessingInstruction)node).getTarget();
        }
        if (node instanceof DocType) {
            return ((DocType)node).getElementName();
        }
        return null;
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
        if(node instanceof Attribute) {
            return "attribute";
        }
        if(node instanceof CDATA) {
            return "cdata";
        }
        if(node instanceof Comment) {
            return "comment";
        }
        if(node instanceof Document) {
            return "document";
        }
        if(node instanceof DocType) {
            return "documentType";
        }
        if(node instanceof Element) {
            return "element";
        }
        if(node instanceof EntityRef) {
            return "entityReference";
        }
        if(node instanceof Namespace) {
            return "namespace";
        }
        if(node instanceof ProcessingInstruction) {
            return "processingInstruction";
        }
        if(node instanceof Text) {
            return "text";
        }
        return "unknown";
    }

    XPathEx createXPathEx(String xpathString) throws TemplateModelException
    {
        try {
            return new JDOMXPathEx(xpathString);
        }
        catch(Exception e) {
            throw new TemplateModelException(e);
        }
    }

    private static final class JDOMXPathEx
    extends
        JDOMXPath
    implements
        XPathEx
    {
        JDOMXPathEx(String path)
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
