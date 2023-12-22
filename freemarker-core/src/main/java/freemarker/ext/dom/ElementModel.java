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
 
package freemarker.ext.dom;

import java.util.Collections;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

class ElementModel extends NodeModel implements TemplateScalarModel {

    public ElementModel(Element element) {
        super(element);
    }
    
    @Override
    public boolean isEmpty() {
        return false;
    }
    
    /**
     * An Element node supports various hash keys.
     * Any key that corresponds to the tag name of any child elements
     * returns a sequence of those elements. The special key "*" returns 
     * all the element's direct children.
     * The "**" key return all the element's descendants in the order they
     * occur in the document.
     * Any key starting with '@' is taken to be the name of an element attribute.
     * The special key "@@" returns a hash of all the element's attributes.
     * The special key "/" returns the root document node associated with this element.
     */
    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("*")) {
            NodeListModel ns = new NodeListModel(this);
            TemplateSequenceModel children = getChildNodes();
            int size = children.size();
            for (int i = 0; i < size; i++) {
                NodeModel child = (NodeModel) children.get(i);
                if (child.node.getNodeType() == Node.ELEMENT_NODE) {
                    ns.add(child);
                }
            }
            return ns;
        } else if (key.equals("**")) {
            return new NodeListModel(((Element) node).getElementsByTagName("*"), this);    
        } else if (key.startsWith("@")) {
            if (key.startsWith("@@")) {
                if (key.equals(AtAtKey.ATTRIBUTES.getKey())) {
                    return new NodeListModel(node.getAttributes(), this);
                } else if (key.equals(AtAtKey.START_TAG.getKey())) {
                    NodeOutputter nodeOutputter = new NodeOutputter(node);
                    return new SimpleScalar(nodeOutputter.getOpeningTag((Element) node));
                } else if (key.equals(AtAtKey.END_TAG.getKey())) {
                    NodeOutputter nodeOutputter = new NodeOutputter(node);
                    return new SimpleScalar(nodeOutputter.getClosingTag((Element) node));
                } else if (key.equals(AtAtKey.ATTRIBUTES_MARKUP.getKey())) {
                    StringBuilder buf = new StringBuilder();
                    NodeOutputter nu = new NodeOutputter(node);
                    nu.outputContent(node.getAttributes(), buf);
                    return new SimpleScalar(buf.toString().trim());
                } else if (key.equals(AtAtKey.PREVIOUS_SIBLING_ELEMENT.getKey())) {
                    Node previousSibling = node.getPreviousSibling();
                    while (previousSibling != null && !this.isSignificantNode(previousSibling)) {
                        previousSibling = previousSibling.getPreviousSibling();
                    }
                    return previousSibling != null && previousSibling.getNodeType() == Node.ELEMENT_NODE
                            ? wrap(previousSibling) : new NodeListModel(Collections.emptyList(), null);  
                } else if (key.equals(AtAtKey.NEXT_SIBLING_ELEMENT.getKey())) {
                    Node nextSibling = node.getNextSibling();
                    while (nextSibling != null && !this.isSignificantNode(nextSibling)) {
                        nextSibling = nextSibling.getNextSibling();
                    }
                    return nextSibling != null && nextSibling.getNodeType() == Node.ELEMENT_NODE
                            ? wrap(nextSibling) : new NodeListModel(Collections.emptyList(), null);  
                } else {
                    // We don't know anything like this that's element-specific; fall back 
                    return super.get(key);
                }
            } else { // Starts with "@", but not with "@@"
                if (DomStringUtil.isXMLNameLike(key, 1)) {
                    Attr att = getAttribute(key.substring(1));
                    if (att == null) { 
                        return new NodeListModel(this);
                    }
                    return wrap(att);
                } else if (key.equals("@*")) {
                    return new NodeListModel(node.getAttributes(), this);
                } else {
                    // We don't know anything like this that's element-specific; fall back 
                    return super.get(key);
                }
            }
        } else if (DomStringUtil.isXMLNameLike(key)) {
            // We interpret key as an element name
            NodeListModel result = ((NodeListModel) getChildNodes()).filterByName(key);
            return result.size() != 1 ? result : result.get(0);
        } else {
            // We don't anything like this that's element-specific; fall back 
            return super.get(key);
        }
    }

    @Override
    public String getAsString() throws TemplateModelException {
        NodeList nl = node.getChildNodes();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < nl.getLength(); i++) {
            Node child = nl.item(i);
            int nodeType = child.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                String msg = "Only elements with no child elements can be processed as text."
                        + "\nThis element with name \""
                        + node.getNodeName()
                        + "\" has a child element named: " + child.getNodeName();
                throw new TemplateModelException(msg);
            } else if (nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE) {
                result.append(child.getNodeValue());
            }
        }
        return result.toString();
    }
    
    @Override
    public String getNodeName() {
        String result = node.getLocalName();
        if (result == null || result.equals("")) {
            result = node.getNodeName();
        }
        return result;
    }
    
    @Override
    String getQualifiedName() {
        String nodeName = getNodeName();
        String nsURI = getNodeNamespace();
        if (nsURI == null || nsURI.length() == 0) {
            return nodeName;
        }
        Environment env = Environment.getCurrentEnvironment();
        String defaultNS = env.getDefaultNS();
        String prefix;
        if (defaultNS != null && defaultNS.equals(nsURI)) {
            prefix = "";
        } else {
            prefix = env.getPrefixForNamespace(nsURI);
            
        }
        if (prefix == null) {
            return null; // We have no qualified name, because there is no prefix mapping
        }
        if (prefix.length() > 0) {
            prefix += ":";
        }
        return prefix + nodeName;
    }
    
    private Attr getAttribute(String qname) {
        Element element = (Element) node;
        Attr result = element.getAttributeNode(qname);
        if (result != null)
            return result;
        int colonIndex = qname.indexOf(':');
        if (colonIndex > 0) {
            String prefix = qname.substring(0, colonIndex);
            String uri;
            if (prefix.equals(Template.DEFAULT_NAMESPACE_PREFIX)) {
                uri = Environment.getCurrentEnvironment().getDefaultNS();
            } else {
                uri = Environment.getCurrentEnvironment().getNamespaceForPrefix(prefix);
            }
            String localName = qname.substring(1 + colonIndex);
            if (uri != null) {
                result = element.getAttributeNodeNS(uri, localName);
            }
        }
        return result;
    }
    
    private boolean isSignificantNode(Node node) throws TemplateModelException {
        return (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE)
                ? !isBlankXMLText(node.getTextContent())
                : node.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE && node.getNodeType() != Node.COMMENT_NODE;
    }
    
    private boolean isBlankXMLText(String s) {
        if (s == null) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!isXMLWhiteSpace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * White space according the XML spec. 
     */
    private boolean isXMLWhiteSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' | c == '\r';
    }

    boolean matchesName(String name, Environment env) {
        return DomStringUtil.matchesName(name, getNodeName(), getNodeNamespace(), env);
    }
}