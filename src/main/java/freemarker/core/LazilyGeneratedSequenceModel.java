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

import freemarker.ext.beans.CollectionModel;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * Same as {@link SingleIterationCollectionModel}, but marks the value as something that's in principle a
 * {@link TemplateSequenceModel}, but to allow lazy result generation a {@link CollectionModel} is used internally.
 * This is an optimization that we do where we consider it to be transparent enough for the user. An operator or
 * built-in should only ever receive a {@link LazilyGeneratedSequenceModel} if it has explicitly allowed its
 * input expression to return such value via calling {@link Expression#enableLazilyGeneratedResult()}.
 */
class LazilyGeneratedSequenceModel extends SingleIterationCollectionModel {
    LazilyGeneratedSequenceModel(TemplateModelIterator iterator) {
        super(iterator);
    }
}
