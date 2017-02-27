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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public final class _CoreLogs {

    // [FM3] Why "Runtime"? "TemplateProcessing" maybe?
    public static final Logger RUNTIME = LoggerFactory.getLogger("org.apache.freemarker.core.Runtime");
    public static final Logger ATTEMPT = LoggerFactory.getLogger("org.apache.freemarker.core.Runtime.Attempt");
    public static final Logger SECURITY = LoggerFactory.getLogger("org.apache.freemarker.core.Security");
    public static final Logger OBJECT_WRAPPER = LoggerFactory.getLogger("org.apache.freemarker.core.model" +
            ".ObjectWrapper");
    public static final Logger TEMPLATE_RESOLVER = LoggerFactory.getLogger(
            "org.apache.freemarker.core.templateresolver");
    public static final Logger DEBUG_SERVER = LoggerFactory.getLogger("org.apache.freemarker.core.debug.server");
    public static final Logger DEBUG_CLIENT = LoggerFactory.getLogger("org.apache.freemarker.core.debug.client");

    private _CoreLogs() {
        // Not meant to be instantiated
    }
    
}
