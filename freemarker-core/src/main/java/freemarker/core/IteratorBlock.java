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
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.Constants;

/**
 * A #list (or #foreach) element, or pre-#else section of it inside a {@link ListElseContainer}.
 */
final class IteratorBlock extends TemplateElement {

    private final Expression listedExp;
    private final String loopVar1Name;
    private final String loopVar2Name;
    private final boolean hashListing;
    private final boolean forEach;

    /**
     * @param listedExp
     *            a variable referring to a sequence or collection or extended hash to list
     * @param loopVar1Name
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
                  String loopVar1Name,
                  String loopVar2Name,
                  TemplateElements childrenBeforeElse,
                  boolean hashListing,
                  boolean forEach) {
        this.listedExp = listedExp;
        this.loopVar1Name = loopVar1Name;
        this.loopVar2Name = loopVar2Name;
        setChildren(childrenBeforeElse);
        this.hashListing = hashListing;
        this.forEach = forEach;

        listedExp.enableLazilyGeneratedResult();
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
            if (env.isClassicCompatible()) {
                listedValue = Constants.EMPTY_SEQUENCE; 
            } else {
                listedExp.assertNonNull(null, env);
            }
        }

        return env.visitIteratorBlock(new IterationContext(listedValue, loopVar1Name, loopVar2Name));
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder buf = new StringBuilder();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        if (forEach) {
            buf.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVar1Name));
            buf.append(" in ");
            buf.append(listedExp.getCanonicalForm());
        } else {
            buf.append(listedExp.getCanonicalForm());
            if (loopVar1Name != null) {
                buf.append(" as ");
                buf.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVar1Name));
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
        return 1 + (loopVar1Name != null ? 1 : 0) + (loopVar2Name != null ? 1 : 0);
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0:
            return listedExp;
        case 1:
            if (loopVar1Name == null) throw new IndexOutOfBoundsException();
            return loopVar1Name;
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
            if (loopVar1Name == null) throw new IndexOutOfBoundsException();
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
        return loopVar1Name != null;
    }

    /**
     * Holds the context of a #list (or #forEach) directive.
     */
    class IterationContext implements LocalContext {
        
        private static final String LOOP_STATE_HAS_NEXT = "_has_next"; // lenght: 9
        private static final String LOOP_STATE_INDEX = "_index"; // length 6
        
        private Object openedIterator;
        private boolean hasNext;
        private TemplateModel loopVar1Value;
        private TemplateModel loopVar2Value;
        private int index;
        private boolean alreadyEntered;
        private Collection<String> localVarNames = null;
        
        /**
         * The name of the 1st loop variable.
         * If the {@code #list} has nested {@code #items}, it's {@code null} outside the {@code #items}.
         * Do not use this to resolve {@link LocalContext#getLocalVariable(String)} and such, as the loop variable might
         * be still out of scope in FTL when this is already filled; use {@link #visibleLoopVar1Name} for that
         * instead.
         */
        private String loopVar1Name;
        /**
         * The name of the 1st loop variable in the {@link LocalContext}. Either {@code null} or {@link #loopVar1Name}.
         * When {@code null}, none of the loop variables are in scope in FTL.
         * It would be more intuitive if the {@link LocalContext} is not in the local stack when they aren't visible,
         * but the {@link LocalContext} is also used for {@code #items} to find its parent, for which we need the tricky
         * scoping of the local context stack {@link Environment#getLocalContextStack()}.
         *
         * (It would be cleaner to have
         * {@code boolean loopVarsVisible} instead, but it's a trick to decrease runtime overhead added because of
         * lambdas. Certainly an unmeasurable difference... yet it just doesn't feel right when new features slows
         * down every existing template a tiny bit, so we try to mitigate that effect.)
         *
         * @since 2.3.29
         */
        private String visibleLoopVar1Name;
        /*
         * The name of the 2nd loop variable, only used if we list key-value pairs.
         * Do not use this to resolve {@link LocalContext#getLocalVariable} and such, when {@link
         * #localContextLoopVar1Name} is {@code null}, as then this is not yet in scope as FTL variable.
         */
        private String loopVar2Name;

        private final TemplateModel listedValue;
        
        public IterationContext(TemplateModel listedValue, String loopVar1Name, String loopVar2Name) {
            this.listedValue = listedValue;
            this.loopVar1Name = loopVar1Name;
            this.loopVar2Name = loopVar2Name;
        }
        
