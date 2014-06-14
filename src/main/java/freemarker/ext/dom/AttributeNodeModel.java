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

import org.w3c.dom.Attr;

import freemarker.core.Environment;
import freemarker.template.TemplateScalarModel;

class AttributeNodeModel extends NodeModel implements TemplateScalarModel {
    
    public AttributeNodeModel(Attr att) {
        super(att);
    }
    
    public String getAsString() {
        return ((Attr) node).getValue();
    }
    
    public String getNodeName() {
        String result = node.getLocalName();
        if (result == null || result.equals("")) {
            result = node.getNodeName();
        }
        return result;
    }
    
    public boolean isEmpty() {
        return true;
    }
    
    String getQualifiedName() {
        String nsURI = node.getNamespaceURI();
        if (nsURI == null || nsURI.equals(""))
            return node.getNodeName();
        Environment env = Environment.getCurrentEnvironment();
        String defaultNS = env.getDefaultNS();
        String prefix = null;
        if (nsURI.equals(defaultNS)) {
            prefix = "D";
        } else {
            prefix = env.getPrefixForNamespace(nsURI);
        }
        if (prefix == null) {
            return null;
        }
        return prefix + ":" + node.getLocalName();
    }
}