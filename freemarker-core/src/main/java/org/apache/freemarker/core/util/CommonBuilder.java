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

import org.apache.freemarker.core.ConfigurationException;

/**
 * Common interface for builders (used for implementing the builder design pattern). This is technically very similar to
 * {@link CommonSupplier}, but the intent is more specific. A is builder stateful object used only for creating a
 * single instance. Generally, it isn't expected to be thread safe.
 */public interface CommonBuilder<ProductT> {

    /**
     * Creates an instance of the product class. This is usually a new instance, though if the product is stateless,
     * it's possibly a shared object instead of a new one. Builders shouldn't allow calling this method for multiple
     * times (not counting calls that threw exceptions), and should throw {@link IllegalStateException} to prevent that.
     *
     * @throws IllegalStateException If this method has already returned successfully once.
     */
    ProductT build() throws ConfigurationException;

}
