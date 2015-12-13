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
import freemarker.template.utility.StringUtil;

import java.util.ArrayList;

class ElementModel extends NodeModel implements TemplateScalarModel {
    private final static ArrayList EMPTY_ARRAYLIST = new ArrayList();
    public ElementModel(Element element) {
        super(element);
    }
    
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
            for (int i = 0; i < children.size(); i++) {
                NodeModel child = (NodeModel) children.get(i);
                if (child.node.getNodeType() == Node.ELEMENT_NODE) {
                    ns.add(child);
                }
            }
            return ns;
        }
        if (key.equals("**")) {
            Element elem = (Element) node;
            return new NodeListModel(elem.getElementsByTagName("*"), this);    
        }
        if (key.startsWith("@")) {
            if (key.equals("@@") || key.equals("@*")) {
                return new NodeListModel(node.getAttributes(), this);
            }
            if (key.equals("@@start_tag")) {
                NodeOutputter nodeOutputter = new NodeOutputter(node);
                return new SimpleScalar(nodeOutputter.getOpeningTag((Element) node));
            }
            if (key.equals("@@end_tag")) {
                NodeOutputter nodeOutputter = new NodeOutputter(node);
                return new SimpleScalar(nodeOutputter.getClosingTag((Element) node));
            }
            if (key.equals("@@attributes_markup")) {
                StringBuilder buf = new StringBuilder();
                NodeOutputter nu = new NodeOutputter(node);
                nu.outputContent(node.getAttributes(), buf);
                return new SimpleScalar(buf.toString().trim());
            }
            if (key.equals("@@previous")) {
                Node previousSibling = node.getPreviousSibling();
                while(previousSibling != null && !this.isSignificantNode(previousSibling)) {
                    previousSibling = previousSibling.getPreviousSibling();
                }
                if(previousSibling == null) {
                    return new NodeListModel(EMPTY_ARRAYLIST, null);
                } else {
                    return wrap(previousSibling);
                }
            }
            if (key.equals("@@next")) {
                Node nextSibling = node.getNextSibling();
                while(nextSibling != null && !this.isSignificantNode(nextSibling)) {
                    nextSibling = nextSibling.getNextSibling();
                }
                if(nextSibling == null) {
                    return new NodeListModel(EMPTY_ARRAYLIST, null);
                }
                else {
                    return wrap(nextSibling);
                }
            }
            if (StringUtil.isXMLID(key.substring(1))) {
                Attr att = getAttribute(key.substring(1));
                if (att == null) { 
                    return new NodeListModel(this);
                }
                return wrap(att);
            }
        }
        if (StringUtil.isXMLID(key)) {
            NodeListModel result = ((NodeListModel) getChildNodes()).filterByName(key);
            if (result.size() == 1) {
                return result.get(0);
            }
            return result;
        }
        return super.get(key);
    }

    public boolean isSignificantNode(Node node) throws TemplateModelException {
        boolean significantNode = false;
        if(node != null) {
            boolean isEmpty = StringUtil.isTrimmableToEmpty(node.getTextContent().toCharArray());
            boolean isPINode = node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE;
            boolean isCommentNode = node.getNodeType() == Node.COMMENT_NODE;
            significantNode = !(isEmpty || isPINode || isCommentNode);
        }
        return significantNode;
    }

    public String getAsString() throws TemplateModelException {
        NodeList nl = node.getChildNodes();
        String result = "";
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
                result += child.getNodeValue();
            }
        }
        return result;
    }
    
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
    
    boolean matchesName(String name, Environment env) {
        return StringUtil.matchesName(name, getNodeName(), getNodeNamespace(), env);
    }
}