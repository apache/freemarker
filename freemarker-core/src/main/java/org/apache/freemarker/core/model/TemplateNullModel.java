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

/**
 * Where an API declares {@link TemplateModel} (or a subclass of it), a {@link TemplateNullModel#INSTANCE} represents
 * the Java {@code null} value, and {@code null} represents that the value (or rather what holds it) doesn't exist
 * at all.
 *
 * <p>Some examples to make this more understandable:</p>
 * <ul>
 *   <li>If you try to read the variable {@code myVar}, but such a variable was never declared anywhere, then that's
 *       a {@code null}. If the variable itself exists, but was set to {@code null}, then that's a
 *       {@link TemplateNullModel#INSTANCE}.
 *   <li>An {@link ObjectWrapper} can also distinguish the two cases.
 *       Let's say you have Java Bean class {@code MyBean}, and an instance of that in the data-model called {@code
 *       myBean}. You have {@code myBean.foo} in the template, which on the FreeMarker API level is translated to a
 *       {@link TemplateHashModel#get(String)} call with key {@code "foo"}. If the {@code MyBean} class doesn't have
 *       a {@code public T getFoo()} method (or a matching readable bean property declared otherwise), then
 *       {@link TemplateHashModel#get(String)} should return {@code null}, otherwise if {@code T getFoo()} returns
 *       {@code null}, then {@link TemplateHashModel#get(String)} should return {@link TemplateNullModel#INSTANCE}.
 *       Note that an {@link ObjectWrapper} has the freedom to follow a different logic; this is just a likely logic.
 * </ul>
 */
public final class TemplateNullModel implements TemplateModel {
    public static final TemplateNullModel INSTANCE = new TemplateNullModel();
    private TemplateNullModel() { }
}

