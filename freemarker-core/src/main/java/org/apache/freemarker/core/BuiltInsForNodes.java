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

package org.apache.freemarker.core;

import static org.apache.freemarker.core.util.CallableUtils.*;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.TemplateNodeModelEx;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.util._StringUtils;

/**
 * A holder for builtins that operate exclusively on (XML-)node left-hand value.
 */
class BuiltInsForNodes {
    
    static class ancestorsBI extends BuiltInForNode {
       @Override
    TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateException {
           AncestorSequence result = new AncestorSequence(env);
           TemplateNodeModel parent = nodeModel.getParentNode();
           while (parent != null) {
               result.add(parent);
               parent = parent.getParentNode();
           }
           return result;
       }

        class AncestorSequence extends NativeSequence implements TemplateFunctionModel, BuiltInCallable {

            private static final int INITIAL_CAPACITY = 12;

            private Environment env;

            AncestorSequence(Environment env) {
                super(INITIAL_CAPACITY);
                this.env = env;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                if (args.length == 0) {
                    return this;
                }
                AncestorSequence result = new AncestorSequence(env);
                int size = getCollectionSize();
                for (int seqIdx = 0; seqIdx < size; seqIdx++) {
                    TemplateNodeModel tnm = (TemplateNodeModel) get(seqIdx);
                    String nodeName = tnm.getNodeName();
                    String nsURI = tnm.getNodeNamespace();
                    if (nsURI == null) {
                        for (int argIdx = 0; argIdx < args.length; argIdx++) {
                            String name = getStringArgument(args, argIdx, this);
                            if (name.equals(nodeName)) {
                                result.add(tnm);
                                break;
                            }
                        }
                    } else {
                        for (int argIdx = 0; argIdx < args.length; argIdx++) {
                            if (_StringUtils.matchesQName(
                                    getStringArgument(args, argIdx, this), nodeName, nsURI, env)) {
                                result.add(tnm);
                                break;
                            }
                        }
                    }
                }
                return result;
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return null;
            }

            @Override
            public String getBuiltInName() {
                return key;
            }

            @Override
            public String getOriginName() {
                return ASTExpBuiltIn.getOriginName(this);
            }
        }
    }
    
    static class childrenBI extends BuiltInForNode {
       @Override
    TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateException {
            return nodeModel.getChildNodes();
       }
    }
    
    static class node_nameBI extends BuiltInForNode {
       @Override
    TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateException {
            return new SimpleString(nodeModel.getNodeName());
       }
    }
    
    
    static class node_namespaceBI extends BuiltInForNode {
        @Override
        TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateException {
            String nsURI = nodeModel.getNodeNamespace();
            return nsURI == null ? null : new SimpleString(nsURI);
        }
    }
    
    static class node_typeBI extends BuiltInForNode {
       @Override
    TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateException {
            return new SimpleString(nodeModel.getNodeType());
        }
    }

    static class parentBI extends BuiltInForNode {
       @Override
    TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateException {
            return nodeModel.getParentNode();
       }
    }
    
    static class rootBI extends BuiltInForNode {
       @Override
    TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env) throws TemplateException {
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
        TemplateModel calculateResult(TemplateNodeModelEx nodeModel, Environment env) throws TemplateException {
            return nodeModel.getPreviousSibling();
        }
    }

    static class nextSiblingBI extends  BuiltInForNodeEx {
        @Override
        TemplateModel calculateResult(TemplateNodeModelEx nodeModel, Environment env) throws TemplateException {
            return nodeModel.getNextSibling();
        }
    }
    
    // Can't be instantiated
    private BuiltInsForNodes() { }

}