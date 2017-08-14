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

import org.apache.freemarker.core.model.impl.JavaMethodModel;

/**
 * For some values it makes sense to carry information about where the value is originating from, and this information
 * can be used in error messages and other kind of debugging information.
 */
public interface TemplateModelWithOriginName extends TemplateModel {

    /**
     * Returns some kind of symbolical name that identifies where the value is originating from. For example, for a
     * {@link JavaMethodModel} it will be like {@code "com.example.somepackage.SomeClass.someMethod"}. This is not
     * intended for machine interpretation, and in that sense it's just an informal name. It should be take into
     * consideration that the this value will be possibly shown in error messages quoted in Java-style (so special
     * character will be escaped).
     */
    String getOriginName();

}
