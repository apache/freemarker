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

package org.apache.freemarker.core.model;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.model.impl.SimpleNumber;

/**
 * "number" template language data type; an object that stores a number. There's only one numerical type as far as the
 * template language is concerned, but it can store its value using whatever Java number type. Making operations between
 * numbers (and so the coercion rules) is up to the {@link ArithmeticEngine}.
 * 
 * <p>
 * Objects of this type should be immutable, that is, calling {@link #getAsNumber()} should always return the same value
 * as for the first time.
 */
public interface TemplateNumberModel extends TemplateModel {

    TemplateNumberModel ZERO = new SimpleNumber(0);
    TemplateNumberModel ONE = new SimpleNumber(1);
    TemplateNumberModel MINUS_ONE = new SimpleNumber(-1);

    /**
     * Returns the numeric value. The return value must not be {@code null}.
     *
     * @return the {@link Number} instance associated with this number model.
     */
    Number getAsNumber() throws TemplateException;
    
}
