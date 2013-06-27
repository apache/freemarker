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
 * 
 * @author Attila Szegedi
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
