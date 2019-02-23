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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.util._StringUtils;

/**
 * AST directive node: {@code #list} element, or pre-{@code #else} section of it inside a
 * {@link ASTDirListElseContainer}.
 */
final class ASTDirList extends ASTDirective {

    private final ASTExpression listedExp;
    private final String nestedContentParam1Name;
    private final String nestedContentParam2Name;
    private final boolean hashListing;

    /**
     * @param listedExp
     *            a variable referring to an iterable or extended hash that we want to list
     * @param nestedContentParam1Name
     *            The name of the variable that will hold the value of the current item when looping through listed value,
     *            or {@code null} if we have a nested {@code #items}. If this is a hash listing then this variable will holds the value
     *            of the hash key.
     * @param nestedContentParam2Name
     *            The name of the variable that will hold the value of the current item when looping through the list,
     *            or {@code null} if we have a nested {@code #items}. If this is a hash listing then it variable will hold the value
     *            from the key-value pair.
     * @param childrenBeforeElse
     *            The nested content to execute if the listed value wasn't empty; can't be {@code null}. If the
     *            nested content parameter is specified in the start tag, this is also what we will iterate over.
     * @param hashListing
     *            Whether this is a key-value pair listing, or a usual listing. This is properly set even if we have
     *            a nested {@code #items}.
     */
    ASTDirList(ASTExpression listedExp,
                  String nestedContentParam1Name,
                  String nestedContentParam2Name,
                  TemplateElements childrenBeforeElse,
                  boolean hashListing) {
        this.listedExp = listedExp;
        this.nestedContentParam1Name = nestedContentParam1Name;
        this.nestedContentParam2Name = nestedContentParam2Name;
        setChildren(childrenBeforeElse);
        this.hashListing = hashListing;
    }
    
