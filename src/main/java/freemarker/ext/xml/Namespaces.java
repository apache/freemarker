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

package freemarker.ext.xml;
import java.util.HashMap;
import java.util.List;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/**
 */
class Namespaces
implements
    TemplateMethodModel,
    Cloneable
{
    private HashMap namespaces;
    private boolean shared;
        
    Namespaces() {
        namespaces = new HashMap();
        namespaces.put("", "");
        namespaces.put("xml", "http://www.w3.org/XML/1998/namespace");
        shared = false;
    }
        
    public Object clone() {
        try {
            Namespaces clone = (Namespaces)super.clone();
            clone.namespaces = (HashMap)namespaces.clone();
            clone.shared = false;
            return clone;
        }
        catch(CloneNotSupportedException e) {
            throw new Error(); // Cannot happen
        }
    }
    
    public String translateNamespacePrefixToUri(String prefix) {
        synchronized(namespaces) {
            return (String)namespaces.get(prefix);
        }   
    }
    
    public Object exec(List arguments) throws TemplateModelException {
        if (arguments.size() != 2) {
            throw new TemplateModelException("_registerNamespace(prefix, uri) requires two arguments");
        }
        registerNamespace((String)arguments.get(0), (String)arguments.get(1));
        return TemplateScalarModel.EMPTY_STRING;
    }
    
    void registerNamespace(String prefix, String uri) {
        synchronized(namespaces) {
            namespaces.put(prefix, uri);
        }   
    }
    
    void markShared() {
        if(!shared) {
            shared = true;
        }
    }
    
    boolean isShared() {
        return shared;
    }
}