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
 * 
 * @author Attila Szegedi
 */
public class FreeMarkerTree extends JTree {
    private static final long serialVersionUID = 1L;

    private final Map nodeMap = new HashMap();
    
    public FreeMarkerTree(Template template) {
        setTemplate(template);
    }

    private TreeNode getNode(TemplateElement element) {
        TreeNode n = (TreeNode)nodeMap.get(element);
        if(n != null) {
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

    public String convertValueToText(Object value, boolean selected,
                                     boolean expanded, boolean leaf, int row,
                                     boolean hasFocus) 
    {
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

        public Enumeration children() {
            final Enumeration e = element.children();
            return new Enumeration() {
                public boolean hasMoreElements() {
                    return e.hasMoreElements();
                }
                public Object nextElement() {
                    return getNode((TemplateElement)e.nextElement());
                }
            };
        }

        public boolean getAllowsChildren() {
            return element.getAllowsChildren();
        }

        public TreeNode getChildAt(int childIndex) {
            return getNode(element.getChildAt(childIndex));
        }

        public int getChildCount() {
            return element.getChildCount();
        }

        public int getIndex(TreeNode node) {
            return element.getIndex(((TemplateElementTreeNode)node).element);
        }

        public TreeNode getParent() {
            return getNode(element.getParent());
        }

        public boolean isLeaf() {
            return element.isLeaf();
        }
        
        
    }
}