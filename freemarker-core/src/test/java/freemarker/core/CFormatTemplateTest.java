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

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class CFormatTemplateTest extends TemplateTest {

    @Before
    public void addDataModelVariables() {
        addToDataModel("s", "a'b\"c\u0001");
    }

    @Test
    public void testBooleanAndNullFormat() throws TemplateException, IOException {
        getConfiguration().setCFormat(CustomCFormat.INSTANCE);
        assertOutput(
                ""
                        + "${true?c} ${false?c} ${null?cn} "
                        + "JSON: <#setting c_format='JSON'>${true?c} ${false?c} ${null?cn}",
                ""
                        + "TRUE FALSE NULL "
                        + "JSON: true false null");
        assertOutput(
                "<#setting boolean_format='c'>"
                        + "${true} ${false} "
                        + "JSON: <#setting c_format='JSON'>${true} ${false}",
                ""
                        + "TRUE FALSE "
                        + "JSON: true false");
    }

    @Test
    public void testStringFormat() throws TemplateException, IOException {
        assertOutput(
                ""
                        + "Default: ${s?c} "
                        + "XS: <#setting c_format='XS'>${s?c} "
                        + "JavaScript: <#setting c_format='JavaScript'>${s?c} "
                        + "JSON: <#setting c_format='JSON'>${s?c} "
                        + "Java: <#setting c_format='Java'>${s?c} ",
                ""
                        + "Default: \"a'b\\\"c\\u0001\" "
                        + "XS: a'b\"c\u0001 "
                        + "JavaScript: \"a'b\\\"c\\x01\" "
                        + "JSON: \"a'b\\\"c\\u0001\" "
                        + "Java: \"a'b\\\"c\\u0001\" ");
    }

    @Test
    public void testUnsafeSetting() throws TemplateException, IOException {
        assertErrorContains("<#setting c_format='com.example.ExploitCFormat()'>", "not allowed");
        assertErrorContains("<#setting cFormat='com.example.ExploitCFormat()'>", "not allowed");
    }

}
