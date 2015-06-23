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
import java.util.Collections;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.Constants;

/**
 * A #list or #foreach element.
 */
final class IteratorBlock extends TemplateElement {

    private final Expression listExp;
    private final String loopVarName;
    private final boolean isForEach;

    /**
     * @param listExp
     *            a variable referring to a sequence or collection ("the list" from now on)
     * @param loopVarName
     *            The name of the variable that will hold the value of the current item when looping through the list.
     * @param nestedBlock
     *            The nested content to execute if the list wasn't empty; can't be {@code null}. If the loop variable
     *            was specified in the start tag, this is also what we will iterator over.
     */
    IteratorBlock(Expression listExp,
                  String loopVarName,
                  TemplateElement nestedBlock,
                  boolean isForEach) 
    {
        this.listExp = listExp;
        this.loopVarName = loopVarName;
        setNestedBlock(nestedBlock);
        this.isForEach = isForEach;
    }

    void accept(Environment env) throws TemplateException, IOException {
        acceptWithResult(env);
    }
    
    boolean acceptWithResult(Environment env) throws TemplateException, IOException 
    {
        TemplateModel listValue = listExp.eval(env);
        if (listValue == null) {
            if (env.isClassicCompatible()) {
                listValue = Constants.EMPTY_SEQUENCE; 
            } else {
                listExp.assertNonNull(null, env);
            }
        }

        return env.visitIteratorBlock(new IterationContext(listValue, loopVarName));
    }

