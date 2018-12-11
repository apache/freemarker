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
 * Thrown when FreeMarker runs into a {@link Number} subclass that it doesn't yet support.  
 */
public class UnsupportedNumberClassException extends RuntimeException {

    private final Class fClass;
    
    public UnsupportedNumberClassException(Class pClass) {
        super("Unsupported number class: " + pClass.getName());
        fClass = pClass;
    }
    
    public Class getUnsupportedClass() {
        return fClass;
    }
    
}
