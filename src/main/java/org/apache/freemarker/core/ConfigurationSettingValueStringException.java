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

import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._StringUtil;

/**
 * Thrown by {@link Configuration#setSetting(String, String)}; The setting name was recognized, but its value
 * couldn't be parsed or the setting couldn't be set for some other reason. This exception should always have a
 * cause exception.
 */
@SuppressWarnings("serial")
public class ConfigurationSettingValueStringException extends ConfigurationException {

    ConfigurationSettingValueStringException(String name, String value, Throwable cause) {
        super("Failed to set FreeMarker configuration setting " + _StringUtil.jQuote(name)
                + " to value " + _StringUtil.jQuote(value) + "; see cause exception.", cause);
        _NullArgumentException.check("cause", cause);
    }

}
