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
 
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import freemarker.core.Environment;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * A class that wraps the root node of a parsed XML document, using
 * the W3C DOM API.
 */

class DocumentModel extends NodeModel implements TemplateHashModel {
    
    private ElementModel rootElement;
    
    DocumentModel(Document doc) {
        super(doc);
    }
    
    @Override
    public String getNodeName() {
        return "@document";
    }
    
    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("*")) {
            return getRootElement();
        } else if (key.equals("**")) {
            NodeList nl = ((Document) node).getElementsByTagName("*");
            return new NodeListModel(nl, this);
        } else if (DomStringUtil.isXMLNameLike(key)) {
            ElementModel em = (ElementModel) NodeModel.wrap(((Document) node).getDocumentElement());
            if (em.matchesName(key, Environment.getCurrentEnvironment())) {
                return em;
            } else {
                return new NodeListModel(this);
            }
        }
        return super.get(key);
    }
    
    ElementModel getRootElement() {
        if (rootElement == null) {
            rootElement = (ElementModel) wrap(((Document) node).getDocumentElement());
        }
        return rootElement;
    }
    
    @Override
    public boolean isEmpty() {
        return false;
    }
} 