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
                throw new UnexpectedTypeException(
                        listSource, list, "collection or sequence", env);
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
