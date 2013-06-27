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
 
package freemarker.ext.dom;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import freemarker.core.Environment;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.StringUtil;

class NodeListModel extends SimpleSequence implements TemplateHashModel {
    
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
        if (key.equals("@@markup") 
            || key.equals("@@nested_markup") 
            || key.equals("@@text"))
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
                    int size = tsm == null ? 0 : tsm.size();
                    for (int j=0; j < size; j++) {
                        result.add(tsm.get(j));
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
}