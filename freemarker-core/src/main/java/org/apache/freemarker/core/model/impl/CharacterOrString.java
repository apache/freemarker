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

package org.apache.freemarker.core.model.impl;

import org.apache.freemarker.core.model.TemplateStringModel;

/**
 * Represents value unwrapped both to {@link Character} and {@link String}. This is needed for unwrapped overloaded
 * method parameters where both {@link Character} and {@link String} occurs on the same parameter position when the
 * {@link TemplateStringModel} to unwrapp contains a {@link String} of length 1.
 */
final class CharacterOrString {

    private final String stringValue;

    CharacterOrString(String stringValue) {
        this.stringValue = stringValue;
    }
    
    String getAsString() {
        return stringValue;
    }

    char getAsChar() {
        return stringValue.charAt(0);
    }
    
}
