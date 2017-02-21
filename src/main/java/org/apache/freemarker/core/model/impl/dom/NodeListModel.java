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
 
package org.apache.freemarker.core.model.impl.dom;

import java.util.ArrayList;
import java.util.List;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core._UnexpectedTypeErrorExplainerTemplateModel;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.model.impl.SimpleSequence;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Used when the result set contains 0 or multiple nodes; shouldn't be used when you have exactly 1 node. For exactly 1
 * node, use {@link NodeModel#wrap(Node)}, because {@link NodeModel} subclasses can have extra features building on that
 * restriction, like single elements with text content can be used as FTL string-s.
 * 
 * <p>
 * This class is not guaranteed to be thread safe, so instances of this shouldn't be used as shared variable (
 * {@link Configuration#setSharedVariable(String, Object)}).
 */
class NodeListModel extends SimpleSequence implements TemplateHashModel, _UnexpectedTypeErrorExplainerTemplateModel {
    
    // [2.4] make these private
    NodeModel contextNode;
    XPathSupport xpathSupport;
    
    private static ObjectWrapper nodeWrapper = new ObjectWrapper() {
        @Override
        public TemplateModel wrap(Object obj) {
            if (obj instanceof NodeModel) {
                return (NodeModel) obj;
            }
            return NodeModel.wrap((Node) obj);
        }
    };
    
    NodeListModel(Node contextNode) {
        this(NodeModel.wrap(contextNode));
    }
    
    NodeListModel(NodeModel contextNode) {
        super(nodeWrapper);
        this.contextNode = contextNode;
    }
    
    NodeListModel(NodeList nodeList, NodeModel contextNode) {
        super(nodeWrapper);
        for (int i = 0; i < nodeList.getLength(); i++) {
            list.add(nodeList.item(i));
        }
        this.contextNode = contextNode;
    }
    
    NodeListModel(NamedNodeMap nodeList, NodeModel contextNode) {
        super(nodeWrapper);
        for (int i = 0; i < nodeList.getLength(); i++) {
            list.add(nodeList.item(i));
        }
        this.contextNode = contextNode;
    }
    
    NodeListModel(List list, NodeModel contextNode) {
        super(list, nodeWrapper);
        this.contextNode = contextNode;
    }
    
    NodeListModel filterByName(String name) throws TemplateModelException {
        NodeListModel result = new NodeListModel(contextNode);
        int size = size();
        if (size == 0) {
            return result;
        }
        Environment env = Environment.getCurrentEnvironment();
        for (int i = 0; i < size; i++) {
            NodeModel nm = (NodeModel) get(i);
            if (nm instanceof ElementModel) {
                if (((ElementModel) nm).matchesName(name, env)) {
                    result.add(nm);
                }
            }
        }
        return result;
    }
    
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }
    
    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        if (size() == 1) {
            NodeModel nm = (NodeModel) get(0);
            return nm.get(key);
        }
        if (key.startsWith("@@")) {
            if (key.equals(AtAtKey.MARKUP.getKey()) 
                    || key.equals(AtAtKey.NESTED_MARKUP.getKey()) 
                    || key.equals(AtAtKey.TEXT.getKey())) {
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < size(); i++) {
                    NodeModel nm = (NodeModel) get(i);
                    TemplateScalarModel textModel = (TemplateScalarModel) nm.get(key);
                    result.append(textModel.getAsString());
                }
                return new SimpleScalar(result.toString());
            } else if (key.length() != 2 /* to allow "@@" to fall through */) {
                // As @@... would cause exception in the XPath engine, we throw a nicer exception now. 
                if (AtAtKey.containsKey(key)) {
                    throw new TemplateModelException(
                            "\"" + key + "\" is only applicable to a single XML node, but it was applied on "
                            + (size() != 0
                                    ? size() + " XML nodes (multiple matches)."
                                    : "an empty list of XML nodes (no matches)."));
                } else {
                    throw new TemplateModelException("Unsupported @@ key: " + key);
                }
            }
        }
        if (DomStringUtil.isXMLNameLike(key) 
                || ((key.startsWith("@")
                        && (DomStringUtil.isXMLNameLike(key, 1)  || key.equals("@@") || key.equals("@*"))))
                || key.equals("*") || key.equals("**")) {
            NodeListModel result = new NodeListModel(contextNode);
            for (int i = 0; i < size(); i++) {
                NodeModel nm = (NodeModel) get(i);
                if (nm instanceof ElementModel) {
                    TemplateSequenceModel tsm = (TemplateSequenceModel) nm.get(key);
                    if (tsm != null) {
                        int size = tsm.size();
                        for (int j = 0; j < size; j++) {
                            result.add(tsm.get(j));
                        }
                    }
                }
            }
            if (result.size() == 1) {
                return result.get(0);
            }
            return result;
        }
        XPathSupport xps = getXPathSupport();
        if (xps != null) {
            Object context = (size() == 0) ? null : rawNodeList(); 
            return xps.executeQuery(context, key);
        } else {
            throw new TemplateModelException(
                    "Can't try to resolve the XML query key, because no XPath support is available. "
                    + "This is either malformed or an XPath expression: " + key);
        }
    }
    
    private List rawNodeList() throws TemplateModelException {
        int size = size();
        ArrayList al = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            al.add(((NodeModel) get(i)).node);
        }
        return al;
    }
    
    XPathSupport getXPathSupport() throws TemplateModelException {
        if (xpathSupport == null) {
            if (contextNode != null) {
                xpathSupport = contextNode.getXPathSupport();
            } else if (size() > 0) {
                xpathSupport = ((NodeModel) get(0)).getXPathSupport();
            }
        }
        return xpathSupport;
    }

    @Override
    public Object[] explainTypeError(Class[] expectedClasses) {
        for (Class expectedClass : expectedClasses) {
            if (TemplateScalarModel.class.isAssignableFrom(expectedClass)
                    || TemplateDateModel.class.isAssignableFrom(expectedClass)
                    || TemplateNumberModel.class.isAssignableFrom(expectedClass)
                    || TemplateBooleanModel.class.isAssignableFrom(expectedClass)) {
                return newTypeErrorExplanation("string");
            } else if (TemplateNodeModel.class.isAssignableFrom(expectedClass)) {
                return newTypeErrorExplanation("node");
            }
        }
        return null;
    }

    private Object[] newTypeErrorExplanation(String type) {
        return new Object[] {
                "This XML query result can't be used as ", type, " because for that it had to contain exactly "
                + "1 XML node, but it contains ", Integer.valueOf(size()), " nodes. "
                + "That is, the constructing XML query has found ",
                isEmpty()
                    ? "no matches."
                    : "multiple matches."
                };
    }
    
}