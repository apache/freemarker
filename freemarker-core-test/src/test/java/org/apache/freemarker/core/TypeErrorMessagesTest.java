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

import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class TypeErrorMessagesTest extends TemplateTest {

    @Test
    public void testNumericalBinaryOperator() {
        assertErrorContains("${n - s}", "\"-\"", "right-hand", "number", "string");
        assertErrorContains("${s - n}", "\"-\"", "left-hand", "number", "string");
    }

    @Test
    public void testGetterMistake() {
        assertErrorContains("${bean.getX}", "${...}",
                "number", "string", "method", "obj.getSomething", "obj.something");
        assertErrorContains("${1 * bean.getX}", "right-hand",
                "number", "\\!string", "method", "obj.getSomething", "obj.something");
        assertErrorContains("<#if bean.isB></#if>", "condition",
                "boolean", "method", "obj.isSomething", "obj.something");
        assertErrorContains("<#if bean.isB></#if>", "condition",
                "boolean", "method", "obj.isSomething", "obj.something");
        assertErrorContains("${bean.voidM}",
                "string", "method", "\\!()");
        assertErrorContains("${bean.intM}",
                "string", "method", "obj.something()");
        assertErrorContains("${bean.intMP}",
                "string", "method", "obj.something(params)");
    }

    @Override
    protected Object createDataModel() {
        return createCommonTestValuesDataModel();
    }

}
