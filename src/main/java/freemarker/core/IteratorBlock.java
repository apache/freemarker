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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateHashModelEx2.KeyValuePair;
import freemarker.template.TemplateHashModelEx2.KeyValuePairIterator;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * A #list (or #foreach) element, or pre-#else section of it inside a {@link ListElseContainer}.
 */
final class IteratorBlock extends TemplateElement {

    private final Expression listedExp;
    private final String loopVarName;
    private final String loopVar2Name;
    private final boolean hashListing;
    private final boolean forEach;

    /**
     * @param listedExp
     *            a variable referring to a sequence or collection or extended hash to list
     * @param loopVarName
     *            The name of the variable that will hold the value of the current item when looping through listed value,
     *            or {@code null} if we have a nested {@code #items}. If this is a hash listing then this variable will holds the value
     *            of the hash key.
     * @param loopVar2Name
     *            The name of the variable that will hold the value of the current item when looping through the list,
     *            or {@code null} if we have a nested {@code #items}. If this is a hash listing then it variable will hold the value
     *            from the key-value pair.
     * @param childrenBeforeElse
     *            The nested content to execute if the listed value wasn't empty; can't be {@code null}. If the loop variable
     *            was specified in the start tag, this is also what we will iterate over.
     * @param hashListing
     *            Whether this is a key-value pair listing, or a usual listing. This is properly set even if we have
     *            a nested {@code #items}.
     * @param forEach
     *            Whether this is {@code #foreach} or a {@code #list}.
     */
    IteratorBlock(Expression listedExp,
                  String loopVarName,
                  String loopVar2Name,
                  TemplateElements childrenBeforeElse,
                  boolean hashListing,
                  boolean forEach) {
        this.listedExp = listedExp;
        this.loopVarName = loopVarName;
        this.loopVar2Name = loopVar2Name;
        setChildren(childrenBeforeElse);
        this.hashListing = hashListing;
        this.forEach = forEach;
    }
    
    boolean isHashListing() {
        return hashListing;
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        acceptWithResult(env);
        return null;
    }
    
    boolean acceptWithResult(Environment env) throws TemplateException, IOException {
        TemplateModel listedValue = listedExp.eval(env);
        if (listedValue == null) {
            listedExp.assertNonNull(null, env);
        }

        return env.visitIteratorBlock(new IterationContext(listedValue, loopVarName, loopVar2Name));
    }

    /**
     * @param loopVariableName
     *            Then name of the loop variable whose context we are looking for, or {@code null} if we simply look for
     *            the innermost context.
     * @return The matching context or {@code null} if no such context exists.
     */
    static IterationContext findEnclosingIterationContext(Environment env, String loopVariableName)
            throws _MiscTemplateException {
        LocalContextStack ctxStack = env.getLocalContextStack();
        if (ctxStack != null) {
            for (int i = ctxStack.size() - 1; i >= 0; i--) {
                Object ctx = ctxStack.get(i);
                if (ctx instanceof IterationContext
                        && (loopVariableName == null
                            || loopVariableName.equals(((IterationContext) ctx).getLoopVariableName())
                            || loopVariableName.equals(((IterationContext) ctx).getLoopVariable2Name())
                            )) {
                    return (IterationContext) ctx;
                }
            }
        }
        return null;
    }
    
