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

import freemarker.template.Configuration;

/**
 * Superclass of all format factories. A format factory is an object that creates instances of a certain kind of
 * {@link TemplateValueFormat}. Generally, they are singletons (one per JVM, or one per {@link Configuration}). They
 * should be thread safe. They may encapsulate a cache and return cached {@link TemplateValueFormat} instances.
 * 
 * @since 2.3.24
 */
public abstract class TemplateValueFormatFactory {

}
