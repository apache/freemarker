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
 
package freemarker.ext.dom;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import freemarker.core.Environment;
import freemarker.core._UnexpectedTypeErrorExplainerTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.StringUtil;

class NodeListModel extends SimpleSequence implements TemplateHashModel, _UnexpectedTypeErrorExplainerTemplateModel {
    
    NodeModel contextNode;
    XPathSupport xpathSupport;
    
    private static ObjectWrapper nodeWrapper = new ObjectWrapper() {
        public TemplateModel wrap(Object obj) {
            if (obj instanceof NodeModel) {
                return (NodeModel) obj;
            }
            return NodeModel.wrap((Node) obj);
        }
    };
    
    NodeListModel(Node node) {
        this(NodeModel.wrap(node));
    }
    
    NodeListModel(NodeModel contextNode) {
        super(nodeWrapper);
        this.contextNode = contextNode;
    }
    
    NodeListModel(NodeList nodeList, NodeModel contextNode) {
        super(nodeWrapper);
        for (int i=0; i<nodeList.getLength();i++) {
            list.add(nodeList.item(i));
        }
        this.contextNode = contextNode;
    }
    
    NodeListModel(NamedNodeMap nodeList, NodeModel contextNode) {
        super(nodeWrapper);
        for (int i=0; i<nodeList.getLength();i++) {
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
        for (int i = 0; i<size; i++) {
            NodeModel nm = (NodeModel) get(i);
            if (nm instanceof ElementModel) {
                if (((ElementModel) nm).matchesName(name, env)) {
                    result.add(nm);
                }
            }
        }
        return result;
    }
    
    public boolean isEmpty() {
        return size() == 0;
    }
    
    public TemplateModel get(String key) throws TemplateModelException {
        if (size() ==1) {
            NodeModel nm = (NodeModel) get(0);
            return nm.get(key);
        }
        if (key.startsWith("@@") &&
            (key.equals("@@markup") 
            || key.equals("@@nested_markup") 
            || key.equals("@@text")))
        {
            StringBuffer result = new StringBuffer();
            for (int i=0; i<size(); i++) {
                NodeModel nm = (NodeModel) get(i);
                TemplateScalarModel textModel = (TemplateScalarModel) nm.get(key);
                result.append(textModel.getAsString());
            }
            return new SimpleScalar(result.toString());
        }
        if (StringUtil.isXMLID(key) 
            || ((key.startsWith("@") && StringUtil.isXMLID(key.substring(1))))
            || key.equals("*") || key.equals("**") || key.equals("@@") || key.equals("@*")) 
        {
            NodeListModel result = new NodeListModel(contextNode);
            for (int i=0; i<size(); i++) {
                NodeModel nm = (NodeModel) get(i);
                if (nm instanceof ElementModel) {
                    TemplateSequenceModel tsm = (TemplateSequenceModel) ((ElementModel) nm).get(key);
                    if (tsm != null) {
                        int size = tsm.size();
                        for (int j=0; j < size; j++) {
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
        }
        throw new TemplateModelException("Key: '" + key + "' is not legal for a node sequence ("
                + this.getClass().getName() + "). This node sequence contains " + size() + " node(s). "
                + "Some keys are valid only for node sequences of size 1. "
                + "If you use Xalan (instead of Jaxen), XPath expression keys work only with "
                + "node lists of size 1.");
    }
    
    private List rawNodeList() throws TemplateModelException {
        int size = size();
        ArrayList al = new ArrayList(size);
        for (int i=0; i<size; i++) {
            al.add(((NodeModel) get(i)).node);
        }
        return al;
    }
    
    XPathSupport getXPathSupport() throws TemplateModelException {
        if (xpathSupport == null) {
            if (contextNode != null) {
                xpathSupport = contextNode.getXPathSupport();
            }
            else if (size() >0) {
                xpathSupport = ((NodeModel) get(0)).getXPathSupport();
            }
        }
        return xpathSupport;
    }

    public Object[] explainTypeError(Class[] expectedClasses) {
        for (int i = 0; i < expectedClasses.length; i++) {
            Class expectedClass = expectedClasses[i];
            if (TemplateScalarModel.class.isAssignableFrom(expectedClass)
                    || TemplateDateModel.class.isAssignableFrom(expectedClass)
                    || TemplateNumberModel.class.isAssignableFrom(expectedClass)
                    || TemplateBooleanModel.class.isAssignableFrom(expectedClass)) {
                return new Object[] {
                        "This XML query result can't be used as string because for that it had to contain exactly "
                        + "1 XML node, but it contains ", new Integer(size()), " nodes. "
                        + "That is, the constructing XML query has found ",
                        isEmpty()
                            ? "no matches."
                            : "multiple matches."
                        };
            }
        }
        return null;
    }
    
}