    @Override
    protected String dump(boolean canonical) {
        StringBuilder buf = new StringBuilder();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        if (forEach) {
            buf.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVarName));
            buf.append(" in ");
            buf.append(listedExp.getCanonicalForm());
        } else {
            buf.append(listedExp.getCanonicalForm());
            if (loopVarName != null) {
                buf.append(" as ");
                buf.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVarName));
                if (loopVar2Name != null) {
                    buf.append(", ");
                    buf.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVar2Name));
                }
            }
        }
        if (canonical) {
            buf.append(">");
            buf.append(getChildrenCanonicalForm());
            if (!(getParentElement() instanceof ListElseContainer)) {
                buf.append("</");
                buf.append(getNodeTypeSymbol());
                buf.append('>');
            }
        }
        return buf.toString();
    }
    
    @Override
    int getParameterCount() {
        return 1 + (loopVarName != null ? 1 : 0) + (loopVar2Name != null ? 1 : 0);
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0:
            return listedExp;
        case 1:
            if (loopVarName == null) throw new IndexOutOfBoundsException();
            return loopVarName;
        case 2:
            if (loopVar2Name == null) throw new IndexOutOfBoundsException();
            return loopVar2Name;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0:
            return ParameterRole.LIST_SOURCE;
        case 1:
            if (loopVarName == null) throw new IndexOutOfBoundsException();
            return ParameterRole.TARGET_LOOP_VARIABLE;
        case 2:
            if (loopVar2Name == null) throw new IndexOutOfBoundsException();
            return ParameterRole.TARGET_LOOP_VARIABLE;
        default: throw new IndexOutOfBoundsException();
        }
    }    
    
    @Override
    String getNodeTypeSymbol() {
        return forEach ? "#foreach" : "#list";
    }

    @Override
    boolean isNestedBlockRepeater() {
        return loopVarName != null;
    }

    /**
     * Holds the context of a #list (or #forEach) directive.
     */
    class IterationContext implements LocalContext {
        
        private static final String LOOP_STATE_HAS_NEXT = "_has_next"; // lenght: 9
        private static final String LOOP_STATE_INDEX = "_index"; // length 6
        
        private Object openedIterator;
        private boolean hasNext;
        private TemplateModel loopVar;
        private TemplateModel loopVar2;
        private int index;
        private boolean alreadyEntered;
        private Collection localVarNames = null;
        
        /** If the {@code #list} has nested {@code #items}, it's {@code null} outside the {@code #items}. */
        private String loopVarName;
        /** Used if we list key-value pairs */
        private String loopVar2Name;
        
        private final TemplateModel listedValue;
        
        public IterationContext(TemplateModel listedValue, String loopVarName, String loopVar2Name) {
            this.listedValue = listedValue;
            this.loopVarName = loopVarName;
            this.loopVar2Name = loopVar2Name;
        }
        
        boolean accept(Environment env) throws TemplateException, IOException {
            return executeNestedContent(env, getChildBuffer());
        }

        void loopForItemsElement(Environment env, TemplateElement[] childBuffer, String loopVarName, String loopVar2Name)
                    throws NonSequenceOrCollectionException, TemplateModelException, InvalidReferenceException,
                    TemplateException, IOException {
            try {
                if (alreadyEntered) {
                    throw new _MiscTemplateException(env,
                            "The #items directive was already entered earlier for this listing.");
                }
                alreadyEntered = true;
                this.loopVarName = loopVarName;
                this.loopVar2Name = loopVar2Name;
                executeNestedContent(env, childBuffer);
            } finally {
                this.loopVarName = null;
                this.loopVar2Name = null;
            }
        }

        /**
         * Executes the given block for the {@link #listedValue}: if {@link #loopVarName} is non-{@code null}, then for
         * each list item once, otherwise once if {@link #listedValue} isn't empty.
         */
        private boolean executeNestedContent(Environment env, TemplateElement[] childBuffer)
                throws TemplateModelException, TemplateException, IOException, NonSequenceOrCollectionException,
                InvalidReferenceException {
            return !hashListing
                    ? executedNestedContentForCollOrSeqListing(env, childBuffer)
                    : executedNestedContentForHashListing(env, childBuffer);
        }

        private boolean executedNestedContentForCollOrSeqListing(Environment env, TemplateElement[] childBuffer)
                throws TemplateModelException, IOException, TemplateException,
                NonSequenceOrCollectionException, InvalidReferenceException {
            final boolean listNotEmpty;
            if (listedValue instanceof TemplateCollectionModel) {
                final TemplateCollectionModel collModel = (TemplateCollectionModel) listedValue;
                final TemplateModelIterator iterModel
                        = openedIterator == null ? collModel.iterator()
                                : ((TemplateModelIterator) openedIterator);
                listNotEmpty = iterModel.hasNext();
                if (listNotEmpty) {
                    if (loopVarName != null) {
                        try {
                            do {
                                loopVar = iterModel.next();
                                hasNext = iterModel.hasNext();
                                env.visit(childBuffer);
                                index++;
                            } while (hasNext);
                        } catch (BreakInstruction.Break br) {
                            // Silently exit loop
                        }
                        openedIterator = null;
                    } else {
                        // We must reuse this later, because TemplateCollectionModel-s that wrap an Iterator only
                        // allow one iterator() call.
                        openedIterator = iterModel;
                        env.visit(childBuffer);
                    }
                }
            } else if (listedValue instanceof TemplateSequenceModel) {
                final TemplateSequenceModel seqModel = (TemplateSequenceModel) listedValue;
                final int size = seqModel.size();
                listNotEmpty = size != 0;
                if (listNotEmpty) {
                    if (loopVarName != null) {
                        try {
                            for (index = 0; index < size; index++) {
                                loopVar = seqModel.get(index);
                                hasNext = (size > index + 1);
                                env.visit(childBuffer);
                            }
                        } catch (BreakInstruction.Break br) {
                            // Silently exit loop
                        }
                    } else {
                        env.visit(childBuffer);
                    }
                }
            } else if (listedValue instanceof TemplateHashModelEx
                    && !NonSequenceOrCollectionException.isWrappedIterable(listedValue)) {
                throw new NonSequenceOrCollectionException(env,
                        new _ErrorDescriptionBuilder("The value you try to list is ",
                                new _DelayedAOrAn(new _DelayedFTLTypeDescription(listedValue)),
                                ", thus you must specify two loop variables after the \"as\"; one for the key, and "
                                + "another for the value, like ", "<#... as k, v>", ")."
                                ));
            } else {
                throw new NonSequenceOrCollectionException(
                        listedExp, listedValue, env);
            }
            return listNotEmpty;
        }

        private boolean executedNestedContentForHashListing(Environment env, TemplateElement[] childBuffer)
                throws TemplateModelException, IOException, TemplateException {
            final boolean hashNotEmpty;
            if (listedValue instanceof TemplateHashModelEx) {
                TemplateHashModelEx listedHash = (TemplateHashModelEx) listedValue; 
                if (listedHash instanceof TemplateHashModelEx2) {
                    KeyValuePairIterator kvpIter
                            = openedIterator == null ? ((TemplateHashModelEx2) listedHash).keyValuePairIterator()
                                    : (KeyValuePairIterator) openedIterator;
                    hashNotEmpty = kvpIter.hasNext();
                    if (hashNotEmpty) {
                        if (loopVarName != null) {
                            try {
                                do {
                                    KeyValuePair kvp = kvpIter.next();
                                    loopVar = kvp.getKey();
                                    loopVar2 = kvp.getValue();
                                    hasNext = kvpIter.hasNext();
                                    env.visit(childBuffer);
                                    index++;
                                } while (hasNext);
                            } catch (BreakInstruction.Break br) {
                                // Silently exit loop
                            }
                            openedIterator = null;
                        } else {
                            // We will reuse this at the #iterms
                            openedIterator = kvpIter;
                            env.visit(childBuffer);
                        }
                    }
                } else { //  not a TemplateHashModelEx2, but still a TemplateHashModelEx
                    TemplateModelIterator keysIter = listedHash.keys().iterator();
                    hashNotEmpty = keysIter.hasNext();
                    if (hashNotEmpty) {
                        if (loopVarName != null) {
                            try {
                                do {
                                    loopVar = keysIter.next();
                                    if (!(loopVar instanceof TemplateScalarModel)) {
                                        throw new NonStringException(env,
                                                new _ErrorDescriptionBuilder(
                                                        "When listing key-value pairs of traditional hash "
                                                        + "implementations, all keys must be strings, but one of them "
                                                        + "was ",
                                                        new _DelayedAOrAn(new _DelayedFTLTypeDescription(loopVar)), "."
                                                        ).tip("The listed value's TemplateModel class was ",
                                                                new _DelayedShortClassName(listedValue.getClass()),
                                                                ", which doesn't implement ",
                                                                new _DelayedShortClassName(TemplateHashModelEx2.class),
                                                                ", which leads to this restriction."));
                                    }
                                    loopVar2 = listedHash.get(((TemplateScalarModel) loopVar).getAsString());
                                    hasNext = keysIter.hasNext();
                                    env.visit(childBuffer);
                                    index++;
                                } while (hasNext);
                            } catch (BreakInstruction.Break br) {
                                // Silently exit loop
                            }
                        } else {
                            env.visit(childBuffer);
                        }
                    }
                }
            } else if (listedValue instanceof TemplateCollectionModel
                    || listedValue instanceof TemplateSequenceModel) {
                throw new NonSequenceOrCollectionException(env,
                        new _ErrorDescriptionBuilder("The value you try to list is ",
                                new _DelayedAOrAn(new _DelayedFTLTypeDescription(listedValue)),
                                ", thus you must specify only one loop variable after the \"as\" (there's no separate "
                                + "key and value)."
                                ));
            } else {
                throw new NonExtendedHashException(
                        listedExp, listedValue, env);
            }
            return hashNotEmpty;
        }

        String getLoopVariableName() {
            return this.loopVarName;
        }

        String getLoopVariable2Name() {
            return this.loopVar2Name;
        }
        
        @Override
        public TemplateModel getLocalVariable(String name) {
            String loopVariableName = this.loopVarName;
            if (loopVariableName != null && name.startsWith(loopVariableName)) {
                switch(name.length() - loopVariableName.length()) {
                    case 0: 
                        return loopVar;
                    case 6: 
                        if (name.endsWith(LOOP_STATE_INDEX)) {
                            return new SimpleNumber(index);
                        }
                        break;
                    case 9: 
                        if (name.endsWith(LOOP_STATE_HAS_NEXT)) {
                            return hasNext ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
                        }
                        break;
                }
            }
            
            if (name.equals(loopVar2Name)) {
                return loopVar2;
            }
            
            return null;
        }
        
        @Override
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
