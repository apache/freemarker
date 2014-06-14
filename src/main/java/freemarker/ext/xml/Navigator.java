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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jaxen.NamespaceContext;

import freemarker.template.TemplateModelException;

/**
 */
abstract class Navigator {
    // Cache of already parsed XPath expressions
    private final Map xpathCache = new WeakHashMap();
    // Operators this navigator defines
    private final Map operators = createOperatorMap();
    private final NodeOperator attributeOperator = getOperator("_attributes");
    private final NodeOperator childrenOperator = getOperator("_children");
    
    NodeOperator getOperator(String key) {
        return (NodeOperator)operators.get(key);
    }
    
    NodeOperator getAttributeOperator() {
        return attributeOperator;
    }
    
    NodeOperator getChildrenOperator() {
        return childrenOperator;
    }
    
    abstract void getAsString(Object node, StringWriter sw)
    throws
        TemplateModelException;

    List applyXPath(List nodes, String xpathString, Object namespaces)
    throws
        TemplateModelException
    {
        XPathEx xpath = null;
        try
        {
            synchronized(xpathCache)
            {
                xpath = (XPathEx)xpathCache.get(xpathString);
                if (xpath == null)
                {
                    xpath = createXPathEx(xpathString);
                    xpathCache.put(xpathString, xpath);
                }
            }
            return xpath.selectNodes(nodes, (NamespaceContext)namespaces);
        }
        catch(Exception e)
        {
            throw new TemplateModelException("Could not evaulate XPath expression " + xpathString, e);
        }
    }
    
    interface XPathEx
    {
        List selectNodes(Object nodes, NamespaceContext namespaces)  throws TemplateModelException;
    }
    
    abstract XPathEx createXPathEx(String xpathString) throws TemplateModelException;

    abstract void getChildren(Object node, String localName, String namespaceUri, List result);
    
    abstract void getAttributes(Object node, String localName, String namespaceUri, List result);

    abstract void getDescendants(Object node, List result);

    abstract Object getParent(Object node);
    
    abstract Object getDocument(Object node);
    
    abstract Object getDocumentType(Object node);

    private void getAncestors(Object node, List result)
    {
        for(;;) {
            Object parent = getParent(node);
            if(parent == null) {
                break;
            }
            result.add(parent);
            node = parent;
        }
    }
    
    abstract void getContent(Object node, List result);

    abstract String getText(Object node);

    abstract String getLocalName(Object node);

    abstract String getNamespacePrefix(Object node);

    String getQualifiedName(Object node) {
        String lname = getLocalName(node);
        if(lname == null) {
            return null;
        }
        String nsprefix = getNamespacePrefix(node);
        if(nsprefix == null || nsprefix.length() == 0) {
            return lname;
        }
        else {
            return nsprefix + ":" + lname;
        }
    }
    
    abstract String getType(Object node);

    abstract String getNamespaceUri(Object node);

    boolean equal(String s1, String s2) {
        return s1 == null ? s2 == null : s1.equals(s2);
    }
    
    private Map createOperatorMap() {
        Map map = new HashMap();
        map.put("_attributes", new AttributesOp());
        map.put("@*", map.get("_attributes"));
        map.put("_children", new ChildrenOp());
        map.put("*", map.get("_children"));
        map.put("_descendantOrSelf", new DescendantOrSelfOp());
        map.put("_descendant", new DescendantOp());
        map.put("_document", new DocumentOp());
        map.put("_doctype", new DocumentTypeOp());
        map.put("_ancestor", new AncestorOp());
        map.put("_ancestorOrSelf", new AncestorOrSelfOp());
        map.put("_content", new ContentOp());
        map.put("_name", new LocalNameOp());
        map.put("_nsprefix", new NamespacePrefixOp());
        map.put("_nsuri", new NamespaceUriOp());
        map.put("_parent", new ParentOp());
        map.put("_qname", new QualifiedNameOp());
        map.put("_text", new TextOp());
        map.put("_type", new TypeOp());
        return map;
    }

    private class ChildrenOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            getChildren(node, localName, namespaceUri, result);
        }
    }

    private class AttributesOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            getAttributes(node, localName, namespaceUri, result);
        }
    }

    private class DescendantOrSelfOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            result.add(node);
            getDescendants(node, result);
        }
    }

    private class DescendantOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            getDescendants(node, result);
        }
    }

    private class AncestorOrSelfOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            result.add(node);
            getAncestors(node, result);
        }
    }

    private class AncestorOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            getAncestors(node, result);
        }
    }

    private class ParentOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            Object parent = getParent(node);
            if(parent != null) {
                result.add(parent);
            }
        }
    }

    private class DocumentOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            Object document = getDocument(node);
            if(document != null) {
                result.add(document);
            }
        }
    }

    private class DocumentTypeOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            Object documentType = getDocumentType(node);
            if(documentType != null) {
                result.add(documentType);
            }
        }
    }

    private class ContentOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            getContent(node, result);
        }
    }

    private class TextOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            String text = getText(node);
            if(text != null) {
                result.add(text);
            }
        }
    }

    private class LocalNameOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            String text = getLocalName(node);
            if(text != null) {
                result.add(text);
            }
        }
    }

    private class QualifiedNameOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            String qname = getQualifiedName(node);
            if(qname != null) {
                result.add(qname);
            }
        }
    }

    private class NamespacePrefixOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            String text = getNamespacePrefix(node);
            if(text != null) {
                result.add(text);
            }
        }
    }

    private class NamespaceUriOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            String text = getNamespaceUri(node);
            if(text != null) {
                result.add(text);
            }
        }
    }

    private class TypeOp implements NodeOperator {
        public void process(Object node, String localName, String namespaceUri, List result)
        {
            result.add(getType(node));
        }
    }
}
