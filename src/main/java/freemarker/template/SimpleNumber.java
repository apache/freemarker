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

package freemarker.template;

import java.io.Serializable;


/**
 * A simple implementation of the <tt>TemplateNumberModel</tt>
 * interface. Note that this class is immutable.
 *
 * <p>This class is thread-safe.
 */
public final class SimpleNumber implements TemplateNumberModel, Serializable {

    /**
     * @serial the value of this <tt>SimpleNumber</tt> 
     */
    private final Number value;

    public SimpleNumber(Number value) {
        this.value = value;
    }

    public SimpleNumber(byte val) {
        this.value = Byte.valueOf(val);
    }

    public SimpleNumber(short val) {
        this.value = Short.valueOf(val);
    }

    public SimpleNumber(int val) {
        this.value = Integer.valueOf(val);
    }

    public SimpleNumber(long val) {
        this.value = Long.valueOf(val);
    }

    public SimpleNumber(float val) {
        this.value = Float.valueOf(val);
    }
    
    public SimpleNumber(double val) {
        this.value = Double.valueOf(val);
    }

    @Override
    public Number getAsNumber() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
