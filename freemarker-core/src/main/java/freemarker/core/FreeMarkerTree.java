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

package freemarker.core;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import freemarker.template.Template;

/**
 * Allows exposure of a FreeMarker template's AST as a Swing tree.
 * 
 * @deprecated Will be removed, as Swing classes aren't accessible on Google App Engine.
 */
@Deprecated
public class FreeMarkerTree extends JTree {
    private static final long serialVersionUID = 1L;

    private final Map nodeMap = new HashMap();
    
    public FreeMarkerTree(Template template) {
        setTemplate(template);
    }

    private TreeNode getNode(TemplateElement element) {
        TreeNode n = (TreeNode) nodeMap.get(element);
        if (n != null) {
            return n;
        }
        n = new TemplateElementTreeNode(element);
        nodeMap.put(element, n);
        return n;
    }

    public void setTemplate(Template template) {
        this.setModel(new DefaultTreeModel(getNode(template.getRootTreeNode())));
        this.invalidate();
    }

    @Override
    public String convertValueToText(Object value, boolean selected,
                                     boolean expanded, boolean leaf, int row,
                                     boolean hasFocus) {
        if (value instanceof TemplateElementTreeNode) {
            return ((TemplateElementTreeNode) value).element.getDescription();
        }
        return value.toString();
    }
    
    private class TemplateElementTreeNode implements TreeNode {
        private final TemplateElement element;
        
        TemplateElementTreeNode(TemplateElement element) {
            this.element = element;
        }

        @Override
        public Enumeration children() {
            final Enumeration e = element.children();
            return new Enumeration() {
                @Override
                public boolean hasMoreElements() {
                    return e.hasMoreElements();
                }
                @Override
                public Object nextElement() {
                    return getNode((TemplateElement) e.nextElement());
                }
            };
        }

        @Override
        public boolean getAllowsChildren() {
            return element.getAllowsChildren();
        }

        @Override
        public TreeNode getChildAt(int childIndex) {
            return getNode(element.getChildAt(childIndex));
        }

        @Override
        public int getChildCount() {
            return element.getChildCount();
        }

        @Override
        public int getIndex(TreeNode node) {
            return element.getIndex(((TemplateElementTreeNode) node).element);
        }

        @Override
        public TreeNode getParent() {
            return getNode(element.getParentElement());
        }

        @Override
        public boolean isLeaf() {
            return element.isLeaf();
        }
        
        
    }
}