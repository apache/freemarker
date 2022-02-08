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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import freemarker.core.Environment;
import freemarker.core._UnexpectedTypeErrorExplainerTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

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
    
    private static final ObjectWrapper NODE_WRAPPER = new ObjectWrapper() {
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
        super(NODE_WRAPPER);
        this.contextNode = contextNode;
    }
    
    NodeListModel(NodeList nodeList, NodeModel contextNode) {
        super(NODE_WRAPPER);
        for (int i = 0; i < nodeList.getLength(); i++) {
            list.add(nodeList.item(i));
        }
        this.contextNode = contextNode;
    }
    
    NodeListModel(NamedNodeMap nodeList, NodeModel contextNode) {
        super(NODE_WRAPPER);
        for (int i = 0; i < nodeList.getLength(); i++) {
            list.add(nodeList.item(i));
        }
        this.contextNode = contextNode;
    }
    
    NodeListModel(List list, NodeModel contextNode) {
        super(list, NODE_WRAPPER);
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
        int size = size();
        if (size == 1) {
            NodeModel nm = (NodeModel) get(0);
            return nm.get(key);
        }
        if (key.startsWith("@@")) {
            if (key.equals(AtAtKey.MARKUP.getKey()) 
                    || key.equals(AtAtKey.NESTED_MARKUP.getKey()) 
                    || key.equals(AtAtKey.TEXT.getKey())) {
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < size; i++) {
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
                            + (size != 0
                                    ? size + " XML nodes (multiple matches)."
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
            for (int i = 0; i < size; i++) {
                NodeModel nm = (NodeModel) get(i);
                if (nm instanceof ElementModel) {
                    TemplateSequenceModel tsm = (TemplateSequenceModel) nm.get(key);
                    if (tsm != null) {
                        int tsmSize = tsm.size();
                        for (int j = 0; j < tsmSize; j++) {
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
        if (xps == null) {
            throw new TemplateModelException(
                    "No XPath support is available (add Apache Xalan or Jaxen as dependency). "
                    + "This is either malformed, or an XPath expression: " + key);
        }
        Object context = (size == 0) ? null : rawNodeList();
        return xps.executeQuery(context, key);
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
        for (int i = 0; i < expectedClasses.length; i++) {
            Class expectedClass = expectedClasses[i];
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
        int size = size();
        return new Object[] {
                "This XML query result can't be used as ", type, " because for that it had to contain exactly "
                + "1 XML node, but it contains ", Integer.valueOf(size), " nodes. "
                + "That is, the constructing XML query has found ",
                size == 0
                    ? "no matches."
                    : "multiple matches."
                };
    }
    
}