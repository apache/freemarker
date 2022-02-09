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

import java.util.List;

import freemarker.ext.dom._ExtDomApi;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateNodeModelEx;
import freemarker.template._ObjectWrappers;

        /**
 * A holder for builtins that operate exclusively on (XML-)node left-hand value.
 */
class BuiltInsForNodes {
    
    static class ancestorsBI extends BuiltInForNode {
        @Override
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
    
    static class childrenBI extends BuiltInForNode {
        @Override
        TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            return nodeModel.getChildNodes();
        }
    }
    
    static class node_nameBI extends BuiltInForNode {
        @Override
        TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            return new SimpleScalar(nodeModel.getNodeName());
        }
    }

    static class node_namespaceBI extends BuiltInForNode {
        @Override
        TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            String nsURI = nodeModel.getNodeNamespace();
            return nsURI == null ? null : new SimpleScalar(nsURI);
        }
    }
    
    static class node_typeBI extends BuiltInForNode {
        @Override
        TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            return new SimpleScalar(nodeModel.getNodeType());
        }
    }

    static class parentBI extends BuiltInForNode {
       @Override
        TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateModelException {
            return nodeModel.getParentNode();
        }
    }
    
    static class rootBI extends BuiltInForNode {
        @Override
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

    static class previousSiblingBI extends BuiltInForNodeEx {
        @Override
        TemplateModel calculateResult(TemplateNodeModelEx nodeModel, Environment env) throws TemplateModelException {
            return nodeModel.getPreviousSibling();
        }
    }

    static class nextSiblingBI extends BuiltInForNodeEx {
        @Override
        TemplateModel calculateResult(TemplateNodeModelEx nodeModel, Environment env) throws TemplateModelException {
            return nodeModel.getNextSibling();
        }
    }
    
    // Can't be instantiated
    private BuiltInsForNodes() { }

    static class AncestorSequence extends SimpleSequence implements TemplateMethodModel {
        
        @SuppressFBWarnings(value="SE_BAD_FIELD",
                justification="Can't make this Serializable, and not extending SimpleSequence would be non-BC.")
        private Environment env;
        
        AncestorSequence(Environment env) {
            super(_ObjectWrappers.SAFE_OBJECT_WRAPPER);
            this.env = env;
        }
        
        @Override
        public Object exec(List names) throws TemplateModelException {
            if (names == null || names.isEmpty()) {
                return this;
            }
            AncestorSequence result = new AncestorSequence(env);
            for (int i = 0; i < size(); i++) {
                TemplateNodeModel tnm = (TemplateNodeModel) get(i);
                String nodeName = tnm.getNodeName();
                String nsURI = tnm.getNodeNamespace();
                if (nsURI == null) {
                    if (names.contains(nodeName)) {
                        result.add(tnm);
                    }
                } else {
                    for (int j = 0; j < names.size(); j++) {
                        if (_ExtDomApi.matchesName((String) names.get(j), nodeName, nsURI, env)) {
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