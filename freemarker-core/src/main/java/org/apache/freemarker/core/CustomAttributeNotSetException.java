/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import java.io.Serializable;

import org.apache.freemarker.core.util._StringUtil;

/**
 * Thrown by {@link ProcessingConfiguration#getCustomAttribute(Serializable)} if the custom attribute is not set.
 */
public class CustomAttributeNotSetException extends SettingValueNotSetException {

    private final Serializable key;

    /**
     * @param key {@link ProcessingConfiguration#getCustomAttribute(Serializable)}
     */
    public CustomAttributeNotSetException(Serializable key) {
        super("customAttributes[" + key instanceof String ? _StringUtil.jQuote(key) : _StringUtil.tryToString(key) +
                        "]", false);
        this.key = key;
    }

    /**
     * The argument to {@link ProcessingConfiguration#getCustomAttribute(Serializable)}.
     */
    public Serializable getKey() {
        return key;
    }
}
