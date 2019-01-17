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
 * Indicates that an argument that must be non-{@code null} was {@code null}. 
 */
@SuppressWarnings("serial")
public class _NullArgumentException extends IllegalArgumentException {

    public _NullArgumentException() {
        super("The argument can't be null");
    }
    
    public _NullArgumentException(String argumentName) {
        super("The \"" + argumentName + "\" argument can't be null");
    }

    public _NullArgumentException(String argumentName, String details) {
        super("The \"" + argumentName + "\" argument can't be null. " + details);
    }
    
    /**
     * Convenience method to protect against a {@code null} argument.
     */
    public static <T> T check(String argumentName, T argumentValue) {
        if (argumentValue == null) {
            throw new _NullArgumentException(argumentName);
        }
        return argumentValue;
    }

    /**
     * Convenience method to protect against a {@code null} argument.
     */
    public static <T> T check(T argumentValue) {
        if (argumentValue == null) {
            throw new _NullArgumentException();
        }
        return argumentValue;
    }
    
}
