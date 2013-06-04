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

package freemarker.template.utility;

import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import freemarker.template.SimpleHash;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * A convenient wrapper class for wrapping a Node in the W3C DOM API.
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 */

public class DOMNodeModel implements TemplateHashModel {

    static private HashMap equivalenceTable = new HashMap();
    static {
        equivalenceTable.put("*", "children");
        equivalenceTable.put("@*", "attributes");
    }

    private Node node;
    private HashMap cache = new HashMap();

    public DOMNodeModel(Node node) {
        this.node = node;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        TemplateModel result = null;
        if (equivalenceTable.containsKey(key)) {
            key = (String) equivalenceTable.get(key);
        }
        if (cache.containsKey(key)) {
            result = (TemplateModel) cache.get(key);
        }
        if (result == null) {
            if ("attributes".equals(key)) {
                NamedNodeMap attributes = node.getAttributes();
                if (attributes != null) {
                    SimpleHash hash = new SimpleHash();
                    for (int i = 0; i<attributes.getLength(); i++) {
                        Attr att = (Attr) attributes.item(i);
                        hash.put(att.getName(), att.getValue());
                    }
                    result = hash;
                }
            }
            else if (key.charAt(0) == '@') {
                if (node instanceof Element) {
                    String attValue = ((Element) node).getAttribute(key.substring(1));
                    result = new SimpleScalar(attValue);
                }
                else {
                    throw new TemplateModelException("Trying to get an attribute value for a non-element node");
                }
            }
            else if ("is_element".equals(key)) {
                result = (node instanceof Element) ?
                    TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
            else if ("is_text".equals(key)) {
                result = (node instanceof Text) ?
                    TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
            else if ("name".equals(key)) {
                result = new SimpleScalar(node.getNodeName());
            }
            else if ("children".equals(key)) {
                result = new NodeListTM(node.getChildNodes());
            }
            else if ("parent".equals(key)) {
                Node parent = node.getParentNode();
                result = (parent == null) ? null : new DOMNodeModel(parent);
            }
            else if ("ancestorByName".equals(key)) {
                result = new AncestorByName();
            }
            else if ("nextSibling".equals(key)) {
                Node next = node.getNextSibling();
                result = (next == null) ? null : new DOMNodeModel(next);
            }
            else if ("previousSibling".equals(key)) {
                Node previous = node.getPreviousSibling();
                result = (previous == null) ? null : new DOMNodeModel(previous);
            }
            else if ("nextSiblingElement".equals(key)) {
                Node next = nextSiblingElement(node);
                result = (next == null) ? null : new DOMNodeModel(next);
            }
            else if ("previousSiblingElement".equals(key)) {
                Node previous = previousSiblingElement(node);
                result = (previous == null) ? null : new DOMNodeModel(previous);
            }
            else if ("nextElement".equals(key)) {
                Node next = nextElement(node);
                result = (next == null) ? null : new DOMNodeModel(next);
            }
            else if ("previousElement".equals(key)) {
                Node previous = previousElement(node);
                result = (previous == null) ? null : new DOMNodeModel(previous);
            }
            else if ("text".equals(key)) {
                result = new SimpleScalar(getText(node));
            }
            cache.put(key, result);
        }
        return result;
    }

    public boolean isEmpty() {
        return false;
    }

    static private String getText(Node node) {
        String result = "";
        if (node instanceof Text) {
            result = ((Text) node).getData();
        }
        else if (node instanceof Element) {
            NodeList children = node.getChildNodes();
            for (int i= 0; i<children.getLength(); i++) {
                result += getText(children.item(i));
            }
        }
        return result;
    }

    static private Element nextSiblingElement(Node node) {
        Node next = node;
        while (next != null) {
            next = next.getNextSibling();
            if (next instanceof Element) {
                return (Element) next;
            }
        }
        return null;
    }

    static private Element previousSiblingElement(Node node) {
        Node previous = node;
        while (previous != null) {
            previous = previous.getPreviousSibling();
            if (previous instanceof Element) {
                return (Element) previous;
            }
        }
        return null;
    }

    static private Element nextElement(Node node) {
        if (node.hasChildNodes()) {
            NodeList children = node.getChildNodes();
            for (int i=0; i<children.getLength();i++) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    return (Element) child;
                }
            }
        }
        Element nextSiblingElement = nextSiblingElement(node);
        if (nextSiblingElement != null) {
            return nextSiblingElement;
        }
        Node parent = node.getParentNode();
        while (parent instanceof Element) {
            Element next = nextSiblingElement(parent);
            if (next != null) {
                return next;
            }
            parent = parent.getParentNode();
        }
        return null;
    }

    static private Element previousElement(Node node) {
        Element result = previousSiblingElement(node);
        if (result != null) {
            return result;
        }
        Node parent = node.getParentNode();
        if (parent instanceof Element) {
            return (Element) parent;
        }
        return null;
    }

    void setParent(DOMNodeModel parent) {
        if (parent != null) {
            cache.put("parent", parent);
        }
    }

    String getNodeName() {
        return node.getNodeName();
    }


    class AncestorByName implements TemplateMethodModel {
        public Object exec(List arguments) throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException("Expecting exactly one string argument here");
            }
            String nodeName = (String) arguments.get(0);
            DOMNodeModel ancestor = (DOMNodeModel) DOMNodeModel.this.get("parent");
            while (ancestor != null) {
                if (nodeName.equals(ancestor.getNodeName())) {
                    return ancestor;
                }
                ancestor = (DOMNodeModel) ancestor.get("parent");
            }
            return null;
        }
    }


    class NodeListTM implements TemplateSequenceModel, TemplateMethodModel {

        private NodeList nodeList;
        private TemplateModel[] nodes;

        NodeListTM(NodeList nodeList) {
            this.nodeList = nodeList;
            nodes = new TemplateModel[nodeList.getLength()];
        }

        public TemplateModel get(int index) {
            DOMNodeModel result = (DOMNodeModel) nodes[index];
            if (result == null) {
                result = new DOMNodeModel(nodeList.item(index));
                nodes[index] = result;
                result.setParent(DOMNodeModel.this);
            }
            return result;
        }

        public int size() {
            return nodes.length;
        }

        public Object exec(List arguments) throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException("Expecting exactly one string argument here");
            }
            if (!(node instanceof Element)) {
                throw new TemplateModelException("Expecting element here.");
            }
            Element elem = (Element) node;
            return new NodeListTM(elem.getElementsByTagName((String) arguments.get(0)));
        }
    }
}

