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

import org.apache.freemarker.core.util._StringUtils;

/**
 * Thrown when you try to read a core (that is, non-custom) configuration setting which wasn't set and isn't inherited
 * from a parent object and has no default either. Because {@link Configuration} specifies a default value for all
 * core settings, objects that has a {@link Configuration} in their inheritance chain (like {@link Environment},
 * {@link Template}) won't throw this.
 */
public class CoreSettingValueNotSetException extends SettingValueNotSetException {

    private final String settingName;

    /**
     * Same as {@link #CoreSettingValueNotSetException(String, Throwable)} with {@code null} cause.
     */
    public CoreSettingValueNotSetException(String settingName) {
        this(settingName, null);
    }

    public CoreSettingValueNotSetException(String settingName, Throwable cause) {
        super("The " + _StringUtils.jQuote(settingName) + " setting is not set in this layer and has no default here "
                + "either.",  cause);
        this.settingName = settingName;
    }

}
