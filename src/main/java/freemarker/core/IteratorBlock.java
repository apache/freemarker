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

    private Expression listExpression;
    private String loopVariableName;
    private boolean isForEach;

    /**
     * @param listExpression a variable referring to a sequence or collection
     * @param loopVariableName an arbitrary index variable name
     * @param nestedBlock the nestedBlock to iterate over
     */
    IteratorBlock(Expression listExpression,
                  String loopVariableName,
                  TemplateElement nestedBlock,
                  boolean isForEach) 
    {
        this.listExpression = listExpression;
        this.loopVariableName = loopVariableName;
        this.nestedBlock = nestedBlock;
        this.isForEach = isForEach;
    }

    void accept(Environment env) throws TemplateException, IOException 
    {
        TemplateModel listValue = listExpression.eval(env);
        if (listValue == null) {
            if (env.isClassicCompatible()) {
                // Classic behavior of simply ignoring null references.
                return;
            }
            listExpression.assertNonNull(null, env);
        }

        env.visitIteratorBlock(new Context(listValue));
    }

    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        if (isForEach) {
            buf.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVariableName));
            buf.append(" in ");
            buf.append(listExpression.getCanonicalForm());
        }
        else {
            buf.append(listExpression.getCanonicalForm());
            buf.append(" as ");
            buf.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVariableName));
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
        case 0: return listExpression;
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
        
        private static final String LOOP_STATE_HAS_NEXT = "_has_next"; // lenght: 9
        private static final String LOOP_STATE_INDEX = "_index"; // length 6 
        
        private boolean hasNext;
        private TemplateModel loopVar;
        private int index;
        private Collection variableNames = null;
        private TemplateModel listValue;
        
        Context(TemplateModel listValue) {
            this.listValue = listValue;
        }
        
        void runLoop(Environment env) throws TemplateException, IOException {
            if (listValue instanceof TemplateCollectionModel) {
                final TemplateCollectionModel tcm = (TemplateCollectionModel)listValue;
                final TemplateModelIterator it = tcm.iterator();
                hasNext = it.hasNext();
                while (hasNext) {
                    loopVar = it.next();
                    hasNext = it.hasNext();
                    if (nestedBlock != null) {
                        env.visitByHiddingParent(nestedBlock);  // TODO why not visitByHiddingParent?
                    }
                    index++;
                }
            } else if (listValue instanceof TemplateSequenceModel) {
                final TemplateSequenceModel tsm = (TemplateSequenceModel) listValue;
                final int size = tsm.size();
                for (index = 0; index < size; index++) {
                    loopVar = tsm.get(index);
                    hasNext = (size > index + 1);
                    if (nestedBlock != null) {
                        env.visitByHiddingParent(nestedBlock);
                    }
                }
            } else if (env.isClassicCompatible()) {
                loopVar = listValue;
                if (nestedBlock != null) {
                    env.visitByHiddingParent(nestedBlock);
                }
            } else {
                throw new NonSequenceOrCollectionException(
                        listExpression, listValue, env);
            }
        }

        public TemplateModel getLocalVariable(String name) {
            if (name.startsWith(loopVariableName)) {
                switch(name.length() - loopVariableName.length()) {
                    case 0: 
                        return loopVar;
                    case 6: 
                        if(name.endsWith(LOOP_STATE_INDEX)) {
                            return new SimpleNumber(index);
                        }
                        break;
                    case 9: 
                        if(name.endsWith(LOOP_STATE_HAS_NEXT)) {
                            return hasNext ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
                        }
                        break;
                }
            }
            return null;
        }
        
        public Collection getLocalVariableNames() {
            if (variableNames == null) {
                variableNames = new ArrayList(3);
                variableNames.add(loopVariableName);
                variableNames.add(loopVariableName + LOOP_STATE_INDEX);
                variableNames.add(loopVariableName + LOOP_STATE_HAS_NEXT);
            }
            return variableNames;
        }
    }

    boolean isNestedBlockRepeater() {
        return true;
    }
    
}
