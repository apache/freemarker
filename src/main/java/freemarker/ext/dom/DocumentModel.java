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
 
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import freemarker.core.Environment;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.StringUtil;

/**
 * A class that wraps the root node of a parsed XML document, using
 * the W3C DOM API.
 */

class DocumentModel extends NodeModel implements TemplateHashModel {
    
    private ElementModel rootElement;
    
    DocumentModel(Document doc) {
        super(doc);
    }
    
    public String getNodeName() {
        return "@document";
    }
    
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("*")) {
            return getRootElement();
        }
        else if (key.equals("**")) {
            NodeList nl = ((Document)node).getElementsByTagName("*");
            return new NodeListModel(nl, this);
        }
        else if (StringUtil.isXMLID(key)) {
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
    
    public boolean isEmpty() {
        return false;
    }
} 