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

package freemarker.core;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import freemarker.template.Template;

/**
 * Allows exposure of a FreeMarker template's AST as a Swing tree.
 * 
 * @deprecated Will be removed, as Swing classes aren't accessible on Google App Engine.
 */
public class FreeMarkerTree extends JTree {

    public FreeMarkerTree(Template template) {
        super(template.getRootTreeNode());
    }

    public void setTemplate(Template template) {
        this.setModel(new DefaultTreeModel(template.getRootTreeNode()));
        this.invalidate();
    }

    public String convertValueToText(Object value, boolean selected,
                                     boolean expanded, boolean leaf, int row,
                                     boolean hasFocus) 
    {
        if (value instanceof TemplateElement) {
            return ((TemplateElement) value).getDescription();
        }
        return value.toString();
    }
    
}
