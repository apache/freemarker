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

/** Don't use this; used internally by FreeMarker, might change without notice. */
public abstract class _DelayedConversionToString {

    private static final String NOT_SET = new String();

    private Object object;
    private volatile String stringValue = NOT_SET;

    public _DelayedConversionToString(Object object) {
        this.object = object;
    }

    @Override
    public String toString() {
        String stringValue = this.stringValue;
        if (stringValue == NOT_SET) {
            synchronized (this) {
                stringValue = this.stringValue;
                if (stringValue == NOT_SET) {
                    stringValue = doConversion(object);
                    this.stringValue = stringValue; 
                    this.object = null;
                }
            }
        }
        return stringValue;
    }

    protected abstract String doConversion(Object obj);

}
