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
 *  
 * @author Attila Szegedi
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
