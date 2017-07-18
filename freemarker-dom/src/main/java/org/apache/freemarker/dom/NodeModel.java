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
 
package org.apache.freemarker.dom;


import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core._UnexpectedTypeErrorExplainerTemplateModel;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.TemplateNodeModelEx;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.slf4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * A base class for wrapping a single W3C DOM_WRAPPER Node as a FreeMarker template model.
 * <p>
 * Note that {@link DefaultObjectWrapper} automatically wraps W3C DOM_WRAPPER {@link Node}-s into this, so you may need do that
 * with this class manually. However, before dropping the {@link Node}-s into the data-model, you certainly want to
 * apply {@link NodeModel#simplify(Node)} on them.
 * <p>
 * This class is not guaranteed to be thread safe, so instances of this shouldn't be used as
 * {@linkplain Configuration#getSharedVariables() shared variable}.
 * <p>
 * To represent a node sequence (such as a query result) of exactly 1 nodes, this class should be used instead of
 * {@link NodeListModel}, as it adds extra capabilities by utilizing that we have exactly 1 node. If you need to wrap a
 * node sequence of 0 or multiple nodes, you must use {@link NodeListModel}.
 */
abstract public class NodeModel implements TemplateNodeModelEx, TemplateHashModel, TemplateSequenceModel,
    AdapterTemplateModel, WrapperTemplateModel, _UnexpectedTypeErrorExplainerTemplateModel {

    static private final Logger LOG = DomLog.LOG;

    private static final Object STATIC_LOCK = new Object();
    
    static private final Map xpathSupportMap = Collections.synchronizedMap(new WeakHashMap());
    
    static private XPathSupport jaxenXPathSupport;
    
    static Class xpathSupportClass;
    
    static {
        try {
            useDefaultXPathSupport();
        } catch (Exception e) {
            // do nothing
        }
        if (xpathSupportClass == null && LOG.isWarnEnabled()) {
            LOG.warn("No XPath support is available.");
        }
    }
    
    /**
     * The W3C DOM_WRAPPER Node being wrapped.
     */
    final Node node;
    private TemplateSequenceModel children;
    private NodeModel parent;
    
    protected NodeModel(Node node) {
        this.node = node;
    }
    
    /**
     * @return the underling W3C DOM_WRAPPER Node object that this TemplateNodeModel
     * is wrapping.
     */
    public Node getNode() {
        return node;
    }
    
    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.startsWith("@@")) {
            if (key.equals(AtAtKey.TEXT.getKey())) {
                return new SimpleScalar(getText(node));
            } else if (key.equals(AtAtKey.NAMESPACE.getKey())) {
                String nsURI = node.getNamespaceURI();
                return nsURI == null ? null : new SimpleScalar(nsURI);
            } else if (key.equals(AtAtKey.LOCAL_NAME.getKey())) {
                String localName = node.getLocalName();
                if (localName == null) {
                    localName = getNodeName();
                }
                return new SimpleScalar(localName);
            } else if (key.equals(AtAtKey.MARKUP.getKey())) {
                StringBuilder buf = new StringBuilder();
                NodeOutputter nu = new NodeOutputter(node);
                nu.outputContent(node, buf);
                return new SimpleScalar(buf.toString());
            } else if (key.equals(AtAtKey.NESTED_MARKUP.getKey())) {
                StringBuilder buf = new StringBuilder();
                NodeOutputter nu = new NodeOutputter(node);
                nu.outputContent(node.getChildNodes(), buf);
                return new SimpleScalar(buf.toString());
            } else if (key.equals(AtAtKey.QNAME.getKey())) {
                String qname = getQualifiedName();
                return qname != null ? new SimpleScalar(qname) : null;
            } else {
                // As @@... would cause exception in the XPath engine, we throw a nicer exception now. 
                if (AtAtKey.containsKey(key)) {
                    throw new TemplateModelException(
                            "\"" + key + "\" is not supported for an XML node of type \"" + getNodeType() + "\".");
                } else {
                    throw new TemplateModelException("Unsupported @@ key: " + key);
                }
            }
        } else {
            XPathSupport xps = getXPathSupport();
            if (xps != null) {
                return xps.executeQuery(node, key);
            } else {
                throw new TemplateModelException(
                        "Can't try to resolve the XML query key, because no XPath support is available. "
                        + "This is either malformed or an XPath expression: " + key);
            }
        }
    }
    
    @Override
    public TemplateNodeModel getParentNode() {
        if (parent == null) {
            Node parentNode = node.getParentNode();
            if (parentNode == null) {
                if (node instanceof Attr) {
                    parentNode = ((Attr) node).getOwnerElement();
                }
            }
            parent = wrap(parentNode);
        }
        return parent;
    }

    @Override
    public TemplateNodeModelEx getPreviousSibling() throws TemplateModelException {
        return wrap(node.getPreviousSibling());
    }

    @Override
    public TemplateNodeModelEx getNextSibling() throws TemplateModelException {
        return wrap(node.getNextSibling());
    }

    @Override
    public TemplateSequenceModel getChildNodes() {
        if (children == null) {
            children = new NodeListModel(node.getChildNodes(), this);
        }
        return children;
    }
    
    @Override
    public final String getNodeType() throws TemplateModelException {
        short nodeType = node.getNodeType();
        switch (nodeType) {
            case Node.ATTRIBUTE_NODE : return "attribute";
            case Node.CDATA_SECTION_NODE : return "text";
            case Node.COMMENT_NODE : return "comment";
            case Node.DOCUMENT_FRAGMENT_NODE : return "document_fragment";
            case Node.DOCUMENT_NODE : return "document";
            case Node.DOCUMENT_TYPE_NODE : return "document_type";
            case Node.ELEMENT_NODE : return "element";
            case Node.ENTITY_NODE : return "entity";
            case Node.ENTITY_REFERENCE_NODE : return "entity_reference";
            case Node.NOTATION_NODE : return "notation";
            case Node.PROCESSING_INSTRUCTION_NODE : return "pi";
            case Node.TEXT_NODE : return "text";
        }
        throw new TemplateModelException("Unknown node type: " + nodeType + ". This should be impossible!");
    }
    
    public TemplateModel exec(List args) throws TemplateModelException {
        if (args.size() != 1) {
            throw new TemplateModelException("Expecting exactly one arguments");
        }
        String query = (String) args.get(0);
        // Now, we try to behave as if this is an XPath expression
        XPathSupport xps = getXPathSupport();
        if (xps == null) {
            throw new TemplateModelException("No XPath support available");
        }
        return xps.executeQuery(node, query);
    }
    
    /**
     * Always returns 1.
     */
    @Override
    public final int size() {
        return 1;
    }
    
    @Override
    public final TemplateModel get(int i) {
        return i == 0 ? this : null;
    }
    
    @Override
    public String getNodeNamespace() {
        int nodeType = node.getNodeType();
        if (nodeType != Node.ATTRIBUTE_NODE && nodeType != Node.ELEMENT_NODE) { 
            return null;
        }
        String result = node.getNamespaceURI();
        if (result == null && nodeType == Node.ELEMENT_NODE) {
            result = "";
        } else if ("".equals(result) && nodeType == Node.ATTRIBUTE_NODE) {
            result = null;
        }
        return result;
    }
    
    @Override
    public final int hashCode() {
        return node.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        return other.getClass() == getClass()
                && ((NodeModel) other).node.equals(node);
    }
    
    /**
     * Creates a {@link NodeModel} from a DOM {@link Node}. It's strongly recommended modify the {@link Node} with
     * {@link #simplify(Node)}, so the DOM will be easier to process in templates.
     * 
     * @param node
     *            The DOM node to wrap. This is typically an {@link Element} or a {@link Document}, but all kind of node
     *            types are supported. If {@code null}, {@code null} will be returned.
     */
    static public NodeModel wrap(Node node) {
        if (node == null) {
            return null;
        }
        NodeModel result = null;
        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE : result = new DocumentModel((Document) node); break;
            case Node.ELEMENT_NODE : result = new ElementModel((Element) node); break;
            case Node.ATTRIBUTE_NODE : result = new AttributeNodeModel((Attr) node); break;
            case Node.CDATA_SECTION_NODE : 
            case Node.COMMENT_NODE :
            case Node.TEXT_NODE : result = new CharacterDataNodeModel((org.w3c.dom.CharacterData) node); break;
            case Node.PROCESSING_INSTRUCTION_NODE : result = new PINodeModel((ProcessingInstruction) node); break;
            case Node.DOCUMENT_TYPE_NODE : result = new DocumentTypeModel((DocumentType) node); break;
            default: throw new IllegalArgumentException(
                    "Unsupported node type: " + node.getNodeType() + " ("
                    + node.getClass().getName() + ")");
        }
        return result;
    }
    
    /**
     * Recursively removes all comment nodes from the subtree.
     *
     * @see #simplify
     */
    static public void removeComments(Node parent) {
        Node child = parent.getFirstChild();
        while (child != null) {
            Node nextSibling = child.getNextSibling();
            if (child.getNodeType() == Node.COMMENT_NODE) {
                parent.removeChild(child);
            } else if (child.hasChildNodes()) {
                removeComments(child);
            }
            child = nextSibling;
        }
    }
    
    /**
     * Recursively removes all processing instruction nodes from the subtree.
     *
     * @see #simplify
     */
    static public void removePIs(Node parent) {
        Node child = parent.getFirstChild();
        while (child != null) {
            Node nextSibling = child.getNextSibling();
            if (child.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                parent.removeChild(child);
            } else if (child.hasChildNodes()) {
                removePIs(child);
            }
            child = nextSibling;
        }
    }
    
    /**
     * Merges adjacent text nodes (where CDATA counts as text node too). Operates recursively on the entire subtree.
     * The merged node will have the type of the first node of the adjacent merged nodes.
     * 
     * <p>Because XPath assumes that there are no adjacent text nodes in the tree, not doing this can have
     * undesirable side effects. Xalan queries like {@code text()} will only return the first of a list of matching
     * adjacent text nodes instead of all of them, while Jaxen will return all of them as intuitively expected. 
     *
     * @see #simplify
     */
    static public void mergeAdjacentText(Node parent) {
        mergeAdjacentText(parent, new StringBuilder(0));
    }
    
    static private void mergeAdjacentText(Node parent, StringBuilder collectorBuf) {
        Node child = parent.getFirstChild();
        while (child != null) {
            Node next = child.getNextSibling();
            if (child instanceof Text) {
                boolean atFirstText = true;
                while (next instanceof Text) { //
                    if (atFirstText) {
                        collectorBuf.setLength(0);
                        collectorBuf.ensureCapacity(child.getNodeValue().length() + next.getNodeValue().length());
                        collectorBuf.append(child.getNodeValue());
                        atFirstText = false;
                    }
                    collectorBuf.append(next.getNodeValue());
                    
                    parent.removeChild(next);
                    
                    next = child.getNextSibling();
                }
                if (!atFirstText && collectorBuf.length() != 0) {
                    ((CharacterData) child).setData(collectorBuf.toString());
                }
            } else {
                mergeAdjacentText(child, collectorBuf);
            }
            child = next;
        }
    }
    
    /**
     * Removes all comments and processing instruction, and unites adjacent text nodes (here CDATA counts as text as
     * well). This is similar to applying {@link #removeComments(Node)}, {@link #removePIs(Node)}, and finally
     * {@link #mergeAdjacentText(Node)}, but it does all that somewhat faster.
     */    
    static public void simplify(Node parent) {
        simplify(parent, new StringBuilder(0));
    }
    
    static private void simplify(Node parent, StringBuilder collectorTextChildBuff) {
        Node collectorTextChild = null;
        Node child = parent.getFirstChild();
        while (child != null) {
            Node next = child.getNextSibling();
            if (child.hasChildNodes()) {
                if (collectorTextChild != null) {
                    // Commit pending text node merge:
                    if (collectorTextChildBuff.length() != 0) {
                        ((CharacterData) collectorTextChild).setData(collectorTextChildBuff.toString());
                        collectorTextChildBuff.setLength(0);
                    }
                    collectorTextChild = null;
                }
                
                simplify(child, collectorTextChildBuff);
            } else {
                int type = child.getNodeType();
                if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE ) {
                    if (collectorTextChild != null) {
                        if (collectorTextChildBuff.length() == 0) {
                            collectorTextChildBuff.ensureCapacity(
                                    collectorTextChild.getNodeValue().length() + child.getNodeValue().length());
                            collectorTextChildBuff.append(collectorTextChild.getNodeValue());
                        }
                        collectorTextChildBuff.append(child.getNodeValue());
                        parent.removeChild(child);
                    } else {
                        collectorTextChild = child;
                        collectorTextChildBuff.setLength(0);
                    }
                } else if (type == Node.COMMENT_NODE) {
                    parent.removeChild(child);
                } else if (type == Node.PROCESSING_INSTRUCTION_NODE) {
                    parent.removeChild(child);
                } else if (collectorTextChild != null) {
                    // Commit pending text node merge:
                    if (collectorTextChildBuff.length() != 0) {
                        ((CharacterData) collectorTextChild).setData(collectorTextChildBuff.toString());
                        collectorTextChildBuff.setLength(0);
                    }
                    collectorTextChild = null;
                }
            }
            child = next;
        }
        
        if (collectorTextChild != null) {
            // Commit pending text node merge:
            if (collectorTextChildBuff.length() != 0) {
                ((CharacterData) collectorTextChild).setData(collectorTextChildBuff.toString());
                collectorTextChildBuff.setLength(0);
            }
        }
    }
    
    NodeModel getDocumentNodeModel() {
        if (node instanceof Document) {
            return this;
        } else {
            return wrap(node.getOwnerDocument());
        }
    }

    /**
     * Tells the system to use (restore) the default (initial) XPath system used by
     * this FreeMarker version on this system.
     */
    static public void useDefaultXPathSupport() {
        synchronized (STATIC_LOCK) {
            xpathSupportClass = null;
            jaxenXPathSupport = null;
            try {
                useXalanXPathSupport();
            } catch (Exception e) {
                // ignore
            }
            if (xpathSupportClass == null) try {
            	useSunInternalXPathSupport();
            } catch (Exception e) {
                // ignore
            }
            if (xpathSupportClass == null) try {
                useJaxenXPathSupport();
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    /**
     * Convenience method. Tells the system to use Jaxen for XPath queries.
     * @throws Exception if the Jaxen classes are not present.
     */
    static public void useJaxenXPathSupport() throws Exception {
        Class.forName("org.jaxen.dom.DOMXPath");
        Class c = Class.forName("org.apache.freemarker.dom.JaxenXPathSupport");
        jaxenXPathSupport = (XPathSupport) c.newInstance();
        synchronized (STATIC_LOCK) {
            xpathSupportClass = c;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using Jaxen classes for XPath support");
        }
    }
    
    /**
     * Convenience method. Tells the system to use Xalan for XPath queries.
     * @throws Exception if the Xalan XPath classes are not present.
     */
    static public void useXalanXPathSupport() throws Exception {
        Class.forName("org.apache.xpath.XPath");
        Class c = Class.forName("org.apache.freemarker.dom.XalanXPathSupport");
        synchronized (STATIC_LOCK) {
            xpathSupportClass = c;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using Xalan classes for XPath support");
        }
    }
    
    static public void useSunInternalXPathSupport() throws Exception {
        Class.forName("com.sun.org.apache.xpath.internal.XPath");
        Class c = Class.forName("org.apache.freemarker.dom.SunInternalXalanXPathSupport");
        synchronized (STATIC_LOCK) {
            xpathSupportClass = c;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using Sun's internal Xalan classes for XPath support");
        }
    }
    
    /**
     * Set an alternative implementation of org.apache.freemarker.dom.XPathSupport to use
     * as the XPath engine.
     * @param cl the class, or <code>null</code> to disable XPath support.
     */
    static public void setXPathSupportClass(Class cl) {
        if (cl != null && !XPathSupport.class.isAssignableFrom(cl)) {
            throw new RuntimeException("Class " + cl.getName()
                    + " does not implement org.apache.freemarker.dom.XPathSupport");
        }
        synchronized (STATIC_LOCK) {
            xpathSupportClass = cl;
        }
    }

    /**
     * Get the currently used org.apache.freemarker.dom.XPathSupport used as the XPath engine.
     * Returns <code>null</code> if XPath support is disabled.
     */
    static public Class getXPathSupportClass() {
        synchronized (STATIC_LOCK) {
            return xpathSupportClass;
        }
    }

    static private String getText(Node node) {
        String result = "";
        if (node instanceof Text || node instanceof CDATASection) {
            result = ((org.w3c.dom.CharacterData) node).getData();
        } else if (node instanceof Element) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                result += getText(children.item(i));
            }
        } else if (node instanceof Document) {
            result = getText(((Document) node).getDocumentElement());
        }
        return result;
    }
    
    XPathSupport getXPathSupport() {
        if (jaxenXPathSupport != null) {
            return jaxenXPathSupport;
        }
        XPathSupport xps = null;
        Document doc = node.getOwnerDocument();
        if (doc == null) {
            doc = (Document) node;
        }
        synchronized (doc) {
            WeakReference ref = (WeakReference) xpathSupportMap.get(doc);
            if (ref != null) {
                xps = (XPathSupport) ref.get();
            }
            if (xps == null) {
                try {
                    xps = (XPathSupport) xpathSupportClass.newInstance();
                    xpathSupportMap.put(doc, new WeakReference(xps));
                } catch (Exception e) {
                    LOG.error("Error instantiating xpathSupport class", e);
                }                
            }
        }
        return xps;
    }
    
    
    String getQualifiedName() throws TemplateModelException {
        return getNodeName();
    }
    
    @Override
    public Object getAdaptedObject(Class hint) {
        return node;
    }
    
    @Override
    public Object getWrappedObject() {
        return node;
    }
    
    @Override
    public Object[] explainTypeError(Class[] expectedClasses) {
        for (Class expectedClass : expectedClasses) {
            if (TemplateDateModel.class.isAssignableFrom(expectedClass)
                    || TemplateNumberModel.class.isAssignableFrom(expectedClass)
                    || TemplateBooleanModel.class.isAssignableFrom(expectedClass)) {
                return new Object[]{
                        "XML node values are always strings (text), that is, they can't be used as number, "
                                + "date/time/dateTime or boolean without explicit conversion (such as "
                                + "someNode?number, someNode?dateTime.xs, someNode?date.xs, someNode?time.xs, "
                                + "someNode?boolean).",
                };
            }
        }
        return null;
    }
    
}
