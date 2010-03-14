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

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelListSequence;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModelIterator;
import freemarker.template.utility.Constants;
import freemarker.template.utility.StringUtil;

/**
 * A holder for builtins that operate exclusively on TemplateSequenceModels.
 */

abstract class SequenceBuiltins {
    abstract static class SequenceBuiltIn extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.getAsTemplateModel(env);
            if (!(model instanceof TemplateSequenceModel)) {
                throw invalidTypeException(model, target, env, "sequence");
            }
            return calculateResult((TemplateSequenceModel) model);
        }
        abstract TemplateModel calculateResult(TemplateSequenceModel tsm)
        throws
            TemplateModelException;
    }

    static class firstBI extends SequenceBuiltIn {
        TemplateModel calculateResult(TemplateSequenceModel tsm)
        throws
            TemplateModelException
        {
            if (tsm.size() == 0) {
                return null;
            }
            return tsm.get(0);
        }
    }

    static class lastBI extends SequenceBuiltIn {
        TemplateModel calculateResult(TemplateSequenceModel tsm)
        throws
            TemplateModelException
        {
            if (tsm.size() == 0) {
                return null;
            }
            return tsm.get(tsm.size() -1);
        }
    }

    static class reverseBI extends SequenceBuiltIn {
        TemplateModel calculateResult(TemplateSequenceModel tsm) {
            if (tsm instanceof ReverseSequence) {
                return ((ReverseSequence) tsm).seq;
            } else {
                return new ReverseSequence(tsm);
            }
        }

        private static class ReverseSequence implements TemplateSequenceModel
        {
            private final TemplateSequenceModel seq;

            ReverseSequence(TemplateSequenceModel seq)
            {
                this.seq = seq;
            }

            public int size() throws TemplateModelException
            {
                return seq.size();
            }

            public TemplateModel get(int index) throws TemplateModelException
            {
                return seq.get(seq.size() - 1 - index);
            }
        }
    }

    static class sortBI extends SequenceBuiltIn {
        
        static final int KEY_TYPE_STRING = 1;
        static final int KEY_TYPE_NUMBER = 2;
        static final int KEY_TYPE_DATE = 3;
        
        TemplateModel calculateResult(TemplateSequenceModel seq)
                throws TemplateModelException {
            return sort(seq, null);
        }
        
        static String startErrorMessage(Object keys) {
            return (keys == null ? "?sort" : "?sort_by(...)") + " failed: ";
        }
        
        /**
         * Sorts a sequence for the <tt>sort</tt> and <tt>sort_by</tt>
         * built-ins.
         * 
         * @param seq the sequence to sort.
         * @param keys the name of the subvariable whose value is used for the
         *     sorting. If the sorting is done by a sub-subvaruable, then this
         *     will be of length 2, and so on. If the sorting is done by the
         *     sequene items directly, then this argument has to be 0 length
         *     array or <code>null</code>.
         * @return a new sorted sequence, or the original sequence if the
         *     sequence length was 0.
         */
        static TemplateSequenceModel sort(TemplateSequenceModel seq, String[] keys)
                throws TemplateModelException {
            int i;
            int keyCnt;

            int ln = seq.size();
            if (ln == 0) {
                return seq;
            }
            
            List res = new ArrayList(ln);
            Object item;
            item = seq.get(0);
            if (keys != null) {
                keyCnt = keys.length;
                if (keyCnt == 0) {
                    keys = null;
                } else {
                    for (i = 0; i < keyCnt; i++) {
                        if (!(item instanceof TemplateHashModel)) {
                            throw new TemplateModelException(
                                    startErrorMessage(keys)
                                    + (i == 0
                                            ? "You can't use ?sort_by when the "
                                              + "sequence items are not hashes."
                                            : "The subvariable "
                                              + StringUtil.jQuote(keys[i - 1])
                                              + " is not a hash, so ?sort_by "
                                              + "can't proceed by getting the "
                                              + StringUtil.jQuote(keys[i])
                                              + " subvariable."));
                        }
                        
                        item = ((TemplateHashModel) item).get(keys[i]);
                        if (item == null) {
                            throw new TemplateModelException(
                                    startErrorMessage(keys)
                                    + "The " + StringUtil.jQuote(keys[i])
                                    + " subvariable "
                                    + (keyCnt == 1
                                        ? "was not found."
                                        : "(specified by ?sort_by argument number "
                                          + (i + 1) + ") was not found."));
                        }
                    }
                }
            } else {
                keyCnt = 0;
            }

            int keyType;
            if (item instanceof TemplateScalarModel) {
                keyType = KEY_TYPE_STRING;
            } else if (item instanceof TemplateNumberModel) {
                keyType = KEY_TYPE_NUMBER;
            } else if (item instanceof TemplateDateModel) {
                keyType = KEY_TYPE_DATE;
            } else {
                throw new TemplateModelException(
                        startErrorMessage(keys)
                        + "Values used for sorting must be numbers, strings, or date/time values.");
            }

            if (keys == null) {
                if (keyType == KEY_TYPE_STRING) {
                    for (i = 0; i < ln; i++) {
                        item = seq.get(i);
                        try {
                            res.add(new KVP(
                                    ((TemplateScalarModel) item).getAsString(),
                                    item));
                        } catch (ClassCastException e) {
                            if (!(item instanceof TemplateScalarModel)) {
                                throw new TemplateModelException(
                                        startErrorMessage(null)
                                        + "All values in the sequence must be "
                                        + "strings, because the first value "
                                        + "was a string. "
                                        + "The value at index " + i
                                        + " is not string.");
                            } else {
                                throw e;
                            }
                        }
                    }
                } else if (keyType == KEY_TYPE_NUMBER) {
                    for (i = 0; i < ln; i++) {
                        item = seq.get(i);
                        try {
                            res.add(new KVP(
                                    ((TemplateNumberModel) item).getAsNumber(),
                                    item));
                        } catch (ClassCastException e) {
                            if (!(item instanceof TemplateNumberModel)) {
                                throw new TemplateModelException(
                                        startErrorMessage(null)
                                        + "All values in the sequence must be "
                                        + "numbers, because the first value "
                                        + "was a number. "
                                        + "The value at index " + i
                                        + " is not number.");
                            } else {
                                throw e;
                            }
                        }
                    }
                } else if (keyType == KEY_TYPE_DATE) {
                    for (i = 0; i < ln; i++) {
                        item = seq.get(i);
                        try {
                            res.add(new KVP(
                                    ((TemplateDateModel) item).getAsDate(),
                                    item));
                        } catch (ClassCastException e) {
                            if (!(item instanceof TemplateNumberModel)) {
                                throw new TemplateModelException(
                                        startErrorMessage(null)
                                        + "All values in the sequence must be "
                                        + "date/time values, because the first "
                                        + "value was a date/time. "
                                        + "The value at index " + i
                                        + " is not date/time.");
                            } else {
                                throw e;
                            }
                        }
                    }
                } else {
                    throw new RuntimeException("FreeMarker bug: Bad key type");
                }
            } else {
                for (i = 0; i < ln; i++) {
                    item = seq.get(i);
                    Object key = item;
                    for (int j = 0; j < keyCnt; j++) {
                        try {
                            key = ((TemplateHashModel) key).get(keys[j]);
                        } catch (ClassCastException e) {
                            if (!(key instanceof TemplateHashModel)) {
                                throw new TemplateModelException(
                                        startErrorMessage(keys)
                                        + "Problem with the sequence item at index " + i + ": "
                                        + "Can't get the " + StringUtil.jQuote(keys[j])
                                        + " subvariable, because the value is not a hash.");
                            } else {
                                throw e;
                            }
                        }
                        if (key == null) {
                            throw new TemplateModelException(
                                    startErrorMessage(keys)
                                    + "Problem with the sequence item at index " + i + ": "
                                    + "The " + StringUtil.jQuote(keys[j])
                                    + " subvariable was not found.");
                        }
                    }
                    if (keyType == KEY_TYPE_STRING) {
                        try {
                            res.add(new KVP(
                                    ((TemplateScalarModel) key).getAsString(),
                                    item));
                        } catch (ClassCastException e) {
                            if (!(key instanceof TemplateScalarModel)) {
                                throw new TemplateModelException(
                                        startErrorMessage(keys)
                                        + "All key values in the sequence must be "
                                        + "date/time values, because the first key "
                                        + "value was a date/time. The key value at "
                                        + "index " + i + " is not a date/time.");
                            } else {
                                throw e;
                            }
                        }
                    } else if (keyType == KEY_TYPE_NUMBER) {
                        try {
                            res.add(new KVP(
                                    ((TemplateNumberModel) key).getAsNumber(),
                                    item));
                        } catch (ClassCastException e) {
                            if (!(key instanceof TemplateNumberModel)) {
                                throw new TemplateModelException(
                                        startErrorMessage(keys)
                                        + "All key values in the sequence must be "
                                        + "numbers, because the first key "
                                        + "value was a number. The key value at "
                                        + "index " + i + " is not a number.");
                            }
                        }
                    } else if (keyType == KEY_TYPE_DATE) {
                        try {
                            res.add(new KVP(
                                    ((TemplateDateModel) key).getAsDate(),
                                    item));
                        } catch (ClassCastException e) {
                            if (!(key instanceof TemplateDateModel)) {
                                throw new TemplateModelException(
                                        startErrorMessage(keys)
                                        + "All key values in the sequence must be "
                                        + "dates, because the first key "
                                        + "value was a date. The key value at "
                                        + "index " + i + " is not a date.");
                            }
                        }
                    } else {
                        throw new RuntimeException("FreeMarker bug: Bad key type");
                    }
                }
            }

            Comparator cmprtr;
            if (keyType == KEY_TYPE_STRING) {
                cmprtr = new LexicalKVPComparator(
                        Environment.getCurrentEnvironment().getCollator());
            } else if (keyType == KEY_TYPE_NUMBER) {
                cmprtr = new NumericalKVPComparator(
                        Environment.getCurrentEnvironment()
                                .getArithmeticEngine());
            } else if (keyType == KEY_TYPE_DATE) {
                cmprtr = new DateKVPComparator();
            } else {
                throw new RuntimeException("FreeMarker bug: Bad key type");
            }

            try {
                Collections.sort(res, cmprtr);
            } catch (ClassCastException exc) {
                throw new TemplateModelException(
                        startErrorMessage(keys)
                        + "Unexpected error while sorting:" + exc, exc);
            }

            for (i = 0; i < ln; i++) {
                res.set(i, ((KVP) res.get(i)).value);
            }

            return new TemplateModelListSequence(res);
        }

        private static class KVP {
            private KVP(Object key, Object value) {
                this.key = key;
                this.value = value;
            }

            private Object key;
            private Object value;
        }

        private static class NumericalKVPComparator implements Comparator {
            private ArithmeticEngine ae;

            private NumericalKVPComparator(ArithmeticEngine ae) {
                this.ae = ae;
            }

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

        private static class LexicalKVPComparator implements Comparator {
            private Collator collator;

            LexicalKVPComparator(Collator collator) {
                this.collator = collator;
            }

            public int compare(Object arg0, Object arg1) {
                return collator.compare(
                        ((KVP) arg0).key, ((KVP) arg1).key);
            }
        }
        
        private static class DateKVPComparator implements Comparator, Serializable {

            public int compare(Object arg0, Object arg1) {
                return ((Date) ((KVP) arg0).key).compareTo(
                        (Date) ((KVP) arg1).key);
            }
        }
        
    }
    
    static class sort_byBI extends sortBI {
        TemplateModel calculateResult(TemplateSequenceModel seq)
        {
            return new BIMethod(seq);
        }
        
        static class BIMethod implements TemplateMethodModelEx {
            TemplateSequenceModel seq;
            
            BIMethod(TemplateSequenceModel seq) {
                this.seq = seq;
            }
            
            public Object exec(List params)
                    throws TemplateModelException {
                if (params.size() == 0) {
                    throw new TemplateModelException(
                            "?sort_by(key) needs exactly 1 argument.");
                }
                String[] subvars;
                Object obj = params.get(0);
                if (obj instanceof TemplateScalarModel) {
                    subvars = new String[]{((TemplateScalarModel) obj).getAsString()};
                } else if (obj instanceof TemplateSequenceModel) {
                    TemplateSequenceModel seq = (TemplateSequenceModel) obj;
                    int ln = seq.size();
                    subvars = new String[ln];
                    for (int i = 0; i < ln; i++) {
                        Object item = seq.get(i);
                        try {
                            subvars[i] = ((TemplateScalarModel) item)
                                    .getAsString();
                        } catch (ClassCastException e) {
                            if (!(item instanceof TemplateScalarModel)) {
                                throw new TemplateModelException(
                                        "The argument to ?sort_by(key), when it "
                                        + "is a sequence, must be a sequence of "
                                        + "strings, but the item at index " + i
                                        + " is not a string." );
                            }
                        }
                    }
                } else {
                    throw new TemplateModelException(
                            "The argument to ?sort_by(key) must be a string "
                            + "(the name of the subvariable), or a sequence of "
                            + "strings (the \"path\" to the subvariable).");
                }
                return sort(seq, subvars); 
            }
        }
    }

    static class seq_containsBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateSequenceModel) {
                return new BIMethodForSequence((TemplateSequenceModel) model, env);
            } else if (model instanceof TemplateCollectionModel) {
                return new BIMethodForCollection((TemplateCollectionModel) model, env);
            } else {
                throw invalidTypeException(model, target, env, "sequence or collection");
            }
        }

        private static class BIMethodForSequence implements TemplateMethodModelEx {
            private TemplateSequenceModel m_seq;
            private Environment m_env;

            private BIMethodForSequence(TemplateSequenceModel seq, Environment env) {
                m_seq = seq;
                m_env = env;
            }

            public Object exec(List args)
                    throws TemplateModelException {
                if (args.size() != 1)
                    throw new TemplateModelException("?seq_contains(...) expects one argument.");
                TemplateModel arg = (TemplateModel) args.get(0);
                int size = m_seq.size();
                for (int i = 0; i < size; i++) {
                    if (modelsEqual(m_seq.get(i), arg, m_env))
                        return TemplateBooleanModel.TRUE;
                }
                return TemplateBooleanModel.FALSE;
            }

        }
    
        private static class BIMethodForCollection implements TemplateMethodModelEx {
            private TemplateCollectionModel m_coll;
            private Environment m_env;

            private BIMethodForCollection(TemplateCollectionModel coll, Environment env) {
                m_coll = coll;
                m_env = env;
            }

            public Object exec(List args)
                    throws TemplateModelException {
                if (args.size() != 1)
                    throw new TemplateModelException("?seq_contains(...) expects one argument.");
                TemplateModel arg = (TemplateModel) args.get(0);
                TemplateModelIterator it = m_coll.iterator();
                while (it.hasNext()) {
                    if (modelsEqual(it.next(), arg, m_env))
                        return TemplateBooleanModel.TRUE;
                }
                return TemplateBooleanModel.FALSE;
            }

        }
    
    }

    static class seq_index_ofBI extends BuiltIn {
        private int m_dir;

        public seq_index_ofBI(int dir) {
            m_dir = dir;
        }

        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (!(model instanceof TemplateSequenceModel))
                throw invalidTypeException(model, target, env, "sequence");
            return new BIMethod((TemplateSequenceModel) model, env);
        }

        private class BIMethod implements TemplateMethodModelEx {
            private TemplateSequenceModel m_seq;
            private Environment m_env;

            private BIMethod(TemplateSequenceModel seq, Environment env) {
                m_seq = seq;
                m_env = env;
            }

            public Object exec(List args)
                    throws TemplateModelException {
                int argcnt = args.size();
                if (argcnt != 1 && argcnt != 2) {
                    throw new TemplateModelException(
                            getBuiltinTemplate() + " expects 1 or 2 arguments.");
                }
                
                int startIndex;
                int seqSize = m_seq.size();
                TemplateModel arg = (TemplateModel) args.get(0);
                if (argcnt > 1) {
                    Object obj = args.get(1);
                    if (!(obj instanceof TemplateNumberModel)) {
                        throw new TemplateModelException(
                                getBuiltinTemplate()
                                + "expects a number as its second argument.");
                    }
                    startIndex = ((TemplateNumberModel) obj).getAsNumber().intValue();
                    if (m_dir == 1) {
                        if (startIndex >= seqSize) {
                            return Constants.MINUS_ONE;
                        }
                        if (startIndex < 0) {
                            startIndex = 0;
                        }
                    } else {
                        if (startIndex >= seqSize) {
                            startIndex = seqSize - 1;
                        }
                        if (startIndex < 0) {
                            return Constants.MINUS_ONE;
                        }
                    }
                } else {
                    if (m_dir == 1) {
                        startIndex = 0;
                    } else {
                        startIndex = seqSize - 1;
                    }
                }
                
                if (m_dir == 1) {
                    for (int i = startIndex; i < seqSize; i++) {
                        if (modelsEqual(m_seq.get(i), arg, m_env))
                            return new SimpleNumber(i);
                    }
                } else {
                    for (int i = startIndex; i >= 0; i--) {
                        if (modelsEqual(m_seq.get(i), arg, m_env))
                            return new SimpleNumber(i);
                    }
                }
                return Constants.MINUS_ONE;
            }
            
            private String getBuiltinTemplate() {
                    if (m_dir == 1)
                        return "?seq_indexOf(...)";
                    else
                        return "?seq_lastIndexOf(...)";
            }
        }
    }

    static class chunkBI extends SequenceBuiltIn {

        TemplateModel calculateResult(TemplateSequenceModel tsm) throws TemplateModelException {
            return new BIMethod(tsm);
        }

        private static class BIMethod implements TemplateMethodModelEx {
            
            private final TemplateSequenceModel tsm;

            private BIMethod(TemplateSequenceModel tsm) {
                this.tsm = tsm;
            }

            public Object exec(List args) throws TemplateModelException {
                int numArgs = args.size();
                if (numArgs != 1 && numArgs !=2) {
                    throw new TemplateModelException(
                            "?chunk(...) expects 1 or 2 arguments.");
                }
                
                Object chunkSize = args.get(0);
                if (!(chunkSize instanceof TemplateNumberModel)) {
                    throw new TemplateModelException(
                            "?chunk(...) expects a number as "
                            + "its 1st argument.");
                }
                
                return new ChunkedSequence(
                        tsm,
                        ((TemplateNumberModel) chunkSize).getAsNumber().intValue(),
                        numArgs > 1 ? (TemplateModel) args.get(1) : null);
            }
        }
        
        private static class ChunkedSequence implements TemplateSequenceModel {
            
            private final TemplateSequenceModel wrappedTsm;
            
            private final int chunkSize;
            
            private final TemplateModel fillerItem;
            
            private final int numberOfChunks;
            
            private ChunkedSequence(
                    TemplateSequenceModel wrappedTsm, int chunkSize, TemplateModel fillerItem)
                    throws TemplateModelException {
                if (chunkSize < 1) {
                    throw new TemplateModelException(
                            "The 1st argument to ?chunk(...) must be at least 1.");
                }
                this.wrappedTsm = wrappedTsm;
                this.chunkSize = chunkSize;
                this.fillerItem = fillerItem;
                numberOfChunks = (wrappedTsm.size() + chunkSize - 1) / chunkSize; 
            }

            public TemplateModel get(final int chunkIndex)
                    throws TemplateModelException {
                if (chunkIndex >= numberOfChunks) {
                    return null;
                }
                
                return new TemplateSequenceModel() {
                    
                    private final int baseIndex = chunkIndex * chunkSize;

                    public TemplateModel get(int relIndex)
                            throws TemplateModelException {
                        int absIndex = baseIndex + relIndex;
                        if (absIndex < wrappedTsm.size()) {
                            return wrappedTsm.get(absIndex);
                        } else {
                            return absIndex < numberOfChunks * chunkSize
                                ? fillerItem
                                : null;
                        }
                    }

                    public int size() throws TemplateModelException {
                        return fillerItem != null || chunkIndex + 1 < numberOfChunks
                                ? chunkSize
                                : wrappedTsm.size() - baseIndex;
                    }
                    
                };
            }

            public int size() throws TemplateModelException {
                return numberOfChunks;
            }
            
        }
        
    }
    
    /*
     * WARNING! This algorithm is duplication of ComparisonExpression.isTrue(...).
     * Thus, if you update this method, then you have to update that too!
     */
    public static boolean modelsEqual(TemplateModel model1, TemplateModel model2,
                                Environment env)
            throws TemplateModelException {
        if (env.isClassicCompatible()) {
            if (model1 == null) {
                model1 = TemplateScalarModel.EMPTY_STRING;
            }
            if (model2 == null) {
                model2 = TemplateScalarModel.EMPTY_STRING;
            }
        }

        int comp = -1;
        if(model1 instanceof TemplateNumberModel && model2 instanceof TemplateNumberModel) {
            Number first = ((TemplateNumberModel) model1).getAsNumber();
            Number second = ((TemplateNumberModel) model2).getAsNumber();
            ArithmeticEngine ae = env.getArithmeticEngine();
            try {
                comp = ae.compareNumbers(first, second);
            } catch (TemplateException ex) {
                throw new TemplateModelException(ex);
            }
        }
        else if(model1 instanceof TemplateDateModel && model2 instanceof TemplateDateModel) {
            TemplateDateModel ltdm = (TemplateDateModel)model1;
            TemplateDateModel rtdm = (TemplateDateModel)model2;
            int ltype = ltdm.getDateType();
            int rtype = rtdm.getDateType();
            if(ltype != rtype) {
                throw new TemplateModelException(
                    "Can not compare dates of different type. Left date is of "
                    + TemplateDateModel.TYPE_NAMES.get(ltype)
                    + " type, right date is of "
                    + TemplateDateModel.TYPE_NAMES.get(rtype) + " type.");
            }
            if(ltype == TemplateDateModel.UNKNOWN) {
                throw new TemplateModelException(
                    "Left date is of UNKNOWN type, and can not be compared.");
            }
            if(rtype == TemplateDateModel.UNKNOWN) {
                throw new TemplateModelException(
                    "Right date is of UNKNOWN type, and can not be compared.");
            }
            Date first = ltdm.getAsDate();
            Date second = rtdm.getAsDate();
            comp = first.compareTo(second);
        }
        else if(model1 instanceof TemplateScalarModel && model2 instanceof TemplateScalarModel) {
            String first = ((TemplateScalarModel) model1).getAsString();
            String second = ((TemplateScalarModel) model2).getAsString();
            comp = env.getCollator().compare(first, second);
        }
        else if(model1 instanceof TemplateBooleanModel && model2 instanceof TemplateBooleanModel) {
            boolean first = ((TemplateBooleanModel)model1).getAsBoolean();
            boolean second = ((TemplateBooleanModel)model2).getAsBoolean();
            comp = (first ? 1 : 0) - (second ? 1 : 0);
        }

        return (comp == 0);
    }
    
}