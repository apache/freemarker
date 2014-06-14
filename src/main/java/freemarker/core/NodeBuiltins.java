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

import java.util.List;

import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.utility.StringUtil;

/**
 * A holder for builtins that operate exclusively on (XML-)node left-hand value.
 */
class NodeBuiltins {
    
    // Can't be instantiated
    private NodeBuiltins() { }
    
    private abstract static class NodeBuiltIn extends BuiltIn {
        TemplateModel _eval(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateNodeModel) {
                return calculateResult((TemplateNodeModel) model, env);
            } else {
                throw new UnexpectedTypeException(target, model, "node", env);
            }
        }
        abstract TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env)
                throws TemplateModelException;
    }

    static class ancestorsBI extends NodeBuiltIn {
       TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
           AncestorSequence result = new AncestorSequence(env);
           TemplateNodeModel parent = nodeModel.getParentNode();
           while (parent != null) {
               result.add(parent);
               parent = parent.getParentNode();
           }
           return result;
       }
    }
    
    static class childrenBI extends NodeBuiltIn {
       TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            return nodeModel.getChildNodes();
       }
    }
    
    
    static class node_nameBI extends NodeBuiltIn {
       TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            return new SimpleScalar(nodeModel.getNodeName());
       }
    }
    
    static class node_typeBI extends NodeBuiltIn {
       TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            return new SimpleScalar(nodeModel.getNodeType());
        }
    }

    static class parentBI extends NodeBuiltIn {
       TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            return nodeModel.getParentNode();
       }
    }
    
    static class rootBI extends NodeBuiltIn {
       TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            TemplateNodeModel result = nodeModel;
            TemplateNodeModel parent = nodeModel.getParentNode();
            while (parent != null) {
                result = parent;
                parent = result.getParentNode();
            }
            return result;
       }
    }
    
    static class node_namespaceBI extends NodeBuiltIn {
        TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            String nsURI = nodeModel.getNodeNamespace();
            return nsURI == null ? null : new SimpleScalar(nsURI);
        }
    }
    
    
    static class AncestorSequence extends SimpleSequence implements TemplateMethodModel {
        
        private Environment env;
        
        AncestorSequence(Environment env) {
            this.env = env;
        }
        
        public Object exec(List names) throws TemplateModelException {
            if (names == null || names.isEmpty()) {
                return this;
            }
            AncestorSequence result = new AncestorSequence(env);
            for (int i=0; i<size(); i++) {
                TemplateNodeModel tnm = (TemplateNodeModel) get(i);
                String nodeName = tnm.getNodeName();
                String nsURI = tnm.getNodeNamespace();
                if (nsURI == null) {
                    if (names.contains(nodeName)) {
                        result.add(tnm);
                    }
                } else {
                    for (int j = 0; j<names.size(); j++) {
                        if (StringUtil.matchesName((String) names.get(j), nodeName, nsURI, env)) {
                            result.add(tnm);
                            break;
                        }
                    }
                }
            }
            return result;
        }
    }    
}