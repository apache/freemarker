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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;

final class HashLiteral extends Expression {

    private final ArrayList keys, values;
    private final int size;

    HashLiteral(ArrayList/*<Expression>*/ keys, ArrayList/*<Expression>*/ values) {
        this.keys = keys;
        this.values = values;
        this.size = keys.size();
        keys.trimToSize();
        values.trimToSize();
    }

    TemplateModel _eval(Environment env) throws TemplateException {
        return new SequenceHash(env);
    }

    public String getCanonicalForm() {
        StringBuffer buf = new StringBuffer("{");
        for (int i = 0; i < size; i++) {
            Expression key = (Expression) keys.get(i);
            Expression value = (Expression) values.get(i);
            buf.append(key.getCanonicalForm());
            buf.append(" : ");
            buf.append(value.getCanonicalForm());
            if (i != size-1) {
                buf.append(",");
            }
        }
        buf.append("}");
        return buf.toString();
    }
    
    String getNodeTypeSymbol() {
        return "{...}";
    }

    boolean isLiteral() {
        if (constantValue != null) {
            return true;
        }
        for (int i = 0; i < size; i++) {
            Expression key = (Expression) keys.get(i);
            Expression value = (Expression) values.get(i);
            if (!key.isLiteral() || !value.isLiteral()) {
                return false;
            }
        }
        return true;
    }


    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
		ArrayList clonedKeys = (ArrayList)keys.clone();
		for (ListIterator iter = clonedKeys.listIterator(); iter.hasNext();) {
            iter.set(((Expression)iter.next()).deepCloneWithIdentifierReplaced(
                    replacedIdentifier, replacement, replacementState));
        }
		ArrayList clonedValues = (ArrayList)values.clone();
		for (ListIterator iter = clonedValues.listIterator(); iter.hasNext();) {
            iter.set(((Expression)iter.next()).deepCloneWithIdentifierReplaced(
                    replacedIdentifier, replacement, replacementState));
        }
    	return new HashLiteral(clonedKeys, clonedValues);
    }

    private class SequenceHash implements TemplateHashModelEx {

        private HashMap keyMap; // maps keys to integer offset
        private TemplateCollectionModel keyCollection, valueCollection; // ordered lists of keys and values

        SequenceHash(Environment env) throws TemplateException {
            keyMap = new HashMap();
            ArrayList keyList = new ArrayList(size);
            ArrayList valueList = new ArrayList(size);
            for (int i = 0; i< size; i++) {
                Expression keyExp = (Expression) keys.get(i);
                Expression valExp = (Expression) values.get(i);
                String key = keyExp.evalAndCoerceToString(env);
                TemplateModel value = valExp.eval(env);
                if (env == null || !env.isClassicCompatible()) {
                    valExp.assertNonNull(value, env);
                }
                keyMap.put(key, value);
                keyList.add(key);
                valueList.add(value);
            }
            keyCollection = new CollectionAndSequence(new SimpleSequence(keyList));
            valueCollection = new CollectionAndSequence(new SimpleSequence(valueList));
        }

        public int size() {
            return size;
        }

        public TemplateCollectionModel keys() {
            return keyCollection;
        }

        public TemplateCollectionModel values() {
            return valueCollection;
        }

        public TemplateModel get(String key) {
            return (TemplateModel) keyMap.get(key);
        }

        public boolean isEmpty() {
            return size == 0;
        }
    }

    int getParameterCount() {
        return size * 2;
    }

    Object getParameterValue(int idx) {
        checkIndex(idx);
        return idx % 2 == 0 ? keys.get(idx / 2) : values.get(idx / 2);
    }

    ParameterRole getParameterRole(int idx) {
        checkIndex(idx);
        return idx % 2 == 0 ? ParameterRole.ITEM_KEY : ParameterRole.ITEM_VALUE;
    }

    private void checkIndex(int idx) {
        if (idx >= size * 2) {
            throw new IndexOutOfBoundsException();
        }
    }
    
}
