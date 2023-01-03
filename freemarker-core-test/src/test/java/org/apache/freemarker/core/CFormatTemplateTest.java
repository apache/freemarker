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
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class CFormatTemplateTest extends TemplateTest {

    @Override
    protected void setupConfigurationBuilder(Configuration.ExtendableBuilder<?> cb) {
        cb.setCFormat(CustomCFormat.INSTANCE);
    }

    @Before
    public void addDataModelVariables() {
        addToDataModel("s", "a'b\"c\u0001");
    }

    @Test
    public void testBooleanAndNullFormat() throws TemplateException, IOException {
        assertOutput(
                ""
                        + "${true?c} ${false?c} ${null?cn} ${noSuchVar?cn} "
                        + "JSON: <#setting cFormat='JSON'>${true?c} ${false?c} ${null?cn} ${noSuchVar?cn}",
                ""
                        + "TRUE FALSE NULL NULL "
                        + "JSON: true false null null");
        assertOutput(
                "<#setting booleanFormat='c'>"
                        + "${true} ${false} "
                        + "JSON: <#setting cFormat='JSON'>${true} ${false}",
                ""
                        + "TRUE FALSE "
                        + "JSON: true false");
    }

    @Test
    public void testStringFormat() throws TemplateException, IOException {
        assertOutput(
                ""
                        + "Default: ${s?c} "
                        + "XS: <#setting cFormat='XS'>${s?c} "
                        + "JavaScript: <#setting cFormat='JavaScript'>${s?c} "
                        + "JSON: <#setting cFormat='JSON'>${s?c} "
                        + "Java: <#setting cFormat='Java'>${s?c} ",
                ""
                        + "Default: \"a'b\\\"c\\u0001\" "
                        + "XS: a'b\"c\u0001 "
                        + "JavaScript: \"a'b\\\"c\\x01\" "
                        + "JSON: \"a'b\\\"c\\u0001\" "
                        + "Java: \"a'b\\\"c\\u0001\" ");
    }

    @Test
    public void testUnsafeSetting() throws TemplateException, IOException {
        assertErrorContains("<#setting cFormat='com.example.ExploitCFormat()'>", "not allowed");
    }

}
