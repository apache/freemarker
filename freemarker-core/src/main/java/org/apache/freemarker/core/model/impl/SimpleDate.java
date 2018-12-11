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

import org.apache.freemarker.core.model.TemplateDateModel;

/**
 * A simple implementation of the <tt>TemplateDateModel</tt>
 * interface. Note that this class is immutable.
 * <p>This class is thread-safe.
 */
public class SimpleDate implements TemplateDateModel, Serializable {
    private final java.util.Date date;
    private final int type;
    
    /**
     * Creates a new date model wrapping the specified date object and
     * having DATE type.
     */
    public SimpleDate(java.sql.Date date) {
        this(date, DATE);
    }
    
    /**
     * Creates a new date model wrapping the specified time object and
     * having TIME type.
     */
    public SimpleDate(java.sql.Time time) {
        this(time, TIME);
    }
    
    /**
     * Creates a new date model wrapping the specified time object and
     * having DATE_TIME type.
     */
    public SimpleDate(java.sql.Timestamp dateTime) {
        this(dateTime, DATE_TIME);
    }
    
    /**
     * Creates a new date model wrapping the specified date object and
     * having the specified type.
     */
    public SimpleDate(java.util.Date date, int type) {
        if (date == null) {
            throw new IllegalArgumentException("date == null");
        }
        this.date = date;
        this.type = type;
    }
    
    @Override
    public java.util.Date getAsDate() {
        return date;
    }

    @Override
    public int getDateType() {
        return type;
    }
    
    @Override
    public String toString() {
        return date.toString();
    }
}
