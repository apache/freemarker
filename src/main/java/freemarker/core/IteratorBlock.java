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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * A #list or #foreach element.
 */
final class IteratorBlock extends TemplateElement {

    private Expression listSource;
    private String loopVariableName;
    private boolean isForEach;

    /**
     * @param listExpression a variable referring to a sequence or collection
     * @param indexName an arbitrary index variable name
     * @param nestedBlock the nestedBlock to iterate over
     */
    IteratorBlock(Expression listExpression,
                  String indexName,
                  TemplateElement nestedBlock,
                  boolean isForEach) 
    {
        this.listSource = listExpression;
        this.loopVariableName = indexName;
        this.isForEach = isForEach;
        this.nestedBlock = nestedBlock;
    }

    void accept(Environment env) throws TemplateException, IOException 
    {
        TemplateModel baseModel = listSource.eval(env);
        if (baseModel == null) {
            if (env.isClassicCompatible()) {
                // Classic behavior of simply ignoring null references.
                return;
            }
            listSource.assertNonNull(baseModel, env);
        }

        env.visitIteratorBlock(new Context(baseModel));
    }

    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        if (isForEach) {
            buf.append(loopVariableName);
            buf.append(" in ");
            buf.append(listSource.getCanonicalForm());
        }
        else {
            buf.append(listSource.getCanonicalForm());
            buf.append(" as ");
            buf.append(loopVariableName);
        }
        if (canonical) {
            buf.append(">");
            if (nestedBlock != null) {
                buf.append(nestedBlock.getCanonicalForm());
            }
            buf.append("</");
            buf.append(getNodeTypeSymbol());
            buf.append('>');
        }
        return buf.toString();
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return listSource;
        case 1: return loopVariableName;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.LIST_SOURCE;
        case 1: return ParameterRole.TARGET_LOOP_VARIABLE;
        default: throw new IndexOutOfBoundsException();
        }
    }    
    
    String getNodeTypeSymbol() {
        return isForEach ? "#foreach" : "#list";
    }

    /**
     * A helper class that holds the context of the loop.
     */
    class Context implements LocalContext {
        private boolean hasNext;
        private TemplateModel loopVar;
        private int index;
        private Collection variableNames = null;
        private TemplateModel list;
        
        Context(TemplateModel list) {
            this.list = list;
        }
        
        
        void runLoop(Environment env) throws TemplateException, IOException {
            if (list instanceof TemplateCollectionModel) {
                TemplateCollectionModel baseListModel = (TemplateCollectionModel)list;
                TemplateModelIterator it = baseListModel.iterator();
                hasNext = it.hasNext();
                while (hasNext) {
                    loopVar = it.next();
                    hasNext = it.hasNext();
                    if (nestedBlock != null) {
                        env.visit(nestedBlock);
                    }
                    index++;
                }
            }
            else if (list instanceof TemplateSequenceModel) {
                TemplateSequenceModel tsm = (TemplateSequenceModel) list;
                int size = tsm.size();
                for (index =0; index <size; index++) {
                    loopVar = tsm.get(index);
                    hasNext = (size > index +1);
                    if (nestedBlock != null) {
                        env.visitByHiddingParent(nestedBlock);
                    }
                }
            }
            else if (env.isClassicCompatible()) {
                loopVar = list;
                if (nestedBlock != null) {
                    env.visitByHiddingParent(nestedBlock);
                }
            }
            else {
                throw new NonSequenceOrCollectionException(
                        listSource, list, env);
            }
        }

        public TemplateModel getLocalVariable(String name) {
            if (name.startsWith(loopVariableName)) {
                switch(name.length() - loopVariableName.length()) {
                    case 0: 
                        return loopVar;
                    case 6: 
                        if(name.endsWith("_index")) {
                            return new SimpleNumber(index);
                        }
                        break;
                    case 9: 
                        if(name.endsWith("_has_next")) {
                            return hasNext ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
                        }
                        break;
                }
            }
            return null;
        }
        
        public Collection getLocalVariableNames() {
            if(variableNames == null) {
                variableNames = new ArrayList(3);
                variableNames.add(loopVariableName);
                variableNames.add(loopVariableName + "_index");
                variableNames.add(loopVariableName + "_has_next");
            }
            return variableNames;
        }
    }
}
