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

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.CollectionAndSequence;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.model.impl.TemplateModelListSequence;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util._StringUtils;

/**
 * A holder for builtins that operate exclusively on sequence or collection left-hand value.
 */
class BuiltInsForSequences {
    
    static class chunkBI extends BuiltInForSequence {

        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            
            private final TemplateSequenceModel tsm;

            private BIMethod(TemplateSequenceModel tsm) {
                this.tsm = tsm;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                int chunkSize = getNumberArgument(args, 0, this).intValue();

                return new ChunkedSequence(tsm, chunkSize, args[1]);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }
        }

        private static class ChunkedSequence implements TemplateSequenceModel {
            
            private final TemplateSequenceModel wrappedTsm;
            
            private final int chunkSize;
            
            private final TemplateModel fillerItem;
            
            private final int numberOfChunks;
            
            private ChunkedSequence(
                    TemplateSequenceModel wrappedTsm, int chunkSize, TemplateModel fillerItem)
                    throws TemplateException {
                if (chunkSize < 1) {
                    throw new TemplateException("The 1st argument to ?', key, ' (...) must be at least 1.");
                }
                this.wrappedTsm = wrappedTsm;
                this.chunkSize = chunkSize;
                this.fillerItem = fillerItem;
                numberOfChunks = (wrappedTsm.size() + chunkSize - 1) / chunkSize; 
            }

            @Override
            public TemplateModel get(final int chunkIndex)
                    throws TemplateException {
                if (chunkIndex >= numberOfChunks) {
                    return null;
                }
                
                return new TemplateSequenceModel() {
                    
                    private final int baseIndex = chunkIndex * chunkSize;

                    @Override
                    public TemplateModel get(int relIndex)
                            throws TemplateException {
                        int absIndex = baseIndex + relIndex;
                        if (absIndex < wrappedTsm.size()) {
                            return wrappedTsm.get(absIndex);
                        } else {
                            return absIndex < numberOfChunks * chunkSize
                                ? fillerItem
                                : null;
                        }
                    }

                    @Override
                    public int size() throws TemplateException {
                        return fillerItem != null || chunkIndex + 1 < numberOfChunks
                                ? chunkSize
                                : wrappedTsm.size() - baseIndex;
                    }
                    
                };
            }

            @Override
            public int size() throws TemplateException {
                return numberOfChunks;
            }
            
        }
        
