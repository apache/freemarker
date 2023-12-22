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
package freemarker.core;

/**
 * Used when creating {@link TemplateDateFormat}-s and {@link TemplateNumberFormat}-s to indicate that the format
 * string (like the value of the {@code dateFormat} setting) is malformed.
 * 
 * @since 2.3.24
 */
public abstract class InvalidFormatStringException extends TemplateValueFormatException {
    
    public InvalidFormatStringException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFormatStringException(String message) {
        this(message, null);
    }

}
