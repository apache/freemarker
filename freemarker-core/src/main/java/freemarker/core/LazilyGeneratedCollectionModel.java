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

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * Similar to {@link SingleIterationCollectionModel}, but marks the value as something that uses lazy evaluation
 * internally, whose laziness should be transparent for the user (in FM2 at least). Values of this type shouldn't be
 * stored without ensuring that the context needed for the generation of the lazily generated elements will be still
 * available when the elements are read. The primary reason for the existence of this class is
 * {@link LocalLambdaExpression}-s, which don't capture the variables they refer to, so for sequences filter/mapped
 * by them we must ensure that all the elements are consumed before the referred variables go out of scope.
 * <p>
 * An operator or built-in should only ever receive a {@link LazilyGeneratedCollectionModel} if it has explicitly
 * allowed its input expression to return such value via calling {@link Expression#enableLazilyGeneratedResult()}
 * during parsing. With other words, an operator or built-in should only ever return
 * {@link LazilyGeneratedCollectionModel} if it its {@link Expression#enableLazilyGeneratedResult()} method was
 * called during the parsing.
 * <p>
 * Note that by accepting {@link LazilyGeneratedCollectionModel}-s the operator/built-in also undertakes taking
 * {@link #isSequence()} into account.
 */
abstract class LazilyGeneratedCollectionModel extends SingleIterationCollectionModel {

    private final boolean sequence;

    /**
     * @param iterator The iterator to read all the elements of this lazily generated collection.
     * @param sequence see {@link #isSequence()}
     */
    protected LazilyGeneratedCollectionModel(TemplateModelIterator iterator, boolean sequence) {
        super(iterator);
        this.sequence = sequence;
    }

    /**
     * If this collection is a sequence according the template author (and we only use {@link TemplateCollectionModel}
     * internally to implement lazy generation). That means that an operator or built-in that accepts sequences must
     * accept this {@link TemplateCollectionModel} value, instead of giving a type error. This of course only applies
     * to operators/built-ins that accept lazy values on the first place (see
     * {@link Expression#enableLazilyGeneratedResult}). Such operators/built-ins must implement their functionality
     * with {@link TemplateCollectionModel} input as well, in additionally to the normal implementation with
     * {@link TemplateSequenceModel} input. If {@link #isSequence()} returns {@code false}, and the operator/built-in
     * doesn't support {@link TemplateCollectionModel} in general, it must fail with type error.
     */
    final boolean isSequence() {
        return sequence;
    }

    /**
     * Returns a "view" of this {@link LazilyGeneratedCollectionModel} where {@link #isSequence()} returns
     * @code true}.
     */
    final LazilyGeneratedCollectionModel withIsSequenceTrue() {
        return isSequence() ? this : withIsSequenceFromFalseToTrue();
    }

    protected abstract LazilyGeneratedCollectionModel withIsSequenceFromFalseToTrue();

}
