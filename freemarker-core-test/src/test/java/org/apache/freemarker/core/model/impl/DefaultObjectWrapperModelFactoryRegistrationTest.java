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

package org.apache.freemarker.core.model.impl;

import static org.junit.Assert.*;

import java.lang.ref.Reference;

import org.apache.freemarker.core.Configuration;
import org.junit.Test;

public class DefaultObjectWrapperModelFactoryRegistrationTest {

    @Test
    public void introspectionSettingChanges() {
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).usePrivateCaches(true)
                .build();
        ClassIntrospector ci1 = ow.getClassIntrospector();
        checkRegisteredModelFactories(ci1, ow.getStaticModels(), ow.getEnumModels());
    }

    private void checkRegisteredModelFactories(ClassIntrospector ci, Object... expected) {
        Object[] actualRefs = ci.getRegisteredModelFactoriesSnapshot();

        scanActuals: for (Object actaulRef : actualRefs) {
            Object actualItem = ((Reference) actaulRef).get();
            for (Object expectedItem : expected) {
                if (actualItem == expectedItem) {
                    continue scanActuals;
                }
            }
            fail("Actual item " + actualItem + " is not among the expected items");
        }

        scanExpecteds: for (Object expectedItem : expected) {
            for (Object ref : actualRefs) {
                Object actualItem = ((Reference) ref).get();
                if (actualItem == expectedItem) {
                    continue scanExpecteds;
                }
            }
            fail("Expected item " + expectedItem + " is not among the actual items");
        }
    }
    
}