    /**
     * @param loopVariableName
     *            Then name of the loop variable whose context we are looking for, or {@code null} if we simply look for
     *            the innermost context.
     * @return The matching context or {@code null} if no such context exists.
     */
    static IterationContext findEnclosingIterationContext(Environment env, String loopVariableName)
            throws _MiscTemplateException {
        ArrayList ctxStack = env.getLocalContextStack();
        if (ctxStack != null) {
            for (int i = ctxStack.size() - 1; i >= 0; i--) {
                Object ctx = ctxStack.get(i);
                if (ctx instanceof IterationContext
                        && (loopVariableName == null
                            || loopVariableName.equals(((IterationContext) ctx).getLoopVariableName()))) {
                    return (IterationContext) ctx;
                }
            }
        }
        return null;
    }
    
    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        if (isForEach) {
            buf.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVarName));
            buf.append(" in ");
            buf.append(listExp.getCanonicalForm());
        }
        else {
            buf.append(listExp.getCanonicalForm());
            if (loopVarName != null) {
                buf.append(" as ");
                buf.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVarName));
            }
        }
        if (canonical) {
            buf.append(">");
            if (getNestedBlock() != null) {
                buf.append(getNestedBlock().getCanonicalForm());
            }
            if (!(getParentElement() instanceof ListElseContainer)) {
                buf.append("</");
                buf.append(getNodeTypeSymbol());
                buf.append('>');
            }
        }
        return buf.toString();
    }
    
    int getParameterCount() {
        return loopVarName != null ? 2 : 1;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0:
            return listExp;
        case 1:
            if (loopVarName == null) throw new IndexOutOfBoundsException();
            return loopVarName;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0:
            return ParameterRole.LIST_SOURCE;
        case 1:
            if (loopVarName == null) throw new IndexOutOfBoundsException();
            return ParameterRole.TARGET_LOOP_VARIABLE;
        default: throw new IndexOutOfBoundsException();
        }
    }    
    
    String getNodeTypeSymbol() {
        return isForEach ? "#foreach" : "#list";
    }

    boolean isNestedBlockRepeater() {
        return loopVarName != null;
    }

    /**
     * Holds the context of a #list (or #forEach) directive.
     */
    class IterationContext implements LocalContext {
        
        private static final String LOOP_STATE_HAS_NEXT = "_has_next"; // lenght: 9
        private static final String LOOP_STATE_INDEX = "_index"; // length 6
        
        private TemplateModelIterator openedIteratorModel;
        private boolean hasNext;
        private TemplateModel loopVar;
        private int index;
        private boolean alreadyEntered;
        private Collection localVarNames = null;
        
        /** If the {@code #list} has nested {@code #items}, it's {@code null} outside the {@code #items}. */
        private String loopVarName;
        
        private final TemplateModel listValue;
        
        public IterationContext(TemplateModel listValue, String loopVariableName) {
            this.listValue = listValue;
            this.loopVarName = loopVariableName;
        }
        
        boolean accept(Environment env) throws TemplateException, IOException {
            return executeNestedBlock(env, getNestedBlock());
        }

        void loopForItemsElement(Environment env, TemplateElement nestedBlock, String loopVarName)
                    throws NonSequenceOrCollectionException, TemplateModelException, InvalidReferenceException,
                    TemplateException, IOException {
            try {
                if (alreadyEntered) {
                    throw new _MiscTemplateException(env,
                            "The #items directive was already entered earlier for this listing.");
                }
                alreadyEntered = true;
                this.loopVarName = loopVarName;
                executeNestedBlock(env, nestedBlock);
            } finally {
                this.loopVarName = null;
            }
        }

        /**
         * Executes the given block for the {@link #listValue}: if {@link #loopVarName} is non-{@code null}, then for
         * each list item once, otherwise once if {@link #listValue} isn't empty.
         */
        private boolean executeNestedBlock(Environment env, TemplateElement nestedBlock)
                throws TemplateModelException, TemplateException, IOException,
                NonSequenceOrCollectionException, InvalidReferenceException {
            return executeNestedBlockInner(env, nestedBlock);
        }

        private boolean executeNestedBlockInner(Environment env, TemplateElement nestedBlock)
                throws TemplateModelException, TemplateException, IOException, NonSequenceOrCollectionException,
                InvalidReferenceException {
            final boolean listNotEmpty;
            if (listValue instanceof TemplateCollectionModel) {
                final TemplateCollectionModel collModel = (TemplateCollectionModel) listValue;
                final TemplateModelIterator iterModel
                        = openedIteratorModel == null ? collModel.iterator() : openedIteratorModel;
                hasNext = iterModel.hasNext();
                listNotEmpty = hasNext;
                if (listNotEmpty) {
                    if (loopVarName != null) {
                        try {
                            while (hasNext) {
                                loopVar = iterModel.next();
                                hasNext = iterModel.hasNext();
                                if (nestedBlock != null) {
                                    env.visitByHiddingParent(nestedBlock);
                                }
                                index++;
                            }
                        } catch (BreakInstruction.Break br) {
                            // Silently exit loop
                        }
                        openedIteratorModel = null;
                    } else {
                        // We must reuse this later, because TemplateCollectionModel-s that wrap an Iterator only
                        // allow one iterator() call.
                        openedIteratorModel = iterModel;
                        if (nestedBlock != null) {
                            env.visitByHiddingParent(nestedBlock);
                        }
                    }
                }
            } else if (listValue instanceof TemplateSequenceModel) {
                final TemplateSequenceModel seqModel = (TemplateSequenceModel) listValue;
                final int size = seqModel.size();
                listNotEmpty = size != 0;
                if (listNotEmpty) {
                    if (loopVarName != null) {
                        try {
                            for (index = 0; index < size; index++) {
                                loopVar = seqModel.get(index);
                                hasNext = (size > index + 1);
                                if (nestedBlock != null) {
                                    env.visitByHiddingParent(nestedBlock);
                                }
                            }
                        } catch (BreakInstruction.Break br) {
                            // Silently exit loop
                        }
                    } else {
                        if (nestedBlock != null) {
                            env.visitByHiddingParent(nestedBlock);
                        }
                    }
                }
            } else if (env.isClassicCompatible()) {
                listNotEmpty = true;
                if (loopVarName != null) {
                    loopVar = listValue;
                    hasNext = false;
                }
                try {
                    if (nestedBlock != null) {
                        env.visitByHiddingParent(nestedBlock);
                    }
                } catch (BreakInstruction.Break br) {
                    // Silently exit "loop"
                }
            } else {
                throw new NonSequenceOrCollectionException(
                        listExp, listValue, env);
            }
            
            return listNotEmpty;
        }

        String getLoopVariableName() {
            return this.loopVarName;
        }

        public TemplateModel getLocalVariable(String name) {
            String loopVariableName = this.loopVarName;
            if (loopVariableName != null && name.startsWith(loopVariableName)) {
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
            String loopVariableName = this.loopVarName;
            if (loopVariableName != null) {
                if (localVarNames == null) {
                    localVarNames = new ArrayList(3);
                    localVarNames.add(loopVariableName);
                    localVarNames.add(loopVariableName + LOOP_STATE_INDEX);
                    localVarNames.add(loopVariableName + LOOP_STATE_HAS_NEXT);
                }
                return localVarNames;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        boolean hasNext() {
            return hasNext;
        }
        
        int getIndex() {
            return index;
        }
        
    }
    
}
