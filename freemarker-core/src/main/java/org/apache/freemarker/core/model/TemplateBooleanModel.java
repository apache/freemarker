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

package org.apache.freemarker.core.model;

import java.io.Serializable;

import org.apache.freemarker.core.TemplateException;

/**
 * "boolean" template language data type; same as in Java; either {@code true} or {@code false}.
 * 
 * <p>
 * Objects of this type should be immutable, that is, calling {@link #getAsBoolean()} should always return the same
 * value as for the first time.
 */
public interface TemplateBooleanModel extends TemplateModel, Serializable {

    /**
     * @return whether to interpret this object as true or false in a boolean context
     */
    boolean getAsBoolean() throws TemplateException;
    
    /**
     * A singleton object to represent boolean false
     */
    TemplateBooleanModel FALSE = new FalseTemplateBooleanModel();

    /**
     * A singleton object to represent boolean true
     */
    TemplateBooleanModel TRUE = new TrueTemplateBooleanModel();
    
}
