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
 
package org.apache.freemarker.dom;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.w3c.dom.Attr;

class AttributeNodeModel extends NodeModel implements TemplateStringModel {
    
    public AttributeNodeModel(Attr att) {
        super(att);
    }
    
    @Override
    public String getAsString() {
        return ((Attr) node).getValue();
    }
    
    @Override
    public String getNodeName() {
        String result = node.getLocalName();
        if (result == null || result.equals("")) {
            result = node.getNodeName();
        }
        return result;
    }
    
    @Override
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