        boolean accept(Environment env) throws TemplateException, IOException {
            return executeNestedContent(env, getChildBuffer());
        }

        void loopForItemsElement(Environment env, TemplateElement[] childBuffer, String loopVarName, String loopVar2Name)
                    throws TemplateException, IOException {
            try {
                if (alreadyEntered) {
                    throw new _MiscTemplateException(env,
                            "The #items directive was already entered earlier for this listing.");
                }
                alreadyEntered = true;
                this.loopVar1Name = loopVarName;
                this.loopVar2Name = loopVar2Name;
                executeNestedContent(env, childBuffer);
            } finally {
                this.loopVar1Name = null;
                this.loopVar2Name = null;
            }
        }

        /**
         * Executes the given block for the {@link #listedValue}: if {@link #loopVar1Name} is non-{@code null}, then for
         * each list item once, otherwise once if {@link #listedValue} isn't empty.
         */
        private boolean executeNestedContent(Environment env, TemplateElement[] childBuffer)
                throws TemplateException, IOException  {
            return !hashListing
                    ? executedNestedContentForCollOrSeqListing(env, childBuffer)
                    : executedNestedContentForHashListing(env, childBuffer);
        }

        private boolean executedNestedContentForCollOrSeqListing(Environment env, TemplateElement[] childBuffer)
                throws IOException, TemplateException {
            final boolean listNotEmpty;
            if (listedValue instanceof TemplateCollectionModel) {
                final TemplateCollectionModel collModel = (TemplateCollectionModel) listedValue;
                final TemplateModelIterator iterModel
                        = openedIterator == null ? collModel.iterator()
                                : ((TemplateModelIterator) openedIterator);
                listNotEmpty = iterModel.hasNext();
                if (listNotEmpty) {
                    if (loopVar1Name != null) {
                        listLoop: do {
                            loopVar1Value = iterModel.next();
                            hasNext = iterModel.hasNext();
                            try {
                                visibleLoopVar1Name = loopVar1Name; // Makes all loop variables visible in FTL
                                env.visit(childBuffer);
                            } catch (BreakOrContinueException br) {
                                if (br == BreakOrContinueException.BREAK_INSTANCE) {
                                    break listLoop;
                                }
                            } finally {
                                visibleLoopVar1Name = null; // Hides all loop variables in FTL
                            }
                            index++;
                        } while (hasNext);
                        openedIterator = null;
                    } else {
                        // We must reuse this later, because TemplateCollectionModel-s that wrap an Iterator only
                        // allow one iterator() call. (Also those returned by ?filter, etc. with lazy result generation.)
                        openedIterator = iterModel;
                        // Note: Loop variables will only become visible inside #items
                        env.visit(childBuffer);
                    }
                }
            } else if (listedValue instanceof TemplateSequenceModel) {
                final TemplateSequenceModel seqModel = (TemplateSequenceModel) listedValue;
                final int size = seqModel.size();
                listNotEmpty = size != 0;
                if (listNotEmpty) {
                    if (loopVar1Name != null) {
                            listLoop: for (index = 0; index < size; index++) {
                                loopVar1Value = seqModel.get(index);
                                hasNext = (size > index + 1);
                                try {
                                    visibleLoopVar1Name = loopVar1Name; // Makes all loop variables visible in FTL
                                    env.visit(childBuffer);
                                } catch (BreakOrContinueException br) {
                                    if (br == BreakOrContinueException.BREAK_INSTANCE) {
                                        break listLoop;
                                    }
                                } finally {
                                    visibleLoopVar1Name = null; // Hides all loop variables in FTL
                                }
                            }
                    } else {
                        // Note: Loop variables will only become visible inside #items
                        env.visit(childBuffer);
                    }
                }
            } else if (env.isClassicCompatible()) {
                listNotEmpty = true;
                if (loopVar1Name != null) {
                    loopVar1Value = listedValue;
                    hasNext = false;
                }
                try {
                    visibleLoopVar1Name = loopVar1Name; // Makes all loop variables visible in FTL
                    env.visit(childBuffer);
                } catch (BreakOrContinueException br) {
                    // Silently exit "loop"
                } finally {
                    visibleLoopVar1Name = null; // Hides all loop variables in FTL
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
                throws IOException, TemplateException {
            final boolean hashNotEmpty;
            if (listedValue instanceof TemplateHashModelEx) {
                TemplateHashModelEx listedHash = (TemplateHashModelEx) listedValue; 
                if (listedHash instanceof TemplateHashModelEx2) {
                    KeyValuePairIterator kvpIter
                            = openedIterator == null ? ((TemplateHashModelEx2) listedHash).keyValuePairIterator()
                                    : (KeyValuePairIterator) openedIterator;
                    hashNotEmpty = kvpIter.hasNext();
                    if (hashNotEmpty) {
                        if (loopVar1Name != null) {
                            listLoop: do {
                                KeyValuePair kvp = kvpIter.next();
                                loopVar1Value = kvp.getKey();
                                loopVar2Value = kvp.getValue();
                                hasNext = kvpIter.hasNext();
                                try {
                                    visibleLoopVar1Name = loopVar1Name; // Makes all loop variables visible in FTL
                                    env.visit(childBuffer);
                                } catch (BreakOrContinueException br) {
                                    if (br == BreakOrContinueException.BREAK_INSTANCE) {
                                        break listLoop;
                                    }
                                } finally {
                                    visibleLoopVar1Name = null; // Hides all loop variables in FTL
                                }
                                index++;
                            } while (hasNext);
                            openedIterator = null;
                        } else {
                            // We will reuse this at #items
                            openedIterator = kvpIter;
                            // Note: Loop variables will only become visible inside #items
                            env.visit(childBuffer);
                        }
                    }
                } else { //  not a TemplateHashModelEx2, but still a TemplateHashModelEx
                    TemplateModelIterator keysIter = listedHash.keys().iterator();
                    hashNotEmpty = keysIter.hasNext();
                    if (hashNotEmpty) {
                        if (loopVar1Name != null) {
                            listLoop: do {
                                loopVar1Value = keysIter.next();
                                if (!(loopVar1Value instanceof TemplateScalarModel)) {
                                    throw _MessageUtil.newKeyValuePairListingNonStringKeyExceptionMessage(
                                            loopVar1Value, (TemplateHashModelEx) listedValue);
                                }
                                loopVar2Value = listedHash.get(((TemplateScalarModel) loopVar1Value).getAsString());
                                hasNext = keysIter.hasNext();
                                try {
                                    visibleLoopVar1Name = loopVar1Name; // Makes all loop variables visible in FTL
                                    env.visit(childBuffer);
                                } catch (BreakOrContinueException br) {
                                    if (br == BreakOrContinueException.BREAK_INSTANCE) {
                                        break listLoop;
                                    }
                                } finally {
                                    visibleLoopVar1Name = null; // Hides all loop variables in FTL
                                }
                                index++;
                            } while (hasNext);
                        } else {
                            // Note: Loop variables will only become visible inside #items
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

        boolean hasVisibleLoopVar(String visibleLoopVarName) {
            String visibleLoopVar1Name = this.visibleLoopVar1Name;
            if (visibleLoopVar1Name == null) {
                return false; // Loop vars aren't in scope in FTL
            }
            return visibleLoopVarName.equals(visibleLoopVar1Name) || visibleLoopVarName.equals(loopVar2Name);
        }

        @Override
        public TemplateModel getLocalVariable(String name) {
            String visibleLoopVar1Name = this.visibleLoopVar1Name; // Not this.loopVar1Name!
            if (visibleLoopVar1Name == null) {
                // Loop variables aren't yet in scope in FTL
                return null;
            }

            if (name.startsWith(visibleLoopVar1Name)) {
                switch(name.length() - visibleLoopVar1Name.length()) {
                    case 0:
                        return loopVar1Value != null ? loopVar1Value
                                : getTemplate().getConfiguration().getFallbackOnNullLoopVariable()
                                        ? null : TemplateNullModel.INSTANCE;
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
                return loopVar2Value != null ? loopVar2Value
                        : getTemplate().getConfiguration().getFallbackOnNullLoopVariable()
                                ? null : TemplateNullModel.INSTANCE;
            }
            
            return null;
        }
        
        @Override
        public Collection<String> getLocalVariableNames() {
            String visibleLoopVar1Name = this.visibleLoopVar1Name; // Not this.loopVar1Name!
            if (visibleLoopVar1Name != null) {
                if (localVarNames == null) {
                    localVarNames = new ArrayList(3);
                    localVarNames.add(visibleLoopVar1Name);
                    localVarNames.add(visibleLoopVar1Name + LOOP_STATE_INDEX);
                    localVarNames.add(visibleLoopVar1Name + LOOP_STATE_HAS_NEXT);
                }
                return localVarNames;
            } else {
                return Collections.emptyList();
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
