/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class CollectionUtilsTest {

    @Test
    public void unmodifiableMap() {
        Map<Object, Object> modifiableMap = new HashMap<>();
        assertNotSame(modifiableMap, _CollectionUtils.unmodifiableMap(modifiableMap));

        Map<Object, Object> wrappedModifiableMap = Collections.unmodifiableMap(modifiableMap);
        assertSame(wrappedModifiableMap, _CollectionUtils.unmodifiableMap(wrappedModifiableMap));

        assertSame(Collections.emptyMap(), _CollectionUtils.unmodifiableMap(Collections.emptyMap()));
    }

}
