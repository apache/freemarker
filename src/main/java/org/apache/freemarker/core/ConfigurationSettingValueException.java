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

import java.util.Date;

import org.apache.freemarker.core.Configuration.ExtendableBuilder;
import org.apache.freemarker.core.util._StringUtil;

/**
 * Thrown by {@link ExtendableBuilder#setSetting(String, String)}; The setting name was recognized, but its value
 * couldn't be parsed or the setting couldn't be set for some other reason. This exception should always have a
 * cause exception.
 */
@SuppressWarnings("serial")
public class ConfigurationSettingValueException extends ConfigurationException {

    public ConfigurationSettingValueException(String name, String value, Throwable cause) {
        this(name, value, true, null, cause);
    }

    public ConfigurationSettingValueException(String name, String value, String reason) {
        this(name, value, true, reason, null);
    }

    /**
     * @param name
     *         The name of the setting
     * @param value
     *         The value of the setting
     * @param showValue
     *         Whether the value of the setting should be shown in the error message. Set to {@code false} if you want
     *         to avoid {@link #toString()}-ing the {@code value}.
     * @param reason
     *         The explanation of why setting the setting has failed; maybe {@code null}, especially if you have a cause
     *         exception anyway.
     * @param cause
     *         The cause exception of this exception (why setting the setting was failed)
     */
    public ConfigurationSettingValueException(String name, Object value, boolean showValue, String reason,
            Throwable cause) {
        super(
                createMessage(
                    name, value, true,
                    reason != null ? ", because: " : (cause != null ? "; see cause exception." : null),
                    reason),
                cause);
    }

    private static String createMessage(String name, Object value, boolean showValue, String detail1, String detail2) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("Failed to set FreeMarker configuration setting ").append(_StringUtil.jQuote(name));
        if (showValue) {
            sb.append(" to value ")
                    .append(
                            value instanceof Number || value instanceof Boolean || value instanceof Date ? value
                            : _StringUtil.jQuote(value));
        } else {
            sb.append(" to the specified value");
        }
        if (detail1 != null) {
            sb.append(detail1);
        }
        if (detail2 != null) {
            sb.append(detail2);
        }
        return sb.toString();
    }

}
