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

import org.apache.freemarker.core.cformat.impl.CustomCFormat;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

import java.io.IOException;

public class BooleanFormatEnvironmentCachingTest extends TemplateTest {

    @Override
    protected void setupConfigurationBuilder(Configuration.ExtendableBuilder<?> cb) {
        cb.setCFormat(CustomCFormat.INSTANCE);
        cb.setBooleanFormat("c");
    }

    @Test
    public void test() throws TemplateException, IOException {
        assertOutput(
                ""
                        + "${true} ${true} ${false} ${false} "
                        + "<#setting cFormat='JSON'>${true} ${true} ${false} ${false} "
                        + "<#setting booleanFormat='y,n'>${true} ${true} ${false} ${false} "
                        + "<#setting cFormat='Java'>${true} ${true} ${false} ${false} "
                        + "<#setting booleanFormat='c'>${true} ${true} ${false} ${false}",
                ""
                        + "TRUE TRUE FALSE FALSE "
                        + "true true false false "
                        + "y y n n "
                        + "y y n n "
                        + "true true false false");
    }
}