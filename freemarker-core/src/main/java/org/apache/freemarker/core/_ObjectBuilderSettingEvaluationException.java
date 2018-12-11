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
 * Don't use this; used internally by FreeMarker, might changes without notice.
 * Thrown by {@link _ObjectBuilderSettingEvaluator}.
 */
public class _ObjectBuilderSettingEvaluationException extends Exception {
    
    public _ObjectBuilderSettingEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public _ObjectBuilderSettingEvaluationException(String message) {
        super(message);
    }

    public _ObjectBuilderSettingEvaluationException(String expected, String src, int location) {
        super("Expression syntax error: Expected a(n) " + expected + ", but "
                + (location < src.length()
                        ? "found character " + _StringUtils.jQuote("" + src.charAt(location)) + " at position "
                            + (location + 1) + "."
                        : "the end of the parsed string was reached.") );
    }
    
}
