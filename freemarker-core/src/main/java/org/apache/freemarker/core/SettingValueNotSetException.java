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

import org.apache.freemarker.core.util._StringUtil;

/**
 * Thrown when you try to read a configuration setting which wasn't set and isn't inherited from a parent object and has
 * no default either. Because {@link Configuration} specifies a default value for all settings, objects that has a
 * {@link Configuration} in their inheritance chain (like {@link Environment}, {@link Template}) won't throw this.
 */
public class SettingValueNotSetException extends IllegalStateException {

    private final String settingName;

    public SettingValueNotSetException(String settingName) {
        this(settingName, true);
    }

    public SettingValueNotSetException(String settingName, boolean quoteSettingName) {
        super("The " + (quoteSettingName ? _StringUtil.jQuote(settingName) : settingName)
                + " setting is not set in this layer and has no default here either.");
        this.settingName = settingName;
    }

}
