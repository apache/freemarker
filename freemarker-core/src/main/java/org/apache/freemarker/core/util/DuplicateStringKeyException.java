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

package org.apache.freemarker.core.util;

/**
 * A string key is duplicated in something where keys must be unique. For example thrown by {@link StringToIndexMap}
 * creator methods.
 */
public class DuplicateStringKeyException extends IllegalArgumentException {

    private final String key;

    public DuplicateStringKeyException(String key) {
        super("Duplicate key: " + _StringUtils.jQuote(key));
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
