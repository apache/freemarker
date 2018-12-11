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
 * An unexpected state was reached that is certainly caused by a bug in FreeMarker.
 */
public class BugException extends RuntimeException {

    private static final String COMMON_MESSAGE
        = "A bug was detected in FreeMarker; please report it with stack-trace";

    public BugException() {
        this((Throwable) null);
    }

    public BugException(String message) {
        this(message, null);
    }

    public BugException(Throwable cause) {
        super(COMMON_MESSAGE, cause);
    }

    public BugException(String message, Throwable cause) {
        super(COMMON_MESSAGE + ": " + message, cause);
    }
    
    public BugException(int value) {
        this(String.valueOf(value));
    }

}
