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
package org.apache.freemarker.core.templateresolver;

import java.io.Serializable;

/**
 * Used for the value of {@link TemplateLoadingResult#getStatus()}.
 */
public enum TemplateLoadingResultStatus {

    /**
     * The template with the requested name doesn't exist (not to be confused with "wasn't accessible due to error"). If
     * there was and error because of which we can't know for sure if the template is there or not (for example we
     * couldn't access the backing store due to a network connection error or other unexpected I/O error or
     * authorization problem), this value must not be used, instead an exception should be thrown by
     * {@link TemplateLoader#load(String, TemplateLoadingSource, Serializable, TemplateLoaderSession)}.
     */
    NOT_FOUND,

    /**
     * If the template was found, but its source and version is the same as that which was provided to
     * {@link TemplateLoader#load(String, TemplateLoadingSource, Serializable, TemplateLoaderSession)} (from a cache
     * presumably), so its content wasn't opened for reading.
     */
    NOT_MODIFIED,

    /**
     * If the template was found and its content is ready for reading.
     */
    OPENED
    
}