        @Override
        TemplateModel calculateResult(TemplateSequenceModel tsm) throws TemplateException {
            return new BIMethod(tsm);
        }
        
    }
    
    static class firstBI extends ASTExpBuiltIn {
        
        @Override
        TemplateModel _eval(Environment env)
                throws TemplateException {
            TemplateModel model = target.eval(env);
            // In 2.3.x only, we prefer TemplateSequenceModel for
            // backward compatibility. In 2.4.x, we prefer TemplateCollectionModel. 
            if (model instanceof TemplateSequenceModel) {
                return calculateResultForSequence((TemplateSequenceModel) model);
            } else if (model instanceof TemplateCollectionModel) {
                return calculateResultForColletion((TemplateCollectionModel) model);
            } else {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        target, model,
                        MessageUtils.SEQUENCE_OR_COLLECTION,
                        MessageUtils.EXPECTED_TYPES_SEQUENCE_OR_COLLECTION,
                        null, env);
            }
        }        
        
        private TemplateModel calculateResultForSequence(TemplateSequenceModel seq)
        throws TemplateException {
            if (seq.size() == 0) {
                return null;
            }
            return seq.get(0);
        }
        
        private TemplateModel calculateResultForColletion(TemplateCollectionModel coll)
        throws TemplateException {
            TemplateModelIterator iter = coll.iterator();
            if (!iter.hasNext()) {
                return null;
            }
            return iter.next();
        }
        
    }

    static class joinBI extends ASTExpBuiltIn {
        
        private class BIMethodForCollection extends BuiltInCallableImpl implements TemplateFunctionModel {
            
            private final Environment env;
            private final TemplateCollectionModel coll;

            private BIMethodForCollection(Environment env, TemplateCollectionModel coll) {
                this.env = env;
                this.coll = coll;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                final String separator = getStringArgument(args, 0, this);
                final String whenEmpty = getOptionalStringArgument(args, 1, this);
                final String afterLast = getOptionalStringArgument(args, 2, this);

                StringBuilder sb = new StringBuilder();

                TemplateModelIterator it = coll.iterator();

                int idx = 0;
                boolean hadItem = false;
                while (it.hasNext()) {
                    TemplateModel item = it.next();
                    if (item != null) {
                        if (hadItem) {
                            sb.append(separator);
                        } else {
                            hadItem = true;
                        }
                        try {
                            sb.append(_EvalUtils.coerceModelToStringOrUnsupportedMarkup(item, null, null, env));
                        } catch (TemplateException e) {
                            throw new TemplateException(e,
                                    "\"?", key, "\" failed at index ", idx, " with this error:\n\n",
                                    MessageUtils.EMBEDDED_MESSAGE_BEGIN,
                                    new _DelayedGetMessageWithoutStackTop(e),
                                    MessageUtils.EMBEDDED_MESSAGE_END);
                        }
                    }
                    idx++;
                }
                if (hadItem) {
                    if (afterLast != null) sb.append(afterLast);
                } else {
                    if (whenEmpty != null) sb.append(whenEmpty);
                }
                return new SimpleScalar(sb.toString());
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.THREE_POSITIONAL_PARAMETERS;
            }

        }

        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof TemplateCollectionModel) {
                if (model instanceof RightUnboundedRangeModel) {
                    throw new TemplateException(
                            "The sequence to join was right-unbounded numerical range, thus it's infinitely long.");
                }
                return new BIMethodForCollection(env, (TemplateCollectionModel) model);
            } else if (model instanceof TemplateSequenceModel) {
                return new BIMethodForCollection(env, new CollectionAndSequence((TemplateSequenceModel) model));
            } else {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        target, model,
                        MessageUtils.SEQUENCE_OR_COLLECTION,
                        MessageUtils.EXPECTED_TYPES_SEQUENCE_OR_COLLECTION,
                        null, env);
            }
        }
   
    }

    static class lastBI extends BuiltInForSequence {
        @Override
        TemplateModel calculateResult(TemplateSequenceModel tsm)
        throws TemplateException {
            if (tsm.size() == 0) {
                return null;
            }
            return tsm.get(tsm.size() - 1);
        }
    }

    static class reverseBI extends BuiltInForSequence {
        private static class ReverseSequence implements TemplateSequenceModel {
            private final TemplateSequenceModel seq;

            ReverseSequence(TemplateSequenceModel seq) {
                this.seq = seq;
            }

            @Override
            public TemplateModel get(int index) throws TemplateException {
                return seq.get(seq.size() - 1 - index);
            }

            @Override
            public int size() throws TemplateException {
                return seq.size();
            }
        }

        @Override
        TemplateModel calculateResult(TemplateSequenceModel tsm) {
            if (tsm instanceof ReverseSequence) {
                return ((ReverseSequence) tsm).seq;
            } else {
                return new ReverseSequence(tsm);
            }
        }
    }

    static class seq_containsBI extends ASTExpBuiltIn {
        private class BIMethodForCollection extends BuiltInCallableImpl implements TemplateFunctionModel {
            private TemplateCollectionModel m_coll;
            private Environment m_env;

            private BIMethodForCollection(TemplateCollectionModel coll, Environment env) {
                m_coll = coll;
                m_env = env;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                TemplateModel arg = args[0];
                TemplateModelIterator it = m_coll.iterator();
                int idx = 0;
                while (it.hasNext()) {
                    if (modelsEqual(idx, it.next(), arg, m_env))
                        return TemplateBooleanModel.TRUE;
                    idx++;
                }
                return TemplateBooleanModel.FALSE;
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }

        }

        private class BIMethodForSequence extends BuiltInCallableImpl implements TemplateFunctionModel {
            private TemplateSequenceModel m_seq;
            private Environment m_env;

            private BIMethodForSequence(TemplateSequenceModel seq, Environment env) {
                m_seq = seq;
                m_env = env;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                TemplateModel arg = args[0];
                int size = m_seq.size();
                for (int i = 0; i < size; i++) {
                    if (modelsEqual(i, m_seq.get(i), arg, m_env))
                        return TemplateBooleanModel.TRUE;
                }
                return TemplateBooleanModel.FALSE;
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }

        }
    
        @Override
        TemplateModel _eval(Environment env)
                throws TemplateException {
            TemplateModel model = target.eval(env);
            // In 2.3.x only, we prefer TemplateSequenceModel for
            // backward compatibility. In 2.4.x, we prefer TemplateCollectionModel. 
            if (model instanceof TemplateSequenceModel) {
                return new BIMethodForSequence((TemplateSequenceModel) model, env);
            } else if (model instanceof TemplateCollectionModel) {
                return new BIMethodForCollection((TemplateCollectionModel) model, env);
            } else {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        target, model,
                        MessageUtils.SEQUENCE_OR_COLLECTION,
                        MessageUtils.EXPECTED_TYPES_SEQUENCE_OR_COLLECTION,
                        null, env);
            }
        }
    
    }
    
    static class seq_index_ofBI extends ASTExpBuiltIn {
        
        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            
            final TemplateSequenceModel m_seq;
            final TemplateCollectionModel m_col;
            final Environment m_env;

            private BIMethod(Environment env)
                    throws TemplateException {
                TemplateModel model = target.eval(env);
                m_seq = model instanceof TemplateSequenceModel
                        ? (TemplateSequenceModel) model
                        : null;
                // [FM3] Rework the below
                // In 2.3.x only, we deny the possibility of collection
                // access if there's sequence access. This is so to minimize
                // the change of compatibility issues; without this, objects
                // that implement both the sequence and collection interfaces
                // would suddenly start using the collection interface, and if
                // that's buggy that would surface now, breaking the application
                // that despite its bugs has worked earlier.
                m_col = m_seq == null && model instanceof TemplateCollectionModel
                        ? (TemplateCollectionModel) model
                        : null;
                if (m_seq == null && m_col == null) {
                    throw MessageUtils.newUnexpectedOperandTypeException(
                            target, model,
                            MessageUtils.SEQUENCE_OR_COLLECTION,
                            MessageUtils.EXPECTED_TYPES_SEQUENCE_OR_COLLECTION,
                            null, env);
                }
                
                m_env = env;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                TemplateModel target = args[0];
                Number startIndex = getOptionalNumberArgument(args, 1, this);
                int foundAtIdx;
                if (startIndex != null) {
                    // TODO [FM3] Prefer Col?
                    // In 2.3.x only, we prefer TemplateSequenceModel for
                    // backward compatibility:
                    foundAtIdx = m_seq != null
                            ? findInSeq(target, startIndex.intValue())
                            : findInCol(target, startIndex.intValue());
                } else {
                    // TODO [FM3] Prefer Col?
                    // In 2.3.x only, we prefer TemplateSequenceModel for
                    // backward compatibility:
                    foundAtIdx = m_seq != null
                            ? findInSeq(target)
                            : findInCol(target);
                }
                return foundAtIdx == -1 ? TemplateNumberModel.MINUS_ONE : new SimpleNumber(foundAtIdx);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }

            int findInCol(TemplateModel target) throws TemplateException {
                return findInCol(target, 0, Integer.MAX_VALUE);
            }
            
            int findInCol(TemplateModel target, int startIndex)
                    throws TemplateException {
                if (m_dir == 1) {
                    return findInCol(target, startIndex, Integer.MAX_VALUE);
                } else {
                    return findInCol(target, 0, startIndex);
                }
            }
        
            int findInCol(TemplateModel target,
                    final int allowedRangeStart, final int allowedRangeEnd)
                    throws TemplateException {
                if (allowedRangeEnd < 0) return -1;
                
                TemplateModelIterator it = m_col.iterator();
                
                int foundAtIdx = -1;  // -1 is the return value for "not found"
                int idx = 0; 
                searchItem: while (it.hasNext()) {
                    if (idx > allowedRangeEnd) break searchItem;
                    
                    TemplateModel current = it.next();
                    if (idx >= allowedRangeStart) {
                        if (modelsEqual(idx, current, target, m_env)) {
                            foundAtIdx = idx;
                            if (m_dir == 1) break searchItem; // "find first"
                            // Otherwise it's "find last".
                        }
                    }
                    idx++;
                }
                return foundAtIdx;
            }

            int findInSeq(TemplateModel target)
                    throws TemplateException {
                final int seqSize = m_seq.size();
                final int actualStartIndex;
                
                if (m_dir == 1) {
                    actualStartIndex = 0;
                } else {
                    actualStartIndex = seqSize - 1;
                }
            
                return findInSeq(target, actualStartIndex, seqSize); 
            }

            private int findInSeq(TemplateModel target, int startIndex)
                    throws TemplateException {
                int seqSize = m_seq.size();
                
                if (m_dir == 1) {
                    if (startIndex >= seqSize) {
                        return -1;
                    }
                    if (startIndex < 0) {
                        startIndex = 0;
                    }
                } else {
                    if (startIndex >= seqSize) {
                        startIndex = seqSize - 1;
                    }
                    if (startIndex < 0) {
                        return -1;
                    }
                }
                
                return findInSeq(target, startIndex, seqSize); 
            }
            
            private int findInSeq(
                    TemplateModel target, int scanStartIndex, int seqSize)
                    throws TemplateException {
                if (m_dir == 1) {
                    for (int i = scanStartIndex; i < seqSize; i++) {
                        if (modelsEqual(i, m_seq.get(i), target, m_env)) return i;
                    }
                } else {
                    for (int i = scanStartIndex; i >= 0; i--) {
                        if (modelsEqual(i, m_seq.get(i), target, m_env)) return i;
                    }
                }
                return -1;
            }
            
        }

        private int m_dir;

        seq_index_ofBI(int dir) {
            m_dir = dir;
        }

        @Override
        TemplateModel _eval(Environment env)
                throws TemplateException {
            return new BIMethod(env);
        }
    }

    static class sort_byBI extends sortBI {
        class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            TemplateSequenceModel seq;
            
            BIMethod(TemplateSequenceModel seq) {
                this.seq = seq;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String[] subvars;
                TemplateModel obj = args[0];
                if (obj instanceof TemplateScalarModel) {
                    subvars = new String[] { ((TemplateScalarModel) obj).getAsString() };
                } else if (obj instanceof TemplateSequenceModel) {
                    TemplateSequenceModel seq = (TemplateSequenceModel) obj;
                    int ln = seq.size();
                    subvars = new String[ln];
                    for (int i = 0; i < ln; i++) {
                        TemplateModel item = seq.get(i);
                        if (!(item instanceof  TemplateScalarModel)) {
                            throw new TemplateException(
                                    "The argument to ?", key, "(key), when it's a sequence, must be a "
                                    + "sequence of strings, but the item at index ", i,
                                    " is not a string.");
                        }
                        subvars[i] = ((TemplateScalarModel) item).getAsString();
                    }
                } else {
                    throw new TemplateException(
                            "The argument to ?", key, "(key) must be a string (the name of the subvariable), or a "
                            + "sequence of strings (the \"path\" to the subvariable).");
                }
                return sort(seq, subvars);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }
        }
        
        @Override
        TemplateModel calculateResult(TemplateSequenceModel seq) {
            return new BIMethod(seq);
        }
    }

    static class sortBI extends BuiltInForSequence {
        
        private static class BooleanKVPComparator implements Comparator, Serializable {

            @Override
            public int compare(Object arg0, Object arg1) {
                // JDK 1.2 doesn't have Boolean.compareTo
                boolean b0 = ((Boolean) ((KVP) arg0).key).booleanValue();
                boolean b1 = ((Boolean) ((KVP) arg1).key).booleanValue();
                if (b0) {
                    return b1 ? 0 : 1;
                } else {
                    return b1 ? -1 : 0;
                }
            }
        }
        private static class DateKVPComparator implements Comparator, Serializable {

            @Override
            public int compare(Object arg0, Object arg1) {
                return ((Date) ((KVP) arg0).key).compareTo(
                        (Date) ((KVP) arg1).key);
            }
        }
        /**
         * Stores a key-value pair.
         */
        private static class KVP {
            private Object key;

            private Object value;
            private KVP(Object key, Object value) {
                this.key = key;
                this.value = value;
            }
        }
        private static class LexicalKVPComparator implements Comparator {
            private Collator collator;

            LexicalKVPComparator(Collator collator) {
                this.collator = collator;
            }

            @Override
            public int compare(Object arg0, Object arg1) {
                return collator.compare(
                        ((KVP) arg0).key, ((KVP) arg1).key);
            }
        }
        private static class NumericalKVPComparator implements Comparator {
            private ArithmeticEngine ae;

            private NumericalKVPComparator(ArithmeticEngine ae) {
                this.ae = ae;
            }

            @Override
            public int compare(Object arg0, Object arg1) {
                try {
                    return ae.compareNumbers(
                            (Number) ((KVP) arg0).key,
                            (Number) ((KVP) arg1).key);
                } catch (TemplateException e) {
                    throw new ClassCastException(
                        "Failed to compare numbers: " + e);
                }
            }
        }
        
        static TemplateException newInconsistentSortKeyTypeException(
                int keyNamesLn, String firstType, String firstTypePlural, int index, TemplateModel key) {
            String valueInMsg;
            String valuesInMsg;
            if (keyNamesLn == 0) {
                valueInMsg  = "value";
                valuesInMsg  = "values";
            } else {
                valueInMsg  = "key value";
                valuesInMsg  = "key values";
            }
            return new TemplateException(
                    startErrorMessage(keyNamesLn, index),
                    "All ", valuesInMsg, " in the sequence must be ",
                    firstTypePlural, ", because the first ", valueInMsg,
                    " was that. However, the ", valueInMsg,
                    " of the current item isn't a ", firstType, " but a ",
                    new _DelayedTemplateLanguageTypeDescription(key), ".");
        }

        /**
         * Sorts a sequence for the <tt>sort</tt> and <tt>sort_by</tt>
         * built-ins.
         * 
         * @param seq the sequence to sort.
         * @param keyNames the name of the subvariable whose value is used for the
         *     sorting. If the sorting is done by a sub-subvaruable, then this
         *     will be of length 2, and so on. If the sorting is done by the
         *     sequene items directly, then this argument has to be 0 length
         *     array or <code>null</code>.
         * @return a new sorted sequence, or the original sequence if the
         *     sequence length was 0.
         */
        static TemplateSequenceModel sort(TemplateSequenceModel seq, String[] keyNames)
                throws TemplateException {
            int ln = seq.size();
            if (ln == 0) return seq;
            
            ArrayList res = new ArrayList(ln);

            int keyNamesLn = keyNames == null ? 0 : keyNames.length;

            // Copy the Seq into a Java List[KVP] (also detects key type at the 1st item):
            int keyType = KEY_TYPE_NOT_YET_DETECTED;
            Comparator keyComparator = null;
            for (int i = 0; i < ln; i++) {
                final TemplateModel item = seq.get(i);
                TemplateModel key = item;
                for (int keyNameI = 0; keyNameI < keyNamesLn; keyNameI++) {
                    try {
                        key = ((TemplateHashModel) key).get(keyNames[keyNameI]);
                    } catch (ClassCastException e) {
                        if (!(key instanceof TemplateHashModel)) {
                            throw new TemplateException(
                                    startErrorMessage(keyNamesLn, i),
                                    (keyNameI == 0
                                            ? "Sequence items must be hashes when using ?sortBy. "
                                            : "The " + _StringUtils.jQuote(keyNames[keyNameI - 1])),
                                    " subvariable is not a hash, so ?sortBy ",
                                    "can't proceed with getting the ",
                                    new _DelayedJQuote(keyNames[keyNameI]),
                                    " subvariable.");
                        } else {
                            throw e;
                        }
                    }
                    if (key == null) {
                        throw new TemplateException(
                                startErrorMessage(keyNamesLn, i),
                                "The " + _StringUtils.jQuote(keyNames[keyNameI]), " subvariable was null or missing.");
                    }
                } // for each key
                
                if (keyType == KEY_TYPE_NOT_YET_DETECTED) {
                    if (key instanceof TemplateScalarModel) {
                        keyType = KEY_TYPE_STRING;
                        keyComparator = new LexicalKVPComparator(
                                Environment.getCurrentEnvironment().getCollator());
                    } else if (key instanceof TemplateNumberModel) {
                        keyType = KEY_TYPE_NUMBER;
                        keyComparator = new NumericalKVPComparator(
                                Environment.getCurrentEnvironment()
                                        .getArithmeticEngine());
                    } else if (key instanceof TemplateDateModel) {
                        keyType = KEY_TYPE_DATE;
                        keyComparator = new DateKVPComparator();
                    } else if (key instanceof TemplateBooleanModel) {
                        keyType = KEY_TYPE_BOOLEAN;
                        keyComparator = new BooleanKVPComparator();
                    } else {
                        throw new TemplateException(
                                startErrorMessage(keyNamesLn, i),
                                "Values used for sorting must be numbers, strings, date/times or booleans.");
                    }
                }
                switch(keyType) {
                    case KEY_TYPE_STRING:
                        try {
                            res.add(new KVP(
                                    ((TemplateScalarModel) key).getAsString(),
                                    item));
                        } catch (ClassCastException e) {
                            if (!(key instanceof TemplateScalarModel)) {
                                throw newInconsistentSortKeyTypeException(
                                        keyNamesLn, "string", "strings", i, key);
                            } else {
                                throw e;
                            }
                        }
                        break;
                        
                    case KEY_TYPE_NUMBER:
                        try {
                            res.add(new KVP(
                                    ((TemplateNumberModel) key).getAsNumber(),
                                    item));
                        } catch (ClassCastException e) {
                            if (!(key instanceof TemplateNumberModel)) {
                                throw newInconsistentSortKeyTypeException(
                                        keyNamesLn, "number", "numbers", i, key);
                            }
                        }
                        break;
                        
                    case KEY_TYPE_DATE:
                        try {
                            res.add(new KVP(
                                    ((TemplateDateModel) key).getAsDate(),
                                    item));
                        } catch (ClassCastException e) {
                            if (!(key instanceof TemplateDateModel)) {
                                throw newInconsistentSortKeyTypeException(
                                        keyNamesLn, "date/time", "date/times", i, key);
                            }
                        }
                        break;
                        
                    case KEY_TYPE_BOOLEAN:
                        try {
                            res.add(new KVP(
                                    Boolean.valueOf(((TemplateBooleanModel) key).getAsBoolean()),
                                    item));
                        } catch (ClassCastException e) {
                            if (!(key instanceof TemplateBooleanModel)) {
                                throw newInconsistentSortKeyTypeException(
                                        keyNamesLn, "boolean", "booleans", i, key);
                            }
                        }
                        break;
                        
                    default:
                        throw new BugException("Unexpected key type");
                }
            }

            // Sort tje List[KVP]:
            try {
                Collections.sort(res, keyComparator);
            } catch (Exception exc) {
                throw new TemplateException(exc,
                        startErrorMessage(keyNamesLn), "Unexpected error while sorting:" + exc);
            }

            // Convert the List[KVP] to List[V]:
            for (int i = 0; i < ln; i++) {
                res.set(i, ((KVP) res.get(i)).value);
            }

            return new TemplateModelListSequence(res);
        }

        static Object[] startErrorMessage(int keyNamesLn) {
            return new Object[] { (keyNamesLn == 0 ? "?sort" : "?sortBy(...)"), " failed: " };
        }
        
        static Object[] startErrorMessage(int keyNamesLn, int index) {
            return new Object[] {
                    (keyNamesLn == 0 ? "?sort" : "?sortBy(...)"),
                    " failed at sequence index ", Integer.valueOf(index),
                    (index == 0 ? ": " : " (0-based): ") };
        }
        
        static final int KEY_TYPE_NOT_YET_DETECTED = 0;

        static final int KEY_TYPE_STRING = 1;

        static final int KEY_TYPE_NUMBER = 2;

        static final int KEY_TYPE_DATE = 3;
        
        static final int KEY_TYPE_BOOLEAN = 4;
        
        @Override
        TemplateModel calculateResult(TemplateSequenceModel seq)
                throws TemplateException {
            return sort(seq, null);
        }
        
    }

    private static boolean modelsEqual(
            int seqItemIndex, TemplateModel seqItem, TemplateModel searchedItem,
            Environment env)
            throws TemplateException {
        try {
            return _EvalUtils.compare(
                    seqItem, null,
                    _EvalUtils.CMP_OP_EQUALS, null,
                    searchedItem, null,
                    null, false,
                    true, true, true, // The last one is true to emulate an old bug for BC 
                    env);
        } catch (TemplateException ex) {
            throw new TemplateException(ex,
                    "This error has occurred when comparing sequence item at 0-based index ", Integer.valueOf(seqItemIndex),
                    " to the searched item:\n", new _DelayedGetMessage(ex));
        }
    }
 
    // Can't be instantiated
    private BuiltInsForSequences() { }
    
}