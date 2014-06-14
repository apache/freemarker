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

import org.w3c.dom.DocumentType;
import org.w3c.dom.ProcessingInstruction;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

class DocumentTypeModel extends NodeModel {
    
    public DocumentTypeModel(DocumentType docType) {
        super(docType);
    }
    
    public String getAsString() {
        return ((ProcessingInstruction) node).getData();
    }
    
    public TemplateSequenceModel getChildren() throws TemplateModelException {
        throw new TemplateModelException("entering the child nodes of a DTD node is not currently supported");
    }
    
    public TemplateModel get(String key) throws TemplateModelException {
        throw new TemplateModelException("accessing properties of a DTD is not currently supported");
    }
    
    public String getNodeName() {
        return "@document_type$" + ((DocumentType) node).getNodeName();
    }
    
    public boolean isEmpty() {
        return true;
    }
}