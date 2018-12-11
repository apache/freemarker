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

import org.apache.freemarker.core.util._StringUtils;

/**
 * Thrown by {@link ProcessingConfiguration#getCustomSetting(Serializable)} if the custom setting is not set.
 */
public class CustomSettingValueNotSetException extends SettingValueNotSetException {

    private final Serializable key;

    /**
     * Same as {@link #CustomSettingValueNotSetException(Serializable, Throwable)} with {@code null} cause.
     */
    public CustomSettingValueNotSetException(Serializable key) {
        this(key, null);
    }

    /**
     * @param key {@link ProcessingConfiguration#getCustomSetting(Serializable)}
     */
    public CustomSettingValueNotSetException(Serializable key, Throwable cause) {
        super("The " + _StringUtils.jQuote(key)
                + (key instanceof String ? "" : " (key class " + key.getClass().getName() + ")")
                + " setting is not set in this layer and has no default here either.",
                cause);
        this.key = key;
    }

    /**
     * The argument to {@link ProcessingConfiguration#getCustomSetting(Serializable)}.
     */
    public Serializable getKey() {
        return key;
    }
}