    boolean isHashListing() {
        return hashListing;
    }

    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        acceptWithResult(env);
        return null;
    }
    
    boolean acceptWithResult(Environment env) throws TemplateException, IOException {
        TemplateModel listedValue = listedExp.eval(env);
        if (listedValue == null) {
            listedExp.assertNonNull(null, env);
        }

        return env.visitIteratorBlock(new IterationContext(listedValue, nestedContentParam1Name, nestedContentParam2Name));
    }

    @Override
    String dump(boolean canonical) {
        StringBuilder buf = new StringBuilder();
        if (canonical) buf.append('<');
        buf.append(getLabelWithoutParameters());
        buf.append(' ');
        buf.append(listedExp.getCanonicalForm());
        if (nestedContentParam1Name != null) {
            buf.append(" as ");
            buf.append(_StringUtils.toFTLTopLevelIdentifierReference(nestedContentParam1Name));
            if (nestedContentParam2Name != null) {
                buf.append(", ");
                buf.append(_StringUtils.toFTLTopLevelIdentifierReference(nestedContentParam2Name));
            }
        }
        if (canonical) {
            buf.append(">");
            buf.append(getChildrenCanonicalForm());
            if (!(getParent() instanceof ASTDirListElseContainer)) {
                buf.append("</");
                buf.append(getLabelWithoutParameters());
                buf.append('>');
            }
        }
        return buf.toString();
    }
    
    @Override
    int getParameterCount() {
        return 1 + (nestedContentParam1Name != null ? 1 : 0) + (nestedContentParam2Name != null ? 1 : 0);
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0:
            return listedExp;
        case 1:
            if (nestedContentParam1Name == null) throw new IndexOutOfBoundsException();
            return nestedContentParam1Name;
        case 2:
            if (nestedContentParam2Name == null) throw new IndexOutOfBoundsException();
            return nestedContentParam2Name;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0:
            return ParameterRole.LIST_SOURCE;
        case 1:
            if (nestedContentParam1Name == null) throw new IndexOutOfBoundsException();
            return ParameterRole.NESTED_CONTENT_PARAMETER;
        case 2:
            if (nestedContentParam2Name == null) throw new IndexOutOfBoundsException();
            return ParameterRole.NESTED_CONTENT_PARAMETER;
        default: throw new IndexOutOfBoundsException();
        }
    }    
    
    @Override
    public String getLabelWithoutParameters() {
        return "#list";
    }

    @Override
    boolean isNestedBlockRepeater() {
        return nestedContentParam1Name != null;
    }

    /**
     * Holds the context of a #list directive.
     */
    class IterationContext implements LocalContext {
        
        private static final String LOOP_STATE_HAS_NEXT = "_has_next"; // lenght: 9
        private static final String LOOP_STATE_INDEX = "_index"; // length 6
        
        private Object openedIterator;
        private boolean hasNext;
        private TemplateModel nestedContentParam;
        private TemplateModel nestedContentParam2;
        private int index;
        private boolean alreadyEntered;
        private Collection localVarNames = null;
        
        /** If the {@code #list} has nested {@code #items}, it's {@code null} outside the {@code #items}. */
        private String nestedContentParam1Name;
        /** Used if we list key-value pairs */
        private String nestedContentParam2Name;
        /**
         * Whether the nested content parameters are visible from the template at the moment.
         * It would be more intuitive if the {@link LocalContext} is not in the local stack when they aren't visible,
         * but the {@link LocalContext} is also used for {@code #items} to find its parent, for which we need the tricky
         * scoping of the local context stack {@link Environment#getLocalContextStack()}.
         */
        private boolean nestedContentParamsVisible;
        
        private final TemplateModel listedValue;
        
        public IterationContext(TemplateModel listedValue,
                String nestedContentParamName, String nestedContentParam2Name) {
            this.listedValue = listedValue;
            this.nestedContentParam1Name = nestedContentParamName;
            this.nestedContentParam2Name = nestedContentParam2Name;
        }
        
        boolean accept(Environment env) throws TemplateException, IOException {
            return executeNestedContent(env, getChildBuffer());
        }

        void loopForItemsElement(Environment env, ASTElement[] childBuffer,
                String nestedContentParamName, String nestedContentParam2Name)
                throws TemplateException, IOException {
            try {
                if (alreadyEntered) {
                    throw new TemplateException(env,
                            "The #items directive was already entered earlier for this listing.");
                }
                alreadyEntered = true;
                this.nestedContentParam1Name = nestedContentParamName;
                this.nestedContentParam2Name = nestedContentParam2Name;
                executeNestedContent(env, childBuffer);
            } finally {
                this.nestedContentParam1Name = null;
                this.nestedContentParam2Name = null;
            }
        }

        /**
         * Executes the given block for the {@link #listedValue}: if {@link #nestedContentParam1Name} is non-{@code
         * null}, then for each list item once, otherwise once if {@link #listedValue} isn't empty.
         */
        private boolean executeNestedContent(Environment env, ASTElement[] childBuffer)
                throws TemplateException, IOException {
            return !hashListing
                    ? executedNestedContentForIterableListing(env, childBuffer)
                    : executedNestedContentForHashListing(env, childBuffer);
        }

        private boolean executedNestedContentForIterableListing(Environment env, ASTElement[] childBuffer)
                throws IOException, TemplateException {
            final boolean listNotEmpty;
            if (listedValue instanceof TemplateIterableModel) {
                final TemplateIterableModel collModel = (TemplateIterableModel) listedValue;
                final TemplateModelIterator iterModel
                        = openedIterator == null ? collModel.iterator()
                                : ((TemplateModelIterator) openedIterator);
                listNotEmpty = iterModel.hasNext();
                if (listNotEmpty) {
                    if (nestedContentParam1Name != null) {
                        listLoop: do {
                                nestedContentParam = iterModel.next();
                                hasNext = iterModel.hasNext();
                                try {
                                    nestedContentParamsVisible = true;
                                    env.executeElements(childBuffer);
                                } catch (BreakOrContinueException br) {
                                    if (br == BreakOrContinueException.BREAK_INSTANCE) {
                                        break listLoop;
                                    }
                                } finally {
                                    nestedContentParamsVisible = false;
                                }
                                index++;
                            } while (hasNext);
                        openedIterator = null;
                    } else {
                        // We must reuse this later, because some TemplateIterableModel can only allow one iterator()
                        // call (such as those wrapping an Iterator).
                        openedIterator = iterModel;
                        // Note: Nested content parameters will only become visible inside #items
                        env.executeElements(childBuffer);
                    }
                }
            } else if (listedValue instanceof TemplateHashModelEx) {
                throw new TemplateException(env,
                        new _ErrorDescriptionBuilder("The value you try to list is ",
                                new _DelayedAOrAn(new _DelayedTemplateLanguageTypeDescription(listedValue)),
                                ", thus you must declare two nested content parameters after the \"as\"; one for the "
                                + "key, and another for the value, like ", "<#... as k, v>", ")."
                                ));
            } else {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        listedExp, listedValue,
                        MessageUtils.EXPECTED_TYPE_ITERABLE_DESC,
                        TemplateIterableModel.class,
                        null, env);
            }
            return listNotEmpty;
        }

        private boolean executedNestedContentForHashListing(Environment env, ASTElement[] childBuffer)
                throws IOException, TemplateException {
            final boolean hashNotEmpty;
            if (listedValue instanceof TemplateHashModelEx) {
                TemplateHashModelEx listedHash = (TemplateHashModelEx) listedValue; 
                TemplateHashModelEx.KeyValuePairIterator kvpIter
                        = openedIterator == null ? listedHash.keyValuePairIterator()
                                : (TemplateHashModelEx.KeyValuePairIterator) openedIterator;
                hashNotEmpty = kvpIter.hasNext();
                if (hashNotEmpty) {
                    if (nestedContentParam1Name != null) {
                        listLoop: do {
                            TemplateHashModelEx.KeyValuePair kvp = kvpIter.next();
                            nestedContentParam = kvp.getKey();
                            nestedContentParam2 = kvp.getValue();
                            hasNext = kvpIter.hasNext();
                            try {
                                nestedContentParamsVisible = true;
                                env.executeElements(childBuffer);
                            } catch (BreakOrContinueException br) {
                                if (br == BreakOrContinueException.BREAK_INSTANCE) {
                                    break listLoop;
                                }
                            } finally {
                                nestedContentParamsVisible = false;
                            }
                            index++;
                        } while (hasNext);
                        openedIterator = null;
                    } else {
                        // We will reuse this at the #iterms
                        openedIterator = kvpIter;
                        // Note: Nested content parameters will only become visible inside #items
                        env.executeElements(childBuffer);
                    }
                }
            } else if (listedValue instanceof TemplateIterableModel) {
                throw new TemplateException(env,
                        new _ErrorDescriptionBuilder("The value you try to list is ",
                                new _DelayedAOrAn(new _DelayedTemplateLanguageTypeDescription(listedValue)),
                                ", thus you must declare only one nested content parameter after the \"as\" (there's "
                                + "no separate key and value)."
                                ));
            } else {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        listedExp, listedValue, TemplateHashModelEx.class, env);
            }
            return hashNotEmpty;
        }

        String getNestedContentParameter1Name() {
            return nestedContentParam1Name;
        }

        String getNestedContentParameter2Name() {
            return nestedContentParam2Name;
        }
        
        @Override
        public TemplateModel getLocalVariable(String name) {
            if (!nestedContentParamsVisible) {
                return null;
            }

            String nestedContentParamName = this.nestedContentParam1Name;
            if (name.startsWith(nestedContentParamName)) {
                switch(name.length() - nestedContentParamName.length()) {
                    case 0: 
                        return nestedContentParam;
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
            
            if (name.equals(nestedContentParam2Name)) {
                return nestedContentParam2;
            }
            
            return null;
        }
        
        @Override
        public Collection<String> getLocalVariableNames() {
            if (!nestedContentParamsVisible) {
                return Collections.EMPTY_LIST;
            }

            String nestedContentParamName = this.nestedContentParam1Name;
            if (localVarNames == null) {
                localVarNames = new ArrayList(3);
                localVarNames.add(nestedContentParamName);
                localVarNames.add(nestedContentParamName + LOOP_STATE_INDEX);
                localVarNames.add(nestedContentParamName + LOOP_STATE_HAS_NEXT);
            }
            return localVarNames;
        }

        boolean hasNext() {
            return hasNext;
        }
        
        int getIndex() {
            return index;
        }
        
    }
    
}
