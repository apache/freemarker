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

/**
 * Exception thrown when a {@link Dialect} detects a problem during parsing (such a required parameter to a directive is
 * missing). The exception will be converted to a {@link ParseException} by the parser, where the error message and
 * cause exception will be the same as of the {@link StaticLinkingCheckException}. 
 */
//TODO [FM3][DIALECTS] will be public
@SuppressWarnings("serial")
final class StaticLinkingCheckException extends Exception {
    
    private final _ErrorDescriptionBuilder messageBuilder;
    private String builtMessage; 
    private final String simpleMessage; 

    public StaticLinkingCheckException(_ErrorDescriptionBuilder messageBuilder) {
        this(messageBuilder, null);
    }
    
    public StaticLinkingCheckException(_ErrorDescriptionBuilder messageBuilder, Throwable cause) {
        _NullArgumentException.check("messageBuilder", messageBuilder);
        this.messageBuilder = messageBuilder;
        simpleMessage = null;
    }

    public StaticLinkingCheckException(String message) {
        this(message, null);
    }
    
    public StaticLinkingCheckException(String message, Throwable cause) {
        _NullArgumentException.check("message", message);
        this.messageBuilder = null;
        simpleMessage = message;
    }

    @Override
    public String getMessage() {
        if (simpleMessage != null) {
            return simpleMessage;
        }
        
        String builtDescription = this.builtMessage;
        if (builtDescription != null) {
            return builtDescription;
        }
        synchronized (this) {
            builtDescription = messageBuilder.toString();
            this.builtMessage = builtDescription;
        }
        return builtDescription;
    }
    
}
