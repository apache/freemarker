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

package org.apache.freemarker.core.model.impl;

import java.io.Serializable;

import org.apache.freemarker.core.model.TemplateStringModel;

/**
 * A simple implementation of the <tt>TemplateStringModel</tt>
 * interface, using a <tt>String</tt>.
 *
 * <p>This class is thread-safe.
 *
 * @see SimpleSequence
 * @see SimpleHash
 */
public final class SimpleString
implements TemplateStringModel, Serializable {
    
    /**
     * @serial the value of this <tt>SimpleString</tt> if it wraps a
     * <tt>String</tt>.
     */
    private final String value;

    /**
     * Constructs a <tt>SimpleString</tt> containing a string value.
     * @param value the string value. If this is {@code null}, its value in FTL will be {@code ""}.
     */
    public SimpleString(String value) {
        this.value = value;
    }

    @Override
    public String getAsString() {
        return (value == null) ? "" : value;
    }

    @Override
    public String toString() {
        // [2.4] Shouldn't return null
        return value;
    }
    
    /**
     * Same as calling the constructor, except that for a {@code null} parameter it returns null. 
     */
    public static SimpleString newInstanceOrNull(String s) {
        return s != null ? new SimpleString(s) : null;
    }
    
}
