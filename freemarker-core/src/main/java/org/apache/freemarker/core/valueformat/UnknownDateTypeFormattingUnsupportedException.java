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

package org.apache.freemarker.core.valueformat;

import org.apache.freemarker.core.model.TemplateDateModel;

/**
 * Thrown when a {@link TemplateDateModel} can't be formatted because its type is {@link TemplateDateModel#UNKNOWN}.
 * 
 * @since 2.3.24
 */
public final class UnknownDateTypeFormattingUnsupportedException extends UnformattableValueException {

    public UnknownDateTypeFormattingUnsupportedException() {
        super("Can't format the date-like value because it isn't "
                + "known if it's desired result should be a date (no time part), a time, or a date-time value.");
    }
    
}
